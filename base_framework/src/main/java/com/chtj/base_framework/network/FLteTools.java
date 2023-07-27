package com.chtj.base_framework.network;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.chtj.base_framework.FBaseTools;

import java.lang.reflect.Method;

public class FLteTools {
    private static volatile FLteTools sInstance;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager tm;
    private SignalStrength signalStrength;

    /**
     * 单例模式
     *
     * @return
     */
    private static FLteTools instance() {
        if (sInstance == null) {
            synchronized (FLteTools.class) {
                if (sInstance == null) {
                    sInstance = new FLteTools();
                }
            }
        }
        return sInstance;
    }

    public static void init() {
        instance().tm = (TelephonyManager) FBaseTools.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        instance().phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                instance().signalStrength = signalStrength;
            }
        };
        instance().tm.listen(instance().phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                | PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    /**
     * 获取dbm
     * @return
     */
    public static String getDbm() {
        String dbmAsu = 0 + " dBm " + 0 + " asu";
        try {
            Method method1 = instance().signalStrength.getClass().getMethod("getDbm");
            int signalDbm = (int) method1.invoke(instance().signalStrength);
            method1 = instance().signalStrength.getClass().getMethod("getAsuLevel");
            int signalAsu = (int) method1.invoke(instance().signalStrength);
            if (-1 == signalDbm) {
                signalDbm = 0;
            }
            if (-1 == signalAsu) {
                signalAsu = 0;
            }
            dbmAsu = signalDbm + " dBm " + signalAsu + " asu";
        } catch (Exception e) {
            dbmAsu = 0 + " dBm " + 0 + " asu";
        }
        return dbmAsu;
    }

    /**
     * 关闭4G信号监听
     */
    public static void cancel() {
        if(instance().tm!=null){
            instance().tm.listen(instance().phoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
            instance().tm = null;
        }
        instance().phoneStateListener = null;
    }
}
