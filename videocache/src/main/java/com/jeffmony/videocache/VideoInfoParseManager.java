package com.jeffmony.videocache;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.listener.IVideoInfoParsedListener;
import com.jeffmony.videocache.model.VideoCacheInfo;

import java.util.Map;

/**
 * @author jeffmony
 * 解析视频类型信息的工具类
 */
public class VideoInfoParseManager {

    private static volatile VideoInfoParseManager sInstance = null;

    private VideoCacheConfig mProxyConfig;

    public static VideoInfoParseManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoInfoParseManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoInfoParseManager();
                }
            }
        }
        return sInstance;
    }

    public void initProxyConfig(@NonNull VideoCacheConfig config) {
        mProxyConfig = config;
    }

    //解析视频类型
    public void parseVideoInfo(VideoCacheInfo cacheInfo, Map<String, String> headers,
                               Map<String, Object> extraParams, IVideoInfoParsedListener listener) {

    }
}
