package com.jeffmony.videocache.utils;


import com.jeffmony.videocache.okhttp.CustomTrustManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;

/**
 * @author jeffmony
 *
 * HttpURLConnection 通用类
 */

public class HttpUtils {

    private static final String TAG = "HttpUtils";
    public static final int MAX_RETRY_COUNT = 100;
    public static final int MAX_REDIRECT = 5;
    public static final int RESPONSE_200 = 200;
    public static final int RESPONSE_206 = 206;
    public static final int RESPONSE_503 = 503;

    public static HttpURLConnection getConnection(String videoUrl, Map<String, String> headers) throws IOException {
        return getConnection(videoUrl, headers, false);
    }

    private static HttpURLConnection getConnection(String videoUrl, Map<String, String> headers, boolean shouldIgnoreCertErrors) throws IOException {
        URL url = new URL(videoUrl);
        int redirectCount = 0;
        while (redirectCount < MAX_REDIRECT) {
            try {
                HttpURLConnection connection = makeConnection(url, headers, shouldIgnoreCertErrors);
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
            } catch (IOException e) {
                if ((e instanceof SSLHandshakeException || e instanceof SSLPeerUnverifiedException) && !shouldIgnoreCertErrors) {
                    //这种情况下需要信任证书重试
                    return getConnection(videoUrl, headers, true);
                } else {
                    throw e;
                }
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

    private static HttpURLConnection makeConnection(URL url, Map<String, String> headers, boolean shouldIgnoreCertErrors) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (shouldIgnoreCertErrors && connection instanceof HttpsURLConnection) {
            trustAllCert((HttpsURLConnection) connection);
        }
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

    public static void trustAllCert(HttpsURLConnection httpsURLConnection) {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            if (sslContext != null) {
                sslContext.init(null, new TrustManager[]{new CustomTrustManager()}, null);
            }
        } catch (Exception e) {
            LogUtils.w(TAG,"SSLContext init failed");
        }
        // Cannot do ssl checkl.
        if (sslContext == null) {
            return;
        }
        // Trust the cert.
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        httpsURLConnection.setHostnameVerifier(hostnameVerifier);
        httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
    }

    public static void closeConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

}
