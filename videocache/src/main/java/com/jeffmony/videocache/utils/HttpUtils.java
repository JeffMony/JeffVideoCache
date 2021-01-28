package com.jeffmony.videocache.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

/**
 * @author jeffmony
 *
 * HttpURLConnection 通用类
 */

public class HttpUtils {

    private static final int MAX_REDIRECT = 5;

    public static HttpURLConnection getConnection(String videoUrl, Map<String, String> headers) throws IOException {
        URL url = new URL(videoUrl);
        int redirectCount = 0;
        while (redirectCount < MAX_REDIRECT) {
            HttpURLConnection connection = makeConnection(url, headers);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MULT_CHOICE ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                url = handleRedirect(url, location);
                redirectCount++;
            } else {
                return connection;
            }
        }
        throw new NoRouteToHostException("Too many redirects: " + redirectCount);
    }

    public static URL handleRedirect(URL originalUrl, String location)
            throws IOException {
        if (location == null) {
            throw new ProtocolException("Null location redirect");
        }
        URL url = new URL(originalUrl, location);
        String protocol = url.getProtocol();
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new ProtocolException("Unsupported protocol redirect: " + protocol);
        }
        return url;
    }

    private static HttpURLConnection makeConnection(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);   //因为我们内部已经做了重定向的功能,不需要在connection内部再做了.
        connection.setConnectTimeout(ProxyCacheUtils.getConfig().getConnTimeOut());
        connection.setReadTimeout(ProxyCacheUtils.getConfig().getReadTimeOut());
        if (headers != null) {
            for (Map.Entry<String, String> item : headers.entrySet()) {
                connection.setRequestProperty(item.getKey(), item.getValue());
            }
        }
        connection.connect();
        return connection;
    }

    public static void closeConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

}
