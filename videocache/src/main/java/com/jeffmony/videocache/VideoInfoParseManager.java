package com.jeffmony.videocache;

import android.net.Uri;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.common.VideoRequest;
import com.jeffmony.videocache.common.VideoType;
import com.jeffmony.videocache.common.VideoParams;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Seg;
import com.jeffmony.videocache.m3u8.M3U8Utils;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.okhttp.OkHttpManager;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.net.HttpURLConnection;

/**
 * @author jeffmony
 * 解析视频类型信息的工具类
 */
public class VideoInfoParseManager {

    private static final String TAG = "VideoInfoParseManager";

    private static volatile VideoInfoParseManager sInstance = null;

    public static VideoInfoParseManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoInfoParseManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoInfoParseManager();
                }
            }
        }
        return sInstance;
    }

    @WorkerThread
    //使用okhttp网路框架去请求数据
    public void parseVideoInfoByOkHttp(VideoRequest videoRequest, VideoCacheInfo cacheInfo) {
        if (TextUtils.equals(VideoParams.UNKNOWN, videoRequest.getContentType())) {
            String videoUrl = cacheInfo.getVideoUrl();
            if (videoUrl.contains("m3u8")) {
                //这种情况下也基本可以认为video是M3U8类型，虽然判断不太严谨，但是mediaplayer也是这么做的
                parseNetworkM3U8Info(videoRequest, cacheInfo);
            } else {
                Uri videoUri = Uri.parse(videoUrl);
                String fileName = videoUri.getLastPathSegment();
                if (!TextUtils.isEmpty(fileName)) {
                    fileName = fileName.toLowerCase();
                    if (fileName.endsWith(".m3u8")) {
                        //当前是M3U8类型
                        parseNetworkM3U8Info(videoRequest, cacheInfo);
                    } else {
                        //不是M3U8类型，说明是整视频
                        parseNonM3U8VideoInfoByOkHttp(videoRequest, cacheInfo);
                    }
                } else {
                    //需要发起请求判定当前视频的类型
                    String contentType;
                    try {
                        contentType = OkHttpManager.getInstance().getContentType(videoUrl, videoRequest.getHeaders());
                    } catch (VideoCacheException e) {
                        videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(e, cacheInfo);
                        return;
                    }

                    if (TextUtils.isEmpty(contentType)) {
                        videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(new VideoCacheException("ContentType is null"), cacheInfo);
                    } else {
                        contentType = contentType.toLowerCase();
                        if (ProxyCacheUtils.isM3U8Mimetype(contentType)) {
                            //当前是M3U8类型
                            parseNetworkM3U8Info(videoRequest, cacheInfo);
                        } else {
                            parseNonM3U8VideoInfoByOkHttp(videoRequest, cacheInfo);
                        }
                    }
                }
            }
        } else {
            if (ProxyCacheUtils.isM3U8Mimetype(videoRequest.getContentType())) {
                //当前是M3U8类型
                parseNetworkM3U8Info(videoRequest, cacheInfo);
            } else {
                //不是M3U8类型，说明是整视频
                parseNonM3U8VideoInfoByOkHttp(videoRequest, cacheInfo);
            }
        }
    }

    //使用android原生的HttpURLConnection发起请求
    @WorkerThread
    public void parseVideoInfo(VideoRequest videoRequest, VideoCacheInfo cacheInfo) {
        if (TextUtils.equals(VideoParams.UNKNOWN, videoRequest.getContentType())) {
            String videoUrl = cacheInfo.getVideoUrl();
            if (videoUrl.contains("m3u8")) {
                //这种情况下也基本可以认为video是M3U8类型，虽然判断不太严谨，但是mediaplayer也是这么做的
                parseNetworkM3U8Info(videoRequest, cacheInfo);
            } else {
                Uri videoUri = Uri.parse(videoUrl);
                String fileName = videoUri.getLastPathSegment();
                if (!TextUtils.isEmpty(fileName)) {
                    fileName = fileName.toLowerCase();
                    if (fileName.endsWith(".m3u8")) {
                        //当前是M3U8类型
                        parseNetworkM3U8Info(videoRequest, cacheInfo);
                    } else {
                        //不是M3U8类型，说明是整视频
                        parseNonM3U8VideoInfo(videoRequest, cacheInfo);
                    }
                } else {
                    //需要发起请求判定当前视频的类型
                    HttpURLConnection connection = null;
                    try {
                        connection = HttpUtils.getConnection(cacheInfo.getVideoUrl(), videoRequest.getHeaders());
                        String contentType = connection.getContentType();
                        if (ProxyCacheUtils.isM3U8Mimetype(contentType)) {
                            //当前是M3U8类型
                            parseNetworkM3U8Info(videoRequest, cacheInfo);
                        } else {
                            //不是M3U8类型，说明是整视频
                            parseNonM3U8VideoInfo(videoRequest, cacheInfo, connection);
                        }
                    } catch (Exception e) {
                        videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(new VideoCacheException(e.getMessage()), cacheInfo);
                    } finally {
                        HttpUtils.closeConnection(connection);
                    }
                }
            }
        } else {
            if (ProxyCacheUtils.isM3U8Mimetype(videoRequest.getContentType())) {
                //当前是M3U8类型
                parseNetworkM3U8Info(videoRequest, cacheInfo);
            } else {
                //不是M3U8类型，说明是整视频
                parseNonM3U8VideoInfo(videoRequest, cacheInfo);
            }
        }
    }

    @WorkerThread
    public void parseProxyM3U8Info(VideoRequest videoRequest, VideoCacheInfo cacheInfo) {
        File proxyM3U8File = new File(cacheInfo.getSavePath(), cacheInfo.getMd5() + StorageUtils.PROXY_M3U8_SUFFIX);
        if (!proxyM3U8File.exists()) {
            parseNetworkM3U8Info(videoRequest, cacheInfo);
        } else {
            File localM3U8File = new File(cacheInfo.getSavePath(), cacheInfo.getMd5() + StorageUtils.LOCAL_M3U8_SUFFIX);
            try {
                M3U8 m3u8 = M3U8Utils.parseLocalM3U8Info(localM3U8File, cacheInfo.getVideoUrl());
                cacheInfo.setTotalTs(m3u8.getSegCount());
                //todo:可以像芒果tv那样自定义字段记录信息，不用再网络请求
                //这里先不抛出空间不足提示，因为还要计算已经下载大小,推迟到task抛出
                M3U8Seg seg = m3u8.getSegList().get(m3u8.getSegCount() / 2); //随机抽取
                HttpURLConnection connection = HttpUtils.getHeadConnection(seg.getUrl(), null);
                long segSize = connection.getContentLength();
                if (segSize != -1) {
                    m3u8.setEstimateSize((long) (segSize / seg.getDuration() * m3u8.getDuration()));
                    m3u8.setNeedLeastSize(seg.getDuration() < 5 ? segSize * 20 : segSize * 10);
                } else {
                    m3u8.setNeedLeastSize(100 * 1024 * 1024); //100MB
                    m3u8.setEstimateSize(1024 * 1024 * 1024); //1GB
                }
                videoRequest.getVideoInfoParsedListener().onM3U8ParsedFinished(videoRequest, m3u8, cacheInfo);
            } catch (Exception e) {
                LogUtils.e(TAG, "parseProxyM3U8Info error.", e);
                videoRequest.getVideoInfoParsedListener().onM3U8ParsedFailed(new VideoCacheException("parseLocalM3U8Info failed", e), cacheInfo);
            }
        }
    }

    /**
     * 解析M3U8视频信息
     *
     * M3U8类型的请求还是建议使用HttpURLConnection
     * @param cacheInfo
     */
    private void parseNetworkM3U8Info(VideoRequest videoRequest, VideoCacheInfo cacheInfo) {
        try {
            M3U8 m3u8 = M3U8Utils.parseNetworkM3U8Info(cacheInfo.getVideoUrl(), cacheInfo.getVideoUrl(), videoRequest.getHeaders(), 0);

            if (m3u8.isIsLive()) {
                //说明M3U8是直播
                videoRequest.getVideoInfoParsedListener().onM3U8LiveCallback(cacheInfo);
            } else if (m3u8.getSegList().isEmpty()) {
                videoRequest.getVideoInfoParsedListener().onM3U8ParsedFailed(new VideoCacheException("parseM3U8Info failed, zero ts"), cacheInfo);
            } else {
                cacheInfo.setVideoType(VideoType.M3U8_TYPE);
                cacheInfo.setTotalTs(m3u8.getSegCount());
                M3U8Seg seg = m3u8.getSegList().get(m3u8.getSegCount() / 2); //随机抽取
                HttpURLConnection connection = HttpUtils.getHeadConnection(seg.getUrl(), null);
                long segSize = connection.getContentLength();
                if (segSize != -1) {
                    m3u8.setEstimateSize((long) (segSize / seg.getDuration() * m3u8.getDuration()));
                    m3u8.setNeedLeastSize(seg.getDuration() < 5 ? segSize * 10 : segSize * 5);
                } else {
                    m3u8.setNeedLeastSize(100 * 1024 * 1024); //100MB
                    m3u8.setEstimateSize(1024 * 1024 * 1024); //1GB
                }
                long availableSpace = StorageUtils.getAllocatableBytes(new File(cacheInfo.getSavePath()));
                LogUtils.i(TAG, "parseNetworkM3U8Info:EstimateSize:"+ m3u8.getEstimateSize() / 1024 / 1024 + "MB,NeedLeastSize:" + m3u8.getNeedLeastSize() / 1024 / 1024 + "MB,availableSpace:" + availableSpace / 1024 / 1024);
                //有需求可以自己实现在空间不足情况下实现部分缓存，目前要求是必须能全部缓存
                if (m3u8.getEstimateSize() >= availableSpace) {
                    videoRequest.getVideoInfoParsedListener().onM3U8ParsedFailed(new VideoCacheException("insufficient space:Need:" + m3u8.getEstimateSize() + ",availableSpace:" + availableSpace), cacheInfo);
                    return;
                }
                // 1.将M3U8结构保存到本地
                File localM3U8File = new File(cacheInfo.getSavePath(), cacheInfo.getMd5() + StorageUtils.LOCAL_M3U8_SUFFIX);
                M3U8Utils.createLocalM3U8File(localM3U8File, m3u8);

                File proxyM3U8File = new File(cacheInfo.getSavePath(), cacheInfo.getMd5() + StorageUtils.PROXY_M3U8_SUFFIX);
                cacheInfo.setLocalPort(ProxyCacheUtils.getLocalPort());
                M3U8Utils.createProxyM3U8File(proxyM3U8File, m3u8, cacheInfo.getMd5(), videoRequest.getHeaders());

                // 2.构建一个本地代理的m3u8结构
                videoRequest.getVideoInfoParsedListener().onM3U8ParsedFinished(videoRequest, m3u8, cacheInfo);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "parseNetworkM3U8Info error.", e);
            videoRequest.getVideoInfoParsedListener().onM3U8ParsedFailed(new VideoCacheException("parseM3U8Info failed, " + e), cacheInfo);
        }
    }

    /**
     * 通过okhttp来请求非M3U8的视频
     *
     * 对于一些短视频,还是建议使用okhttp网络请求框架
     * @param cacheInfo
     */
    private void parseNonM3U8VideoInfoByOkHttp(VideoRequest videoRequest, VideoCacheInfo cacheInfo) {
        cacheInfo.setVideoType(VideoType.OTHER_TYPE);
        long contentLength;

        try {
            contentLength = OkHttpManager.getInstance().getContentLength(cacheInfo.getVideoUrl(), videoRequest.getHeaders());
            if (contentLength > 0) {
                cacheInfo.setTotalSize(contentLength);
                videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFinished(videoRequest, cacheInfo);
            } else {
                videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(new VideoCacheException(""), cacheInfo);
            }
        } catch (VideoCacheException e) {
            videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(e, cacheInfo);
        }
    }

    /**
     * 解析非M3U8视频类型信息
     * @param cacheInfo
     */
    private void parseNonM3U8VideoInfo(VideoRequest videoRequest, VideoCacheInfo cacheInfo) {
        HttpURLConnection connection = null;
        try {
            connection = HttpUtils.getConnection(cacheInfo.getVideoUrl(), videoRequest.getHeaders());
            parseNonM3U8VideoInfo(videoRequest, cacheInfo, connection);
        } catch (Exception e) {
            videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(new VideoCacheException(e.getMessage()), cacheInfo);
        } finally {
            HttpUtils.closeConnection(connection);
        }
    }

    private void parseNonM3U8VideoInfo(VideoRequest videoRequest, VideoCacheInfo cacheInfo, HttpURLConnection connection) {
        cacheInfo.setVideoType(VideoType.OTHER_TYPE);
        String length = connection.getHeaderField("content-length");
        try {
            long totalLength = Long.parseLong(length);
            if (totalLength > 0) {
                cacheInfo.setTotalSize(totalLength);
                videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFinished(videoRequest, cacheInfo);
            } else {
                videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(new VideoCacheException("Total length is illegal"), cacheInfo);
            }
        } catch (Exception e) {
            videoRequest.getVideoInfoParsedListener().onNonM3U8ParsedFailed(new VideoCacheException(e.getMessage()), cacheInfo);
        }
    }

}
