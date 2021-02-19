package com.jeffmony.playersdk.impl;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.util.Map;

public class ExoPlayerImpl extends BasePlayerImpl {

    private static final String TAG = "ExoPlayerImpl";

    private static final int PREPARE_NULL = 0x0;
    private static final int PREPARING_STATE = 0x1;
    private static final int PREPARED_STATE = 0x2;

    private boolean mIsInitPlayerListener = false;
    private PlayerEventListener mEventListener;
    private PlayerVideoListener mVideoListener;
    private SimpleExoPlayer mExoPlayer;
    private MediaSource mMediaSource;
    private int mPrepareState = PREPARE_NULL;

    public ExoPlayerImpl(Context context) {
        super(context);
        mExoPlayer = new SimpleExoPlayer.Builder(context).build();
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IllegalArgumentException, SecurityException, IllegalStateException {
        String playUrl;
        if (mPlayerSettings.getLocalProxyEnable()) {
            mIsM3U8 = ProxyCacheUtils.isM3U8(uri.toString(), null);
            playUrl = ProxyCacheUtils.getProxyUrl(uri.toString(), null, null);
            //请求放在客户端,非常便于控制
            mLocalProxyVideoControl.startRequestVideoInfo(uri.toString(), null, null);
        } else {
            playUrl = uri.toString();
        }
        mMediaSource = createMediaSource(Uri.parse(playUrl), null);
    }

    @Override
    public void setSurface(Surface surface) {
        mExoPlayer.setVideoSurface(surface);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if (!mIsInitPlayerListener) {
            initPlayerListener();
        }
        mPrepareState = PREPARING_STATE;
        mExoPlayer.prepare(mMediaSource);
    }

    @Override
    public void start() throws IllegalStateException {
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void stop() throws IllegalStateException {
        mExoPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        mExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters parameters = new PlaybackParameters(speed);
        mExoPlayer.setPlaybackParameters(parameters);
    }

    @Override
    public long getCurrentPosition() {
        return mExoPlayer.getCurrentPosition();
    }

    @Override
    public long getBufferedPosition() {
        if (mPlayerSettings.getLocalProxyEnable()) {
            return (long) (mProxyCachePercent * mExoPlayer.getDuration() / 100);
        }
        return mExoPlayer.getBufferedPosition();
    }

    @Override
    public long getDuration() {
        return mExoPlayer.getDuration();
    }

    @Override
    public void reset() {
        mExoPlayer.stop();
    }

    @Override
    public void release() {
        mExoPlayer.removeVideoListener(mVideoListener);
        mExoPlayer.removeListener(mEventListener);
        mLocalProxyVideoControl.releaseLocalProxyResources();
        mExoPlayer.release();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (mPlayerSettings.getLocalProxyEnable()) {
            mLocalProxyVideoControl.seekToCachePosition(msec);
        }
        mExoPlayer.seekTo(msec);
    }

    private void initPlayerListener() {
        mEventListener = new PlayerEventListener();
        mVideoListener = new PlayerVideoListener();
        mExoPlayer.addListener(mEventListener);
        mExoPlayer.addVideoListener(mVideoListener);
        mIsInitPlayerListener = true;
    }

    private DataSource.Factory buildDataSourceFactory() {
        String userAgent = Util.getUserAgent(mContext, "JeffPlayerSDK");
        DefaultDataSourceFactory upstreamFactory;
        upstreamFactory = new DefaultDataSourceFactory(mContext, new DefaultHttpDataSourceFactory(userAgent));
        return upstreamFactory;
    }

    private MediaSource createMediaSource(Uri uri, String extension) {
        int type = Util.inferContentType(uri, extension);
        if (mIsM3U8) {
            type = C.TYPE_HLS;
        }
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            for(int index =0; index < trackGroups.length; index++) {
                TrackGroup group = trackGroups.get(index);
                for (int jIndex = 0; jIndex < group.length; jIndex++) {
                    Format format = group.getFormat(jIndex);
                    LogUtils.w(TAG, "onTracksChanged format=" + Format.toLogString(format));
                }
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            LogUtils.i(TAG, "onPlayerStateChanged playWhenReady="+playWhenReady+", playbackState="+playbackState);
            switch(playbackState) {
                case Player.STATE_BUFFERING:
                    break;
                case Player.STATE_IDLE:
                    break;
                case Player.STATE_READY:
                    if (mPrepareState == PREPARING_STATE) {
                        notifyOnPrepared();
                        mPrepareState = PREPARED_STATE;
                    }
                    break;
                case Player.STATE_ENDED:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            notifyOnError(error.type, error.getCause().getMessage());
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            LogUtils.i(TAG, "onIsPlayingChanged isPlaying="+isPlaying);
        }
    }

    private class PlayerVideoListener implements VideoListener {

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            notifyOnVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio, 1.0f);
        }

        @Override
        public void onRenderedFirstFrame() {

        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {

        }
    }
}
