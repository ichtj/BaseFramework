package com.zgkx.change;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.zgkx.change.test.MainActivity2;

public class UsbReceiver extends BroadcastReceiver {
    private static final String TAG = UsbReceiver.class.getSimpleName();//固件升级重启后的结果回复
    public static final String ACTION_FW_UPDATE_RESULT = "android.intent.action.CN_OTA_RESULT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            Intent intent1 = new Intent(Message.USB_PERMISSION);
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            intent1.putExtra(UsbManager.EXTRA_DEVICE, device);
            Log.d(TAG, "Broadcasting USB_CONNECTED device=" + device.toString());
            context.sendBroadcast(intent1);
        } else if (action.equals(ACTION_FW_UPDATE_RESULT)) {//固件升级成功
            Log.d(TAG,"onReceive:>=ACTION_UPDATE_RESULT");
            boolean isComplete = intent.getBooleanExtra("isComplete", false);
            String errMeg = intent.getStringExtra("errMeg");
            Log.d(TAG, "onReceive: isComplete="+isComplete+",errMeg="+errMeg);
        }else if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.d(TAG, "onReceive: ACTION_BOOT_COMPLETED");
            context.startActivity(new Intent(context, MainActivity.class));
        }
    }
}
