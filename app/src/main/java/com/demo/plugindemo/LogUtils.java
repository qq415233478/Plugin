package com.demo.plugindemo;

/**
 * Created by Administrator on 2018/3/22.
 */

public class LogUtils {
    public static final String TAG = "qujq";

    public static void d(String msg) {
        android.util.Log.d(TAG, msg);
    }

    public static void d(String tag, String msg) {
        android.util.Log.d(tag, "[qujq]" + msg);
    }

    public static void e(String msg) {
        android.util.Log.e(TAG, msg);
    }

    public static void e(String tag, String msg) {
        android.util.Log.e(tag, "[qujq]" + msg);
    }
}
