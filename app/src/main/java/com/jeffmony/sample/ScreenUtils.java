package com.jeffmony.sample;

import android.content.Context;
import android.view.WindowManager;

public class ScreenUtils {

    public static Size getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        Size size = new Size(width, height);
        return size;
    }
}
