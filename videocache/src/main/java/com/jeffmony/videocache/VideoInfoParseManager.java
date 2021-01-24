package com.jeffmony.videocache;

public class VideoInfoParseManager {

    private static volatile VideoInfoParseManager sInstance = null;

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
}
