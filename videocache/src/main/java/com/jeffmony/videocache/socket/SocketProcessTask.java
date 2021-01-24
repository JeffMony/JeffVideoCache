package com.jeffmony.videocache.socket;

import com.jeffmony.videocache.common.VideoCacheConfig;

import java.net.Socket;

public class SocketProcessTask implements Runnable {

    private static final String TAG  = "SocketProcessTask";
    private final VideoCacheConfig mConfig;
    private final Socket mSocket;

    public SocketProcessTask(Socket socket, VideoCacheConfig config) {
        mConfig = config;
        mSocket = socket;
    }

    @Override
    public void run() {

    }
}
