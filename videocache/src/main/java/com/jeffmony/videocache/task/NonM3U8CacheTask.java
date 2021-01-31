package com.jeffmony.videocache.task;

import com.jeffmony.videocache.model.VideoCacheInfo;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NonM3U8CacheTask extends VideoCacheTask {

    public NonM3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        super(cacheInfo, headers);
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

    }

    @Override
    public void resumeCacheTask() {

    }

    @Override
    public void seekToCacheTask(int curTs) {
        //非M3U8视频忽略这个调用
    }

    @Override
    public void seekToCacheTask(long curLength) {
        if (mIsCompleted) {
            notifyOnTaskCompleted();
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(6, 6,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
    }
}
