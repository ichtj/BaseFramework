package com.chtj.base_framework.upgrade;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.File;

/**
 * 固件升级服务管理
 */
public class FUpgradeService extends Service {
    private static final String TAG = "FUpgradeService";
    public static final String ACTION_UPDATE_RESULT = "android.intent.action.CN_OTA_RESULT";
    public static final int OK_RESULT = 0x1001;//固件升级成功
    public static final int ERR_RESULT = 0x1002;//固件升级失败

    Handler mWorkHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Intent updateIntent = new Intent(ACTION_UPDATE_RESULT);
            switch (msg.what) {
                case OK_RESULT://发送升级成功的广播
                    updateIntent.putExtra("isComplete", true);
                    updateIntent.putExtra("errMeg", "");
                    Log.d(TAG, "onCreate: ota update successful!");
                    break;
                case ERR_RESULT://发送升级失败的广播
                    updateIntent.putExtra("isComplete", false);
                    updateIntent.putExtra("errMeg", msg.obj.toString());
                    Log.d(TAG, "onCreate: ota update failed! errMeg=" + msg.obj.toString());
                    break;
            }
            updateIntent.setPackage(getPackageName());
            updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendBroadcast(updateIntent);
            deleteUpdateReocrd();
        }
    };

    /**
     * 删除历史缓存文件 无用的固件包
     */
    public void deleteUpdateReocrd() {
        try {
            new File("/data/update.zip").delete();
        } catch (Exception e) {
            Log.e(TAG, "errMeg1:" ,e);
        }
        try {
            new File("/data/misc/.update").delete();
        }catch (Exception e){
            Log.e(TAG,"errMeg2:",e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        String action = intent.getStringExtra("action");
        Log.d(TAG, "onStartCommand: action=" + action);
        switch (action) {
            case "connect":
                String path = intent.getStringExtra("otaPath");
                String otaPath = path + "/update.zip";
                File file = new File(otaPath);
                boolean isExist = file.exists();
                Log.d(TAG, "onStartCommand: otaPath=" + otaPath + ",exist=" + isExist);
                if (isExist) {
                    FUpgradeDialog.instance().showUpdateDialog(otaPath, FUpgradeService.this);
                }
                break;
            case "disconnect":
                FUpgradeDialog.instance().dismissDialog();
                break;
            case "reboot":
                if(Build.VERSION.SDK_INT>=30){
                    String[] upDownVersion=FUpgradeTools.readFileData("/data/misc/.update").split(";");
                    if(upDownVersion.length>1){
                        int checkVersion=FUpgradeTools.compareVersion(upDownVersion[0],upDownVersion[1]);
                        if(checkVersion==1){
                            sendCompleteReceiver();
                        }else if(checkVersion==0){
                            sendErrReceiver("Same firmware version");
                        }else{
                            sendErrReceiver("Update.zip Low version");
                        }
                    }else{
                        sendErrReceiver("firmware version not recorded");
                    }
                }else{
                    //获取cache/recovery/last_install文件
                    String upLastInstall = FRecoverySystemTools.readLastInstallCommand();
                    //获取cache/recovery/last_flag文件
                    String upLastUpdate = FRecoverySystemTools.readLastUpdateCommand();
                    Log.d(TAG, "onStartCommand: upLastInstall=" + upLastInstall + ",upLastUpdate=" + upLastUpdate);
                    if (upLastInstall != null && upLastUpdate != null) {
                        String[] lastInstallResult = upLastInstall.split("\n");
                        String[] lastFlagResult = upLastUpdate.split(";");
                        if (lastInstallResult.length > 1 && lastFlagResult.length == 4) {
                            String currentVersion = FRecoverySystemTools.getCurrentFwVersion();
                            if (lastInstallResult[1].equals("1") && lastFlagResult[3].equals(currentVersion)) {
                                sendCompleteReceiver();
                            } else {
                                sendErrReceiver("last_install !=1 or currentFwVersion!=upFwVersion");
                            }
                        } else {
                            sendErrReceiver("last_install file and last_update file err 0");
                        }
                    }
                }
                break;
        }
        return START_NOT_STICKY;
    }

    /**
     * 发送固件升级失败的广播
     *
     * @param errStr 失败的原因
     */
    private void sendErrReceiver(String errStr) {
        Message msg = new Message();
        msg.what = ERR_RESULT;
        msg.obj = errStr;
        mWorkHandler.sendMessageDelayed(msg, 3500);
    }

    /**
     * 发送固件升级成功的广播
     */
    private void sendCompleteReceiver() {
        Message msg = new Message();
        msg.what = OK_RESULT;
        mWorkHandler.sendMessageDelayed(msg, 3500);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        FUpgradeDialog.instance().dismissDialog();
    }
}
