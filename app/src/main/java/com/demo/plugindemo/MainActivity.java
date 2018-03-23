package com.demo.plugindemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {
    public static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String dexPath = AppUtils.copyPluginApk(this, "plugin.apk");
        LogUtils.d(TAG, "dexPath:" + dexPath);
        String optimizedDirectory = getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();
        LogUtils.d(TAG, "optimizedDirectory:" + optimizedDirectory);
        DexClassLoader mDexClassLoader = new DexClassLoader(dexPath, optimizedDirectory, null, getClassLoader());
        try {
            Class clazz = mDexClassLoader.loadClass("com.demo.plugin.User");
            LogUtils.d(TAG, "clazz:" + clazz.getSimpleName());
            Field[] fields = clazz.getFields();
            for (Field field : fields) {
                LogUtils.d(TAG, field.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, e.getLocalizedMessage());
        }
    }

    public void onClick(View v) {
        Intent intent = new Intent(this, TargetActivity.class);
        intent.putExtra("aaa", 1);
        startActivity(intent);
    }
}
