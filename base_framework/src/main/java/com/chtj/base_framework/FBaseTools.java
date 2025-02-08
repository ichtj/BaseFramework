package com.chtj.base_framework;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public final class FBaseTools {
    //全局上下文
    static Context sApp;
    private static volatile FBaseTools sInstance;
    private static final String NAME = "framework_upgrade";
    private static final String KEY_UPGRADE = "upgrade";

    /**
     * 单例模式
     *
     * @return
     */
    public static FBaseTools instance() {
        if (sInstance == null) {
            synchronized (FBaseTools.class) {
                if (sInstance == null) {
                    sInstance = new FBaseTools();
                }
            }
        }
        return sInstance;
    }
    /**
     * 初始化上下文，注册interface
     *
     * @param application 全局上下文
     */
    public void create(Application application) {
        FBaseTools.sApp = application.getApplicationContext();
    }

    /**
     * 控制固件升级流程是否启用
     *
     * @param status 是否启用更新固件的流程
     */
    public static void enableUpgrade(boolean status) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(KEY_UPGRADE, status).commit();
    }

    /**
     * 获取是否启用固件升级流程
     *
     * @return
     */
    public static boolean getUpgradeStatus() {
        SharedPreferences sharedPreferences =getContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_UPGRADE, false);
    }

    /**
     * 获取ApplicationContext
     *
     * @return ApplicationContext
     */
    public static Context getContext() {
        if (sApp != null) {
            return sApp;
        }
        throw new NullPointerException("should be initialized in application");
    }
}
