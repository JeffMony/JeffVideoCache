package com.jeffmony.playersdk.impl;

import android.content.Context;
import android.net.Uri;

import java.util.Map;

public class IjkPlayerImpl extends BasePlayerImpl {
    public IjkPlayerImpl(Context context) {
        super(context);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IllegalArgumentException, SecurityException, IllegalStateException {

    }

    @Override
    public void prepareAsync() throws IllegalStateException {

    }

    @Override
    public void start() throws IllegalStateException {

    }

    @Override
    public void stop() throws IllegalStateException {

    }

    @Override
    public void pause() throws IllegalStateException {

    }

    @Override
    public void setSpeed(float speed) {

    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public void reset() {

    }

    @Override
    public void release() {

    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {

    }
}
