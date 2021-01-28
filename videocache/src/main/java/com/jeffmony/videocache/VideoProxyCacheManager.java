package com.jeffmony.videocache;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.listener.IVideoInfoParsedListener;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.proxy.VideoLocalProxyServer;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jeffmony
 *
 * 本地代理的综合管理类
 */

public class VideoProxyCacheManager {

    private static volatile VideoProxyCacheManager sInstance = null;
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
        ProxyCacheUtils.setVideoCacheConfig(config);
        new VideoLocalProxyServer();  //初始化本地代理服务
    }

    /**
     *
     * @param videoUrl  视频url
     */
    public void startVideoRequest(String videoUrl) {
        startVideoRequest(videoUrl, new HashMap<>());
    }

    /**
     *
     * @param videoUrl 视频url
     * @param headers  请求的头部信息
     */
    public void startVideoRequest(String videoUrl, Map<String, String> headers) {
        startVideoRequest(videoUrl, headers, new HashMap<>());
    }

    /**
     *
     * @param videoUrl    视频url
     * @param headers     请求的头部信息
     * @param extraParams 额外参数，这个map很有用，例如我已经知道当前请求视频的类型和长度，都可以在extraParams中设置,
     *                    详情见VideoParams
     */
    public void startVideoRequest(String videoUrl, Map<String, String> headers, Map<String, Object> extraParams) {
        String md5 = ProxyCacheUtils.computeMD5(videoUrl);
        File saveDir = new File(ProxyCacheUtils.getConfig().getFilePath(), md5);
        VideoCacheInfo cacheInfo = StorageUtils.readVideoCacheInfo(saveDir);
        if (cacheInfo == null) {
            //之前没有缓存信息
            cacheInfo = new VideoCacheInfo(videoUrl);
        }

        VideoInfoParseManager.getInstance().parseVideoInfo(cacheInfo, headers, extraParams, new IVideoInfoParsedListener() {
            @Override
            public void onM3U8ParsedFinished(M3U8 m3u8) {

            }

            @Override
            public void onM3U8ParsedFailed(VideoCacheException e) {

            }

            @Override
            public void onM3U8LiveCallback() {

            }

            @Override
            public void onNonM3U8ParsedFinished() {

            }

            @Override
            public void onNonM3U8ParsedFailed(VideoCacheException e) {

            }
        });

    }

}
