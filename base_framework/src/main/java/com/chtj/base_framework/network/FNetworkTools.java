package com.chtj.base_framework.network;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

public class FNetworkTools {
    private static final String TAG = "FNetworkTools";
    /**
     * 获取该uid的流量消耗
     *
     * @param uid 应用uid
     * @return
     */
    /**
     * 获取流量使用情况
     * @param type 0: Ethernet, 1: WiFi, 2: Mobile
     * @param uid 应用UID
     * @param startTime 开始时间 单位：毫秒
     * @param endTime 结束时间 单位：毫秒
     * @return 流量使用总和，单位：字节
     */
    public static long getUsageByReflection(
            Context context, int type, int uid, long startTime, long endTime) {
        Object statsSession = null;
        try {
            Log.d(TAG, "getUsageByReflection type=" + type + ", uid=" + uid
                    + ", startTime=" + startTime + ", endTime=" + endTime);

            Class<?> smClazz = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = smClazz.getMethod("getService", String.class);
            IBinder binder = (IBinder) getServiceMethod.invoke(null, "netstats");
            if (binder == null) return 0;

            Class<?> stubClazz = Class.forName("android.net.INetworkStatsService$Stub");
            Method asInterfaceMethod = stubClazz.getMethod("asInterface", IBinder.class);
            Object statsService = asInterfaceMethod.invoke(null, binder);
            if (statsService == null) return 0;

            try {
                statsService.getClass().getMethod("forceUpdate").invoke(statsService);
            } catch (Throwable e) {
                Log.w(TAG, "forceUpdate failed, continue", e);
            }

            statsSession = statsService.getClass().getMethod("openSession").invoke(statsService);

            Class<?> templateClazz = Class.forName("android.net.NetworkTemplate");
            Object template;

            if (type == 0) {
                template = templateClazz.getMethod("buildTemplateEthernet").invoke(null);
            } else if (type == 1) {
                template = templateClazz.getMethod("buildTemplateWifiWildcard").invoke(null);
            } else {
                String subscriberId = getDefaultSubscriberId(context);
                Log.d(TAG, "mobile subscriberId=" + (subscriberId == null ? "null" : "not-null"));

                if (subscriberId == null || subscriberId.length() == 0) {
                    Log.e(TAG, "4G usage query failed: subscriberId is null");
                    return 0;
                }

                template = templateClazz
                        .getMethod("buildTemplateMobileAll", String.class)
                        .invoke(null, subscriberId);
            }

            int fields = 0x02 | 0x08; // FIELD_RX_BYTES | FIELD_TX_BYTES

            Method getHistoryMethod = statsSession.getClass().getMethod(
                    "getHistoryForUid",
                    templateClazz,
                    int.class,
                    int.class,
                    int.class,
                    int.class
            );

            Object history = getHistoryMethod.invoke(
                    statsSession,
                    template,
                    uid,
                    -1,
                    0,
                    fields
            );

            Class<?> entryClazz = Class.forName("android.net.NetworkStatsHistory$Entry");
            Method getValuesMethod = history.getClass().getMethod(
                    "getValues",
                    long.class,
                    long.class,
                    long.class,
                    entryClazz
            );

            Object entry = getValuesMethod.invoke(
                    history,
                    startTime,
                    endTime,
                    System.currentTimeMillis(),
                    null
            );

            if (entry == null) return 0;

            long rx = entry.getClass().getField("rxBytes").getLong(entry);
            long tx = entry.getClass().getField("txBytes").getLong(entry);
            return rx + tx;
        } catch (Throwable e) {
            Log.e(TAG, "反射获取流量失败", e);
            return 0;
        } finally {
            if (statsSession != null) {
                try {
                    statsSession.getClass().getMethod("close").invoke(statsSession);
                } catch (Throwable e) {
                    Log.w(TAG, "close NetworkStatsSession failed", e);
                }
            }
        }
    }

    private static String getDefaultSubscriberId(Context context) {
        try {
            int subId = -1;
            try {
                Class<?> subMgrClass = Class.forName("android.telephony.SubscriptionManager");
                Method getDefaultDataSubId =
                        subMgrClass.getMethod("getDefaultDataSubscriptionId");
                Object value = getDefaultDataSubId.invoke(null);
                if (value instanceof Integer) {
                    subId = (Integer) value;
                }
            } catch (Throwable e) {
                Log.w(TAG, "get default data subId failed", e);
            }

            Object telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) return null;

            Class<?> tmClass = Class.forName("android.telephony.TelephonyManager");

            if (subId >= 0) {
                try {
                    Method createForSubId =
                            tmClass.getMethod("createForSubscriptionId", int.class);
                    telephonyManager = createForSubId.invoke(telephonyManager, subId);
                } catch (Throwable e) {
                    Log.w(TAG, "createForSubscriptionId failed, use default TelephonyManager", e);
                }
            }

            try {
                Method getSubscriberId = telephonyManager.getClass().getMethod("getSubscriberId");
                return (String) getSubscriberId.invoke(telephonyManager);
            } catch (Throwable e) {
                Log.w(TAG, "TelephonyManager.getSubscriberId failed", e);
            }

            if (subId >= 0) {
                try {
                    Method getSubscriberIdWithSubId =
                            telephonyManager.getClass().getMethod("getSubscriberId", int.class);
                    return (String) getSubscriberIdWithSubId.invoke(telephonyManager, subId);
                } catch (Throwable e) {
                    Log.w(TAG, "TelephonyManager.getSubscriberId(subId) failed", e);
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "getDefaultSubscriberId failed", e);
        }
        return null;
    }
}

