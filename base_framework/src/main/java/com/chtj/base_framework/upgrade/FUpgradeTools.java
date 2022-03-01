package com.chtj.base_framework.upgrade;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.RecoverySystem;
import android.util.Log;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.FCmdTools;
import com.chtj.base_framework.FCommonTools;
import com.chtj.base_framework.entity.CommonValue;
import com.chtj.base_framework.entity.UpgradeBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 更新工具类
 * 针对固件升级 APK等
 * 请将任务放置到子线程中调用 防止执行超时
 */
public class FUpgradeTools {
    private static final String TAG = "FUpgradeTools";

    /**
     * 固件最终存放的地址
     */
    public static final String SAVA_FW_COPY_PATH = "/data/update.zip";

    /*校验中*/
    public static final int I_CHECK = 0x101;
    /*复制到system/data下*/
    public static final int I_COPY = 0x102;
    /*安装中*/
    public static final int I_INSTALLING = 0x103;

    /**
     * 固件系统升级
     *
     * @param upBean
     */
    public static void firmwareUpgrade(UpgradeBean upBean) {
        if (Build.VERSION.SDK_INT >= 30) {
            FUpgradeReceiver.setfUpgradeInterface(upBean.getUpInterface());
            upBean.getUpInterface().installStatus(FUpgradeTools.I_CHECK);
            Intent intent=new Intent("action.firmware.update.bypath");
            intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            intent.putExtra("path",upBean.getFilePath());
            FBaseTools.getContext().sendBroadcast(intent);
            return;
        }
        if (FUpgradePool.newInstance().isTaskEnd()) {
            //判断是否有任务正在执行 有则忽略 无则向下执行
            FUpgradePool.newInstance().addExecuteTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = new File(upBean.getFilePath());
                        if (file.exists()) {
                            upBean.getUpInterface().installStatus(I_CHECK);
                            RecoverySystem.verifyPackage(file, new RecoverySystem.ProgressListener() {
                                @SuppressLint("WrongConstant")
                                @Override
                                public void onProgress(int progress) {
                                    if (progress == 100) {
                                        upBean.getUpInterface().installStatus(I_COPY);
                                        try {
                                            copyFile(upBean.getFilePath(), SAVA_FW_COPY_PATH);
                                            upBean.getUpInterface().installStatus(I_INSTALLING);
                                            FRecoverySystemTools.installPackage(FBaseTools.getContext(), new File(SAVA_FW_COPY_PATH));
                                        } catch (Throwable e) {
                                            upBean.getUpInterface().error(e.getMessage());
                                        }
                                    }
                                }
                            }, null);
                        } else {
                            upBean.getUpInterface().error("Firmware does not exist");
                        }
                    } catch (Throwable e) {
                        upBean.getUpInterface().error(e.getMessage());
                    }
                }
            });
        } else {
            upBean.getUpInterface().warning("The ota upgrade task has been run, please do not repeat it!");
        }
    }

    /**
     * 复制文件到data目录下
     *
     * @param oldPath 源地址
     * @param newPath 目标地址
     */
    private static void copyFile(String oldPath, String newPath) throws Throwable {
        //int bytesum = 0;
        int byteread = 0;
        File oldfile = new File(oldPath);
        if (oldfile.exists()) { //文件存在时
            InputStream inStream = new FileInputStream(oldPath); //读入原文件
            FileOutputStream fs = new FileOutputStream(newPath);
            byte[] buffer = new byte[1444];
            while ((byteread = inStream.read(buffer)) != -1) {
                //bytesum += byteread;
                fs.write(buffer, 0, byteread);
            }
            Log.d(TAG, "copyFile: finsh complete!");
            inStream.close();
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method set = c.getMethod("set", String.class, String.class);
                set.invoke(c, "persist.sys.firstRun", "true");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 卸载普通应用(第三方应用)
     *
     * @param packageName 包名
     * @param isRebootNow 是否立即重启
     * @return
     */
    public CommonValue uninstallNormalApp(String packageName, boolean isRebootNow) {
        String[] commands = new String[]{
                "pm uninstall  " + packageName,
                isRebootNow ? FCommonTools.CMD_REBOOT : "",
        };
        FCmdTools.CommandResult commandResult = FCmdTools.execCommand(commands, true);
        if (commandResult.result == 0) {
            return CommonValue.EXEU_COMPLETE;
        } else {
            return CommonValue.EXEU_UNKNOWN_ERR;
        }
    }

    /**
     * 卸载系统应用
     *
     * @param apkName     包名
     * @param isRebootNow 是否立即重启
     * @return
     */
    public CommonValue uninstallSystemApp(String apkName, boolean isRebootNow) {
        String replaceStr = apkName.replace(".apk", "") + "*";
        String[] commands = new String[]{
                "rm -rf /system/priv-app/" + replaceStr,
                isRebootNow ? FCommonTools.CMD_REBOOT : "",
        };
        FCmdTools.CommandResult commandResult = FCmdTools.execCommand(commands, true);
        if (commandResult.result == 0) {
            return CommonValue.EXEU_COMPLETE;
        } else {
            if (commandResult.errorMsg != null && commandResult.errorMsg.contains("Read-only file system")) {
                return CommonValue.CMD_READ_ONLY;
            } else {
                return CommonValue.EXEU_UNKNOWN_ERR;
            }
        }
    }


    /**
     * App安装升级
     *
     * @param apkPath apk存放地址
     * @param isSys   是否是系统应用
     * @return 操作结果
     */
    public static CommonValue installApk(String apkPath, boolean isSys) {
        return installApk(apkPath, isSys, false);
    }

    /**
     * App安装升级
     *
     * @param apkPath     apk存放地址
     * @param isSys       是否是系统应用
     * @param isRebootNow 是否执行重启
     * @return 操作结果
     */
    public static CommonValue installApk(String apkPath, boolean isSys, boolean isRebootNow) {
        String[] commands = null;
        if (isSys) {
            String[] fileInfo = apkPath.split("/");
            String fileName = fileInfo[fileInfo.length - 1].replace(".apk", "");
            commands = new String[]{
                    "rm -rf /system/priv-app/" + fileName + "*",
                    "cp -rf " + apkPath + " /system/priv-app/",
                    "chmod 777 /system/priv-app/" + fileName + "*",
                    isRebootNow ? FCommonTools.CMD_REBOOT : "",
            };
        } else {
            commands = new String[]{
                    "pm install -rf " + apkPath,
                    isRebootNow ? FCommonTools.CMD_REBOOT : "",
            };
        }
        FCmdTools.CommandResult commandResult = FCmdTools.execCommand(commands, true);
        if (commandResult.result == 0) {
            return CommonValue.EXEU_COMPLETE;
        } else {
            if (commandResult.errorMsg != null && commandResult.errorMsg.contains("Read-only file system")) {
                return CommonValue.CMD_READ_ONLY;
            } else {
                return CommonValue.EXEU_UNKNOWN_ERR;
            }
        }
    }

    /**
     * 版本号比较
     *
     * @return 0代表相等，1代表左边大，-1代表右边大
     * Utils.compareVersion("1.0.358_20180820090554","1.0.358_20180820090553")=1
     */
    public static int compareVersion(String left, String right) {
        if (left.equals(right)) {
            return 0;
        }
        String[] version1Array = left.split("[._]");
        String[] version2Array = right.split("[._]");
        int index = 0;
        int minLen = Math.min(version1Array.length, version2Array.length);
        long diff = 0;

        while (index < minLen
                && (diff = Long.parseLong(version1Array[index])
                - Long.parseLong(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            for (int i = index; i < version1Array.length; i++) {
                if (Long.parseLong(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Long.parseLong(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    /**
     * 读取文件内容
     *
     * @param fileName 路径+文件名称
     * @return 读取到的内容
     */
    public static String readFileData(String fileName) {
        String result = "";
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                return "";
            }
            FileInputStream fis = new FileInputStream(file);
            //获取文件长度
            int lenght = fis.available();
            byte[] buffer = new byte[lenght];
            fis.read(buffer);
            if (fis != null) {
                fis.close();
            }
            //将byte数组转换成指定格式的字符串
            result = new String(buffer, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "readFileData: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取U盘tf等ota包路径
     *
     * @return 存在update.zip的路径 并且这个update.zip这个文件大于100M
     */
    public static List<String> getDeviceUpdatePathList() {
        List<String> filePath = new ArrayList<>();
        int sdk = Build.VERSION.SDK_INT;
        String fileRootPath = "";
        File file = null;
        if (sdk >= 24) {
            fileRootPath = "/storage/";
            file = new File(fileRootPath);
            File[] fDevicesList = file.listFiles();
            if (fDevicesList != null && fDevicesList.length > 0) {
                for (int i = 0; i < fDevicesList.length; i++) {
                    if (!fDevicesList[i].getName().equals("emulated") && !fDevicesList[i].getName().equals("self")) {
                        File[] fDeviceInfo = fDevicesList[i].listFiles();
                        for (int j = 0; j < fDeviceInfo.length; j++) {
                            if (fDeviceInfo[j].getName().equals("update.zip") && fDeviceInfo[j].length() > 104857600) {
                                //文件大于一百兆 并且名称为update.zip
                                filePath.add(fDeviceInfo[j].getAbsolutePath());
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            fileRootPath = "/mnt/media_rw/";//extsd udisk
            file = new File(fileRootPath);
            File[] fDevicesList = file.listFiles();
            if (fDevicesList != null && fDevicesList.length > 0) {
                for (int i = 0; i < fDevicesList.length; i++) {
                    if (fDevicesList[i].getName().equals("sdisk") && fDevicesList[i].getName().equals("udisk")) {
                        File[] fDeviceInfo = fDevicesList[i].listFiles();
                        for (int j = 0; j < fDeviceInfo.length; j++) {
                            if (fDeviceInfo[j].getName().equals("update.zip") && fDeviceInfo[j].length() > 104857600) {
                                //文件大于一百兆 并且名称为update.zip
                                filePath.add(fDeviceInfo[j].getAbsolutePath());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return filePath;
    }


}
