package com.tastes.util;

import android.util.Log;

import com.tastes.BuildConfig;


public class LogWrapper {
    //private static final String TAG = "TAG";
    private static final boolean DEBUG = BuildConfig.DEBUG;

	public static void e(String tag, String msg) {
        if(DEBUG) {
            Log.e(tag, msg==null?"msg is null":msg);
        }
	}
}