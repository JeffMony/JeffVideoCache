package com.jeffmony.videocache.utils;

import com.jeffmony.videocache.common.VideoMime;

import java.io.Closeable;
import java.net.URLDecoder;
import java.security.MessageDigest;

/**
 * @author jeffmony
 *
 * 本地代理相关的工具类
 */
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

    private static String bytesToHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    //当前mimetype是否是M3U8类型
    public static boolean isM3U8Mimetype(String mimeType) {
        return mimeType.contains(VideoMime.MIME_TYPE_M3U8_1) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_2) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_3) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_4) ||
                mimeType.contains(VideoMime.MIME_TYPE_M3U8_5);
    }
}
