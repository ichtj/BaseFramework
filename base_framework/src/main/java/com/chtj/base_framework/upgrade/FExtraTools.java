package com.chtj.base_framework.upgrade;

public class FExtraTools {
    public static final String ACTION_UPDATE_RESULT = "android.intent.action.CN_OTA_RESULT";
    public static final String ACTION_UPDATE = "android.intent.action.CN_OTA_UPDATE";
    public static final String ACTION_MX8_UPDATE = "action.firmware.update.bypath";
    public static final String ACTION_MX8_UPDATE_RESULT = "action.firmware.update.result";

    public static final int OK_RESULT = 0x1001;//固件升级成功
    public static final int ERR_RESULT = 0x1002;//固件升级失败

    public static final String OTA_NAME="update.zip";
    //固件最终存放的地址
    public static final String SAVA_FW_COPY_PATH = "/data/"+ FExtraTools.OTA_NAME;


    public static final String ACTION="action";
    public static final String ACTION_USB_CONNECT="connect";
    public static final String ACTION_USB_DISCONNECT="disconnect";
    public static final String ACTION_BOOT_COMPLETE="boot_complete";

    public static final String EXTRA_OTAPATH="otaPath";
    public static final String EXTRA_ISCOMPLETE="isComplete";
    public static final String EXTRA_ERRMEG="errMeg";

    public static final String EXTRA_STATUSCODE="statusCode";
    public static final String EXTRA_STATUSSTR="statusStr";


    /*校验中*/
    public static final int I_CHECK = 0x101;
    /*复制到system/data下*/
    public static final int I_COPY = 0x102;
    /*安装中*/
    public static final int I_INSTALLING = 0x103;
}
