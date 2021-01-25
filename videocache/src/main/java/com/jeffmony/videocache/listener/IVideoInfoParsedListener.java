package com.jeffmony.videocache.listener;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.m3u8.M3U8;

/**
 * @author jeffmony
 * 解析video info的回调监听
 */
public interface IVideoInfoParsedListener {

    //M3U8视频解析成功
    void onM3U8ParsedFinished(M3U8 m3u8);

    //M3U8视频解析失败
    void onM3U8ParsedFailed(VideoCacheException e);

    //M3U8视频是直播
    void onM3U8LiveCallback();

    //非M3U8视频解析成功
    void onNonM3U8ParsedFinished();

    //非M3U8视频解析失败
    void onNonM3U8ParsedFailed(VideoCacheException e);
}
