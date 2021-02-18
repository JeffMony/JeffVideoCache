package com.jeffmony.videocache.task;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.common.VideoParams;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Ts;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.socket.request.ResponseState;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class M3U8CacheTask extends VideoCacheTask {

    private static final String TAG = "M3U8CacheTask";

    private M3U8 mM3U8;
    private int mCachedTs;
    private int mTotalTs;
    private Map<Integer, Long> mTsLengthMap;
    private List<M3U8Ts> mTsList;

    public M3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers, M3U8 m3u8) {
        super(cacheInfo, headers);
        mM3U8 = m3u8;
        mTsList = m3u8.getTsList();
        mTotalTs = cacheInfo.getTotalTs();
        mCachedTs = cacheInfo.getCachedTs();
        mTsLengthMap = cacheInfo.getTsLengthMap();
    }

    @Override
    public void startCacheTask() {
        if (isTaskRunning()) {
            return;
        }
        notifyOnTaskStart();
        initM3U8TsInfo();
        seekToCacheTask(mCachedTs);
    }

    private void initM3U8TsInfo() {
        long tempCachedSize = 0;
        int tempCachedTs = 0;
        for (int index = 0; index < mTsList.size(); index++) {
            M3U8Ts ts = mTsList.get(index);
            File tempTsFile = new File(mSaveDir, ts.getTsName());
            if (tempTsFile.exists() && tempTsFile.length() > 0) {
                ts.setFileSize(tempTsFile.length());
                mTsLengthMap.put(index, tempTsFile.length());
                tempCachedSize += tempTsFile.length();
                tempCachedTs++;
            } else {
                break;
            }
        }
        mCachedTs = tempCachedTs;
        mCachedSize = tempCachedSize;
    }

    @Override
    public void pauseCacheTask() {

    }

    @Override
    public void resumeCacheTask() {

    }

    @Override
    public void seekToCacheTask(long curLength) {
        //M3U8视频可以不用考虑这个
    }

    @Override
    public void seekToCacheTask(int curTs) {
        if (mIsCompleted) {
            notifyOnTaskCompleted();
            return;
        }
        mTaskExecutor = new ThreadPoolExecutor(6, 6,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());

        for (int index = curTs; index < mTotalTs; index++) {
            final M3U8Ts ts = mTsList.get(index);
            final int tsIndex = index;
            mTaskExecutor.execute(() -> {
                try {
                    downloadTsTask(ts, tsIndex);
                } catch (Exception e) {
                    LogUtils.w(TAG, "M3U8 ts video download failed, exception=" + e);
                    notifyOnTaskFailed(e);
                }
            });
        }
    }

    private void downloadTsTask(M3U8Ts ts, int tsIndex) throws Exception {
        String tsName = tsIndex + StorageUtils.TS_SUFFIX;
        File tsFile = new File(mSaveDir, tsName);
        if (!tsFile.exists()) {
            // ts is network resource, download ts file then rename it to local file.
            downloadTsFile(ts, tsFile, 0);
        }

        //确保当前文件下载完整
        if (tsFile.exists() && tsFile.length() == ts.getContentLength()) {
            //只有这样的情况下才能保证当前的ts文件真正被下载下来了
            mTsLengthMap.put(tsIndex, tsFile.length());
            ts.setName(tsName);
            ts.setFileSize(tsFile.length());

            //更新进度

        }
    }

    private void downloadTsFile(M3U8Ts ts, File tsFile, int retryCount) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = HttpUtils.getConnection(ts.getUrl(), mHeaders);
            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseState.OK.getResponseCode() || responseCode == ResponseState.PARTIAL_CONTENT.getResponseCode()) {
                inputStream = connection.getInputStream();
                long contentLength = connection.getContentLength();
                ts.setContentLength(contentLength);
                saveTsFile(inputStream, tsFile, contentLength);
            } else {
                if (retryCount < VideoParams.RETRY_COUNT) {
                    LogUtils.i(TAG, "Download exception, retry it, exception");
                    retryDownloadTsFile(ts, tsFile, retryCount);
                } else {
                    LogUtils.i(TAG, "M3U8 ts video downloadFile code=" + responseCode + ", url=" + ts.getUrl());
                    throw new VideoCacheException("retry download exceed the limit times");
                }

            }
        } catch (Exception e) {
            if (retryCount < VideoParams.RETRY_COUNT) {
                LogUtils.i(TAG, "Download exception, retry it, exception = " + e);
                retryDownloadTsFile(ts, tsFile, retryCount);
            } else {
                LogUtils.w(TAG, "downloadFile failed, exception=" + e.getMessage());
                throw e;
            }
        } finally {
            if (connection != null)
                connection.disconnect();
            ProxyCacheUtils.close(inputStream);
        }
    }

    private void retryDownloadTsFile(M3U8Ts ts, File file, int retryCount) throws Exception  {
        if (isTaskRunning()) {
            if (retryCount < VideoParams.RETRY_COUNT) {
                downloadTsFile(ts, file, retryCount + 1);
            } else {
                throw new VideoCacheException("retry download exceed the limit times");
            }
        } else {
            throw new VideoCacheException("Download thread has been shutdown");
        }
    }

    private void saveTsFile(InputStream inputStream, File file, long contentLength) throws IOException {
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

    private void notifyOnM3U8TaskProgress(float percent, long cachedSize, float speed, Map<Integer, Long> tsLengthMap) {
        mListener.onM3U8TaskProgress(percent, cachedSize, speed, tsLengthMap);
    }
}
