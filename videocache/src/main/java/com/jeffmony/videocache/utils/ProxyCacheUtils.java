package com.jeffmony.videocache.utils;

import java.io.Closeable;
import java.net.URLDecoder;

public class ProxyCacheUtils {

    private static final String TAG = "ProxyCacheUtils";

    public static final String LOCAL_PROXY_HOST = "127.0.0.1";
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LogUtils.w(TAG,"ProxyCacheUtils close " + closeable + " failed, exception = " + e);
            }
        }
    }

    public static String decodeUri(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF-8");
        } catch (Exception ignored) {
            LogUtils.w(TAG,"Encoding not supported, ignored: " + ignored.getMessage());
        }
        return decoded;
    }
}
