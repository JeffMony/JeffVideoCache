package com.jeffmony.playersdk;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.jeffmony.playersdk.common.PlayerSettings;

import java.io.IOException;
import java.util.Map;

public interface IPlayer {

    void initPlayerSettings(PlayerSettings settings);

    void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    void setSurface(Surface surface);

    void prepareAsync() throws IllegalStateException;

    void start() throws IllegalStateException;

    void stop() throws IllegalStateException;

    void pause() throws IllegalStateException;

    void setSpeed(float speed);

    long getCurrentPosition();

    long getBufferedPosition();

    long getDuration();

    boolean isPlaying();

    void reset();

    void release();

    void seekTo(long msec) throws IllegalStateException;

    void setOnPreparedListener(OnPreparedListener listener);

    void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener);

    void setOnErrorListener(OnErrorListener listener);

    void setOnCompletionListener(OnCompletionListener listener);

    void setOnProxyCacheInfoListener(OnProxyCacheInfoListener listener);

    interface OnCompletionListener {
        void onCompletion();
    }

    interface OnPreparedListener {
        void onPrepared();
    }

    interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(int width, int height,
                                int rotationDegree,
                                float pixelRatio,
                                float darRatio);
    }

    interface OnErrorListener {
        void onError(int what, String msg);
    }

    interface OnProxyCacheInfoListener {
        void onProxyCacheInfo(int msg, Map<String, Object> params);
    }
}
