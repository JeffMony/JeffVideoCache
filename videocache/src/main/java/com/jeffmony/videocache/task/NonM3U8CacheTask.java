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
        startRequestVideoRange(0L);   //传入一个起始位置,需要到已缓存的文件中去查找一下
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
        LogUtils.i(TAG, "pauseCacheTask");
        if (isTaskRunning()) {
            mTaskExecutor.shutdownNow();
        }

        //暂停之后还是需要将当前的range数据保存起来
        if (!mCacheInfo.isCompleted() && mRequestRange != null) {
            long tempRangeStart = mRequestRange.getStart();
            long tempRangeEnd = mCachedSize;
            mRequestRange = new VideoRange(tempRangeStart, tempRangeEnd);
            updateVideoRangeInfo();
        }
    }

    @Override
    public void stopCacheTask() {
        LogUtils.i(TAG, "stopCacheTask");
        if (isTaskRunning()) {
            mTaskExecutor.shutdownNow();
        }
        if (!mCacheInfo.isCompleted() && mRequestRange != null) {
            long tempRangeStart = mRequestRange.getStart();
            long tempRangeEnd = mCachedSize;
            mRequestRange = new VideoRange(tempRangeStart, tempRangeEnd);
            updateVideoRangeInfo();
        }
    }

    @Override
    public void resumeCacheTask() {
        LogUtils.i(TAG, "resumeCacheTask");
        if (isTaskShutdown() && mCachedSize < mTotalSize) {
            startRequestVideoRange(mCachedSize);   //传入一个起始位置,需要到已缓存的文件中去查找一下
        }
    }

    @Override
    public void seekToCacheTask(float percent) {
        //非M3U8视频用不到,因为这样估计请求的起始点,gap太大了.
        //有拖动进度条, 肯定不能从原来的range 开始请求了, 有新的range请求, 那就要停掉原来的range请求
    }

    @Override
    public void seekToCacheTask(long startPosition) {
        //真正的拖动进度条, 这儿要真正构建新的range请求
        boolean shouldSeekToCacheTask = shouldSeekToCacheTask(startPosition);
        LogUtils.i(TAG, "seekToCacheTask ====> shouldSeekToCacheTask="+shouldSeekToCacheTask+", startPosition="+startPosition);
        if (shouldSeekToCacheTask) {
            pauseCacheTask();
            startRequestVideoRange(startPosition);
        }
    }

    /**
     * true   ====>  表示重新发起请求
     * false  ====>  表示没有必要重新发起请求
     *
     * @param startPosition
     * @return
     */
    private boolean shouldSeekToCacheTask(long startPosition) {

        //当前文件下载完成, 不需要执行range request请求
        if (mCacheInfo.isCompleted()) {
            return false;
        }
        if (mRequestRange != null) {
            boolean result = mRequestRange.getStart() <= startPosition && startPosition < mRequestRange.getEnd();
            if (result) {
                //当前拖动到的位置已经在request range中了, 没有必要重新发起请求了
                if (mCachedSize >= startPosition) {
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean isMp4PositionSegExisted(long startPosition) {
        if (mCacheInfo.isCompleted()) {
            return true;
        }
        if (mVideoSegMap != null) {
            for(Map.Entry entry : mVideoSegMap.entrySet()) {
                long start = (long)entry.getKey();
                long end = (long)entry.getValue();

                //只有这样,才能说明当前的startPosition处于一个VideoRange范围内
                if (start <=startPosition && startPosition < end) {
                    return true;
                }
            }
        }
        if (mRequestRange != null) {
            boolean result = mRequestRange.getStart() <= startPosition && startPosition < mRequestRange.getEnd();
            result = result && (mCachedSize >= startPosition);
            return result;
        }
        return false;
    }

    private void startRequestVideoRange(long curLength) {
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(1, 1,
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

                mCachedSize = requestStart;
                LogUtils.i(TAG, "Start request : " + mRequestRange + ", CurrentCachedSize="+mCachedSize);
                connection = HttpUtils.getConnection(mVideoUrl, mHeaders);
                inputStream = connection.getInputStream();
                LogUtils.i(TAG, "Receive response");

                byte[] buffer = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
                int readLength;
                while((readLength = inputStream.read(buffer)) != -1) {
                    if (mCachedSize >= requestEnd) {
                        mCachedSize = requestEnd;
                    }
                    if (mCachedSize + readLength > requestEnd) {
                        long read = requestEnd - mCachedSize;
                        randomAccessFile.write(buffer, 0, (int)read);
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
                startRequestVideoRange(position);
            }
        }
    }

    private void updateVideoRangeInfo() {
        if (mVideoRangeMap.size() > 0) {
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
            LogUtils.i(TAG, "updateVideoRangeInfo--->finalVideoRange: " + finalVideoRange);

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
        } else {
            LogUtils.i(TAG, "updateVideoRangeInfo--->mRequestRange : " + mRequestRange);
            mVideoRangeMap.put(mRequestRange.getStart(), mRequestRange);
        }

        LinkedHashMap<Long, Long> tempSegMap = new LinkedHashMap<>();
        for(Map.Entry<Long, VideoRange> entry : mVideoRangeMap.entrySet()) {
            VideoRange videoRange = entry.getValue();
            LogUtils.i(TAG, "updateVideoRangeInfo--->Result videoRange : " + videoRange);
            tempSegMap.put(videoRange.getStart(), videoRange.getEnd());
        }
        mVideoSegMap.clear();
        mVideoSegMap.putAll(tempSegMap);
        mCacheInfo.setVideoSegMap(mVideoSegMap);

        if (mVideoRangeMap.size() == 1) {
            VideoRange videoRange = mVideoRangeMap.get(0L);
            LogUtils.i(TAG, "updateVideoRangeInfo---> videoRange : " + videoRange);
            if (videoRange != null && videoRange.equals(new VideoRange(0, mTotalSize))) {
                LogUtils.i(TAG, "updateVideoRangeInfo--->Set completed");
                mCacheInfo.setIsCompleted(true);
            }
        }
        saveVideoInfo();
    }
}
