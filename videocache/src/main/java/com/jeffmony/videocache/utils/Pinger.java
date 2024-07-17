package com.jeffmony.videocache.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.jeffmony.videocache.proxy.LocalProxyVideoServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Pings {@link LocalProxyVideoServer} to make sure it works.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */

public class Pinger {

    public static final String PING_REQUEST = "ping";
    public static final String PING_RESPONSE = "ping ok";
    private static final String TAG = "Pinger";
    private static final ExecutorService pingExecutor = Executors.newSingleThreadExecutor();

    public static boolean ping(int maxAttempts, int startTimeout) {
        Preconditions.checkArgument(maxAttempts >= 1);
        Preconditions.checkArgument(startTimeout > 0);

        int timeout = startTimeout;
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
                Future<Boolean> pingFuture = pingExecutor.submit(new PingCallable());
                boolean pinged = pingFuture.get(timeout, MILLISECONDS);
                if (pinged) {
                    return true;
                }
            } catch (TimeoutException e) {
                LogUtils.w(TAG, "Error pinging server (attempt: " + attempts + ", timeout: " + timeout + "). ");
            } catch (InterruptedException | ExecutionException e) {
                LogUtils.e(TAG, "Error pinging server due to unexpected error" + e);
            }
            attempts++;
            timeout *= 2;
        }
        String error = String.format(Locale.US, "Error pinging server (attempts: %d, max timeout: %d). "
                , attempts, timeout / 2);
        LogUtils.e(TAG, error);
        return false;
    }


    public static boolean isPingRequest(String request) {
        return PING_REQUEST.equals(request);
    }

    public static void responseToPing(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write("HTTP/1.1 200 OK\n\n".getBytes());
        out.write(PING_RESPONSE.getBytes());
        out.flush();
    }

    private static boolean pingServer() {
        String pingUrl = getPingUrl();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(pingUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            connection.connect();
            //return (200 <= responseCode && responseCode < 400);
            return 200 == connection.getResponseCode();
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    private static String getPingUrl() {
        return String.format(Locale.US, "http://%s:%d/%s", ProxyCacheUtils.LOCAL_PROXY_HOST, ProxyCacheUtils.getLocalPort(), PING_REQUEST);
    }

    private static class PingCallable implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            return pingServer();
        }
    }

}
