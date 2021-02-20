package com.jeffmony.videocache.task;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.listener.IVideoCacheTaskListener;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.utils.StorageUtils;
import com.jeffmony.videocache.utils.VideoProxyThreadUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class VideoCacheTask {

    protected VideoCacheInfo mCacheInfo;
    protected Map<String, String> mHeaders;
    protected IVideoCacheTaskListener mListener;
    protected ThreadPoolExecutor mTaskExecutor;

    protected long mCachedSize;      //当前缓存大小
    protected long mLastCachedSize;  //上一次缓存大小
    protected long mTotalSize;
    protected long mLastInvokeTime;
    protected float mPercent = 0.0f;
    protected float mSpeed = 0.0f;
    protected File mSaveDir;

    public VideoCacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        mCacheInfo = cacheInfo;
        mHeaders = headers;
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mCachedSize = cacheInfo.getCachedSize();
        mTotalSize = cacheInfo.getTotalSize();
        mSaveDir = new File(cacheInfo.getSavePath());
        if (!mSaveDir.exists()) {
            mSaveDir.mkdir();
        }
    }

    public void setTaskListener(@NonNull IVideoCacheTaskListener listener) {
        mListener = listener;
    }

    public abstract void startCacheTask();

    public abstract void pauseCacheTask();

    public abstract void seekToCacheTask(float percent);

    public abstract void resumeCacheTask();

    protected void notifyOnTaskStart() {
        mListener.onTaskStart();
    }

    protected void notifyOnTaskFailed(Exception e) {
        mListener.onTaskFailed(e);
    }

    protected void notifyOnTaskCompleted() {
        mListener.onTaskCompleted(mTotalSize);
    }

    protected boolean isTaskRunning() {
        return mTaskExecutor != null && !mTaskExecutor.isShutdown();
    }

    protected void saveVideoInfo() {
        VideoProxyThreadUtils.submitRunnableTask(() -> StorageUtils.saveVideoCacheInfo(mCacheInfo, mSaveDir));
    }
}
