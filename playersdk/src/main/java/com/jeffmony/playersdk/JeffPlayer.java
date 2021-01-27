package com.jeffmony.playersdk;

import android.content.Context;
import android.net.Uri;

import com.jeffmony.playersdk.common.PlayerType;
import com.jeffmony.playersdk.impl.BasePlayerImpl;
import com.jeffmony.playersdk.impl.ExoPlayerImpl;
import com.jeffmony.playersdk.impl.IjkPlayerImpl;

import java.io.IOException;
import java.util.Map;

public class JeffPlayer implements IPlayer {

    private BasePlayerImpl mPlayerImpl;

    public JeffPlayer(Context context) {
        this(context, PlayerType.EXO_PLAYER);
    }

    public JeffPlayer(Context context, PlayerType type) {
        if (type == PlayerType.EXO_PLAYER) {
            mPlayerImpl = new ExoPlayerImpl(context);
        } else {
            mPlayerImpl = new IjkPlayerImpl(context);
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayerImpl.setDataSource(context, uri, headers);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mPlayerImpl.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mPlayerImpl.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        mPlayerImpl.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        mPlayerImpl.pause();
    }

    @Override
    public void setSpeed(float speed) {
        mPlayerImpl.setSpeed(speed);
    }

    @Override
    public long getCurrentPosition() {
        return mPlayerImpl.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mPlayerImpl.getDuration();
    }

    @Override
    public void reset() {
        mPlayerImpl.reset();
    }

    @Override
    public void release() {
        mPlayerImpl.release();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mPlayerImpl.seekTo(msec);
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mPlayerImpl.setOnPreparedListener(listener);
    }

    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mPlayerImpl.setOnVideoSizeChangedListener(listener);
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mPlayerImpl.setOnErrorListener(listener);
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        mPlayerImpl.setOnCompletionListener(listener);
    }
}
