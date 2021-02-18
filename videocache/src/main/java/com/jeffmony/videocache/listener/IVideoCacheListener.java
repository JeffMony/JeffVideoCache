package com.jeffmony.videocache.listener;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.model.VideoCacheInfo;

public interface IVideoCacheListener {

    void onCacheStart(VideoCacheInfo cacheInfo);

    void onCacheProgress(VideoCacheInfo cacheInfo);

    void onCacheError(VideoCacheInfo cacheInfo, VideoCacheException exception);

    void onCacheFinished(VideoCacheInfo cacheInfo);
}
