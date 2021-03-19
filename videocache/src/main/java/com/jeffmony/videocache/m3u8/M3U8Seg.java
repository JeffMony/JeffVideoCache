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
public class M3U8Seg implements Comparable<M3U8Seg> {
    private String mUrl;                   //分片的网络url
    private String mName;                  //分片的文件名
    private float mDuration;               //分片的时长
    private int mSegIndex;                 //分片索引位置，起始索引为0
    private long mFileSize;                //分片文件大小
    private long mContentLength;           //分片文件的网络请求的content-length
    private boolean mHasDiscontinuity;     //当前分片文件前是否有Discontinuity
    private boolean mHasKey;               //分片文件是否加密
    private String mMethod;                //分片文件的加密方法
    private String mKeyUrl;                //分片文件的密钥地址
    private String mKeyIv;                 //密钥IV
    private int mRetryCount;               //重试请求次数

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public float getDuration() {
        return mDuration;
    }

    public void setDuration(float duration) {
        mDuration = duration;
    }

    public int getSegIndex() {
        return mSegIndex;
    }

    public void setSegIndex(int segIndex) {
        mSegIndex = segIndex;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public void setContentLength(long contentLength) {
        mContentLength = contentLength;
    }

    public boolean isHasDiscontinuity() {
        return mHasDiscontinuity;
    }

    public void setHasDiscontinuity(boolean hasDiscontinuity) {
        mHasDiscontinuity = hasDiscontinuity;
    }

    public boolean isHasKey() {
        return mHasKey;
    }

    public void setHasKey(boolean hasKey) {
        mHasKey = hasKey;
    }

    public String getMethod() {
        return mMethod;
    }

    public void setMethod(String method) {
        mMethod = method;
    }

    public String getKeyUrl() {
        return mKeyUrl;
    }

    public void setKeyUrl(String keyUrl) {
        mKeyUrl = keyUrl;
    }

    public String getKeyIv() {
        return mKeyIv;
    }

    public void setKeyIv(String keyIv) {
        mKeyIv = keyIv;
    }

    public String getTsName() {
        return mSegIndex + StorageUtils.TS_SUFFIX;
    }

    public void setRetryCount(int retryCount) { mRetryCount = retryCount; }

    public int getRetryCount() { return mRetryCount; }

    @Override
    public int compareTo(M3U8Seg m3u8Ts) {
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
                File.separator + md5 + File.separator + mSegIndex + StorageUtils.TS_SUFFIX +
                ProxyCacheUtils.TS_PROXY_SPLIT_STR + ProxyCacheUtils.map2Str(headers);
        String proxyUrl = String.format(Locale.US, "http://%s:%d/%s", ProxyCacheUtils.LOCAL_PROXY_HOST,
                ProxyCacheUtils.getLocalPort(), ProxyCacheUtils.encodeUriWithBase64(proxyExtraInfo));
        return proxyUrl;
    }

}
