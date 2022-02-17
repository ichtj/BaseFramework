package com.chtj.base_framework.upgrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.util.SparseArray;

public class FUpgradeReceiver extends BroadcastReceiver {
    private static final String TAG = "OtaUpgradeReceiver";
    private static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    private static final String ACTION_RESULT = "action.firmware.update.result";
    private static FUpgradeInterface fUpgradeInterface;

    private static final SparseArray<String> CODE_TO_NAME_MAP = new SparseArray<>();

    static {
        CODE_TO_NAME_MAP.put(0, "SUCCESS");
        CODE_TO_NAME_MAP.put(1, "ERROR");
        CODE_TO_NAME_MAP.put(4, "FILESYSTEM_COPIER_ERROR");
        CODE_TO_NAME_MAP.put(5, "POST_INSTALL_RUNNER_ERROR");
        CODE_TO_NAME_MAP.put(6, "PAYLOAD_MISMATCHED_TYPE_ERROR");
        CODE_TO_NAME_MAP.put(7, "INSTALL_DEVICE_OPEN_ERROR");
        CODE_TO_NAME_MAP.put(8, "KERNEL_DEVICE_OPEN_ERROR");
        CODE_TO_NAME_MAP.put(9, "DOWNLOAD_TRANSFER_ERROR");
        CODE_TO_NAME_MAP.put(10, "PAYLOAD_HASH_MISMATCH_ERROR");
        CODE_TO_NAME_MAP.put(11, "PAYLOAD_SIZE_MISMATCH_ERROR");
        CODE_TO_NAME_MAP.put(12, "DOWNLOAD_PAYLOAD_VERIFICATION_ERROR");
        CODE_TO_NAME_MAP.put(15, "NEW_ROOTFS_VERIFICATION_ERROR");
        CODE_TO_NAME_MAP.put(20, "DOWNLOAD_STATE_INITIALIZATION_ERROR");
        CODE_TO_NAME_MAP.put(26, "DOWNLOAD_METADATA_SIGNATURE_MISMATCH");
        CODE_TO_NAME_MAP.put(48, "USER_CANCELLED");
        CODE_TO_NAME_MAP.put(52, "UPDATED_BUT_NOT_ACTIVE");
    }

    public static void setfUpgradeInterface(FUpgradeInterface upgradeInterface) {
        fUpgradeInterface=upgradeInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED://开机完成
                Log.d(TAG, "onReceive: action=" + action);
                Intent bootIntent = new Intent(context, FUpgradeService.class);
                bootIntent.putExtra("action", "reboot");
                context.startService(bootIntent);
                break;
            case Intent.ACTION_MEDIA_MOUNTED://设备接入
                String inOtaPath = intent.getData().toString().replace("file://", "");
                if (!inOtaPath.contains("storage/emulated")) {//防止系统重启完成之后挂载了sdcard对此服务造成影响
                    Log.d(TAG, "onReceive: action=" + action + ", inOtaPath=" + inOtaPath);
                    Intent serviceIntent = new Intent(context, FUpgradeService.class);
                    serviceIntent.putExtra("action", "connect");
                    serviceIntent.putExtra("otaPath", inOtaPath);
                    context.startService(serviceIntent);
                }
                break;
            case Intent.ACTION_MEDIA_UNMOUNTED://设备卸载
                String unOtaPath = intent.getData().toString().replace("file://", "");
                Log.d(TAG, "onReceive: action=" + action + ", unOtaPath=" + unOtaPath);
                Intent unMounted = new Intent(context, FUpgradeService.class);
                unMounted.putExtra("action", "disconnect");
                unMounted.putExtra("otaPath", unOtaPath);
                context.startService(unMounted);
                break;
            case ACTION_USB_STATE://usb状态
                Log.d(TAG, "onReceive: action=" + action);
                break;
            case UsbManager.ACTION_USB_DEVICE_ATTACHED://usb设备已接入
            case UsbManager.ACTION_USB_ACCESSORY_ATTACHED:
                //UsbDevice idevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                //Log.d(TAG, "onReceive: action=" + action + ", USB Connected.. idevice=" + idevice);
                //Log.d(TAG, "onReceive: device ATTACHED");
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED://usb设备已卸载
            case UsbManager.ACTION_USB_ACCESSORY_DETACHED:
                //UsbDevice odevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                //Log.d(TAG, "onReceive: action=" + action + ", USB DisConnected..");
                //Log.d(TAG, "onReceive: device DETACHED");
                break;
            case ACTION_RESULT:
                int errorCode = intent.getIntExtra("errcode", -1);
                Log.d(TAG, "onReceive: ACTION_RESULT>" + errorCode);
                if (errorCode == 0) {
                    try {
                        if(fUpgradeInterface!=null){
                            fUpgradeInterface.installStatus(FUpgradeTools.I_INSTALLING);
                        }
                        Intent rebootIntent = new Intent(Intent.ACTION_REBOOT);
                        rebootIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(rebootIntent);
                    } catch (Exception ex) {
                        Log.e(TAG, "onReceive: ", ex);
                    }
                }else{
                    if(fUpgradeInterface!=null){
                        fUpgradeInterface.error(getCodeName(errorCode));
                    }
                }
                break;
        }
    }

    /**
     * converts error code to error name
     */
    public static String getCodeName(int errorCode) {
        return CODE_TO_NAME_MAP.get(errorCode);
    }
}
