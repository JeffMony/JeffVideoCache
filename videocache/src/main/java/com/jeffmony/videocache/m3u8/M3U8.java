package com.jeffmony.videocache.m3u8;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jeffmony
 *
 * M3U8文件结构
 */
public class M3U8 {
    private final String mUrl;                 //M3U8的url
    private float mTargetDuration;       //指定的duration
    private float mTotalDuration; //总时长
    private int mSequence = 0;           //序列起始值
    private int mVersion = 3;            //版本号
    private boolean mIsLive;             //是否是直播
    private final List<M3U8Seg> mSegList;      //分片seg 列表

    private long estimateSize = 0; //预估总大小

    private long needLeastSize = 0; //要求设备留出最低的可用空间

    public M3U8(String url) {
        mUrl = url;
        mSegList = new ArrayList<>();
    }

    public float getDuration() {
        return mTotalDuration;
    }

    public void setDuration(float mTotalDuration) {
        this.mTotalDuration = mTotalDuration;
    }

    public long getEstimateSize() {
        return estimateSize;
    }

    public void setEstimateSize(long estimateSize) {
        this.estimateSize = estimateSize;
    }

    public long getNeedLeastSize() {
        return needLeastSize;
    }

    public void setNeedLeastSize(long needLeastSize) {
        this.needLeastSize = needLeastSize;
    }

    public String getUrl() {
        return mUrl;
    }

    public float getTargetDuration() {
        return mTargetDuration;
    }

    public void setTargetDuration(float targetDuration) {
        mTargetDuration = targetDuration;
    }

    public int getSequence() {
        return mSequence;
    }

    public void setSequence(int sequence) {
        mSequence = sequence;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        mVersion = version;
    }

    public boolean isIsLive() {
        return mIsLive;
    }

    public void setIsLive(boolean isLive) {
        mIsLive = isLive;
    }

    public void addSeg(M3U8Seg ts) {
        mSegList.add(ts);
    }

    public List<M3U8Seg> getSegList() {
        return mSegList;
    }

    public int getSegCount() {
        return mSegList.size();
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
