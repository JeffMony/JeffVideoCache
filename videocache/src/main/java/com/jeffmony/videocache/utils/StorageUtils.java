package com.jeffmony.videocache.utils;

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

    private static final Object sFileLock = new Object();
    public static final String INFO_FILE = "video.info";

    public static VideoCacheInfo readVideoCacheInfo(File dir) {
        File file = new File(dir, INFO_FILE);
        if (!file.exists()) {
            LogUtils.i(TAG,"readProxyCacheInfo failed, file not exist.");
            return null;
        }
        ObjectInputStream fis = null;
        try {
            synchronized (sFileLock) {
                fis = new ObjectInputStream(new FileInputStream(file));
                VideoCacheInfo info = (VideoCacheInfo) fis.readObject();
                return info;
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"readVideoCacheInfo failed, exception=" + e.getMessage());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
                LogUtils.w(TAG,"readVideoCacheInfo failed, close fis failed.");
            }
        }
        return null;
    }

    public static void saveVideoCacheInfo(VideoCacheInfo info, File dir) {
        File file = new File(dir, INFO_FILE);
        ObjectOutputStream fos = null;
        try {
            synchronized (sFileLock) {
                fos = new ObjectOutputStream(new FileOutputStream(file));
                fos.writeObject(info);
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"saveVideoCacheInfo failed, exception=" + e.getMessage());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                LogUtils.w(TAG,"saveVideoCacheInfo failed, close fos failed.");
            }
        }
    }
}
