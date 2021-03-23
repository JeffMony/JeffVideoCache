package com.jeffmony.videocache.socket.response;

import android.text.TextUtils;

import com.jeffmony.videocache.VideoLockManager;
import com.jeffmony.videocache.VideoProxyCacheManager;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.request.ResponseState;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Map;

/**
 * @author jeffmony
 * MP4视频的local server端
 */
public class Mp4Response extends BaseResponse {
    private static final String TAG = "Mp4Response";

    private File mFile;
    private String mMd5;
    private long mStartPosition;

    public Mp4Response(HttpRequest request, String videoUrl, Map<String, String> headers, long time) {
        super(request, videoUrl, headers, time);
        mMd5 = ProxyCacheUtils.computeMD5(videoUrl);
        mFile = new File(mCachePath, mMd5 + File.separator + mMd5 + StorageUtils.NON_M3U8_SUFFIX);
        mResponseState = ResponseState.OK;
        String rangeStr = request.getRangeString();
        mStartPosition = getRequestStartPosition(rangeStr);
        LogUtils.i(TAG, "Range header=" + request.getRangeString() + ", start position="+mStartPosition +", instance="+this);
        if (mStartPosition != -1) {
            //服务端将range起始位置设置到客户端
            VideoProxyCacheManager.getInstance().setVideoRangeRequest(videoUrl, mStartPosition);
        }
    }

    /**
     * 获取range请求的起始位置
     * bytes=15372019-
     * @param rangeStr
     * @return
     */
    private long getRequestStartPosition(String rangeStr) {
        if (TextUtils.isEmpty(rangeStr)) {
            return -1L;
        }
        if (rangeStr.startsWith("bytes=")) {
            rangeStr = rangeStr.substring("bytes=".length());
            if (rangeStr.contains("-")) {
                return Long.parseLong(rangeStr.split("-")[0]);
            }
        }
        return -1L;
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {
        if (TextUtils.isEmpty(mMd5)) {
            throw new VideoCacheException("Current md5 is illegal");
        }
        Object lock = VideoLockManager.getInstance().getLock(mMd5);
        int waitTime = WAIT_TIME;
        long totalSize = VideoProxyCacheManager.getInstance().getTotalSize(mMd5);
        while (!mFile.exists() && totalSize <= 0) {
            synchronized (lock) {
                lock.wait(waitTime);
            }
        }
        boolean isPositionSegExisted = VideoProxyCacheManager.getInstance().isMp4PositionSegExisted(mVideoUrl, mStartPosition);
        while(!isPositionSegExisted) {
            synchronized (lock) {
                lock.wait(waitTime);
            }
            isPositionSegExisted = VideoProxyCacheManager.getInstance().isMp4PositionSegExisted(mVideoUrl, mStartPosition);
        }
        LogUtils.i(TAG, "Current VideoFile exists : " + mFile.exists() + ", File length=" + mFile.length()+", instance=" + this);
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(mFile, "r");
            if (randomAccessFile == null) {
                throw new VideoCacheException("Current File is not found");
            }
            int bufferedSize = StorageUtils.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[bufferedSize];
            long offset = 0;

            //说明mp4文件下载完成了
            if (VideoProxyCacheManager.getInstance().isMp4Completed(mVideoUrl)) {
                LogUtils.i(TAG, "Current VideoFile is okay, instance="+this);
                randomAccessFile.seek(offset);
                int readLength;
                while((readLength = randomAccessFile.read(buffer, 0, buffer.length)) != -1) {
                    offset += readLength;
                    outputStream.write(buffer, 0, readLength);
                    randomAccessFile.seek(offset);
                }
            } else {
                long available = randomAccessFile.length();
                //mp4文件没有下载完成
                while(shouldSendResponse(socket, mMd5)) {
                    if (available == 0) {
                        synchronized (lock) {
                            waitTime = getDelayTime(waitTime);
                            lock.wait(waitTime);
                        }
                        available = randomAccessFile.length();
                        if (waitTime < MAX_WAIT_TIME) {
                            waitTime *= 2;
                        }
                    } else {
                        //randomAccessFile seek到具体的offset
                        randomAccessFile.seek(offset);
                        int readLength;
                        while ((readLength = randomAccessFile.read(buffer, 0, buffer.length)) != -1) {
                            boolean shouldWriteResponseData = VideoProxyCacheManager.getInstance().shouldWriteResponseData(mVideoUrl, offset + readLength);
                            if (!shouldWriteResponseData && (offset < mStartPosition)) {
                                //说明后续没有数据了,中间出现了断档
                                break;
                            }
                            LogUtils.i(TAG, "READ LENGTH="+readLength+", offset="+offset+", instance="+this);
                            offset += readLength;
                            outputStream.write(buffer, 0, readLength);
                            randomAccessFile.seek(offset);
                        }
                        //接下来要写入到socket中, 一定要判断randomAccessFile中是否有数据可以写
                        if (offset == totalSize && (VideoProxyCacheManager.getInstance().isMp4Completed(mVideoUrl) || VideoProxyCacheManager.getInstance().isMp4CompletedFromPosition(mVideoUrl, offset - bufferedSize))) {
                            LogUtils.i(TAG, "# Video file is cached in local storage. instance=" + this);
                            break;
                        }

                        boolean shouldWriteResponseData = VideoProxyCacheManager.getInstance().shouldWriteResponseData(mVideoUrl, offset + readLength);
                        if (shouldWriteResponseData) {
                            continue;
                        }
                        long lastAvailable = offset;
                        available = VideoProxyCacheManager.getInstance().getMp4CachedPosition(mVideoUrl, lastAvailable);
                        waitTime = WAIT_TIME;
                        while(available - lastAvailable < bufferedSize && shouldSendResponse(socket, mMd5)) {
                            if (VideoProxyCacheManager.getInstance().isMp4Completed(mVideoUrl) || VideoProxyCacheManager.getInstance().isMp4CompletedFromPosition(mVideoUrl, offset)) {
                                LogUtils.i(TAG, "## Video file is cached in local storage. instance=" + this);
                                break;
                            }
                            synchronized (lock) {
                                LogUtils.d(TAG, "LOCK wait, instance="+this);
                                waitTime = getDelayTime(waitTime);
                                lock.wait(waitTime);
                            }
                            available = VideoProxyCacheManager.getInstance().getMp4CachedPosition(mVideoUrl, lastAvailable);
                            LogUtils.i(TAG, "available="+available+", lastAvailable="+lastAvailable+", instance="+this);
                            if (waitTime < MAX_WAIT_TIME) {
                                waitTime *= 2;
                            }
                        }
                    }
                }
            }
            LogUtils.i(TAG, "Send video info end, instance="+this);
        } catch (Exception e) {
            LogUtils.w(TAG, "Send video info failed, exception="+e+", this="+this);
            throw e;
        } finally {
            ProxyCacheUtils.close(randomAccessFile);
        }
    }
}
