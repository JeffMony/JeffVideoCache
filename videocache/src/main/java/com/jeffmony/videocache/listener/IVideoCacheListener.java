package com.jeffmony.videocache.listener;

import com.jeffmony.videocache.model.VideoCacheInfo;

public interface IVideoCacheListener {

    void onCacheStart(VideoCacheInfo cacheInfo);

    void onCacheProgress(VideoCacheInfo cacheInfo);

    void onCacheError(VideoCacheInfo cacheInfo, String msg, int errorCode);

    void onCacheForbidden(VideoCacheInfo cacheInfo);

    void onCacheFinished(VideoCacheInfo cacheInfo);
}
