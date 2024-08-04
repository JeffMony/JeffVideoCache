package com.jeffmony.videocache.common;

import android.os.Handler;

import com.jeffmony.videocache.m3u8.M3U8;
import com.jeffmony.videocache.model.VideoCacheInfo;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.response.BaseResponse;
import com.jeffmony.videocache.socket.response.M3U8Response;
import com.jeffmony.videocache.socket.response.M3U8SegResponseNew;
import com.jeffmony.videocache.socket.response.Mp4Response;
import com.jeffmony.videocache.task.M3U8CacheTaskNew;
import com.jeffmony.videocache.task.Mp4CacheTask;
import com.jeffmony.videocache.task.VideoCacheTask;

import java.util.Map;

/**
 * 可以自定义响应和缓存逻辑
 */
public class SourceCreator {
    public BaseResponse createMp4Response(HttpRequest request, String videoUrl, Map<String, String> headers, long time) throws Exception {
        return new Mp4Response(request, videoUrl, headers, time);
    }

    public BaseResponse createM3U8Response(HttpRequest request, String videoUrl, Map<String, String> headers, long time) {
        return  new M3U8Response(request, videoUrl, headers, time);
    }

    public BaseResponse createM3U8SegResponse(HttpRequest request, String parentUrl, String videoUrl, Map<String, String> headers, long time, String fileName) throws Exception {
        return new M3U8SegResponseNew(request, parentUrl, videoUrl, headers, time, fileName);
    }

    public VideoCacheTask createM3U8CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers, M3U8 m3u8, Handler handler) {
        return new M3U8CacheTaskNew(cacheInfo, m3u8, handler);
    }

    public VideoCacheTask createMp4CacheTask(VideoCacheInfo cacheInfo, Map<String, String> headers) {
        return new Mp4CacheTask(cacheInfo, headers);
    }
}
