package com.jeffmony.playersdk.impl;

import android.content.Context;
import android.net.Uri;

import com.jeffmony.playersdk.IPlayer;

import java.io.IOException;
import java.util.Map;

public abstract class BasePlayerImpl {

    private Context mContext;
    private IPlayer.OnPreparedListener mOnPreparedListener;
    private IPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private IPlayer.OnErrorListener mOnErrorListener;
    private IPlayer.OnCompletionListener mOnCompletionListener;

    public BasePlayerImpl(Context context) {
        mContext = context.getApplicationContext();
    }

    public abstract void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    public abstract void prepareAsync() throws IllegalStateException;

    public abstract void start() throws IllegalStateException;

    public abstract void stop() throws IllegalStateException;

    public abstract void pause() throws IllegalStateException;

    public abstract void setSpeed(float speed);

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract void reset();

    public abstract void release();

    public abstract void seekTo(long msec) throws IllegalStateException;

    public void setOnPreparedListener(IPlayer.OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnVideoSizeChangedListener(IPlayer.OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    public void setOnErrorListener(IPlayer.OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnCompletionListener(IPlayer.OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }
}
