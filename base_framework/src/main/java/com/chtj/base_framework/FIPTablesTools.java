package com.chtj.base_framework;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * iptables 网络访问管理
 * 开启网络 禁用网络等
 */
public class FIPTablesTools {
    private static final String TAG = FIPTablesTools.class.getSimpleName();

    // 执行iptables命令并返回执行结果
    public static boolean executeIptablesCommand(String command) {
        try {
            FCmdTools.CommandResult commandResult=FCmdTools.execCommand(command,true);
            Log.d(TAG, "executeIptablesCommand: result>>"+commandResult.result+",succMeg>>"+commandResult.successMsg+",errMeg>>"+commandResult.errorMsg);
            return commandResult.result==0;
        } catch (Throwable e) {
            return false;
        }
    }

    // 禁止应用访问网络
    public static boolean blockAppInternet(String packageName) {
        return executeIptablesCommand("iptables -A OUTPUT -p tcp -m owner --uid-owner " + getUidByPackageName(packageName) + " -j DROP");
    }

    // 允许应用访问网络
    public static boolean allowAppInternet(String packageName) {
        return executeIptablesCommand("iptables -D OUTPUT -p tcp -m owner --uid-owner " + getUidByPackageName(packageName) + " -j DROP");
    }

    /**
     * 获取应用uid
     */
    private static int getUidByPackageName(String packageName) {
        PackageManager packageManager = FBaseTools.getContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return applicationInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
