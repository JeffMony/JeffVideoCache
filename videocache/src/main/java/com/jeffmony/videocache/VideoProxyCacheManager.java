package com.jeffmony.videocache;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.proxy.VideoLocalProxyServer;

/**
 * @author jeffmony
 *
 * 本地代理的综合管理类
 */

public class VideoProxyCacheManager {

    private static volatile VideoProxyCacheManager sInstance = null;
    private VideoCacheConfig mProxyConfig;
    private Handler mProxyHandler;

    public static VideoProxyCacheManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoProxyCacheManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoProxyCacheManager();
                }
            }
        }
        return sInstance;
    }

    private VideoProxyCacheManager() {
        HandlerThread handlerThread = new HandlerThread("proxy_cache_thread");
        mProxyHandler = new Handler(handlerThread.getLooper());
        handlerThread.start();
    }

    /**
     * 构建代理缓存的属性
     */
    public static class Builder {

        private long mExpireTime = 7 * 24 * 60 * 60 * 1000;
        private long mMaxCacheSize = 2 * 1024 * 1024 * 1024;
        private String mFilePath;
        private int mReadTimeOut = 30 * 1000;
        private int mConnTimeOut = 30 * 1000;
        private int mPort;

        public Builder setExpireTime(long expireTime) {
            mExpireTime = expireTime;
            return this;
        }

        public Builder setMaxCacheSize(long maxCacheSize) {
            mMaxCacheSize = maxCacheSize;
            return this;
        }

        public Builder setFilePath(String filePath) {
            mFilePath = filePath;
            return this;
        }

        public Builder setReadTimeOut(int readTimeOut) {
            mReadTimeOut = readTimeOut;
            return this;
        }

        public Builder setConnTimeOut(int connTimeOut) {
            mConnTimeOut = connTimeOut;
            return this;
        }

        public Builder setPort(int port) {
            mPort = port;
            return this;
        }

        public VideoCacheConfig build() {
            return new VideoCacheConfig(mExpireTime, mMaxCacheSize, mFilePath, mReadTimeOut, mConnTimeOut, mPort);
        }
    }

    public void initProxyConfig(@NonNull VideoCacheConfig config) {
        mProxyConfig = config;
        new VideoLocalProxyServer(config);  //初始化本地代理服务
    }

}
