package com.jeffmony.videocache.task;

import com.jeffmony.videocache.model.VideoCacheInfo;

import java.util.Map;

public class NonM3U8CacheTask extends VideoCacheTask {

    public NonM3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        super(cacheInfo, headers);
    }

    @Override
    public void startCacheTask() {

    }

    @Override
    public void pauseCacheTask() {

    }

    @Override
    public void resumeCacheTask() {

    }
}
