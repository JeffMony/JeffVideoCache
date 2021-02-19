package com.jeffmony.videocache.task;

import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.model.VideoRange;

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
 * |-----|      |-------|              |--------------|
 *
 * |--------------------|              |--------------|
 *
 * |--------------------------------------------------|
 */
public class NonM3U8CacheTask extends VideoCacheTask {

    private LinkedHashMap<Long, Long> mVideoSegMap;
    private LinkedHashMap<Long, VideoRange> mVideoRangeMap;

    public NonM3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        super(cacheInfo, headers);
        mTotalSize = cacheInfo.getTotalSize();
        mVideoSegMap = cacheInfo.getVideoSegMap();
        if (mVideoSegMap == null) {
            mVideoSegMap = new LinkedHashMap<>();
        }
    }

    @Override
    public void startCacheTask() {
        if (isTaskRunning()) {
            return;
        }
        notifyOnTaskStart();

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
        if (mIsCompleted) {
            notifyOnTaskCompleted();
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(6, 6,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
    }
}
