package com.jeffmony.videocache.task;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Seg;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class M3U8CacheTask extends VideoCacheTask {

    private static final String TAG = "M3U8CacheTask";

    private static final int THREAD_POOL_COUNT = 6;
    private static final int CONTINUOUS_SUCCESS_TS_THRESHOLD = 6;
    private volatile int mM3U8DownloadPoolCount;
    private volatile int mContinuousSuccessSegCount;   //连续请求分片成功的个数

    private int mCachedSegCount;
    private int mTotalSegCount;
    private Map<Integer, Long> mSegLengthMap;
    private List<M3U8Seg> mSegList;

    public M3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers, M3U8 m3u8) {
        super(cacheInfo, headers);
        mSegList = m3u8.getSegList();
        mTotalSegCount = cacheInfo.getTotalTs();
        mCachedSegCount = cacheInfo.getCachedTs();
        mSegLengthMap = cacheInfo.getTsLengthMap();
        if (mSegLengthMap == null) {
            mSegLengthMap = new HashMap<>();
        }

        mHeaders.put("Connection", "close");
    }

    @Override
    public void startCacheTask() {
        if (isTaskRunning()) {
            return;
        }
        notifyOnTaskStart();
        initM3U8TsInfo();
        int seekIndex = mCachedSegCount > 1 && mCachedSegCount <= mTotalSegCount ? mCachedSegCount - 1 : mCachedSegCount;
        seekToCacheTask(seekIndex);
    }

    private void initM3U8TsInfo() {
        long tempCachedSize = 0;
        int tempCachedTs = 0;
        for (int index = 0; index < mSegList.size(); index++) {
            M3U8Seg ts = mSegList.get(index);
            File tempTsFile = new File(mSaveDir, ts.getTsName());
            if (tempTsFile.exists() && tempTsFile.length() > 0) {
                ts.setFileSize(tempTsFile.length());
                mSegLengthMap.put(index, tempTsFile.length());
                tempCachedSize += tempTsFile.length();
                tempCachedTs++;
            } else {
                break;
            }
        }
        mCachedSegCount = tempCachedTs;
        mCachedSize = tempCachedSize;
    }

    @Override
    public void pauseCacheTask() {
        if (mTaskExecutor != null && !mTaskExecutor.isShutdown()) {
            mTaskExecutor.shutdownNow();
        }
    }

    @Override
    public void stopCacheTask() {
        pauseCacheTask();
    }

    @Override
    public void resumeCacheTask() {

    }

    @Override
    public void seekToCacheTask(float percent) {
        int seekTsIndex = (int)(percent * mTotalSegCount);
        seekToCacheTask(seekTsIndex);
    }

    private void seekToCacheTask(int curTs) {
        if (mCacheInfo.isCompleted()) {
            notifyOnTaskCompleted();
            return;
        }
        if (mTaskExecutor != null && !mTaskExecutor.isShutdown()) {
            //已经存在的任务不需要重新创建了
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(THREAD_POOL_COUNT, THREAD_POOL_COUNT, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        for (int index = curTs; index < mTotalSegCount; index++) {
            final M3U8Seg seg = mSegList.get(index);
            final int tsIndex = index;
            mTaskExecutor.execute(() -> {
                try {
                    startDownloadSegTask(seg, tsIndex);
                } catch (Exception e) {
                    LogUtils.w(TAG, "M3U8 ts video download failed, exception=" + e);
                    notifyOnTaskFailed(e);
                }
            });
        }
    }

    private void startDownloadSegTask(M3U8Seg seg, int tsIndex) throws Exception {
        String tsName = tsIndex + StorageUtils.TS_SUFFIX;
        File tsFile = new File(mSaveDir, tsName);
        if (!tsFile.exists()) {
            // ts is network resource, download ts file then rename it to local file.
            downloadSegFile(seg, tsFile);
        }

        //确保当前文件下载完整
        if (tsFile.exists() && tsFile.length() == seg.getContentLength()) {
            //只有这样的情况下才能保证当前的ts文件真正被下载下来了
            mSegLengthMap.put(tsIndex, tsFile.length());
            seg.setName(tsName);
            seg.setFileSize(tsFile.length());
            //更新进度
            notifyCacheProgress();
        }
    }

    private void downloadSegFile(M3U8Seg seg, File segFile) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = HttpUtils.getConnection(seg.getUrl(), mHeaders);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpUtils.RESPONSE_200 || responseCode == HttpUtils.RESPONSE_206) {
                seg.setRetryCount(0);
                if (mContinuousSuccessSegCount > CONTINUOUS_SUCCESS_TS_THRESHOLD && mM3U8DownloadPoolCount < THREAD_POOL_COUNT) {
                    mM3U8DownloadPoolCount += 1;
                    mContinuousSuccessSegCount -= 1;
                    setThreadPoolArgument(mM3U8DownloadPoolCount, mM3U8DownloadPoolCount);
                }
                inputStream = connection.getInputStream();
                long contentLength = connection.getContentLength();
                seg.setContentLength(contentLength);
                saveSegFile(inputStream, segFile, contentLength);
            } else {
                mContinuousSuccessSegCount = 0;
                if (responseCode == HttpUtils.RESPONSE_503) {
                    if (mM3U8DownloadPoolCount > 1) {
                        mM3U8DownloadPoolCount -= 1;
                        setThreadPoolArgument(mM3U8DownloadPoolCount, mM3U8DownloadPoolCount);
                        downloadSegFile(seg, segFile);
                    } else {
                        seg.setRetryCount(seg.getRetryCount() + 1);
                        if (seg.getRetryCount() < HttpUtils.MAX_RETRY_COUNT) {
                            downloadSegFile(seg, segFile);
                        } else {
                            throw new VideoCacheException("retry download exceed the limit times, threadPool overload.");
                        }
                    }
                } else {
                    throw new VideoCacheException("download failed, responseCode=" + responseCode);
                }
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "downloadFile failed, exception=" + e.getMessage());
            throw e;
        } finally {
            HttpUtils.closeConnection(connection);
            ProxyCacheUtils.close(inputStream);
        }
    }

    private void saveSegFile(InputStream inputStream, File file, long contentLength) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            int len;
            byte[] buf = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
            while ((len = inputStream.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        } catch (IOException e) {
            LogUtils.w(TAG,file.getAbsolutePath() + " saveFile failed, exception=" + e);
            if (file.exists() && contentLength > 0 && contentLength == file.length()) {
                //这时候说明file已经下载完成了
            } else {
                file.delete();
            }
            throw e;
        } finally {
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(fos);
        }
    }

    private void notifyCacheProgress() {
        updateM3U8TsInfo();
        if (mCachedSegCount > mTotalSegCount) {
            mCachedSegCount = mTotalSegCount;
        }
        mCacheInfo.setCachedTs(mCachedSegCount);
        mCacheInfo.setTsLengthMap(mSegLengthMap);
        mCacheInfo.setCachedSize(mCachedSize);
        float percent = mCachedSegCount * 1.0f * 100 / mTotalSegCount;

        if (!ProxyCacheUtils.isFloatEqual(percent, mPercent)) {
            long nowTime = System.currentTimeMillis();
            if (mCachedSize > mLastCachedSize && nowTime > mLastInvokeTime) {
                mSpeed = (mCachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
            }
            mListener.onM3U8TaskProgress(percent, mCachedSize, mSpeed, mSegLengthMap);
            mPercent = percent;
            mCacheInfo.setPercent(percent);
            mCacheInfo.setSpeed(mSpeed);
            mLastInvokeTime = nowTime;
            mLastCachedSize = mCachedSize;
            saveVideoInfo();
        }

        boolean isCompleted = true;
        for (M3U8Seg ts : mSegList) {
            File tsFile = new File(mSaveDir, ts.getTsName());
            if (!tsFile.exists()) {
                isCompleted = false;
                break;
            }
        }
        mCacheInfo.setIsCompleted(isCompleted);
        if (isCompleted) {
            mCacheInfo.setTotalSize(mCachedSize);
            mTotalSize = mCachedSize;
            notifyOnTaskCompleted();
            saveVideoInfo();
        }
    }

    private void updateM3U8TsInfo() {
        long tempCachedSize = 0;
        int tempCachedTs = 0;
        for (int index = 0; index < mSegList.size(); index++) {
            M3U8Seg ts = mSegList.get(index);
            File tempTsFile = new File(mSaveDir, ts.getTsName());
            if (tempTsFile.exists() && tempTsFile.length() > 0) {
                ts.setFileSize(tempTsFile.length());
                mSegLengthMap.put(index, tempTsFile.length());
                tempCachedSize += tempTsFile.length();
                tempCachedTs++;
            }
        }
        mCachedSegCount = tempCachedTs;
        mCachedSize = tempCachedSize;
    }
}
