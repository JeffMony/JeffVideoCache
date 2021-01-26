package com.jeffmony.videocache.model;

import java.io.Serializable;
import java.util.Map;

public class VideoCacheInfo implements Serializable {
    private String mVideoUrl;                      //视频的url
    private int mVideoType;                        //视频类型
    private long mCachedSize;                      //已经缓存的大小，M3U8文件忽略这个变量
    private long mTotalSize;                       //总大小
    private int mCachedTs;                         //已经缓存的ts个数
    private int mTotalTs;                          //总的ts个数
    private Map<Integer, Long> mTsLengthMap;       //key表示ts的索引，value表示索引分片的content-length

    public VideoCacheInfo(String url) {
        mVideoUrl = url;
    }

    public String getVideoUrl() {
        return mVideoUrl;
    }

    public void setVideoType(int type) {
        mVideoType = type;
    }

    public int getVideoType() {
        return mVideoType;
    }

    public void setCachedSize(long cachedSize) {
        mCachedSize = cachedSize;
    }

    public long getCachedSize() {
        return mCachedSize;
    }

    public void setTotalSize(long totalSize) {
        mTotalSize = totalSize;
    }

    public long getTotalSize() {
        return mTotalSize;
    }

    public void setCachedTs(int cachedTs) {
        mCachedTs = cachedTs;
    }

    public int getCachedTs() {
        return mCachedTs;
    }

    public void setTotalTs(int totalTs) {
        mTotalTs = totalTs;
    }

    public void setTsLengthMap(Map<Integer, Long> tsLengthMap) {
        mTsLengthMap = tsLengthMap;
    }

    public Map<Integer, Long> getTsLengthMap() {
        return mTsLengthMap;
    }

    public String toString() {
        return "VideoCacheInfo[" +
                "url=" + mVideoUrl + "," +
                "type=" + mVideoType + "," +
                "cachedSize=" + mCachedSize + "," +
                "totalSize=" + mTotalSize  + "," +
                "cachedTs=" + mCachedTs + "," +
                "totalTs=" + mTotalTs +
                "]";
    }
}
