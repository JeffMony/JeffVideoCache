package com.jeffmony.playersdk.impl;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.view.Surface;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkPlayerImpl extends BasePlayerImpl {
    private static final String TAG = "IjkPlayerImpl";

    private IjkMediaPlayer mIjkPlayer;

    public IjkPlayerImpl(Context context) {
        super(context);
        mIjkPlayer = new IjkMediaPlayer();

        //不用MediaCodec编解码
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        //不用opensles编解码
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10000000);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 16);
        mIjkPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        initPlayerListeners();
    }

    private void initPlayerListeners() {
        mIjkPlayer.setOnPreparedListener(mOnPreparedListener);
        mIjkPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mIjkPlayer.setOnErrorListener(mOnErrorListener);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
        mIjkPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void setSurface(Surface surface) {
        mIjkPlayer.setSurface(surface);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mIjkPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mIjkPlayer.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        mIjkPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        mIjkPlayer.pause();
    }

    @Override
    public void setSpeed(float speed) {
        mIjkPlayer.setSpeed(speed);
    }

    @Override
    public long getCurrentPosition() {
        return mIjkPlayer.getCurrentPosition();
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public long getDuration() {
        return mIjkPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return mIjkPlayer.isPlaying();
    }

    @Override
    public void reset() {
        mIjkPlayer.reset();
    }

    @Override
    public void release() {
        mIjkPlayer.release();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mIjkPlayer.seekTo(msec);
    }

    private IjkMediaPlayer.OnPreparedListener mOnPreparedListener = new IjkMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer mp) {
            notifyOnPrepared();
        }

    };

    private IjkMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new IjkMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            notifyOnVideoSizeChanged(width, height, sar_num, sar_den, 0);
        }
    };

    private IjkMediaPlayer.OnVideoDarSizeChangedListener mOnVideoDarSizeChangedListener = new IjkMediaPlayer.OnVideoDarSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den, int dar_num, int dar_den) {
            float pixelRatio = sar_num * 1.0f / sar_den;
            if (Float.compare(pixelRatio, Float.NaN) == 0) {
                pixelRatio = 1.0f;
            }
            float darRatio = dar_num * 1.0f / dar_den;
            notifyOnVideoSizeChanged(width, height, 0, pixelRatio, darRatio);
        }
    };

    private IjkMediaPlayer.OnErrorListener mOnErrorListener = new IjkMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            notifyOnError(what, "" + extra);
            return true;
        }
    };
}
