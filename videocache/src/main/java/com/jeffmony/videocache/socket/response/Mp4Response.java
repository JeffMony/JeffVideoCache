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
    private long mStartPosition = -1L;

    public Mp4Response(HttpRequest request, String videoUrl, Map<String, String> headers) {
        super(request, videoUrl, headers);
        mMd5 = ProxyCacheUtils.computeMD5(videoUrl);
        mFile = new File(mCachePath, mMd5 + File.separator + mMd5 + StorageUtils.NON_M3U8_SUFFIX);
        mResponseState = ResponseState.OK;
        String rangeStr = request.getRangeString();
        mStartPosition = getRequestStartPosition(rangeStr);
        LogUtils.i(TAG, "Range header=" + request.getRangeString() + ", start position="+mStartPosition);
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
        long totalSize = VideoProxyCacheManager.getInstance().getTotalSizeByMd5(mMd5);
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
        LogUtils.i(TAG, "Current VideoFile exists : " + mFile.exists() + ", this=" + this);
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(mFile, "r");
            if (randomAccessFile == null) {
                throw new VideoCacheException("Current File is not found");
            }
            long available = randomAccessFile.length();
            int bufferedSize = StorageUtils.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[bufferedSize];
            long offset = 0;
            if (totalSize == available) {
                LogUtils.i(TAG, "Current VideoFile is okay");
                randomAccessFile.seek(offset);
                int readLength;
                while((readLength = randomAccessFile.read(buffer, 0, buffer.length)) != -1) {
                    offset += readLength;
                    outputStream.write(buffer, 0, readLength);
                    randomAccessFile.seek(offset);
                }
            } else {
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
                        randomAccessFile.seek(offset);
                        int readLength;
                        while((readLength = randomAccessFile.read(buffer, 0, buffer.length)) != -1) {
                            offset += readLength;
                            outputStream.write(buffer, 0, readLength);
                            randomAccessFile.seek(offset);
                        }

                        if (offset == totalSize) {
                            LogUtils.i(TAG, "Send video info end, this="+this);
                            break;
                        }

                        if (offset < available) {
                            continue;
                        }

                        long lastAvailable = available;
                        available = randomAccessFile.length();
                        waitTime = WAIT_TIME;
                        while(available - lastAvailable < bufferedSize && shouldSendResponse(socket, mMd5)) {
                            if (available == totalSize) {
                                LogUtils.i(TAG, "Send video info end, this="+this);
                                break;
                            }

                            synchronized (lock) {
                                lock.wait(getDelayTime(waitTime));
                            }
                            available = randomAccessFile.length();
                            if (waitTime < MAX_WAIT_TIME) {
                                waitTime *= 2;
                            }
                        }
                    }
                }
            }
            LogUtils.i(TAG, "Send video info end, this="+this);
        } catch (Exception e) {
            LogUtils.w(TAG, "Send video info failed, exception="+e+", this="+this);
            throw e;
        } finally {
            ProxyCacheUtils.close(randomAccessFile);
        }
    }
}
