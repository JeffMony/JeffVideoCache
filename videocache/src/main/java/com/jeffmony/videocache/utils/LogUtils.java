package com.jeffmony.videocache.utils;

import android.util.Log;

/**
 * @author jeffmony
 *
 * sdk中log的通用类
 */

public class LogUtils {

    private static boolean DEBUG = true;

    public static void i(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {
            Log.w(tag, msg);
        }
    }
}
