package com.chtj.base_framework.upgrade;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.RecoverySystem;
import android.util.Log;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.FCmdTools;
import com.chtj.base_framework.FCommonTools;
import com.chtj.base_framework.R;
import com.chtj.base_framework.entity.CommonValue;
import com.chtj.base_framework.entity.UpgradeBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
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
    public static String REGULAR_GET_VERSION = "V[0-9]{1,2}(\\.\\d{1,3})";

    public static String getPlatform() {
        FCmdTools.CommandResult commandResult = FCmdTools.execCommand("getprop ro.board.platform", true);
        return commandResult.successMsg;
    }

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
                            File file = new File(upBean.getFilePath());
                            if (file.exists()) {
                                upBean.getiUpgrade().installStatus(FExtras.I_CHECK);
                                RecoverySystem.verifyPackage(file, new RecoverySystem.ProgressListener() {
                                    @SuppressLint("WrongConstant")
                                    @Override
                                    public void onProgress(int progress) {
                                        if (progress == 100) {
                                            upBean.getiUpgrade().installStatus(FExtras.I_COPY);
                                            try {
                                                copyFile(upBean.getFilePath(), FExtras.SAVA_FW_COPY_PATH);
                                                upBean.getiUpgrade().installStatus(FExtras.I_INSTALLING);
                                                FRecoverySystemTools.installPackage(FBaseTools.getContext(), new File(FExtras.SAVA_FW_COPY_PATH));
                                            } catch (Throwable e) {
                                                upBean.getiUpgrade().error(e.getMessage());
                                            }
                                        }
                                    }
                                }, null);
                            } else {
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
     * 正则表达式通用模型
     *
     * @param input 字符串内容
     * @param regex 正则表达式
     * @return 符合规则的集合
     */
    public static List<String> getMatches(String input, String regex) {
        List<String> matches = new ArrayList<>();
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        // 创建Matcher对象
        Matcher matcher = pattern.matcher(input);
        // 查找匹配
        while (matcher.find()) {
            // 将匹配的部分添加到结果列表
            matches.add(matcher.group());
        }
        return matches;
    }

    /**
     * 获取update.zip的固件版本
     *
     * @param fwInfo 固件信息
     * @return 版本
     */
    public static String getOtaZipVersion(String fwInfo) {
        try (BufferedReader reader = new BufferedReader(new StringReader(fwInfo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("post-build=")) {
                    return FUpgradeTools.getMatches(line, FUpgradeTools.REGULAR_GET_VERSION).get(0).replace("V", "").replace("v", "");
                }
            }
        } catch (Throwable e) {
        }
        return "";
    }


    /**
     * 获取当前固件版本
     */
    public static String sysFwVersion() {
        String upFwVersion = FUpgradeTools.getMatches(Build.DISPLAY, REGULAR_GET_VERSION).get(0);
        return upFwVersion.replace("V", "").replace("v", "");
    }


    /**
     * 检查是否为mx8
     */
    public static boolean isMx8() {
        String platform = FUpgradeTools.getPlatform();
        Log.d(TAG, "checkMx8: platform>>" + platform);
        return Build.VERSION.SDK_INT >= 30 && !platform.startsWith("rk356");
    }

    /**
     * 复制文件到data目录下
     *
     * @param oldPath 源地址
     * @param newPath 目标地址
     */
    private static void copyFile(String oldPath, String newPath) throws Throwable {
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
     * 是否为空
     *
     * @param str 字符串
     * @return true 空 false 非空
     */
    public static Boolean isEmpty(String str) {
        if (str == null || str.length() == 0 || "null".equals(str)) {
            return true;
        }
        return false;
    }

    /**
     * 判断obj是否为空
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null || obj.toString().length() == 0 || "null".equals(obj.toString())) {
            return true;
        }
        return false;
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
