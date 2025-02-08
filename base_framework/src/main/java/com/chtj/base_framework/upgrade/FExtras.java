package com.chtj.base_framework.upgrade;

import android.content.Context;

import com.chtj.base_framework.R;

public class FExtras {
    public static final String ACTION_UPDATE = "android.intent.action.FS_OTA_UPDATE";
    public static final String ACTION_UPDATE_RESULT = "android.intent.action.FS_OTA_RESULT";
    public static final String ACTION_MX8_UPDATE = "action.firmware.update.bypath";
    public static final String ACTION_MX8_UPDATE_RESULT = "action.firmware.update.result";

    public static final String RECOVERY_DIR = "/cache/recovery";
    //private static final String UPDATE_LAST_INSTALL_FILE = RECOVERY_DIR + "last_install";
    public static final String UPDATE_LAST_UPDATE_FILE = RECOVERY_DIR + "last_update";
    public static final String UPDATE_ZIP_VERSION_PATH = "META-INF/com/android/metadata";

    public static final String OTA_NAME="update.zip";
    public static final String SAVA_FW_COPY_PATH = "/data/"+ FExtras.OTA_NAME;//固件最终存放的地址
    public static final String MX8_UPGRADE_RESULT = "/data/misc/.update";

    public static final String ACTION="action";
    public static final String ACTION_USB_CONNECT="connect";
    public static final String ACTION_USB_DISCONNECT="disconnect";
    public static final String ACTION_BOOT_COMPLETE="boot_complete";

    public static final String EXTRA_OTAPATH="otaPath";
    public static final String EXTRA_UP_TYPE="upType";
    public static final String EXTRA_ISCOMPLETE="isComplete";
    public static final String EXTRA_ERRMEG="errMeg";

    public static final String UP_TYPE_DIALOG="dialog";
    public static final String UP_TYPE_SILENCE="silence";

    public static final String EXTRA_STATUSCODE="statusCode";
    public static final String EXTRA_STATUSSTR="statusStr";

    /*校验中*/
    public static final int I_CHECK = 0x101;
    /*复制到system/data下*/
    public static final int I_COPY = 0x102;
    /*安装中*/
    public static final int I_INSTALLING = 0x103;

    /*固件升级成功*/
    public static final int STATUS_OK = 0x1001;
    /*固件升级失败*/
    public static final int STATUS_ERR = 0x1002;



    public static String formatStatus(Context context, int status){
        if (status==I_CHECK){
            return context.getString(R.string.status_check_firmware);
        }else if(status==I_COPY){
            return context.getString(R.string.status_start_copefw);
        }else if(status==I_INSTALLING){
            return context.getString(R.string.status_start_writefw);
        }else{
            return context.getString(R.string.status_other_fail);
        }
    }
}
