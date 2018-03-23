package com.demo.plugindemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public class HookUtils {

    private Context mContext;
    private Class mProxyActivity;

    public HookUtils(Context context, Class proxyActivity) {
        mContext = context;
        mProxyActivity = proxyActivity;
    }

    public void hookAMS() throws Exception {
        Class<?> ActivityManagerNativeClss = Class.forName("android.app.ActivityManagerNative");
        Field gDefault = ActivityManagerNativeClss.getDeclaredField("gDefault");
        gDefault.setAccessible(true);
        Object vDefault = gDefault.get(null);

        //反射SingleTon
        Class<?> SingletonClass = Class.forName("android.util.Singleton");
        Field mInstance = SingletonClass.getDeclaredField("mInstance");
        mInstance.setAccessible(true);
        //到这里已经拿到ActivityManager对象
        Object iActivityManagerObject = mInstance.get(vDefault);
        AMSInvocationHandler handler = new AMSInvocationHandler(iActivityManagerObject);

        //开始动态代理，用代理对象替换掉真实的ActivityManager
        Class<?> IActivityManagerIntercept = Class.forName("android.app.IActivityManager");
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{IActivityManagerIntercept}, handler);
        //替换掉这个对象
        mInstance.set(vDefault, proxy);
    }

    public void hookPMS() throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        //获取主线程对象
        Object activityThread = currentActivityThreadMethod.invoke(null);
        Method getPackageManager = activityThread.getClass().getDeclaredMethod("getPackageManager");
        Object iPackageManager = getPackageManager.invoke(activityThread);
        PMSInvocationHandler handler = new PMSInvocationHandler(iPackageManager);
        Class<?> iPackageManagerIntercept = Class.forName("android.content.pm.IPackageManager");
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iPackageManagerIntercept}, handler);
        // 获取 sPackageManager 属性
        Field iPackageManagerField = activityThread.getClass().getDeclaredField("sPackageManager");
        iPackageManagerField.setAccessible(true);
        iPackageManagerField.set(activityThread, proxy);
    }

    public void hookSystemHandler() throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        //获取主线程对象
        Object activityThread = currentActivityThreadMethod.invoke(null);
        //获取mH字段
        Field mH = activityThreadClass.getDeclaredField("mH");
        mH.setAccessible(true);
        //获取Handler
        Handler handler = (Handler) mH.get(activityThread);
        //获取原始的mCallBack字段
        Field mCallBack = Handler.class.getDeclaredField("mCallback");
        mCallBack.setAccessible(true);
        //这里设置了我们自己实现了接口的CallBack对象
        mCallBack.set(handler, new ActivityThreadHandlerCallback(handler));
    }

    private class ActivityThreadHandlerCallback implements Handler.Callback {
        Handler mHandler;

        ActivityThreadHandlerCallback(Handler handler) {
            mHandler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            LogUtils.d("handleMessage");
            //替换之前的Intent
            if (msg.what == 100) {
                LogUtils.d("launchActivity");
                handleLaunchActivity(msg);
            }
//            //传递false是不是就不用自己懂啊用handleMessage了??
            mHandler.handleMessage(msg);
            return true;
        }

        private void handleLaunchActivity(Message msg) {
            Object obj = msg.obj;//ActivityClientRecord
            try {
                Field intentField = obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                Intent proxyIntent = (Intent) intentField.get(obj);
                Intent realIntent = proxyIntent.getParcelableExtra("oldIntent");
                if (realIntent != null) {
                    proxyIntent.setComponent(realIntent.getComponent());
                }
            } catch (Exception e) {
                LogUtils.d("launchActivity failed");
            }
        }
    }

    private class AMSInvocationHandler implements InvocationHandler {
        private Object iActivityManagerObject;

        private AMSInvocationHandler(Object iActivityManagerObject) {
            this.iActivityManagerObject = iActivityManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            //查找到真实的intent,替换为自定义的proxyIntent,并将原来的intent保存起来
            if ("startActivity".contains(method.getName())) {
                LogUtils.d("启动Activity");
                Intent intent = null;
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        intent = (Intent) args[i];
                        index = i;
                    }
                }
                if (intent != null) {
                    Intent proxyIntent = (Intent) intent.clone();
                    ComponentName componentName = new ComponentName(mContext, mProxyActivity);
                    proxyIntent.setComponent(componentName);
                    proxyIntent.putExtra("oldIntent", intent);
                    args[index] = proxyIntent;
                }
            }
            return method.invoke(iActivityManagerObject, args);
        }
    }


    private class PMSInvocationHandler implements InvocationHandler {
        private Object iPackagerManagerObject;

        private PMSInvocationHandler(Object iPackagerManagerObject) {
            this.iPackagerManagerObject = iPackagerManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            LogUtils.d("PackageManager", "method:" + method.getName());
            // 关键！！！ 报错的原因是因为 getActivityInfo 返回 null , 因此就造一个空的返回，如果可以的话可以自己配置 ActivityInfo
            if ("getActivityInfo".equals(method.getName())) {
                ActivityInfo activityInfo = new ActivityInfo();
                return activityInfo;
            }
            return method.invoke(iPackagerManagerObject, args);
        }
    }
}

