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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chtj.base_framework.FScreentTools;
import com.chtj.base_framework.FStorageTools;
import com.chtj.base_framework.entity.UpgradeBean;
import com.chtj.base_framework.upgrade.FExtras;
import com.chtj.base_framework.upgrade.FUpgradeService;
import com.chtj.base_framework.upgrade.IUpgrade;
import com.chtj.base_framework.upgrade.FUpgradeTools;
import com.face_chtj.base_iotutils.FileDialogSelectUtils;
import com.face_chtj.base_iotutils.FormatViewUtils;

import java.io.File;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityInfo";
    private TextView tvPkg;
    private TextView tvRemarks;
    private TextView tvFwVersion;
    private TextView tvFileSystem;
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
        tvFileSystem = findViewById(R.id.tvFileSystem);
        tvRemarks = findViewById(R.id.tvRemarks);
        tvPkg = findViewById(R.id.tvPkg);
        tvFwVersion = findViewById(R.id.tvFwVersion);
        tvFwVersion.setText(Build.DISPLAY);
        fwReceiver = new FwReceiver();
        IntentFilter filter = new IntentFilter(FExtras.ACTION_UPDATE_RESULT);
        registerReceiver(fwReceiver, filter);
        FormatViewUtils.setMovementMethod(tvRemarks);
        setTitle("" + getString(R.string.app_name) + "：" + getPkgInfo());
        tvFileSystem.setText(getString(R.string.main_current_filesystem,FStorageTools.getFileSystem()==0?"ext4":"f2fs"));
        tvPkg.setText(getString(R.string.main_pkg, getPackageName()));
        PermissionsUtils.with(this)
                .addPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                .initPermission();
        FUpgradeService.startServie(this);
        Log.d(TAG, "onCreate: "+FScreentTools.takeScreenshot());
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

    public void updateClick(View view) {
        new FileDialogSelectUtils(this, new File("/sdcard/"), new FileDialogSelectUtils.FileSelectCallback() {
            @Override
            public void onFileSelected(List<File> selectedFiles) {
                if (selectedFiles!=null&&selectedFiles.size()>0){
                    for (int i = 0; i < selectedFiles.size(); i++) {
                        File updateFile = selectedFiles.get(i);
                        if (!updateFile.exists()) {
                            handler.sendMessage(handler.obtainMessage(0x00, getString(R.string.update_sdcard_notfound)));
                        } else {
                            FUpgradeTools.firmwareUpgrade(new UpgradeBean(updateFile.getAbsolutePath(), new IUpgrade() {
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

                                @Override
                                public void upFwVersion(String version) {
                                    Log.d(TAG, "upFwVersion: version>>"+version);
                                }
                            }));
                        }
                    }
                }
            }
        }).setSizeRatio(0.5f,0.5f).setSingleSelect(true).show();
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