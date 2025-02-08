package com.chtj.base_framework.upgrade;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.R;
import com.chtj.base_framework.entity.UpgradeBean;

import java.io.File;
import java.util.Arrays;

/**
 * 固件升级服务管理
 */
public class FUpgradeService extends Service {
    private static final String TAG = FUpgradeService.class.getSimpleName();

    public static void startServie(Context context){
        boolean status= FBaseTools.getUpgradeStatus();
        if (status){
            Intent bootIntent = new Intent(context, FUpgradeService.class);
            bootIntent.putExtra(FExtras.ACTION, FExtras.ACTION_BOOT_COMPLETE);
            context.startService(bootIntent);
        }
    }

    public static void startServiceUgrade(Context context, String action, String upType, String otaPath) {
        boolean status= FBaseTools.getUpgradeStatus();
        if (status) {
            Intent serviceIntent = new Intent(context, FUpgradeService.class);
            serviceIntent.putExtra(FExtras.ACTION, action);
            serviceIntent.putExtra(FExtras.EXTRA_OTAPATH, otaPath);
            serviceIntent.putExtra(FExtras.EXTRA_UP_TYPE, upType);
            context.startService(serviceIntent);
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Intent resultIntent = new Intent(FExtras.ACTION_UPDATE_RESULT);
            boolean isComplete = msg.what == FExtras.STATUS_OK;
            resultIntent.putExtra(FExtras.EXTRA_ISCOMPLETE, isComplete);
            resultIntent.putExtra(FExtras.EXTRA_ERRMEG, isComplete ? "" : msg.obj.toString());
            resultIntent.setPackage(getPackageName());
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "isComplete>>" + isComplete + ",errMeg>>" + msg.obj.toString());
            sendBroadcast(resultIntent);
            deleteUpdateReocrd();
        }
    };

    /**
     * 删除历史缓存文件 无用的固件包
     */
    public void deleteUpdateReocrd() {
        try {
            new File(FExtras.SAVA_FW_COPY_PATH).delete();
        } catch (Throwable throwable) {
        }
        try {
            new File(FExtras.MX8_UPGRADE_RESULT).delete();
        } catch (Throwable throwable) {
        }
        try {
            new File(FExtras.UPDATE_LAST_UPDATE_FILE).delete();
        } catch (Throwable throwable) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra(FExtras.ACTION);
            Log.d(TAG, "onStartCommand: action=" + action);
            if (!FUpgradeTools.isEmpty(action)) {
                switch (action) {
                    case FExtras.ACTION_USB_CONNECT:
                        String otaPath = intent.getStringExtra(FExtras.EXTRA_OTAPATH);
                        if (!TextUtils.isEmpty(otaPath)) {
                            if (new File(otaPath).isDirectory()) {
                                otaPath = otaPath + File.separator + FExtras.OTA_NAME;
                            }
                            if (new File(otaPath).exists()) {
                                String upType = intent.getStringExtra(FExtras.EXTRA_UP_TYPE);
                                if (upType.equals(FExtras.UP_TYPE_DIALOG)) {
                                    FUpgradeDialog.showUpdateDialog(otaPath, FUpgradeService.this);
                                } else {
                                    upgradeSilence(otaPath);
                                }
                            }
                        } else {
                            sendErrReceiver(getString(R.string.otapath_is_null));
                        }
                        break;
                    case FExtras.ACTION_USB_DISCONNECT:
                        FUpgradeDialog.dismissDialog(intent.getStringExtra(FExtras.EXTRA_OTAPATH));
                        break;
                    case FExtras.ACTION_BOOT_COMPLETE:
                        if (FUpgradeTools.isMx8()) {
                            String[] upDownVersion = FUpgradeTools.readFileData(FExtras.MX8_UPGRADE_RESULT).split(";");
                            Log.d(TAG, "onStartCommand: upDownVersion>>" + Arrays.toString(upDownVersion));
                            if (upDownVersion.length > 1) {
                                int diffVersion = FUpgradeTools.compareVersion(upDownVersion[0], upDownVersion[1]);
                                Log.d(TAG, "onStartCommand: diffVersion>>" + diffVersion);
                                if (diffVersion == 1) {
                                    sendCompleteReceiver();
                                } else if (diffVersion == 0) {
                                    sendErrReceiver(getString(R.string.same_firmware_version));
                                } else {
                                    sendErrReceiver(getString(R.string.update_zip_low_version));
                                }
                            }
                        }
                        String upLastUpdate = FRecoverySystemTools.readLastUpdateCommand();
                        Log.d(TAG, "onStartCommand: upLastUpdate>>" + upLastUpdate);
                        if (!FUpgradeTools.isEmpty(upLastUpdate)) {
                            String[] lastFlagResult = upLastUpdate.split(";");
                            String lastFwVersion = lastFlagResult[2];
                            String lastOtaFwVersion = lastFlagResult[3];
                            String currFwVersion = FUpgradeTools.sysFwVersion();
                            if (!lastFwVersion.equals(currFwVersion) && currFwVersion.equals(lastOtaFwVersion)) {
                                //判断升级前的版本是否与当前版本一致 以及 当前版本是否等于需要升级的固件版本
                                sendCompleteReceiver();
                            } else {
                                sendErrReceiver(getString(R.string.check_version_fail));
                            }
                        }
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    public void upgradeSilence(String otaPath) {
        FUpgradeTools.firmwareUpgrade(new UpgradeBean(otaPath, new IUpgrade() {
            @Override
            public void installStatus(int installStatus) {
                Log.d(TAG, "installStatus: >>"+installStatus);
            }

            @Override
            public void error(String error) {
                sendErrReceiver(error);
            }

            @Override
            public void warning(String warning) {
                Log.d(TAG, "warning: >>"+warning);
            }
        }));

    }

    /**
     * 发送固件升级失败的广播
     *
     * @param description 失败的原因
     */
    private void sendErrReceiver(String description) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(FExtras.STATUS_ERR, description), 3500);
    }

    /**
     * 发送固件升级成功的广播
     */
    private void sendCompleteReceiver() {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(FExtras.STATUS_OK, ""), 3500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FUpgradeDialog.dismissDialog();
    }
}
