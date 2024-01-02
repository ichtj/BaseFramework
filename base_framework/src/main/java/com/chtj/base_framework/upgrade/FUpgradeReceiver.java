package com.chtj.base_framework.upgrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FUpgradeReceiver extends BroadcastReceiver {
    private static final String TAG = FUpgradeReceiver.class.getSimpleName();
    private static IUpgrade anInterface;

    public static void setfUpgradeInterface(IUpgrade upgradeInterface) {
        anInterface = upgradeInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: action=" + action);
        if (!FUpgradeTools.isEmpty(action)) {
            switch (action) {
                case Intent.ACTION_BOOT_COMPLETED://开机完成
                    Intent bootIntent = new Intent(context, FUpgradeService.class);
                    bootIntent.putExtra(FExtras.ACTION, FExtras.ACTION_BOOT_COMPLETE);
                    context.startService(bootIntent);
                    break;
                case Intent.ACTION_MEDIA_MOUNTED://设备接入
                    String inOtaPath = intent.getData().toString().replace("file://", "");
                    if (!inOtaPath.contains("storage/emulated")) {//防止系统重启完成之后挂载了sdcard对此服务造成影响
                        Log.d(TAG, "onReceive: inOtaPath=" + inOtaPath);
                        FUpgradeService.startServiceUgrade(context, FExtras.ACTION_USB_CONNECT, inOtaPath);
                    }
                    break;
                case Intent.ACTION_MEDIA_EJECT://设备卸载
                    String usbPath = intent.getData().getPath();
                    FUpgradeService.startServiceUgrade(context, FExtras.ACTION_USB_DISCONNECT, usbPath);
                    break;
                case FExtras.ACTION_MX8_UPDATE_RESULT:
                    int errorCode = intent.getIntExtra(FExtras.EXTRA_STATUSCODE, -1);
                    if (errorCode == FExtras.I_CHECK || errorCode == FExtras.I_COPY || errorCode == FExtras.I_INSTALLING) {
                        if (anInterface != null) {
                            anInterface.installStatus(errorCode);
                        }
                    } else {
                        if (anInterface != null) {
                            String statusStr = intent.getStringExtra(FExtras.EXTRA_STATUSSTR);
                            anInterface.error(statusStr);
                        }
                    }
                    break;
                case FExtras.ACTION_UPDATE:
                    String otaPath = intent.getStringExtra(FExtras.EXTRA_OTAPATH);
                    FUpgradeService.startServiceUgrade(context, FExtras.ACTION_USB_CONNECT, otaPath);
                    break;
            }
        }
    }
}
