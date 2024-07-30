package com.jeffmony.videocache.task;


import com.coolerfall.download.DownloadCallback;
import com.coolerfall.download.DownloadManager;
import com.coolerfall.download.DownloadRequest;
import com.coolerfall.download.Downloader;
import com.coolerfall.download.OkHttpDownloader;
import com.coolerfall.download.URLDownloader;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * to download ts video,etc.
 */
public class FileDownloadManager {
    private static final String TAG = "FileDownloadManager";
    private static final class InstanceHolder {
        static final FileDownloadManager sInstance = new FileDownloadManager();
    }
    private final DownloadManager mDownloader;

    public static FileDownloadManager getInstance() {
        return InstanceHolder.sInstance;
    }

    private FileDownloadManager() {
        Downloader downloader;
        if (ProxyCacheUtils.getConfig().useOkHttp()) {
            downloader = OkHttpDownloader.create();
        } else {
            downloader = URLDownloader.create();
        }
        mDownloader = new DownloadManager.Builder().
                context(ProxyCacheUtils.getConfig().getContext()).
                downloader(downloader).
                threadPoolSize(Runtime.getRuntime().availableProcessors() * 2 + 1). //io密集型
                logger(message -> LogUtils.i(TAG, message)).
                build();
    }

    public boolean isDownloading(int downloadId) {
        return mDownloader.isDownloading(downloadId);
    }

    public boolean isDownloading(String url) {
        return mDownloader.isDownloading(url);
    }

    /**
     *
     * @param file
     * @param url
     * @param downloadCallback
     * @return download id, if the id is not set, then manager will generate one. if the request is in downloading, then -1 will be returned
     */
    public int addTsVideoTask(File file, String url, DownloadCallback downloadCallback) {
        DownloadRequest request = new DownloadRequest.Builder()
                .destinationFilePath(file.getAbsolutePath())
                .downloadCallback(downloadCallback)
                //.downloadId(tsIndex) //确保唯一性；如果不设置，sdk的downloadId是自增的;不设置，防止冲突
                .retryTime(2)
                //.allowedNetworkTypes(DownloadRequest.NETWORK_WIFI | DownloadRequest.NETWORK_MOBILE)
                .progressInterval(1000, TimeUnit.MILLISECONDS)
                .url(url) // fixme header inject
                .build();
        return mDownloader.add(request);
    }

    public void cancelTask(int downloadId) {
        mDownloader.cancel(downloadId);
    }

    public void release() {
        //单例，不要退出
        mDownloader.release();
    }
}
