package com.jeffmony.videocache.task;

import android.os.Handler;
import android.os.SystemClock;

import com.coolerfall.download.DownloadCallback;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Seg;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class M3U8CacheTaskNew extends VideoCacheTask {

    private static final String TAG = "M3U8CacheTaskNew";
    private final int mTotalSegCount;
    private final List<M3U8Seg> mSegList;
    private final FileDownloadManager mFileDownloadManager;
    private final int M3U8_CACHE_STEP = 1; //初始任务数，后续会通过网络情况进行调整

    //存放正在下载的ts编号(key)和任务id(value)，避免产生不同downloadId但url相同的任务。ts编号: 负代表是EXT-X-MAP,特别地，Integer.MIN_VALUE代表-0
    private final Map<Integer, Integer> mDownLoadIdMap = new HashMap<>();
    private final List<Integer> mSegIndexUncachedList = new ArrayList<>(); //存放还没有缓存的ts编号
    private int mRightCachingPos = 0; //mSeekIndex右边的任务，优先下载，指向mSegIndexUncachedList
    private final Handler mHandler;
    private int mCachedSegCount;
    private volatile int mSeekIndex = 0; //当前正在请求的ts索引
    private int downloadTask = M3U8_CACHE_STEP; //同时下载任务数
    private float maxSpeed = 0;
    private float minSpeed = 0;
    private int expectedSeekIndex = 0;

    private volatile boolean initFlag = false;


    public M3U8CacheTaskNew(VideoCacheInfo cacheInfo, M3U8 m3u8, final Handler handler) {
        super(cacheInfo, null);
//        HandlerThread handlerThread = new HandlerThread("M3U8CacheTask");
//        handlerThread.start();
        mHandler = new Handler(handler.getLooper());
        mFileDownloadManager = FileDownloadManager.getInstance();
        mSegList = m3u8.getSegList();
        mTotalSegCount = m3u8.getSegCount();
        mCachedSegCount = cacheInfo.getCachedTs();
        StorageUtils.saveVideoCacheInfo(mCacheInfo, mSaveDir); //只更新一次
    }

    @Override
    public void startCacheTask() {
        if (initFlag) {
            return;
        }
        initFlag = true;
        isStart = true;
        //避免下载成功回调过快导致scheduleCacheTask和addCacheTask线程不安全
        mHandler.post(() -> {
            //第一个任务必须是能下载的，后续任务都靠onSuccess驱动
            init();
            scheduleCacheTask(M3U8_CACHE_STEP);
            notifyOnTaskStart();
        });
    }

    @Override
    public void pauseCacheTask() {
        LogUtils.i(TAG, "pauseCacheTask");
        isStart = false;
        mHandler.post(this::cancelAllTask);
    }

    @Override
    public void stopCacheTask() {
        LogUtils.i(TAG, "start stopCacheTask");
        isStart = false;
        mHandler.post(() -> {
            LogUtils.i(TAG, "execute stopCacheTask");
            resetCacheTask();
            mHandler.removeCallbacksAndMessages(null);
        });
    }

    @Override
    public void resumeCacheTask() {
        //mPlayer.start();
        LogUtils.i(TAG, "resumeCacheTask");
        if (isStart) {
            return;
        }
        isStart = true;
        mHandler.post(() -> {
            int success = 0;
            if (!mDownLoadIdMap.isEmpty()) {
                //to avoid java.util.ConcurrentModificationException
                List<Integer> tmpList = new ArrayList<>(mDownLoadIdMap.size());
                tmpList.addAll(mDownLoadIdMap.keySet());
                mDownLoadIdMap.clear();
                for (Integer value: tmpList) {
                    success += startDownloadSegTask(mSegList.get(value != Integer.MIN_VALUE ? Math.abs(value) : 0));
                }
            }
            if (success == 0 && !mSegIndexUncachedList.isEmpty()) {
                scheduleCacheTask(M3U8_CACHE_STEP);
            }
        });
    }

    @Override
    public void seekToCacheTaskFromClient(float percent) {
    }

    @Override
    public void seekToCacheTaskFromServer(long startPosition) {
    }

    @Override
    public void seekToCacheTaskFromServer(int segIndex) {}

    @Override
    public void seekToCacheTaskFromServer(int segIndex, long time) {
        //来自于外部线程调用
        mHandler.post(() -> {
            if (ProxyCacheUtils.getSocketTime() != time) {
                //解决由于线程调度问题，可能旧的请求后于新的请求执行，这样子就乱套了，不过概率应该比较小
                LogUtils.e(TAG, "seekToCacheTaskFromServer: out of date:" + segIndex);
                return;
            }
            mSeekIndex = segIndex;
            LogUtils.i(TAG, "seekToCacheTaskFromServer segIndex=" + mSeekIndex + " expectedSeekIndex:" + expectedSeekIndex);
            //可能会有重复请求
            if (mSeekIndex == expectedSeekIndex || mSeekIndex == expectedSeekIndex - 1) {
                expectedSeekIndex = mSeekIndex + 1;
                return;
            }
            //将缓存指针移动到seek的后面，保障用户体验
            LogUtils.i(TAG, "seekToCacheTaskFromServer, user seek, scheduleCacheTask");
            expectedSeekIndex = mSeekIndex + 1;
            resetCacheTask();
            scheduleCacheTask(M3U8_CACHE_STEP);
        });
    }

    private final DownloadCallback mCallback = new DownloadCallback() {
        @Override
        public void onStart(int downloadId, long totalBytes) {

        }

        @Override
        public void onRetry(int downloadId) {

        }

        @Override
        public void onProgress(int downloadId, long bytesWritten, long totalBytes) {

        }

        @Override
        public void onSuccess(int downloadId, String filePath, long totalBytes, long time) {
            //更新进度,子线程回调
            //mHandler不能做耗时操作，否则会影响消息处理速度
            mHandler.post(() -> {
                long start = SystemClock.uptimeMillis();
                float speed = ((float) totalBytes / 1024f) / ((float) time / 1000) ; //kb/s
                Integer index = null;
                Iterator<Map.Entry<Integer, Integer>> iterator = mDownLoadIdMap.entrySet().iterator();
                Map.Entry<Integer, Integer> entry;
                while (iterator.hasNext()) {
                    entry = iterator.next();
                    if (entry.getValue() == downloadId) {
                        index = entry.getKey();
                        iterator.remove();
                        break;
                    }
                }
                if (index != null && index >= 0) {
                    mCachedSegCount++;
                    mCachedSize += totalBytes;
                    //动态调整，适应网络情况
                    if (mDownLoadIdMap.isEmpty()) {
                        maxSpeed = 0;
                        minSpeed = 0;
                    }
                    maxSpeed = Math.max(speed, maxSpeed);
                    minSpeed = Math.min(speed, minSpeed == 0 ? speed : minSpeed);
                }
                int tsIndex = -1;
                if (index != null) {
                    tsIndex = index != Integer.MIN_VALUE ? Math.abs(index) : 0;
                    int shadow = index != 0 && index != Integer.MIN_VALUE ? -index : (index == 0 ? Integer.MIN_VALUE : 0);
                    if (mDownLoadIdMap.get(shadow) == null) {
                        mSegIndexUncachedList.remove(Integer.valueOf(tsIndex));
                    }
                }
                //cdn一般会比较慢，多个文件同时下载可能会限制，需要动态调整
                if (tsIndex >= 0) {
                    M3U8Seg seg = mSegList.get(tsIndex);
                    float midPoint = (maxSpeed + minSpeed) / 2;
                    LogUtils.d(TAG, "onSuccess: speed:"+ speed + "KB/S,maxSpeed:" + maxSpeed + "KB/S" + ",minSpeed:" + minSpeed + "KB/S"
                            + "\n,(maxSpeed + minSpeed)/2:" + midPoint + "KB/s" + ",downloadTask:" + downloadTask
                            + "\n,filePath:" + filePath
                            + "\n,seg duration:" + seg.getDuration()
                            + "\n,total Size:" + totalBytes / 1024f + "KB,time:" + time/1000f + "S,mDownLoadIdList size:" + mDownLoadIdMap.size());
                    //如果下载速度满足播放器消费情况，则串行下载
                    if ((float) time / 1000 <= seg.getDuration()) {
                        //do nothing
                    } else if (speed < midPoint && ((midPoint - speed) / midPoint) < 0.4) {
                        //do noting
                    } else if (speed >= midPoint) {
                        //同时允许最大任务数为3个，太多可能拖慢播放器正在请求的ts
                        downloadTask = downloadTask < 3 ? downloadTask + 1 : downloadTask;
                    } else if (speed < midPoint) {
                        downloadTask = downloadTask > 1 ? downloadTask - 1 : downloadTask;
                    }
                }
                if (mDownLoadIdMap.size() < downloadTask) {
                    scheduleCacheTask(downloadTask - mDownLoadIdMap.size());
                }
                LogUtils.d(TAG, "onSuccess scheduleCacheTask, mDownLoadIdList size:" + mDownLoadIdMap.size() + ",cost:" + (SystemClock.uptimeMillis() - start));
                notifyCacheProgress();
                LogUtils.d(TAG, "onSuccess total cost:" + (SystemClock.uptimeMillis() - start));
            });
        }

        @Override
        public void onFailure(int downloadId, int statusCode, String errMsg) {
            notifyOnTaskFailed(new Exception("downloadId:" + downloadId + "statusCode:" + statusCode + errMsg));
        }
    };

    private void init() {
        //如果ts过多，遍历会耗时
        int tempCachedTs = 0;
        long tempCachedSize = 0;
        for (int index = 0; index < mSegList.size(); index++) {
            M3U8Seg ts = mSegList.get(index);
            File tempTsFile = new File(mSaveDir, ts.getSegName());
            boolean hasFinished = true;
            if (tempTsFile.exists() && tempTsFile.length() > 0) {
                tempCachedSize += tempTsFile.length();
                tempCachedTs++;
            } else {
                hasFinished = false;
            }
            if (hasFinished && ts.hasInitSegment()) {
                String initSegmentName = ts.getInitSegmentName();
                File initSegmentFile = new File(mSaveDir, initSegmentName);
                if (initSegmentFile.exists() && initSegmentFile.length() > 0) {
                    //ignore
                } else {
                    hasFinished = false;
                }
            }
            if (!hasFinished) {
                mSegIndexUncachedList.add(index);
            }
        }
        mCachedSegCount = tempCachedTs;
        mCachedSize = tempCachedSize;
        mTotalSize = mCachedSize;
        mCacheInfo.setIsCompleted(mSegIndexUncachedList.isEmpty());
    }

    private void resetCacheTask() {
        cancelAllTask();
        mDownLoadIdMap.clear();
        mRightCachingPos = 0;
        mLastInvokeTime = 0;
    }

    private void cancelAllTask() {
        for (int value : mDownLoadIdMap.values()) {
            mFileDownloadManager.cancelTask(value);
        }
    }

    private void scheduleCacheTask(int m3u8CacheStep) {
        if (mSegIndexUncachedList.isEmpty()) {
            notifyOnTaskCompleted();
            return;
        }
        int success = 0;
        //mSegIndexUncachedList会动态变化，但0到mRightCachingPos这部分元素下标是稳定的
        for (int i = mRightCachingPos; i < mSegIndexUncachedList.size(); i++) {
            int sgeIndex = mSegIndexUncachedList.get(i);
            LogUtils.d(TAG, "scheduleCacheTask sgeIndex:" + sgeIndex + ",mSeekIndex:" + mSeekIndex + ",mRightCachingPos:" + mRightCachingPos);
            if (sgeIndex < mSeekIndex) {
                LogUtils.i(TAG, "scheduleCacheTask ignore:" + sgeIndex + ", mSeekIndex=" + mSeekIndex);
                mRightCachingPos = i;
                continue;
            }
            final M3U8Seg seg = mSegList.get(sgeIndex);
            success += startDownloadSegTask(seg);
            if (success >= m3u8CacheStep) {
                return;
            }
        }
        //如果mSeekIndex之后都是已经缓存的，则缓存之前的任务
        for (int i = 0; i < mSegIndexUncachedList.size(); i++) {
            int sgeIndex = mSegIndexUncachedList.get(i);
            if (sgeIndex >= mSeekIndex) {
                return;
            }
            final M3U8Seg seg = mSegList.get(sgeIndex);
            success += startDownloadSegTask(seg);
            if (success >= m3u8CacheStep) {
                return;
            }
        }
    }

    private int startDownloadSegTask(M3U8Seg seg) {
        LogUtils.i(TAG, "startDownloadSegTask index=" + seg.getSegIndex() +", duration="+ seg.getDuration() + ", url=" + seg.getUrl());
        int result = 0;
        if (seg.hasInitSegment()) {
            String initSegmentName = seg.getInitSegmentName();
            File initSegmentFile = new File(mSaveDir, initSegmentName);
            if (!initSegmentFile.exists()) {
                int ret = mFileDownloadManager.addTsVideoTask(initSegmentFile, seg.getInitSegmentUri(), mCallback);
                if (ret != -1) {
                    mDownLoadIdMap.put(seg.getSegIndex() > 0 ? -seg.getSegIndex() : Integer.MIN_VALUE, ret); //用Integer.MIN_VALUE标识-0
                    result++;
                }
            }
        }
        String segName = seg.getSegName();
        File segFile = new File(mSaveDir, segName);
        if (!segFile.exists()) {
            // ts is network resource, download ts file then rename it to local file.
            //downloadSegFile(seg, segFile, seg.getUrl());
            int ret = mFileDownloadManager.addTsVideoTask(segFile, seg.getUrl(), mCallback);
            if (ret != -1) {
                mDownLoadIdMap.put(seg.getSegIndex(), ret);
                result++;
            }
        }
        return result;
    }

    private void notifyCacheProgress() {
        long nowTime = SystemClock.uptimeMillis();
        //1s更新一次
        if (nowTime - mLastInvokeTime <= 1000 && !mSegIndexUncachedList.isEmpty()) {
            return;
        }
        if (mCachedSegCount > mTotalSegCount) {
            mCachedSegCount = mTotalSegCount;
        }
        mCacheInfo.setCachedTs(mCachedSegCount);
        mCacheInfo.setCachedSize(mCachedSize);
        float percent = mCachedSegCount * 1.0f * 100 / mTotalSegCount;

        if (!ProxyCacheUtils.isFloatEqual(percent, mPercent)) {
            if (mCachedSize > mLastCachedSize) {
                mSpeed = (mCachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
            }
            mListener.onM3U8TaskProgress(percent, mCachedSize, mSpeed);
            mPercent = percent;
            mCacheInfo.setPercent(percent);
            mCacheInfo.setSpeed(mSpeed);
            mLastCachedSize = mCachedSize;
        }
        mLastInvokeTime = nowTime;
        boolean isCompleted = mSegIndexUncachedList.isEmpty();
        if (isCompleted) {
            mCacheInfo.setIsCompleted(true);
            mCacheInfo.setTotalSize(mCachedSize);
            mTotalSize = mCachedSize;
            notifyOnTaskCompleted();
        }
        LogUtils.d(TAG, "notifyCacheProgress cost:"  + (SystemClock.uptimeMillis() - nowTime));
    }
}
