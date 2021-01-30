package com.jeffmony.videocache;

import android.net.Uri;
import android.text.TextUtils;


import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.common.VideoType;
import com.jeffmony.videocache.common.VideoParams;
import com.jeffmony.videocache.listener.IVideoInfoParsedListener;
import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.m3u8.M3U8Utils;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;
import com.jeffmony.videocache.utils.VideoParamsUtils;
import com.jeffmony.videocache.utils.VideoProxyThreadUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * @author jeffmony
 * 解析视频类型信息的工具类
 */
public class VideoInfoParseManager {

    private static final String TAG = "VideoInfoParseManager";

    private static volatile VideoInfoParseManager sInstance = null;

    private IVideoInfoParsedListener mListener;
    private Map<String, String> mHeaders;
    private String mContentType;
    private long mContentLength;

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

    //解析视频类型
    public void parseVideoInfo(VideoCacheInfo cacheInfo, Map<String, String> headers,
                               Map<String, Object> extraParams, IVideoInfoParsedListener listener) {
        mListener = listener;
        mHeaders = headers;
        //这两个值开发者可以设置
        mContentType = VideoParamsUtils.getStringValue(extraParams, VideoParams.CONTENT_TYPE);
        mContentLength = VideoParamsUtils.getLongValue(extraParams, VideoParams.CONTENT_LENGTH);

        VideoProxyThreadUtils.submitRunnableTask(new Runnable() {
            @Override
            public void run() {
                parseVideoInfo(cacheInfo);
            }
        });
    }

    private void parseVideoInfo(VideoCacheInfo cacheInfo) {
        if (TextUtils.equals(VideoParams.UNKNOWN, mContentType)) {
            String videoUrl = cacheInfo.getVideoUrl();
            if (videoUrl.contains("m3u8")) {
                parseM3U8Info(cacheInfo);
            } else {
                Uri videoUri = Uri.parse(videoUrl);
                String fileName = videoUri.getLastPathSegment();
                if (!TextUtils.isEmpty(fileName)) {
                    fileName = fileName.toLowerCase();
                    if (fileName.endsWith(".m3u8")) {
                        //当前是M3U8类型
                        parseM3U8Info(cacheInfo);
                    } else {
                        //不是M3U8类型，说明是整视频
                        mListener.onNonM3U8ParsedFinished();
                    }
                } else {
                    //需要发起请求判定当前视频的类型
                    HttpURLConnection connection = null;
                    try {
                        connection = HttpUtils.getConnection(cacheInfo.getVideoUrl(), mHeaders);
                        String contentType = connection.getContentType();
                        if (ProxyCacheUtils.isM3U8Mimetype(contentType)) {
                            //当前是M3U8类型
                            parseM3U8Info(cacheInfo);
                        } else {
                            //不是M3U8类型，说明是整视频
                            mListener.onNonM3U8ParsedFinished();
                        }
                    } catch (Exception e) {
                        mListener.onNonM3U8ParsedFailed(new VideoCacheException(e.getMessage()));
                    } finally {
                        HttpUtils.closeConnection(connection);
                    }
                }
            }
        } else {
            if (ProxyCacheUtils.isM3U8Mimetype(mContentType)) {
                //当前是M3U8类型
                parseM3U8Info(cacheInfo);
            } else {
                //不是M3U8类型，说明是整视频
                mListener.onNonM3U8ParsedFinished();
            }
        }
    }

    private void parseM3U8Info(VideoCacheInfo cacheInfo) {
        cacheInfo.setVideoType(VideoType.HLS_TYPE);
        try {
            M3U8 m3u8 = M3U8Utils.parseNetworkM3U8Info(cacheInfo.getVideoUrl(), mHeaders);

            // 1.将M3U8结构保存到本地
            VideoProxyThreadUtils.submitRunnableTask(new Runnable() {
                @Override
                public void run() {
                    File localM3U8File = new File(cacheInfo.getSavePath(), cacheInfo.getMd5() + StorageUtils.LOCAL_M3U8_SUFFIX);
                    try {
                        M3U8Utils.createLocalM3U8File(localM3U8File, m3u8);
                    } catch (Exception e) {
                        LogUtils.w(TAG, "parseM3U8Info->createLocalM3U8File failed, exception="+e);
                    }
                }
            });

            File proxyM3U8File = new File(cacheInfo.getSavePath(), cacheInfo.getMd5() + StorageUtils.PROXY_M3U8_SUFFIX);
            if (proxyM3U8File.exists() && cacheInfo.getLocalPort() == ProxyCacheUtils.getLocalPort()) {
                //说明本地代理文件存在，连端口号都一样的，不用做任何改变

                //Do nothing.
            } else {
                cacheInfo.setLocalPort(ProxyCacheUtils.getLocalPort());
                M3U8Utils.createProxyM3U8File(proxyM3U8File, m3u8, cacheInfo.getMd5(), mHeaders);
            }

            // 2.构建一个本地代理的m3u8结构
            mListener.onM3U8ParsedFinished(m3u8);
        } catch (Exception e) {
            mListener.onNonM3U8ParsedFailed(new VideoCacheException("parseM3U8Info failed, " + e.getMessage()));
        }
    }

}
