package com.jeffmony.videocache.common;

import com.jeffmony.videocache.listener.IVideoInfoParsedListener;
import com.jeffmony.videocache.utils.VideoParamsUtils;

import java.util.Map;

public class VideoRequest {
    private final String videoUrl;
    private final Map<String, String> headers;
    private final Map<String, Object> extraParams;

    private final IVideoInfoParsedListener iVideoInfoParsedListener;

    private final String contentType;

    private final long contentLength;

    private VideoRequest(Builder builder) {
        this.videoUrl = builder.videoUrl;
        this.headers = builder.headers;
        this.extraParams = builder.extraParams;
        this.iVideoInfoParsedListener = builder.iVideoInfoParsedListener;
        this.contentLength = VideoParamsUtils.getLongValue(this.extraParams, VideoParams.CONTENT_LENGTH);
        this.contentType = VideoParamsUtils.getStringValue(this.extraParams, VideoParams.CONTENT_TYPE);
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public IVideoInfoParsedListener getVideoInfoParsedListener() {
        return iVideoInfoParsedListener;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public static final class Builder {
        private final String videoUrl;
        private Map<String, String> headers;
        private Map<String, Object> extraParams;

        private IVideoInfoParsedListener iVideoInfoParsedListener;

        public Builder(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder extraParams(Map<String, Object> extraParams) {
            this.extraParams = extraParams;
            return this;
        }

        public Builder videoInfoParsedListener(IVideoInfoParsedListener iVideoInfoParsedListener) {
            this.iVideoInfoParsedListener = iVideoInfoParsedListener;
            return this;
        }

        public VideoRequest build() {
            return new VideoRequest(this);
        }
    }
}
