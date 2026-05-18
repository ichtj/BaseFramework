/*************************************************************************
 > File Name: RKRecoverySystem.java
 > Author: jkand.huang
 > Mail: jkand.huang@rock-chips.com
 > Created Time: Wed 02 Nov 2016 03:10:47 PM CST
 ************************************************************************/
package com.chtj.base_framework.upgrade;

import android.content.Context;
import android.os.RecoverySystem;
import android.util.Log;

import com.chtj.base_framework.FUtils;
import com.chtj.base_framework.R;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class FRecoverySystemTools {
    private static final String TAG = FRecoverySystemTools.class.getSimpleName();
    public static void installPackage(Context context,IUpgrade iUpgrade, File packageFile) throws Throwable {
        String readFileInfo = readZipContent(packageFile.getAbsolutePath(), FExtras.UPDATE_ZIP_VERSION_PATH);
        String otaZipVersion = FUpgradeTools.getFirstPkgVersion(readFileInfo);
        iUpgrade.upFwVersion(otaZipVersion);
        Thread.sleep(1500);
        String currFwVersion = FUtils.sysFwVersion();
        int versionCount=FUpgradeTools.getPostBuildCount(readFileInfo);
        List<String> versionList=FUpgradeTools.getPostBuildValues(readFileInfo);
        if (versionCount==2&&versionList.size()==2){
            if (!currFwVersion.equals(versionList.get(1))){
                throw new IOException(context.getString(R.string.check_benchmark_version_fail));
            }
        }
        if (!FUtils.isEmpty(otaZipVersion)) {
            if (!FUtils.isEmpty(currFwVersion)) {
                if (checkVersion(currFwVersion,otaZipVersion)){
                    writeFlagCommand(packageFile.getCanonicalPath(), otaZipVersion, currFwVersion);
                    File file = new File("/data/misc/app_flag.txt");
                    if (file.exists()) {
                        file.delete();
                    }
                    iUpgrade.installStatus(FExtras.I_INSTALLING);
                    Thread.sleep(1500);
                    RecoverySystem.installPackage(context, packageFile);
                }else{
                    throw new IOException(context.getString(R.string.check_low_version_fail));
                }
            } else {
                throw new IOException(context.getString(R.string.read_fw_version_fail));
            }
        } else {
            throw new IOException(context.getString(R.string.zip_read_version_fail));
        }
    }

    public static boolean checkVersion(String currVersion, String otaVersion) {
        double currInt = Double.parseDouble(currVersion.replace("V",""));
        double otaInt = Double.parseDouble(otaVersion.replace("V",""));
        Log.d("fw_upgrade", "checkVersion: currInt>>"+currInt+",otaInt>>"+otaInt);
        return otaInt > currInt;
    }


    /**
     * 获取指定路径下压缩包中的文件 并且取到文件中的内容
     *
     * @param file     压缩包的路径
     * @param fileName 压缩包内的文件所在路径
     * @return 文件内容
     */
    public static String readZipContent(String file, String fileName) {
        try {
            ZipFile zf = new ZipFile(file);
            ZipEntry ze = zf.getEntry(fileName);
            InputStream in = zf.getInputStream(ze);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = br.readLine()) != null) {
                result.append(line + "\n");
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "readZipContent: ", e);
        }
        return "";
    }

    /**
     * 提取 update_ext4.zip（字节数组）从最外层 update.zip 中
     */
    public static boolean extractInnerZipToFile(File updateZipFile, String entryName, File outZipFile) {
        try (ZipFile zipFile = new ZipFile(updateZipFile)) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                Log.e(TAG, "Entry not found: " + entryName);
                return false;
            }

            // 创建父目录
            File parentDir = outZipFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (InputStream is = zipFile.getInputStream(entry);
                 FileOutputStream fos = new FileOutputStream(outZipFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }

            Log.i(TAG, "Saved to: " + outZipFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 复制文件到data目录下
     *
     * @param oldPath 源地址
     * @param newPath 目标地址
     */
    public static boolean copyFile(String oldPath, String newPath) throws Throwable {
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
            inStream.close();
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method set = c.getMethod("set", String.class, String.class);
                set.invoke(c, "persist.sys.firstRun", "true");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "copyFile: finsh complete!");
            return true;
        }
        return false;
    }

    public static String readLastUpdateCommand() {
        return FUtils.readFileData(FExtras.UPDATE_LAST_UPDATE_FILE);
    }

    public static void writeFlagCommand(String path, String upFwVersion, String currentFwVersion) throws IOException {
        new File(FExtras.RECOVERY_DIR).mkdirs();
        new File(FExtras.UPDATE_LAST_UPDATE_FILE).delete();
        FileWriter writer = new FileWriter(FExtras.UPDATE_LAST_UPDATE_FILE);
        try {
            String writeData = "updating;" + path + ";" + currentFwVersion + ";" + upFwVersion;
            writer.write(writeData);
        } catch (Exception e) {
        } finally {
            writer.close();
        }
    }
}
