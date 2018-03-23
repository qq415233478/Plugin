package com.demo.plugindemo;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class AppUtils {

    public static String copyPluginApk(Context context, String apkName) {
        InputStream in = null;
        FileOutputStream out = null;
        String path = context.getApplicationContext().getFilesDir()
                .getAbsolutePath() + "/" + apkName;
        File file = new File(path);
        if (!file.exists()) {
            try {
                in = context.getAssets().open(apkName); // 从assets目录下复制
                out = new FileOutputStream(file);
                int length = -1;
                byte[] buf = new byte[1024];
                while ((length = in.read(buf)) != -1) {
                    out.write(buf, 0, length);
                }
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return path;
    }
}
