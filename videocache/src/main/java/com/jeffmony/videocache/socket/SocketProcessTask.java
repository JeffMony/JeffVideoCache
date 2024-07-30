package com.jeffmony.videocache.socket;

import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.jeffmony.videocache.common.SourceCreator;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.response.BaseResponse;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.Pinger;
import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketProcessTask implements Runnable {

    private static final String TAG  = "SocketProcessTask";
    private static final AtomicInteger sRequestCountAtomic = new AtomicInteger(0);
    private final Socket mSocket;

    private final SourceCreator mSourceCreator;

    private final long mSocketTaskCreateTime = SystemClock.uptimeMillis();

    public SocketProcessTask(Socket socket) {
        mSocket = socket;
        mSourceCreator = ProxyCacheUtils.getConfig().getSourceCreator();
    }

    @Override
    public void run() {
        sRequestCountAtomic.addAndGet(1);
        LogUtils.i(TAG, "sRequestCountAtomic : " + sRequestCountAtomic.get());
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = mSocket.getOutputStream();
            inputStream = mSocket.getInputStream();
            HttpRequest request = new HttpRequest(inputStream, mSocket.getInetAddress());
            while(!mSocket.isClosed()) {
                request.parseRequest();
                BaseResponse response;
                String url = request.getUri();
                url = url.substring(1);
                LogUtils.d(TAG, "request url=" + url);
                if (Pinger.isPingRequest(url)) {
                    Pinger.responseToPing(mSocket);
                    break;
                }
                url = ProxyCacheUtils.decodeUriWithBase64(url);
                LogUtils.d(TAG, "decode request url=" + url);
                //m3u8里的ts视频请求:Range header=null
                LogUtils.d(TAG, "Range header=" + request.getRangeString());
                //最新的请求可以获得回应，旧的关闭；意味着播放器内部只能一个socket在请求数据
                long currentTime = mSocketTaskCreateTime; //System.currentTimeMillis();
                ProxyCacheUtils.setSocketTime(currentTime);
                if (url.contains(ProxyCacheUtils.VIDEO_PROXY_SPLIT_STR)) {
                    String[] videoInfoArr = url.split(ProxyCacheUtils.VIDEO_PROXY_SPLIT_STR);
                    if (videoInfoArr.length < 3) {
                        throw new VideoCacheException("Local Socket Error Argument");
                    }
                    String videoUrl = videoInfoArr[0];
                    String videoTypeInfo = videoInfoArr[1];
                    String videoHeaders = videoInfoArr[2];

                    Map<String, String> headers = ProxyCacheUtils.str2Map(videoHeaders);
                    LogUtils.d(TAG, videoUrl + "\n" + videoTypeInfo + "\n" + videoHeaders);

                    if (TextUtils.equals(ProxyCacheUtils.M3U8, videoTypeInfo)) {
                        response = mSourceCreator.createM3U8Response(request, videoUrl, headers, currentTime);
                    } else if (TextUtils.equals(ProxyCacheUtils.NON_M3U8, videoTypeInfo)) {
                        response = mSourceCreator.createMp4Response(request, videoUrl, headers, currentTime);
                    } else {
                        //无法从已知的信息判定视频信息，需要重新请求
                        HttpURLConnection connection = HttpUtils.getConnection(videoUrl, headers);
                        String contentType = connection.getContentType();
                        if (ProxyCacheUtils.isM3U8Mimetype(contentType)) {
                            response = mSourceCreator.createM3U8Response(request, videoUrl, headers, currentTime);
                        } else {
                            response = mSourceCreator.createMp4Response(request, videoUrl, headers, currentTime);
                        }
                    }
                    response.sendResponse(mSocket, outputStream);
                } else if (url.contains(ProxyCacheUtils.SEG_PROXY_SPLIT_STR)) {
                    //说明是M3U8 ts格式的文件
                    String[] videoInfoArr = url.split(ProxyCacheUtils.SEG_PROXY_SPLIT_STR);
                    if (videoInfoArr.length < 4) {
                        throw new VideoCacheException("Local Socket for M3U8 ts file Error Argument");
                    }
                    String parentUrl = videoInfoArr[0];
                    String videoUrl = videoInfoArr[1];
                    String fileName = videoInfoArr[2];
                    String videoHeaders = videoInfoArr[3];
                    Map<String, String> headers = ProxyCacheUtils.str2Map(videoHeaders);
                    LogUtils.d(TAG,  "ts request: parentUrl:" + parentUrl + "\nvideoUrl:" + videoUrl + "\nfileName:" + fileName + "\nvideoHeaders:" + videoHeaders);
                    response = mSourceCreator.createM3U8SegResponse(request, parentUrl, videoUrl, headers, currentTime, fileName);
                    response.sendResponse(mSocket, outputStream);
                } else {
                    throw new VideoCacheException("Local Socket Error url");
                }
                break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.w(TAG,"socket request failed, exception=" + e);
        } finally {
            ProxyCacheUtils.close(outputStream);
            ProxyCacheUtils.close(inputStream);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ProxyCacheUtils.close(mSocket);
            } else {
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        LogUtils.e(TAG,"close " + mSocket + " failed, exception = " + e);
                    }
                }
            }
            int count = sRequestCountAtomic.decrementAndGet();
            LogUtils.i(TAG, "finally Socket solve count = " + count);
        }
    }
}
