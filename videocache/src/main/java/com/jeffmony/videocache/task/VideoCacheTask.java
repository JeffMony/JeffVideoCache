package com.jeffmony.videocache.task;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.listener.IVideoCacheTaskListener;
import com.jeffmony.videocache.model.VideoCacheInfo;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class VideoCacheTask {

    protected VideoCacheInfo mCacheInfo;
    protected Map<String, String> mHeaders;
    protected IVideoCacheTaskListener mListener;
    protected ThreadPoolExecutor mTaskExecutor;

    protected long mCachedSize;
    protected long mTotalSize;
    protected boolean mIsCompleted;

    public VideoCacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        mCacheInfo = cacheInfo;
        mHeaders = headers;
        mCachedSize = cacheInfo.getCachedSize();
        mTotalSize = cacheInfo.getTotalSize();
        mIsCompleted = cacheInfo.isCompleted();
    }

    public void setTaskListener(@NonNull IVideoCacheTaskListener listener) {
        mListener = listener;
    }

    public abstract void startCacheTask();

    public abstract void pauseCacheTask();

    public abstract void resumeCacheTask();


    protected void notifyOnTaskStart() {
        mListener.onTaskStart();
    }

    protected void notifyOnTaskFailed(Exception e) {
        mListener.onTaskFailed(e);
    }

    protected void notifyOnTaskCompleted() {
        mListener.onTaskCompleted();
    }
}
