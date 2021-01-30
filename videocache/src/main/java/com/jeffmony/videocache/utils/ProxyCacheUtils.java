package com.jeffmony.videocache.utils;

import android.text.TextUtils;
import android.util.Base64;

import com.jeffmony.videocache.common.VideoCacheConfig;
import com.jeffmony.videocache.common.VideoMime;

import java.io.Closeable;
import java.io.File;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jeffmony
 *
 * 本地代理相关的工具类
 */
public class ProxyCacheUtils {

    private static final String TAG = "ProxyCacheUtils";

    public static final String LOCAL_PROXY_HOST = "127.0.0.1";
    public static final String LOCAL_PROXY_URL = "http://" + LOCAL_PROXY_HOST;
    public static final String TS_PROXY_SPLIT_STR = "&jeffmony_ts&";
    public static final String VIDEO_PROXY_SPLIT_STR = "&jeffmony_video&";
    public static final String HEADER_SPLIT_STR = "&jeffmony_header&";
    public static final String UNKNOWN = "unknown";

    private static VideoCacheConfig sConfig;
    private static int sLocalPort = 0;

    public static void setVideoCacheConfig(VideoCacheConfig config) {
        sConfig = config;
    }

    public static VideoCacheConfig getConfig() {
        return sConfig;
    }

    public static void setLocalPort(int port) {
        sLocalPort = port;
    }

    public static int getLocalPort() {
        return sLocalPort;
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LogUtils.w(TAG,"ProxyCacheUtils close " + closeable + " failed, exception = " + e);
            }
        }
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

    public static String encodeUri(String str) {
        try {
            return Base64.encodeToString(str.getBytes("utf-8"), Base64.DEFAULT);
        } catch (Exception e) {
            return str;
        }
    }

    public static String decodeUri(String str) {
        return new String(Base64.decode(str, Base64.DEFAULT));
    }

    public static String map2Str(Map<String, String> headers) {
        if (headers == null || headers.size() == 0) {
            return UNKNOWN;
        }
        StringBuilder headerStr = new StringBuilder();
        for (Map.Entry item : headers.entrySet()) {
            String key = (String) item.getKey();
            String value = (String) item.getValue();
            headerStr.append(key + HEADER_SPLIT_STR + value);
            headerStr.append("\n");
        }
        return headerStr.toString();
    }

    public static Map<String, String> str2Map(String headerStr) {
        Map<String, String> headers = new HashMap<>();
        if (!TextUtils.isEmpty(headerStr) && !TextUtils.equals(headerStr, UNKNOWN)) {
            String[] headerLines = headerStr.split("\n");
            for (String headerItems : headerLines) {
                String[] headerArr = headerItems.split(HEADER_SPLIT_STR);
                if (headerArr.length >= 2) {
                    headers.put(headerArr[0], headerArr[1]);
                }
            }
        }
        return headers;
    }
}
