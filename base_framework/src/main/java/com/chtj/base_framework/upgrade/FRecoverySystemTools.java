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

import com.chtj.base_framework.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class FRecoverySystemTools {
    public static void installPackage(Context context,IUpgrade iUpgrade, File packageFile) throws IOException {
        String readFileInfo = readZipContent(packageFile.getAbsolutePath(), FExtras.UPDATE_ZIP_VERSION_PATH);
        String otaZipVersion = FUpgradeTools.getFirstPkgVersion(readFileInfo);
        String currFwVersion = FUpgradeTools.sysFwVersion();
        int versionCount=FUpgradeTools.getPostBuildCount(readFileInfo);
        List<String> versionList=FUpgradeTools.getPostBuildValues(readFileInfo);
        if (versionCount==2&&versionList.size()==2){
            if (!currFwVersion.equals(versionList.get(1))){
                throw new IOException(context.getString(R.string.check_benchmark_version_fail));
            }
        }
        if (!FUpgradeTools.isEmpty(otaZipVersion)) {
            if (!FUpgradeTools.isEmpty(currFwVersion)) {
                if (checkVersion(currFwVersion,otaZipVersion)){
                    writeFlagCommand(packageFile.getCanonicalPath(), otaZipVersion, currFwVersion);
                    File file = new File("/data/misc/app_flag.txt");
                    if (file.exists()) {
                        file.delete();
                    }
                    iUpgrade.installStatus(FExtras.I_INSTALLING);
                    try{
                        Thread.sleep(1500);
                    }catch(Throwable throwable){
                        throwable.printStackTrace();
                    }
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
        }
        return "";
    }

    public static String readLastUpdateCommand() {
        return FUpgradeTools.readFileData(FExtras.UPDATE_LAST_UPDATE_FILE);
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
