package com.jeffmony.videocache.utils;

import android.util.Log;

/**
 * @author jeffmony
 *
 * sdk中log的通用类
 */

public class LogUtils {
    private static final String PREFIX = "VideoCache-";

    private static boolean DEBUG = false;
    private static boolean INFO = true;
    private static boolean WARN = true;

    private static boolean ERROR = true;

    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(PREFIX + tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (INFO) {
            Log.i(PREFIX + tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (WARN) {
            Log.w(PREFIX + tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (ERROR) {
            Log.e(PREFIX + tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (ERROR) {
            Log.e(PREFIX + tag, msg, tr);
        }
    }
}
