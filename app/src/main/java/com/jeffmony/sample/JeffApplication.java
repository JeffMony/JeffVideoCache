package com.jeffmony.sample;

import android.app.Application;

import com.jeffmony.videocache.VideoProxyCacheManager;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;

public class JeffApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        File saveFile = StorageUtils.getVideoFileDir(this);
        if (!saveFile.exists()) {
            saveFile.mkdir();
        }
        VideoProxyCacheManager.Builder builder = new VideoProxyCacheManager.Builder().
                setFilePath(saveFile.getAbsolutePath()).    //缓存存储位置
                setConnTimeOut(60 * 1000).                  //网络连接超时
                setReadTimeOut(60 * 1000).                  //网络读超时
                setExpireTime(2 * 24 * 60 * 60 * 1000).     //2天的过期时间
                setMaxCacheSize(2 * 1024 * 1024 * 1024);    //2G的存储上限
        VideoProxyCacheManager.getInstance().initProxyConfig(builder.build());

    }
}
