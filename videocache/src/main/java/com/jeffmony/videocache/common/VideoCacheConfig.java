package com.jeffmony.videocache.common;

public class VideoCacheConfig {

    private long mExpireTime;      //video cache中的过期时间，超过过期时间采用LRU清理规则清理video cache数据
    private long mMaxCacheSize;    //设置video cache最大的缓存限制,例如超过2G，则采用LRU清理规则清理video cache数据
    private String mFilePath;      //video cache存储的位置
    private int mReadTimeOut;      //网络读超时
    private int mConnTimeOut;      //网络建连超时
    private int mPort;             //本地代理的端口

    public VideoCacheConfig(long expireTime, long maxCacheSize, String filePath,
                            int readTimeOut, int connTimeOut, int port) {
        mExpireTime = expireTime;
        mMaxCacheSize = maxCacheSize;
        mFilePath = filePath;
        mReadTimeOut = readTimeOut;
        mConnTimeOut = connTimeOut;
        mPort = port;
    }

    public long getExpireTime() {
        return mExpireTime;
    }

    public long getMaxCacheSize() {
        return mMaxCacheSize;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public int getReadTimeOut() {
        return mReadTimeOut;
    }

    public int getConnTimeOut() {
        return mConnTimeOut;
    }

    public int getPort() {
        return mPort;
    }
}
