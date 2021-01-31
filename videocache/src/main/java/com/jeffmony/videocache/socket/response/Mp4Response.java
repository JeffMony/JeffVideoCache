package com.jeffmony.videocache.socket.response;

import com.jeffmony.videocache.socket.request.HttpRequest;

import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * @author jeffmony
 * MP4视频的local server端
 */
public class Mp4Response extends BaseResponse {
    public Mp4Response(HttpRequest request, String videoUrl, Map<String, String> headers) {
        super(request, videoUrl, headers);
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {

    }
}
