package com.jeffmony.videocache;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VideoLockManager {

    private static volatile VideoLockManager sInstance = null;
    private final Map<String, Object> mLockMap = new ConcurrentHashMap<>();

    private VideoLockManager() {

    }

    public static VideoLockManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoLockManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoLockManager();
                }
            }
        }
        return sInstance;
    }

    public Object getLock(@NonNull String md5) {
        Object lock = mLockMap.get(md5);
        if (lock == null) {
            synchronized (this) {
                lock = mLockMap.get(md5);
                if (lock == null) {
                    lock = new Object();
                    mLockMap.put(md5, lock);
                }
            }
        }
        return lock;
    }

    public void removeLock(@NonNull String md5) {
        mLockMap.remove(md5);
    }
}
