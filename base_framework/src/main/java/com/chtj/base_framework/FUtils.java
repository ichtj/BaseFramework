package com.chtj.base_framework;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FUtils {
    private static final String TAG = FUtils.class.getSimpleName();
    public static String REGULAR_GET_VERSION = "V[0-9]{1,2}(\\.\\d{1,3})";


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

    public static String getPlatform() {
        FCmdTools.CommandResult commandResult = FCmdTools.execCommand("getprop ro.board.platform", true);
        return commandResult.successMsg;
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
     * 获取当前固件版本
     */
    public static String sysFwVersion() {
        List<String> versionArrayInfo=FUtils.getMatches(Build.DISPLAY, REGULAR_GET_VERSION);
        if (versionArrayInfo.isEmpty()){
            return "";
        }
        String upFwVersion = versionArrayInfo.get(0);
        return upFwVersion.replace("V", "").replace("v", "");
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
}
