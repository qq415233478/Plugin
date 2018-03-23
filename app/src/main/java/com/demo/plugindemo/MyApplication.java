package com.demo.plugindemo;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            HookUtils hookUtils = new HookUtils(this, ProxyActivity.class);
            hookUtils.hookSystemHandler();
            hookUtils.hookAMS();
            hookUtils.hookPMS();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
