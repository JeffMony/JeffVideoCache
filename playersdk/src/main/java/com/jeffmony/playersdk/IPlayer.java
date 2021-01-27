package com.jeffmony.playersdk;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Map;

public interface IPlayer {

    void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    void prepareAsync() throws IllegalStateException;

    void start() throws IllegalStateException;

    void stop() throws IllegalStateException;

    void pause() throws IllegalStateException;

    void setSpeed(float speed);

    long getCurrentPosition();

    long getDuration();

    void reset();

    void release();

    void seekTo(long msec) throws IllegalStateException;

    void setOnPreparedListener(OnPreparedListener listener);

    void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener);

    void setOnErrorListener(OnErrorListener listener);

    void setOnCompletionListener(OnCompletionListener listener);

    interface OnCompletionListener {
        void onCompletion(IPlayer mp);
    }

    interface OnPreparedListener {
        void onPrepared(IPlayer mp);
    }

    interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(IPlayer mp, int width, int height,
                                int rotationDegree,
                                float pixelRatio,
                                float darRatio);
    }

    interface OnErrorListener {
        void onError(IPlayer mp, int what, String msg);
    }
}
