package com.jeffmony.videocache;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.common.ProxyMessage;
import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.listener.IVideoCacheTaskListener;
import com.jeffmony.videocache.listener.IVideoInfoParsedListener;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.proxy.VideoLocalProxyServer;
import com.jeffmony.videocache.task.M3U8CacheTask;
import com.jeffmony.videocache.task.NonM3U8CacheTask;
import com.jeffmony.videocache.task.VideoCacheTask;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jeffmony
 *
 * 本地代理的综合管理类
 */

public class VideoProxyCacheManager {

    private static volatile VideoProxyCacheManager sInstance = null;
    private ProxyMessageHandler mProxyHandler;

    private Map<String, VideoCacheTask> mCacheTaskMap = new ConcurrentHashMap<>();
    private Map<String, VideoCacheInfo> mCacheInfoMap = new ConcurrentHashMap<>();

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
        mProxyHandler = new ProxyMessageHandler(handlerThread.getLooper());
        handlerThread.start();
    }

    private class ProxyMessageHandler extends Handler {

        public ProxyMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
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
    public void startRequestVideoInfo(String videoUrl) {
        startRequestVideoInfo(videoUrl, new HashMap<>());
    }

    /**
     *
     * @param videoUrl 视频url
     * @param headers  请求的头部信息
     */
    public void startRequestVideoInfo(String videoUrl, Map<String, String> headers) {
        startRequestVideoInfo(videoUrl, headers, new HashMap<>());
    }

    /**
     *
     * @param videoUrl    视频url
     * @param headers     请求的头部信息
     * @param extraParams 额外参数，这个map很有用，例如我已经知道当前请求视频的类型和长度，都可以在extraParams中设置,
     *                    详情见VideoParams
     */
    public void startRequestVideoInfo(String videoUrl, Map<String, String> headers, Map<String, Object> extraParams) {
        String md5 = ProxyCacheUtils.computeMD5(videoUrl);
        File saveDir = new File(ProxyCacheUtils.getConfig().getFilePath(), md5);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
        VideoCacheInfo videoCacheInfo = StorageUtils.readVideoCacheInfo(saveDir);
        if (videoCacheInfo == null) {
            //之前没有缓存信息
            videoCacheInfo = new VideoCacheInfo(videoUrl);
            videoCacheInfo.setMd5(md5);
            videoCacheInfo.setSavePath(saveDir.getAbsolutePath());
        }

        VideoInfoParseManager.getInstance().parseVideoInfo(videoCacheInfo, headers, extraParams, new IVideoInfoParsedListener() {
            @Override
            public void onM3U8ParsedFinished(M3U8 m3u8, VideoCacheInfo cacheInfo) {
                //开始发起请求M3U8视频中的ts数据
                startM3U8Task(m3u8, cacheInfo, headers);
            }

            @Override
            public void onM3U8ParsedFailed(VideoCacheException e, VideoCacheInfo cacheInfo) {
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_ERROR, cacheInfo).sendToTarget();
            }

            @Override
            public void onM3U8LiveCallback(VideoCacheInfo cacheInfo) {
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_FORBIDDEN, cacheInfo).sendToTarget();
            }

            @Override
            public void onNonM3U8ParsedFinished(VideoCacheInfo cacheInfo) {
                //开始发起请求视频数据
                startNonM3U8Task(cacheInfo, headers);
            }

            @Override
            public void onNonM3U8ParsedFailed(VideoCacheException e, VideoCacheInfo cacheInfo) {
                mProxyHandler.obtainMessage(ProxyMessage.MSG_VIDEO_PROXY_ERROR, cacheInfo).sendToTarget();
            }
        });

    }

    /**
     * 开始缓存M3U8任务
     * @param m3u8
     * @param cacheInfo
     * @param headers
     */
    private void startM3U8Task(M3U8 m3u8, VideoCacheInfo cacheInfo, Map<String, String> headers) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(cacheInfo.getVideoUrl());
        if (cacheTask == null) {
            cacheTask = new M3U8CacheTask(cacheInfo, headers, m3u8);
        }
        startVideoCacheTask(cacheTask, cacheInfo);
    }

    /**
     * 开始缓存非M3U8任务
     * @param cacheInfo
     * @param headers
     */
    private void startNonM3U8Task(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(cacheInfo.getVideoUrl());
        if (cacheTask == null) {
            cacheTask = new NonM3U8CacheTask(cacheInfo, headers);
        }
        startVideoCacheTask(cacheTask, cacheInfo);
    }

    private void startVideoCacheTask(VideoCacheTask cacheTask, VideoCacheInfo cacheInfo) {
        cacheTask.setTaskListener(new IVideoCacheTaskListener() {
            @Override
            public void onTaskStart() {

            }

            @Override
            public void onTaskProgress(float percent, long cachedSize, float speed) {

            }

            @Override
            public void onM3U8TaskProgress(float percent, long cachedSize, float speed, Map<Integer, Long> tsLengthMap) {

            }

            @Override
            public void onTaskFailed(Exception e) {

            }

            @Override
            public void onTaskCompleted() {

            }
        });

        cacheTask.startCacheTask();
    }

    /**
     * 暂停缓存任务
     * @param url
     */
    public void pauseCacheTask(String url) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(url);
        if (cacheTask != null) {
            cacheTask.pauseCacheTask();
        }
    }

    /**
     * 恢复缓存任务
     * @param url
     */
    public void resumeCacheTask(String url) {
        VideoCacheTask cacheTask = mCacheTaskMap.get(url);
        if (cacheTask != null) {
            cacheTask.resumeCacheTask();
        }
    }

}
