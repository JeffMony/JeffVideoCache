package com.jeffmony.videocache.okhttp;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * okhttp全局管理类
 * 全局唯一的实例
 */
public class OkHttpUtils {
    private static final String TAG = "OkHttpUtils";

    private OkHttpUtils() {}

    public static OkHttpClient getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;

        private OkHttpClient mClient;

        Singleton() {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(60 * 1000L, TimeUnit.MILLISECONDS);
            builder.readTimeout(60 * 1000L, TimeUnit.MILLISECONDS);
            builder.followRedirects(false);
            builder.followSslRedirects(false);
            ConnectionPool connectionPool = new ConnectionPool(50, 5 * 60, TimeUnit.SECONDS);
            builder.connectionPool(connectionPool);
            mClient = builder.build();
        }

        public OkHttpClient getInstance() { return mClient; }
    }

}
