package com.face.upgrade;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chtj.base_framework.entity.CommonValue;
import com.chtj.base_framework.entity.IpConfigInfo;
import com.chtj.base_framework.entity.UpgradeBean;
import com.chtj.base_framework.network.FEthTools;
import com.chtj.base_framework.upgrade.FExtras;
import com.chtj.base_framework.upgrade.IUpgrade;
import com.chtj.base_framework.upgrade.FUpgradeTools;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityInfo";
    private TextView tvPkg;
    private TextView tvRemarks;
    private FwReceiver fwReceiver;

    class FwReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String data = "action>>" + intent.getAction();
            if (intent.getExtras() != null) {
                data += ",isComplete>>" + intent.getBooleanExtra(FExtras.EXTRA_ISCOMPLETE,false);
                data += ",errMsg>>" + intent.getStringExtra(FExtras.EXTRA_ERRMEG);
            }
            handler.sendMessage(handler.obtainMessage(0x00, data));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvRemarks = findViewById(R.id.tvRemarks);
        tvPkg = findViewById(R.id.tvPkg);
        fwReceiver = new FwReceiver();
        IntentFilter filter = new IntentFilter(FExtras.ACTION_UPDATE_RESULT);
        registerReceiver(fwReceiver, filter);
        FormatViewUtils.setMovementMethod(tvRemarks);
        setTitle("" + getString(R.string.app_name) + "：" + getPkgInfo());
        tvPkg.setText(getString(R.string.main_pkg, getPackageName()));
        PermissionsUtils.with(this)
                .addPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                .initPermission();
    }

    private String getPkgInfo() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            String versionName = pi.versionName;
            int versionCode = pi.versionCode;
            return versionName + "  " + versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /**
     * 打开以太网
     */
    public void openEthClick(View view) {
        FEthTools.openEth();//开启以太网
    }

    /**
     * 关闭以太网
     */
    public void closeEthClick(View view) {
        FEthTools.closeEth();//关闭以太网
    }

    public void dhcpClick(View view) {
        CommonValue commonValue2 = FEthTools.setEthDhcp();
        Log.d(TAG, "dhcpClick: " + commonValue2.getRemarks());
    }

    public void staticClick(View view) {
        CommonValue commonValue = FEthTools.setStaticIp(new IpConfigInfo("192.168.1.155",
                "8.8.8.8", "8.8.4.4", "192.168.1.1", "255.255.255.0"));
        Log.d(TAG, "staticClick: " + commonValue.getRemarks());
    }

    public void updateClick(View view) {
        String updateFile = "/sdcard/update.zip";
        if (!new File(updateFile).exists()) {
            handler.sendMessage(handler.obtainMessage(0x00, getString(R.string.update_sdcard_notfound)));
        } else {
            FUpgradeTools.firmwareUpgrade(new UpgradeBean(updateFile, new IUpgrade() {
                @Override
                public void installStatus(int installStatus) {
                    Log.d(TAG, "installStatus: " + installStatus);
                    handler.sendMessage(handler.obtainMessage(0x00, FExtras.formatStatus(MainActivity.this,installStatus)));
                }

                @Override
                public void error(String error) {
                    Log.d(TAG, "error: " + error);
                    handler.sendMessage(handler.obtainMessage(0x00, "error: " + error));
                }

                @Override
                public void warning(String warning) {
                    Log.d(TAG, "warning: " + warning);
                    handler.sendMessage(handler.obtainMessage(0x00, "warning: " + warning));
                }
            }));
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            FormatViewUtils.formatData(tvRemarks, msg.obj.toString(), "yyyyMMddHHmmss");
        }
    };

    public void clearTvRemarksClick(View view) {
        FormatViewUtils.scrollBackToTop(tvRemarks);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(fwReceiver);
    }
}