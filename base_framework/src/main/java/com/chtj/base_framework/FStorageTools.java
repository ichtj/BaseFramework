package com.chtj.base_framework;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.util.Log;

import com.chtj.base_framework.entity.Space;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 存储，空间相关工具类
 */
public class FStorageTools {
    private static final String TAG = "FStorageTools";
    public static final String TYPE_B = "B";
    public static final String TYPE_KB = "KB";
    public static final String TYPE_MB = "MB";
    public static final String TYPE_GB = "GB";
    public static final String TYPE_TB = "TB";

    public static double formatSize(long size, String unit) {
        double formattedSize = size;
        String[] units = {"B", "KB", "MB", "GB", "TB"};

        int index = 0;
        while (formattedSize >= 1024 && index < units.length - 1) {
            formattedSize /= 1024;
            index++;
        }
        if (unit.equals("B")) {
            formattedSize *= Math.pow(1024, index);
            //formattedUnit = units[0];
        } else if (unit.equals("KB")) {
            formattedSize *= Math.pow(1024, index - 1);
            //formattedUnit = units[1];
        } else if (unit.equals("MB")) {
            formattedSize *= Math.pow(1024, index - 2);
            //formattedUnit = units[2];
        } else if (unit.equals("GB")) {
            formattedSize *= Math.pow(1024, index - 3);
            //formattedUnit = units[3];
        } else if (unit.equals("TB")) {
            formattedSize *= Math.pow(1024, index - 4);
            //formattedUnit = units[4];
        }
        // 要保留小数点后两位，使用模式"0.00"
        String pattern = "0.00";
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        // 格式化原始值
        String formattedValue = decimalFormat.format(formattedSize);
        // 将格式化后的字符串转换回double
        return Double.parseDouble(formattedValue);
    }

    /**
     * 获取ram占用
     *
     * @return
     */
    public static Space getRamSpace(String unit) {
        try {
            ActivityManager activityManager = (ActivityManager) FBaseTools.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            long totalRam = memoryInfo.totalMem;
            long availableRam = memoryInfo.availMem;
            long userRam=totalRam-availableRam;
            return new Space(formatSize(totalRam, unit), formatSize(userRam, unit), formatSize(availableRam, unit));
        } catch (Throwable throwable) {
            return new Space(formatSize(0, unit), formatSize(0, unit), formatSize(0, unit));
        }
    }

    /**
     * 获取rom占用
     *
     * @return
     */
    public static Space getRomSpace(String unit) {
        try {
            // 获取 ROM 存储信息
            StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long blockSize = statFs.getBlockSizeLong();
            long totalBlocks = statFs.getBlockCountLong();
            long availableBlocks = statFs.getAvailableBlocksLong();

            // 计算存储空间信息
            long totalSpace = totalBlocks * blockSize;
            long availableSpace = availableBlocks * blockSize;
            long usedSpace = totalSpace - availableSpace;
            return new Space(formatSize(totalSpace, unit), formatSize(usedSpace, unit), formatSize(availableSpace, unit));
        } catch (Throwable throwable) {
            return new Space(formatSize(0, unit), formatSize(0, unit), formatSize(0, unit));
        }
    }


    /**
     * 获取SDcard空间
     *
     * @return
     */
    public static Space getSdcardSpace(String unit) {
        try {
            // 获取 SD 卡存储信息
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            long blockSize = statFs.getBlockSizeLong();
            long totalBlocks = statFs.getBlockCountLong();
            long availableBlocks = statFs.getAvailableBlocksLong();
            // 计算存储空间信息
            long totalSpace = totalBlocks * blockSize;
            long availableSpace = availableBlocks * blockSize;
            long usedSpace = totalSpace - availableSpace;
            return new Space(formatSize(totalSpace, unit), formatSize(usedSpace, unit), formatSize(availableSpace, unit));
        } catch (Throwable throwable) {
            return new Space(formatSize(0, unit), formatSize(0, unit), formatSize(0, unit));
        }
    }

    /**
     * 获取手机内部空间总大小
     *
     * @return 大小，字节为单位
     */
    public static long getTotalInternalMemorySize() {
        //获取内部存储根目录
        File path = Environment.getDataDirectory();
        //系统的空间描述类
        StatFs stat = new StatFs(path.getPath());
        //每个区块占字节数
        long blockSize = stat.getBlockSize();
        //区块总数
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    /**
     * 获取手机内部可用空间大小
     *
     * @return 大小，字节为单位
     */
    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        //获取可用区块数量
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }


    /**
     * 获取TF卡存储空间
     *
     * @return
     */
    public static Space getTfSpace(String unit) {
        try {
            FCmdTools.CommandResult commandResult = null;
            int sdk = Build.VERSION.SDK_INT;
            if (sdk >= 24) {
                String appStr = "-rn";
                if (sdk >= 30) {
                    appStr = "";
                }
                commandResult = FCmdTools.execCommand("df | grep " + appStr + " /mnt/media_rw", true);
                if (commandResult.successMsg != null && commandResult.successMsg.length() > 0) {
                    String[] result = commandResult.successMsg.substring(4).trim().replaceAll("\\s+", " ").split(" ");
                    return new Space((Integer.valueOf(result[2]) / 1024), (Integer.valueOf(result[3]) / 1024), (Integer.valueOf(result[4]) / 1024));
                } else {
                    return new Space(formatSize(0, unit), formatSize(0, unit), formatSize(0, unit));
                }
            } else {
                commandResult = FCmdTools.execCommand("busybox df -m | grep /mnt/media_rw/extsd", true);
                Log.d(TAG, "getTfSpace: successMeg=" + commandResult.successMsg);
                if (commandResult.successMsg != null && commandResult.successMsg.length() > 0) {
                    String[] resultCall = commandResult.successMsg.substring(4).trim().replaceAll("\\s+", " ").split(" ");
                    return new Space(Long.parseLong(resultCall[0]), Long.parseLong(resultCall[1]), Long.parseLong(resultCall[2]));
                } else {
                    return new Space(formatSize(0, unit), formatSize(0, unit), formatSize(0, unit));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "errMeg:" + e.getMessage());
            return new Space(formatSize(0, unit), formatSize(0, unit), formatSize(0, unit));
        }
    }

    /**
     * 获取cpu占用率
     */
    public static double getCpuUsage() {
        try {
            long[] cpuTime = getCpuTime();
            long idleTime = cpuTime[3];
            long totalTime = getTotalCpuTime(cpuTime);
            // Calculate CPU usage percentage
            double cpuUsagePercentage = calculateCpuUsage(idleTime, totalTime);
            BigDecimal bigDecimal = new BigDecimal(cpuUsagePercentage);
            BigDecimal roundedValue = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
            return roundedValue.doubleValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static long[] getCpuTime() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
        String line = reader.readLine();
        reader.close();
        String[] fields = line.split("\\s+");
        long[] cpuTime = new long[fields.length - 1];
        for (int i = 1; i < fields.length; i++) {
            cpuTime[i - 1] = Long.parseLong(fields[i]);
        }
        return cpuTime;
    }

    private static long getTotalCpuTime(long[] cpuTime) {
        long total = 0;
        for (long time : cpuTime) {
            total += time;
        }
        return total;
    }

    private static double calculateCpuUsage(long idleTime, long totalTime) {
        long elapsedCpuTime = Process.getElapsedCpuTime() * 1000; // Convert to nanoseconds

        // Calculate CPU usage percentage
        double cpuUsagePercentage = ((totalTime - idleTime) / (double) elapsedCpuTime) * 100;

        return cpuUsagePercentage;
    }
}
