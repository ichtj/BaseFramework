package com.chtj.base_framework.upgrade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.chtj.base_framework.FSettingsTools;
import com.chtj.base_framework.R;

import java.io.File;

/**
 * 固件升级服务管理
 */
public class FUpgradeService extends Service {
    private static final String TAG = FUpgradeService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 0x001;

    public static void startServiceUgrade(Context context, String action, String otaPath) {
        Intent serviceIntent = new Intent(context, FUpgradeService.class);
        serviceIntent.putExtra(FExtraTools.ACTION, action);
        serviceIntent.putExtra(FExtraTools.EXTRA_OTAPATH, otaPath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    Handler mWorkHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Intent updateIntent = new Intent(FExtraTools.ACTION_UPDATE_RESULT);
            switch (msg.what) {
                case FExtraTools.OK_RESULT://发送升级成功的广播
                    updateIntent.putExtra(FExtraTools.EXTRA_ISCOMPLETE, true);
                    updateIntent.putExtra(FExtraTools.EXTRA_ERRMEG, "");
                    Log.d(TAG, "onCreate: ota update successful!");
                    break;
                case FExtraTools.ERR_RESULT://发送升级失败的广播
                    updateIntent.putExtra(FExtraTools.EXTRA_ISCOMPLETE, false);
                    updateIntent.putExtra(FExtraTools.EXTRA_ERRMEG, msg.obj.toString());
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
            new File(FExtraTools.SAVA_FW_COPY_PATH).delete();
        } catch (Exception e) {
            Log.e(TAG, "errMeg1:", e);
        }
        try {
            new File("/data/misc/.update").delete();
        } catch (Exception e) {
            Log.e(TAG, "errMeg2:", e);
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
        String action = intent.getStringExtra(FExtraTools.ACTION);
        Log.d(TAG, "onStartCommand: action=" + action);
        switch (action) {
            case FExtraTools.ACTION_USB_CONNECT:
                String otaPath = intent.getStringExtra(FExtraTools.EXTRA_OTAPATH);
                if (!TextUtils.isEmpty(otaPath)) {
                    if (new File(otaPath).isDirectory()) {
                        otaPath = otaPath + File.separator + FExtraTools.OTA_NAME;
                    }
                    boolean isExist = new File(otaPath).exists();
                    Log.d(TAG, "onStartCommand: otaPath=" + otaPath + ",exist=" + isExist);
                    if (isExist) {
                        FUpgradeDialog.showUpdateDialog(otaPath, FUpgradeService.this);
                    }
                } else {
                    sendErrReceiver(getString(R.string.otapath_is_null));
                }
                break;
            case FExtraTools.ACTION_USB_DISCONNECT:
                FUpgradeDialog.dismissDialog(intent.getStringExtra(FExtraTools.EXTRA_OTAPATH));
                break;
            case FExtraTools.ACTION_BOOT_COMPLETE:
                if (Build.VERSION.SDK_INT >= 30) {
                    String[] upDownVersion = FUpgradeTools.readFileData("/data/misc/.update").split(";");
                    if (upDownVersion.length > 1) {
                        int checkVersion = FUpgradeTools.compareVersion(upDownVersion[0], upDownVersion[1]);
                        if (checkVersion == 1) {
                            sendCompleteReceiver();
                        } else if (checkVersion == 0) {
                            sendErrReceiver(getString(R.string.same_firmware_version));
                        } else {
                            sendErrReceiver(getString(R.string.update_zip_low_version));
                        }
                    }
                } else {
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
                                sendErrReceiver(getString(R.string.version_not_equal));
                            }
                        } else {
                            sendErrReceiver(getString(R.string.read_last_install_update_fail));
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
        msg.what = FExtraTools.ERR_RESULT;
        msg.obj = errStr;
        mWorkHandler.sendMessageDelayed(msg, 3500);
    }

    /**
     * 发送固件升级成功的广播
     */
    private void sendCompleteReceiver() {
        Message msg = new Message();
        msg.what = FExtraTools.OK_RESULT;
        mWorkHandler.sendMessageDelayed(msg, 3500);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        startNotify();
    }

    private void startNotify() {
        // 设置前台通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_id";
            String channelName = "channel_name";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle(getString(R.string.firmware_upgrade)) // 设置通知标题
                    .setContentText(getString(R.string.firmware_upgrade_service_run)) // 设置通知内容
                    .setSmallIcon(R.drawable.ic_zj) // 设置通知小图标
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        FUpgradeDialog.dismissDialog();
    }
}
