package com.jeffmony.videocache.task;

import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Ts;
import com.jeffmony.videocache.model.VideoCacheInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class M3U8CacheTask extends VideoCacheTask {

    private M3U8 mM3U8;
    private int mCachedTs;
    private int mTotalTs;
    private Map<Integer, Long> mTsLengthMap;
    private List<M3U8Ts> mTsList;

    public M3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers, M3U8 m3u8) {
        super(cacheInfo, headers);
        mM3U8 = m3u8;
        mTsList = m3u8.getTsList();
        mTotalTs = cacheInfo.getTotalTs();
        mCachedTs = cacheInfo.getCachedTs();
        mTsLengthMap = cacheInfo.getTsLengthMap();
    }

    @Override
    public void startCacheTask() {
        if (mIsCompleted) {
            notifyOnTaskCompleted();
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(6, 6,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    @Override
    public void pauseCacheTask() {

    }

    @Override
    public void resumeCacheTask() {

    }
}
