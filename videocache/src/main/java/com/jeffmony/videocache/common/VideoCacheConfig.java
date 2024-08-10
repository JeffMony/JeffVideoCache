package com.jeffmony.videocache.common;

import android.content.Context;

public class VideoCacheConfig {

    private final long mExpireTime;      //video cache中的过期时间，超过过期时间采用LRU清理规则清理video cache数据
    private final long mMaxCacheSize;    //设置video cache最大的缓存限制,例如超过2G，则采用LRU清理规则清理video cache数据
    private final String mFilePath;      //video cache存储的位置
    private final int mReadTimeOut;      //网络读超时
    private final int mConnTimeOut;      //网络建连超时
    private final boolean mIgnoreCert;   //是否忽略证书校验
    private int mPort;             //本地代理的端口
    private boolean mUseOkHttp;    //使用okhttp接管网络请求

    private final SourceCreator mSourceCreator; //可以自定义响应、下载逻辑

    private final Context mContext;

    public VideoCacheConfig(Context context, long expireTime, long maxCacheSize, String filePath,
                            int readTimeOut, int connTimeOut, boolean ignoreCert,
                            int port, boolean useOkHttp, SourceCreator sourceCreator) {
        mContext = context;
        mExpireTime = expireTime;
        mMaxCacheSize = maxCacheSize;
        mFilePath = filePath;
        mReadTimeOut = readTimeOut;
        mConnTimeOut = connTimeOut;
        mIgnoreCert = ignoreCert;
        mPort = port;
        mUseOkHttp = useOkHttp;
        mSourceCreator = sourceCreator != null ? sourceCreator : new SourceCreator();
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

    public void setPort(int port) {
        mPort = port;
    }

    public int getPort() {
        return mPort;
    }

    public void setUseOkHttp(boolean useOkHttp) {
        mUseOkHttp = useOkHttp;
    }

    public boolean useOkHttp() { return mUseOkHttp; }

    public boolean ignoreCert() { return mIgnoreCert; }

    public SourceCreator getSourceCreator() {
        return mSourceCreator;
    }

    public Context getContext() {
        return mContext;
    }
}
