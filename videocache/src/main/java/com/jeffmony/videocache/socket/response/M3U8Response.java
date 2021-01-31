package com.jeffmony.videocache.socket.response;

import com.jeffmony.videocache.socket.request.HttpRequest;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * @author jeffmony
 * M3U8视频的local server端
 */
public class M3U8Response extends BaseResponse {

    public M3U8Response(HttpRequest request, String videoUrl, Map<String, String> headers) {
        super(request, videoUrl, headers);
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {

    }
}
