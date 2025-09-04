package com.chtj.base_framework.network;

import android.content.Context;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.FCmdTools;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;

import static android.net.NetworkStats.SET_ALL;
import static android.net.NetworkStats.TAG_NONE;
import static android.net.NetworkStatsHistory.FIELD_RX_BYTES;
import static android.net.NetworkStatsHistory.FIELD_TX_BYTES;

public class FNetworkTools {
    private static final String TAG = "FNetworkTools";

    /**
     * 获取dns
     *
     * @return
     */
    public static String[] getNetWorkDns() {
        FCmdTools.CommandResult commandResult = FCmdTools.execCommand("getprop | grep net.dns", true);
        if (commandResult.result == 0 && commandResult.successMsg != null) {
            if (commandResult.successMsg.length() > 0) {
                String[] result = commandResult.successMsg.replace("]: [", ":").replace("][", "];[").split(";");
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 获取该uid的流量消耗
     *
     * @param uid 应用uid
     * @return
     */
    public static String getEthUsage(int uid) {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 24) {
            long total = getEthAppUsage(uid, getTimesMonthMorning(), getNow());
            String totalPhrase = Formatter.formatFileSize(FBaseTools.getContext(), total);
            return totalPhrase;
        } else {
            long receiveRx = TrafficStats.getUidRxBytes(uid);//获取某个网络UID的接受字节数 总接收量
            long sendTx = TrafficStats.getUidTxBytes(uid);//获取某个网络UID的发送字节数 总接收量
            double traffic = receiveRx + sendTx;
            double sumTraffic = getDouble(traffic / 1024 / 1024);
            return sumTraffic + "MB";
        }
    }

    public static double getDouble(double d) {
        return (double) Math.round(d * 100) / 100;
    }

    /**
     * 获取系统总计消耗的以太网流量
     */
    public static long getEthTotalUsage(long startTime, final long endTime) {
        long value = 0;
        try {
            INetworkStatsService mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            mStatsService.forceUpdate();
            INetworkStatsSession mStatsSession = mStatsService.openSession();
            NetworkTemplate mTemplate = NetworkTemplate.buildTemplateEthernet();
            NetworkStatsHistory networkStatsHistory = mStatsSession.getHistoryForNetwork(mTemplate, NetworkStatsHistory.FIELD_ALL);
            NetworkStatsHistory.Entry entry = null;
            entry = networkStatsHistory.getValues(startTime, endTime, System.currentTimeMillis(), entry);
            value = entry != null ? entry.rxBytes + entry.txBytes : 0;
            mStatsSession.close();
        } catch (RemoteException e) {
        }
        return value;
    }

    /**
     * 获取系统总计消耗的移动数据流量
     */
    public static long getMobileTotalUsage(long startTime, long endTime) {
        long value = 0;
        try {
            INetworkStatsService mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            mStatsService.forceUpdate();
            INetworkStatsSession mStatsSession = mStatsService.openSession();
            NetworkTemplate mTemplate = NetworkTemplate.buildTemplateMobileWildcard();
            NetworkStatsHistory networkStatsHistory = mStatsSession.getHistoryForNetwork(mTemplate, NetworkStatsHistory.FIELD_RX_BYTES | NetworkStatsHistory.FIELD_TX_BYTES);
            NetworkStatsHistory.Entry entry = null;
            entry = networkStatsHistory.getValues(startTime, endTime, System.currentTimeMillis(), entry);
            value = entry != null ? entry.rxBytes + entry.txBytes : 0;
            mStatsSession.close();
        } catch (RemoteException e) {
        }
        return value;
    }

    /**
     * 获取系统总计消耗的 Wi-Fi 流量
     */
    public static long getWifiTotalUsage(long startTime, long endTime) {
        long value = 0;
        try {
            INetworkStatsService mStatsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            mStatsService.forceUpdate();
            INetworkStatsSession mStatsSession = mStatsService.openSession();
            NetworkTemplate mTemplate = NetworkTemplate.buildTemplateWifiWildcard();
            NetworkStatsHistory history = mStatsSession.getHistoryForNetwork(mTemplate,
                    NetworkStatsHistory.FIELD_RX_BYTES | NetworkStatsHistory.FIELD_TX_BYTES);
            NetworkStatsHistory.Entry entry = null;
            entry = history.getValues(startTime, endTime, System.currentTimeMillis(), entry);
            value = entry != null ? entry.rxBytes + entry.txBytes : 0;
            mStatsSession.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 根据UID获取该应用的以太网上下行流量
     */
    public static long getEthAppUsage(int uid, long startTime, long endTime) {
        long value = 0;
        try {
            INetworkStatsService mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            mStatsService.forceUpdate();
            INetworkStatsSession mStatsSession = mStatsService.openSession();
            NetworkTemplate mTemplate = NetworkTemplate.buildTemplateEthernet();
            NetworkStatsHistory networkStatsHistory = mStatsSession.getHistoryForUid(mTemplate, uid, SET_ALL, TAG_NONE, FIELD_RX_BYTES | FIELD_TX_BYTES);

            NetworkStatsHistory.Entry entry = null;
            entry = networkStatsHistory.getValues(startTime, endTime, System.currentTimeMillis(), entry);
            value = entry != null ? entry.rxBytes + entry.txBytes : 0;
            mStatsSession.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 根据 UID 获取应用 Wi-Fi 流量
     */
    public static long getWifiAppUsage(int uid, long startTime, long endTime) {
        long value = 0;
        try {
            INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            statsService.forceUpdate();
            INetworkStatsSession statsSession = statsService.openSession();
            NetworkTemplate template = NetworkTemplate.buildTemplateWifiWildcard();
            NetworkStatsHistory history = statsSession.getHistoryForUid(template, uid, NetworkStats.SET_ALL, NetworkStats.TAG_NONE,
                    NetworkStatsHistory.FIELD_RX_BYTES | NetworkStatsHistory.FIELD_TX_BYTES);
            NetworkStatsHistory.Entry entry = null;
            entry = history.getValues(startTime, endTime, System.currentTimeMillis(), entry);
            value = entry != null ? entry.rxBytes + entry.txBytes : 0;
            statsSession.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 根据 UID 获取应用移动数据流量
     */
    public static long getMobileAppUsage(int uid, long startTime, long endTime) {
        long value = 0;
        try {
            INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            statsService.forceUpdate();
            INetworkStatsSession statsSession = statsService.openSession();
            NetworkTemplate template = NetworkTemplate.buildTemplateMobileWildcard();
            NetworkStatsHistory history = statsSession.getHistoryForUid(template, uid, NetworkStats.SET_ALL, NetworkStats.TAG_NONE,
                    NetworkStatsHistory.FIELD_RX_BYTES | NetworkStatsHistory.FIELD_TX_BYTES);
            NetworkStatsHistory.Entry entry = null;
            entry = history.getValues(startTime, endTime, System.currentTimeMillis(), entry);
            value = entry != null ? entry.rxBytes + entry.txBytes : 0;
            statsSession.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 获取据当前时间的一个月之前
     */
    public static long getTimesMonthMorning() {
        Date currentTime = new Date();
        long now = currentTime.getTime();
        currentTime = new Date(now - 86400000 * 24);
        long now1 = currentTime.getTime();
        currentTime = new Date(now1 - 86400000 * 6);
        return currentTime.getTime();
    }

    /**
     * 获取昨天的时间戳
     */
    public static long getYesterDayTime() {
        long nowTime = System.currentTimeMillis();
        long yesterdayTime = nowTime - 86400000;
        return yesterdayTime;
    }

    /**
     * 得到现在的时间戳
     */
    public static long getNow() {
        long lTime = Calendar.getInstance().getTimeInMillis();
        return lTime;
    }
}

