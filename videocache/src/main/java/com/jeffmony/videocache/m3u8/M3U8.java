package com.jeffmony.videocache.m3u8;

import android.text.TextUtils;

import com.jeffmony.videocache.utils.UrlUtils;

import java.util.List;

/**
 * @author jeffmony
 *
 * M3U8文件结构
 */
public class M3U8 {
    private String mUrl;                 //M3U8的url
    private String mBaseUrl;             //url的baseurl
    private String mHostUrl;             //url的hosturl
    private float mTargetDuration;       //指定的duration
    private int mSequence = 0;           //序列起始值
    private int mVersion = 3;            //版本号
    private boolean mIsLive;             //是否是直播
    private List<M3U8Ts> mTsList;        //ts 列表

    public M3U8(String url) {
        mUrl = url;
        mBaseUrl = UrlUtils.getBaseUrl(url);
        mHostUrl = UrlUtils.getHostUrl(url);
    }

    public String getUrl() {
        return mUrl;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public String getHostUrl() {
        return mHostUrl;
    }

    public float getTargetDuration() {
        return mTargetDuration;
    }

    public void setTargetDuration(float mTargetDuration) {
        this.mTargetDuration = mTargetDuration;
    }

    public int getSequence() {
        return mSequence;
    }

    public void setSequence(int mSequence) {
        this.mSequence = mSequence;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int mVersion) {
        this.mVersion = mVersion;
    }

    public boolean isIsLive() {
        return mIsLive;
    }

    public void setIsLive(boolean mIsLive) {
        this.mIsLive = mIsLive;
    }

    public void addTs(M3U8Ts ts) {
        mTsList.add(ts);
    }

    public List<M3U8Ts> getTsList() {
        return mTsList;
    }

    public long getDuration() {
        long duration = 0L;
        for (M3U8Ts ts : mTsList) {
            duration += ts.getDuration();
        }
        return duration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof M3U8) {
            M3U8 m3u8 = (M3U8) obj;
            return TextUtils.equals(mUrl, m3u8.getUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
