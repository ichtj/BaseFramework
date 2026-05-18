package com.chtj.base_framework.upgrade;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.RecoverySystem;
import android.util.Log;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.FStorageTools;
import com.chtj.base_framework.FSysPropertiesTools;
import com.chtj.base_framework.FUtils;
import com.chtj.base_framework.R;
import com.chtj.base_framework.entity.UpgradeBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 更新工具类
 * 针对固件升级 APK等
 * 请将任务放置到子线程中调用 防止执行超时
 */
public class FUpgradeTools {
    private static final String TAG = FUpgradeTools.class.getSimpleName();
    /**
     * 固件系统升级
     *
     * @param upBean
     */
    public static void firmwareUpgrade(UpgradeBean upBean) {
        Context context = FBaseTools.getContext();
        if (FUpgradeTools.isMx8()) {
            FUpgradeReceiver.setfUpgradeInterface(upBean.getiUpgrade());
            upBean.getiUpgrade().installStatus(FExtras.I_CHECK);
            Intent intent = new Intent(FExtras.ACTION_MX8_UPDATE);
            intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            intent.putExtra("path", upBean.getFilePath());
            FBaseTools.getContext().sendBroadcast(intent);
        } else {
            if (FUpgradePool.isTaskEnd()) {
                //判断是否有任务正在执行 有则忽略 无则向下执行
                FUpgradePool.addExecuteTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            upBean.getiUpgrade().installStatus(FExtras.I_COPY);
                            File file=null;
                            int sdk = Build.VERSION.SDK_INT;
                            if (sdk>=30&& FSysPropertiesTools.getSysBoolValue("persist.fw.model",false)){
                                int fsType=FStorageTools.getFileSystem();
                                Log.d(TAG, "run: fsType>>"+fsType);
                                if (fsType==0){
                                    file=new File(FExtras.SAVA_FW_COPY_PATH);
                                    boolean isUnZipSucc=FRecoverySystemTools. extractInnerZipToFile(new File(upBean.getFilePath()).getAbsoluteFile(), FExtras.OTA_EXT4_NAME,file);
                                    if (!isUnZipSucc){
                                        upBean.getiUpgrade().error(context.getString(R.string.firmware_not_exist_ext4));
                                        return;
                                    }
                                }else if (fsType==1){
                                    file = new File(FExtras.SAVA_FW_COPY_PATH);
                                    boolean isUnZipSucc=FRecoverySystemTools. extractInnerZipToFile(new File(upBean.getFilePath()).getAbsoluteFile(), FExtras.OTA_NAME,file);
                                    if (!isUnZipSucc){
                                        upBean.getiUpgrade().error(context.getString(R.string.firmware_not_exist_ext4));
                                        return;
                                    }
                                }else{
                                    upBean.getiUpgrade().error(context.getString(R.string.unrecognized_filesystem));
                                    return;
                                }
                            }else{
                                boolean isSucc=FRecoverySystemTools.copyFile(upBean.getFilePath(), FExtras.SAVA_FW_COPY_PATH);
                                if (!isSucc){
                                    upBean.getiUpgrade().error(context.getString(R.string.firmware_donot_exist));
                                    return;
                                }
                                file=new File(FExtras.SAVA_FW_COPY_PATH);
                            }
                            if(file.exists()) {
                                Log.d(TAG, "file_path: "+file.getAbsolutePath());
                                upBean.getiUpgrade().installStatus(FExtras.I_CHECK);
                                RecoverySystem.verifyPackage(file, new RecoverySystem.ProgressListener() {
                                    @SuppressLint("WrongConstant")
                                    @Override
                                    public void onProgress(int progress) {
                                        if (progress == 100) {
                                            try {
                                                FRecoverySystemTools.installPackage(FBaseTools.getContext(),upBean.getiUpgrade(), new File(FExtras.SAVA_FW_COPY_PATH));
                                            } catch (Throwable e) {
                                                upBean.getiUpgrade().error(e.getMessage());
                                            }
                                        }
                                    }}, null);
                            }else{
                                upBean.getiUpgrade().error(context.getString(R.string.firmware_donot_exist));
                            }
                        } catch (Throwable e) {
                            upBean.getiUpgrade().error(e.getMessage());
                        }
                    }
                });
            } else {
                upBean.getiUpgrade().warning(context.getString(R.string.upgrade_task_repeat));
            }
        }
    }

    /**
     * 获取update.zip的固件中的第一个版本信息
     *
     * @param fwInfo 固件信息
     * @return 版本
     */
    public static String getFirstPkgVersion(String fwInfo) {
        try (BufferedReader reader = new BufferedReader(new StringReader(fwInfo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("post-build=")) {
                    return FUtils.getMatches(line, FUtils.REGULAR_GET_VERSION).get(0).replace("V", "").replace("v", "");
                }
            }
        } catch (Throwable e) {
        }
        return "";
    }

    public static int getPostBuildCount(String otaData) {
        Pattern pattern = Pattern.compile(FUtils.REGULAR_GET_VERSION);
        Matcher matcher = pattern.matcher(otaData);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static List<String> getPostBuildValues(String otaData) {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile(FUtils.REGULAR_GET_VERSION);
        Matcher matcher = pattern.matcher(otaData);
        while (matcher.find()) {
            matches.add(matcher.group().replace("V", "").replace("v", ""));
        }
        return matches;
    }

    /**
     * 检查是否为mx8
     */
    public static boolean isMx8() {
        String platform = FUtils.getPlatform();
        Log.d(TAG, "checkMx8: platform>>" + platform);
        return Build.VERSION.SDK_INT >= 30 && !platform.startsWith("rk356");
    }

}
