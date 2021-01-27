package com.jeffmony.videocache;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.common.VideoType;
import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.common.VideoParams;
import com.jeffmony.videocache.listener.IVideoInfoParsedListener;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.VideoParamsUtils;
import com.jeffmony.videocache.utils.VideoProxyThreadUtils;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * @author jeffmony
 * 解析视频类型信息的工具类
 */
public class VideoInfoParseManager {

    private static volatile VideoInfoParseManager sInstance = null;

    private VideoCacheConfig mProxyConfig;
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

    public void initProxyConfig(@NonNull VideoCacheConfig config) {
        mProxyConfig = config;
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
                        connection = HttpUtils.getConnection(cacheInfo.getVideoUrl(), mHeaders, mProxyConfig);
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

    }

}
