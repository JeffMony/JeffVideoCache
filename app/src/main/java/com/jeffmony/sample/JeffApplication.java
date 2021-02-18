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
                setFilePath(saveFile.getAbsolutePath()).
                setConnTimeOut(60 * 1000).
                setReadTimeOut(60 * 1000).
                setExpireTime(2 * 24 * 60 * 60 * 1000).
                setMaxCacheSize(2 * 1024 * 1024 * 1024);
        VideoProxyCacheManager.getInstance().initProxyConfig(builder.build());

    }
}
