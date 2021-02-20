package com.jeffmony.videocache.task;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.model.VideoRange;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;
import com.jeffmony.videocache.utils.VideoRangeUtils;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author jeffmony
 * 非M3U8视频要非常注意了，因为在用户拖动播放进度条的时候回产生多个分片，需要对这些分片进行有效的管理
 *
 *   seg1           seg2                      seg3
 * |-----|      |-------|              |--------------|
 *
 *         seg1                               seg2
 * |--------------------|              |--------------|
 *
 *                          seg1
 * |--------------------------------------------------|
 */
public class NonM3U8CacheTask extends VideoCacheTask {

    private static final String TAG = "NonM3U8CacheTask";

    private LinkedHashMap<Long, Long> mVideoSegMap;    //本地序列化的range结构
    private LinkedHashMap<Long, VideoRange> mVideoRangeMap;    //已经缓存的video range结构
    private VideoRange mRequestRange;    //当前请求的video range

    private String mMd5;
    private String mVideoUrl;

    public NonM3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        super(cacheInfo, headers);
        mTotalSize = cacheInfo.getTotalSize();
        mVideoSegMap = cacheInfo.getVideoSegMap();
        if (mVideoSegMap == null) {
            mVideoSegMap = new LinkedHashMap<>();
        }
        if (mVideoRangeMap == null) {
            mVideoRangeMap = new LinkedHashMap<>();
        }
        mMd5 = cacheInfo.getMd5();
        mVideoUrl = cacheInfo.getVideoUrl();
        initVideoSegInfo();
    }

    private void initVideoSegInfo() {
        if (mVideoSegMap.size() == 0) {
            //当前没有缓存,需要从头下载
            mRequestRange = new VideoRange(0, mTotalSize);
        } else {
            for (Map.Entry<Long, Long> entry : mVideoSegMap.entrySet()) {
                //因为mVideoSegMap是顺序存储的,所有这样的操作是可以的
                long start = entry.getKey();
                long end = entry.getValue();
                mVideoRangeMap.put(start, new VideoRange(start, end));
                /**
                 * mVideoRangeMap中的key是起始位置, value是存储的VideoRange结构
                 */
            }
        }
    }

    @Override
    public void startCacheTask() {
        if (isTaskRunning()) {
            return;
        }
        notifyOnTaskStart();
        seekToCacheTask(0L);   //传入一个起始位置,需要到已缓存的文件中去查找一下
    }


    /**
     *
     *  pos
     *         |--------------|          |-------------------|      |-------------------|
     *
     *                 pos
     *         |--------------|          |-------------------|      |-------------------|
     *
     *                             pos
     *         |--------------|          |-------------------|      |-------------------|
     *
     *                                            pos
     *         |--------------|          |-------------------|      |-------------------|
     *
     *                                                         pos
     *         |--------------|          |-------------------|      |-------------------|
     *
     *                                                                                        pos
     *         |--------------|          |-------------------|      |-------------------|
     * @param position
     * @return
     */
    public VideoRange getRequestRange(long position) {
        if (mVideoRangeMap.size() == 0) {
            return new VideoRange(0, mTotalSize);
        } else {
            long start = -1;
            long end = -1;
            for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                if (position < videoRange.getStart()) {
                    end = videoRange.getStart();
                } else if (position <= videoRange.getEnd()) {
                    start = videoRange.getEnd();
                } else {
                    //说明position 在当前的videoRange之后
                }
            }
            if (start == -1) {
                start = position;
            }
            if (end == -1) {
                end = mTotalSize;
            }
            return new VideoRange(start, end);
        }
    }

    @Override
    public void pauseCacheTask() {
        if (mTaskExecutor != null && !mTaskExecutor.isShutdown()) {
            mTaskExecutor.shutdownNow();
        }
    }

    @Override
    public void resumeCacheTask() {

    }

    @Override
    public void seekToCacheTask(float percent) {

    }

    private void seekToCacheTask(long curLength) {
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(6, 6,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());

        mTaskExecutor.execute(() -> {
            File videoFile;
            try {
                videoFile = new File(mSaveDir, mMd5 + StorageUtils.NON_M3U8_SUFFIX);
                if (!videoFile.exists()) {
                    videoFile.createNewFile();
                }
            } catch (Exception e) {
                notifyOnTaskFailed(new VideoCacheException("Cannot create video file, exception="+e));
                return;
            }

            mRequestRange = getRequestRange(curLength);
            long requestStart = mRequestRange.getStart();
            long requestEnd = mRequestRange.getEnd();
            mHeaders.put("Range", "bytes=" + requestStart + "-" + requestEnd);
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(videoFile.getAbsolutePath(), "rw");
                randomAccessFile.seek(requestStart);

                LogUtils.i(TAG, "Start request");
                connection = HttpUtils.getConnection(mVideoUrl, mHeaders);
                inputStream = connection.getInputStream();
                LogUtils.i(TAG, "Receive response");

                byte[] buffer = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
                int readLength;
                while((readLength = inputStream.read(buffer)) != -1) {
                    if (mCachedSize > requestEnd) {
                        mCachedSize = requestEnd;
                    }
                    if (mCachedSize + readLength > requestEnd) {
                        randomAccessFile.write(buffer, 0, (int)(requestEnd- mCachedSize));
                        mCachedSize += (requestEnd - mCachedSize);
                        mCachedSize = requestEnd;
                    } else {
                        randomAccessFile.write(buffer, 0, readLength);
                        mCachedSize += readLength;
                    }

                    notifyCacheProgress();

                    if (mCachedSize >= requestEnd) {
                        //缓存好了一段,开始缓存下一段
                        requestNextEmptyVideoRange(requestEnd);
                    }
                }
            } catch (Exception e) {
                notifyOnTaskFailed(e);
            } finally {
                ProxyCacheUtils.close(inputStream);
                ProxyCacheUtils.close(randomAccessFile);
                HttpUtils.closeConnection(connection);
            }
        });
    }

    private void notifyCacheProgress() {
        mCacheInfo.setCachedSize(mCachedSize);
        float percent = mCachedSize * 1.0f * 100 / mTotalSize;
        if (!ProxyCacheUtils.isFloatEqual(percent, mPercent)) {
            long nowTime = System.currentTimeMillis();
            if (mCachedSize > mLastCachedSize && nowTime > mLastInvokeTime) {
                mSpeed = (mCachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
            }
            mListener.onTaskProgress(percent, mCachedSize, mSpeed);
            mPercent = percent;
            mCacheInfo.setPercent(percent);
            mCacheInfo.setSpeed(mSpeed);
            mLastInvokeTime = nowTime;
            mLastCachedSize = mCachedSize;
            saveVideoInfo();
        }
    }

    private void requestNextEmptyVideoRange(long position) {
        //这时候已经缓存好了一段分片,可以更新一下video range数据结构了
        updateVideoRangeInfo();
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
        } else {
            if (position == mTotalSize) {
                //说明已经缓存好,但是整视频中间还有一些洞,但是不影响,可以忽略
            } else {
                //开启下一段视频分片的缓存
                seekToCacheTask(position);
            }
        }
    }

    private void updateVideoRangeInfo() {
        if (mVideoRangeMap.size() == 0) {
            mVideoRangeMap.put(mRequestRange.getStart(), mRequestRange);
        } else {
            long finalStart = -1;
            long finalEnd = -1;

            long requestStart = mRequestRange.getStart();
            long requestEnd = mRequestRange.getEnd();
            for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                long startResult = VideoRangeUtils.determineVideoRangeByPosition(videoRange, requestStart);
                long endResult = VideoRangeUtils.determineVideoRangeByPosition(videoRange, requestEnd);

                if (finalStart == -1) {
                    if (startResult == 1) {
                        finalStart = requestStart;
                    } else if (startResult == 2) {
                        finalStart = videoRange.getStart();
                    } else {
                        //先别急着赋值,还要看下一个videoRange
                    }
                }
                if (finalEnd == -1) {
                    if (endResult == 1) {
                        finalEnd = requestEnd;
                    } else if (endResult == 2) {
                        finalEnd = videoRange.getEnd();
                    } else {
                        //先别急着赋值,还要看下一个videoRange
                    }
                }
            }
            if (finalStart == -1) {
                finalStart = requestStart;
            }
            if (finalEnd == -1) {
                finalEnd = requestEnd;
            }

            VideoRange finalVideoRange = new VideoRange(finalStart, finalEnd);
            LogUtils.i(TAG, "finalVideoRange: " + finalVideoRange);

            LinkedHashMap<Long, VideoRange> tempVideoRangeMap = new LinkedHashMap<>();
            for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                if (VideoRangeUtils.containsVideoRange(finalVideoRange, videoRange)) {
                    tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                } else if (VideoRangeUtils.compareVideoRange(finalVideoRange, videoRange) == 1) {
                    tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                    tempVideoRangeMap.put(videoRange.getStart(), videoRange);
                } else if (VideoRangeUtils.compareVideoRange(finalVideoRange, videoRange) == 2) {
                    tempVideoRangeMap.put(videoRange.getStart(), videoRange);
                    tempVideoRangeMap.put(finalVideoRange.getStart(), finalVideoRange);
                }
            }

            mVideoRangeMap.clear();
            mVideoRangeMap.putAll(tempVideoRangeMap);

            LinkedHashMap<Long, Long> tempSegMap = new LinkedHashMap<>();
            for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
                VideoRange videoRange = entry.getValue();
                LogUtils.i(TAG, "Result videoRange : " + videoRange);
                tempSegMap.put(videoRange.getStart(), videoRange.getEnd());
            }
            mVideoSegMap.clear();
            mVideoSegMap.putAll(tempSegMap);
            mCacheInfo.setVideoSegMap(mVideoSegMap);
        }
        if (mVideoRangeMap.size() == 1) {
            VideoRange videoRange = mVideoRangeMap.get(0);
            if (videoRange != null && videoRange.getStart() == 0L && videoRange.getEnd() == mTotalSize) {
                mCacheInfo.setIsCompleted(true);
            }
        }
        saveVideoInfo();
    }
}
