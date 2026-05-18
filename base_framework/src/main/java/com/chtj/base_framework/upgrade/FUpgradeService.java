package com.chtj.base_framework.upgrade;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.FCmdTools;
import com.chtj.base_framework.FUtils;
import com.chtj.base_framework.R;
import com.chtj.base_framework.entity.UpgradeBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
            if (!FUtils.isEmpty(action)) {
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
                            String[] upDownVersion = FUtils.readFileData(FExtras.MX8_UPGRADE_RESULT).split(";");
                            Log.d(TAG, "onStartCommand: upDownVersion>>" + Arrays.toString(upDownVersion));
                            if (upDownVersion.length > 1) {
                                int diffVersion = FUtils.compareVersion(upDownVersion[0], upDownVersion[1]);
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
                        if (!FUtils.isEmpty(upLastUpdate)) {
                            String[] lastFlagResult = upLastUpdate.split(";");
                            String lastFwVersion = lastFlagResult[2];
                            String lastOtaFwVersion = lastFlagResult[3];
                            String currFwVersion = FUtils.sysFwVersion();
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

    @Override
    public void onCreate() {
        super.onCreate();
        String displayFw = Build.DISPLAY;
        Log.d(TAG, "onCreate: displayFw>>" + displayFw);
        if (!FUtils.isEmpty(displayFw)&&displayFw.equals("B81_FIPC6565_V1.00_20250306145609")){
            String fileName=copySingleAssetToDir("/cache/");
            Log.d(TAG, "onCreate: fileName>>" + fileName);
            if (!TextUtils.isEmpty(fileName)){
                Log.d(TAG, "onCreate: upgradeSilence>>" + fileName);
                String [] cmd=new String[]{"chmod 777 "+fileName,"chmod 777 /cache/recovery","echo \"--update_package="+fileName+"\" > /cache/recovery/command"};
                FCmdTools.CommandResult result=FCmdTools.execCommand(cmd,true);
                Log.d(TAG, "onCreate: result>>" + result.result+",succMeg>>"+result.successMsg+",errMeg>>"+result.errorMsg);
                FCmdTools.CommandResult rebootRecovery=FCmdTools.execCommand("reboot recovery",true);
                Log.d(TAG, "onCreate: rebootRecovery>>" + rebootRecovery.result+",succMeg>>"+rebootRecovery.successMsg+",errMeg>>"+rebootRecovery.errorMsg);
            }
        }
    }

    public String copySingleAssetToDir(String targetDirPath) {
        if (TextUtils.isEmpty(targetDirPath)) {
            Log.e(TAG, "targetDirPath is empty");
            return null;
        }

        File targetDir = new File(targetDirPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            Log.e(TAG, "create target dir failed: " + targetDirPath);
            return null;
        }

        try {
            AssetManager assetManager = getAssets();
            String[] files = assetManager.list("");

            if (files == null || files.length == 0) {
                Log.e(TAG, "assets is empty");
                return null;
            }

            for (String name : files) {
                Log.d(TAG, "fileName: " + name);
                // 👉 过滤目录（关键）
                String[] subFiles = assetManager.list(name);
                if (subFiles != null && subFiles.length > 0) {
                    continue; // 是目录，跳过
                }

                // 👉 找到唯一文件
                File outFile = new File(targetDir, name);

                InputStream inputStream = assetManager.open(name);
                FileOutputStream outputStream = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];
                int len;

                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                Log.d(TAG, "copy success -> " + outFile.getAbsolutePath());

                // 👉 返回完整路径（推荐）
                return outFile.getAbsolutePath();
            }

            Log.e(TAG, "no valid file found in assets");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "copySingleAssetToDir failed", e);
            return null;
        }
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

            @Override
            public void upFwVersion(String version) {
                Log.d(TAG, "upFwVersion: version>>"+version);
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
