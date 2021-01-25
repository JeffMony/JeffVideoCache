package com.jeffmony.videocache.socket;

import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.response.BaseResponse;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketProcessTask implements Runnable {

    private static final String TAG  = "SocketProcessTask";
    private static AtomicInteger sRequestCountAtomic = new AtomicInteger(0);
    private final VideoCacheConfig mConfig;
    private final Socket mSocket;

    public SocketProcessTask(Socket socket, VideoCacheConfig config) {
        mConfig = config;
        mSocket = socket;
    }

    @Override
    public void run() {
        sRequestCountAtomic.addAndGet(1);
        LogUtils.i(TAG, "sRequestCountAtomic : " + sRequestCountAtomic.get());
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            HttpRequest request = new HttpRequest(inputStream, mSocket.getInetAddress());
            while(!mSocket.isClosed()) {
                request.parseRequest();
                BaseResponse response;
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.w(TAG,"socket request failed, exception=" + e);
        } finally {
            ProxyCacheUtils.close(outputStream);
            ProxyCacheUtils.close(inputStream);
            ProxyCacheUtils.close(mSocket);
            int count = sRequestCountAtomic.decrementAndGet();
            LogUtils.i(TAG, "finally Socket solve count = " + count);
        }
    }
}
