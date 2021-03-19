package com.jeffmony.videocache.utils;

import android.content.Context;

import com.jeffmony.videocache.model.VideoCacheInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author jeffmony
 * 本地代理存储相关的工具类
 */

public class StorageUtils {

    private static final String TAG = "StorageUtils";

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final String INFO_FILE = "video.info";
    public static final String LOCAL_M3U8_SUFFIX = "_local.m3u8";
    public static final String PROXY_M3U8_SUFFIX = "_proxy.m3u8";
    public static final String M3U8_SUFFIX = ".m3u8";
    public static final String NON_M3U8_SUFFIX = ".video";

    private static final Object sInfoFileLock = new Object();

    public static File getVideoFileDir(Context context) {
        return new File(context.getExternalFilesDir("Video"), "jeffmony");
    }

    public static VideoCacheInfo readVideoCacheInfo(File dir) {
        LogUtils.i(TAG, "readVideoCacheInfo : dir=" + dir.getAbsolutePath());
        File file = new File(dir, INFO_FILE);
        if (!file.exists()) {
            LogUtils.i(TAG,"readProxyCacheInfo failed, file not exist.");
            return null;
        }
        ObjectInputStream fis = null;
        try {
            synchronized (sInfoFileLock) {
                fis = new ObjectInputStream(new FileInputStream(file));
                VideoCacheInfo info = (VideoCacheInfo) fis.readObject();
                return info;
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"readVideoCacheInfo failed, exception=" + e.getMessage());
        } finally {
            ProxyCacheUtils.close(fis);
        }
        return null;
    }

    public static void saveVideoCacheInfo(VideoCacheInfo info, File dir) {
        File file = new File(dir, INFO_FILE);
        ObjectOutputStream fos = null;
        try {
            synchronized (sInfoFileLock) {
                fos = new ObjectOutputStream(new FileOutputStream(file));
                fos.writeObject(info);
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"saveVideoCacheInfo failed, exception=" + e.getMessage());
        } finally {
            ProxyCacheUtils.close(fos);
        }
    }
}
