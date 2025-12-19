package com.chtj.base_framework.network;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
/**
 * rk3568工具类：根据 uid 和 网络类型 获取流量使用情况
 */
public class TrafficStatsHelper {
    private static final String TEST_SUBSCRIBER_PROP = "test.subscriberid";
    public static final int NETWORK_4G = 4;   // "4G" networks
    public static final int NETWORK_ETH = 9;  // ETH networks
    public static final int NETWORK_WIFI = 1; // Wi-Fi network

    private static final String TAB_3G = "3g";
    private static final String TAB_4G = "4g";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";
    private static final String TAB_ETHERNET = "ethernet";

    private INetworkStatsService mStatsService;
    private INetworkStatsSession mStatsSession;
    private Context mContext;

    // 单例对象
    private static TrafficStatsHelper instance;

    // 私有构造
    private TrafficStatsHelper(Context context) {
        mContext = context.getApplicationContext();
        try {
            mStatsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            mStatsSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException("无法打开网络统计服务", e);
        }
    }

    // 获取单例实例
    public static synchronized TrafficStatsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TrafficStatsHelper(context);
        }
        return instance;
    }

    /**
     * 根据网络类型生成 NetworkTemplate
     */
    private NetworkTemplate buildTemplate(String netType) {
        if (TAB_MOBILE.equals(netType)) {
            return NetworkTemplate.buildTemplateMobileAll(getActiveSubscriberId(mContext));
        } else if (TAB_3G.equals(netType)) {
            return NetworkTemplate.buildTemplateMobile3gLower(getActiveSubscriberId(mContext));
        } else if (TAB_4G.equals(netType)) {
            return NetworkTemplate.buildTemplateMobile4g(getActiveSubscriberId(mContext));
        } else if (TAB_WIFI.equals(netType)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        } else if (TAB_ETHERNET.equals(netType)) {
            return NetworkTemplate.buildTemplateEthernet();
        } else {
            throw new IllegalArgumentException("未知网络类型: " + netType);
        }
    }

    public String getNetType(int netType){
        switch (netType){
            case NETWORK_4G:
                return TAB_4G;
            case NETWORK_ETH:
                return TAB_ETHERNET;
            case NETWORK_WIFI:
                return TAB_WIFI;
            default:
                return "-1";
        }
    }

    /**
     * 获取某个 UID 在指定时间段内的流量 (单位：字节)
     */
    public long getUidBytes(int uid, String netType, long startTime, long endTime) {
        try {
            NetworkTemplate template = buildTemplate(netType);
            NetworkStats stats = mStatsSession.getSummaryForAllUid(template, startTime, endTime, false);

            NetworkStats.Entry entry = null;
            final int size = stats != null ? stats.size() : 0;
            for (int i = 0; i < size; i++) {
                entry = stats.getValues(i, entry);
                if (entry.uid == uid) {
                    return entry.rxBytes + entry.txBytes;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    /**
     * 获取 Wi-Fi 总流量
     */
    public long getWifiTotalBytes(long startTime, long endTime) {
        return getTotalBytesByType(TAB_WIFI, startTime, endTime);
    }

    /**
     * 获取以太网总流量
     */
    public long getEthTotalBytes(long startTime, long endTime) {
        return getTotalBytesByType(TAB_ETHERNET, startTime, endTime);
    }

    /**
     * 获取 4G 总流量
     */
    public long getMobile4GTotalBytes(long startTime, long endTime) {
        return getTotalBytesByType(TAB_4G, startTime, endTime);
    }

    /**
     * 获取 4G 总流量
     */
    public long getMobileMoBileTotalBytes(long startTime, long endTime) {
        return getTotalBytesByType(TAB_MOBILE, startTime, endTime);
    }

    /**
     * 统计指定网络类型的总流量
     */
    private long getTotalBytesByType(String netType, long startTime, long endTime) {
        long total = 0L;
        try {
            NetworkTemplate template = buildTemplate(netType);
            NetworkStats stats = mStatsSession.getSummaryForAllUid(template, startTime, endTime, false);
            NetworkStats.Entry entry = null;
            final int size = stats != null ? stats.size() : 0;
            for (int i = 0; i < size; i++) {
                entry = stats.getValues(i, entry);
                total += entry.rxBytes + entry.txBytes;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * 根据包名获取 uid
     */
    public int getUidByPackageName(String packageName) {
        try {
            ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            return info.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 获取订阅 ID
     */
    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final String actualSubscriberId = tele.getSubscriberId();
        return SystemProperties.get(TEST_SUBSCRIBER_PROP, actualSubscriberId);
    }

    /**
     * 关闭 session
     */
    public void close() {
        mStatsSession = null;
        instance = null;
    }
}



