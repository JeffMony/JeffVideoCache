package com.jeffmony.playersdk.control;

import androidx.annotation.NonNull;

import com.jeffmony.playersdk.impl.BasePlayerImpl;
import com.jeffmony.videocache.VideoProxyCacheManager;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.listener.IVideoCacheListener;
import com.jeffmony.videocache.model.VideoCacheInfo;

import java.util.Map;

public class LocalProxyVideoControl {

    private BasePlayerImpl mPlayer;
    private String mVideoUrl;

    private IVideoCacheListener mListener = new IVideoCacheListener() {
        @Override
        public void onCacheStart(VideoCacheInfo cacheInfo) {

        }

        @Override
        public void onCacheProgress(VideoCacheInfo cacheInfo) {

        }

        @Override
        public void onCacheError(VideoCacheInfo cacheInfo, VideoCacheException exception) {

        }

        @Override
        public void onCacheFinished(VideoCacheInfo cacheInfo) {

        }
    };

    public LocalProxyVideoControl(@NonNull BasePlayerImpl player) {
        mPlayer = player;
    }

    public void startRequestVideoInfo(String videoUrl, Map<String, String> headers, Map<String, Object> extraParams) {
        mVideoUrl = videoUrl;
        VideoProxyCacheManager.getInstance().addCacheListener(videoUrl, mListener);
        VideoProxyCacheManager.getInstance().startRequestVideoInfo(videoUrl, headers, extraParams);
    }

    public void releaseLocalProxyResources() {
        VideoProxyCacheManager.getInstance().removeCacheListener(mVideoUrl);
    }
}
