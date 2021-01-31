package com.jeffmony.videocache.m3u8;

import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * @author jeffmony
 *
 * M3U8文件中TS文件的结构
 */
public class M3U8Ts implements Comparable<M3U8Ts> {
    private String mUrl;                   //ts的网络url
    private String mName;                  //ts的文件名
    private float mDuration;               //ts的时长
    private int mTsIndex;                  //ts索引位置，起始索引为0
    private long mFileSize;                //ts文件大小
    private long mContentLength;           //ts网络请求的content-length
    private boolean mHasDiscontinuity;     //当前ts前是否有Discontinuity
    private boolean mHasKey;               //ts是否加密
    private String mMethod;                //ts加密方法
    private String mKeyUrl;                //密钥地址
    private String mKeyIv;                 //密钥IV

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public float getDuration() {
        return mDuration;
    }

    public void setDuration(float mDuration) {
        this.mDuration = mDuration;
    }

    public int getTsIndex() {
        return mTsIndex;
    }

    public void setTsIndex(int mTsIndex) {
        this.mTsIndex = mTsIndex;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long mFileSize) {
        this.mFileSize = mFileSize;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public void setContentLength(long mContentLength) {
        this.mContentLength = mContentLength;
    }

    public boolean isHasDiscontinuity() {
        return mHasDiscontinuity;
    }

    public void setHasDiscontinuity(boolean mHasDiscontinuity) {
        this.mHasDiscontinuity = mHasDiscontinuity;
    }

    public boolean isHasKey() {
        return mHasKey;
    }

    public void setHasKey(boolean mHasKey) {
        this.mHasKey = mHasKey;
    }

    public String getMethod() {
        return mMethod;
    }

    public void setMethod(String mMethod) {
        this.mMethod = mMethod;
    }

    public String getKeyUrl() {
        return mKeyUrl;
    }

    public void setKeyUrl(String mKeyUrl) {
        this.mKeyUrl = mKeyUrl;
    }

    public String getKeyIv() {
        return mKeyIv;
    }

    public void setKeyIv(String mKeyIv) {
        this.mKeyIv = mKeyIv;
    }

    public String getTsName() {
        return mTsIndex + StorageUtils.TS_SUFFIX;
    }

    @Override
    public int compareTo(M3U8Ts m3u8Ts) {
        return mUrl.compareTo(m3u8Ts.getUrl());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getTsProxyUrl(String md5, Map<String, String> headers) {
        //三个字符串
        //1.ts的url
        //2.ts存储的位置
        //3.ts url对应的请求headers
        String proxyExtraInfo = mUrl + ProxyCacheUtils.TS_PROXY_SPLIT_STR +
                File.separator + md5 + File.separator + mTsIndex + StorageUtils.TS_SUFFIX +
                ProxyCacheUtils.TS_PROXY_SPLIT_STR + ProxyCacheUtils.map2Str(headers);
        String proxyUrl = String.format(Locale.US, "http://%s:%d/%s", ProxyCacheUtils.LOCAL_PROXY_HOST,
                ProxyCacheUtils.getLocalPort(), ProxyCacheUtils.encodeUri(proxyExtraInfo));
        return proxyUrl;
    }

}
