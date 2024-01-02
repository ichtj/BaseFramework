package com.android.upgrade;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chtj.base_framework.entity.UpgradeBean;
import com.chtj.base_framework.upgrade.IUpgrade;
import com.chtj.base_framework.upgrade.FUpgradeTools;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionsUtils.with(this)
                .addPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                .initPermission();
    }

    public void updateClick(View view) {
        String updateFile = "/sdcard/update.zip";
        if (!new File(updateFile).exists()) {
            Toast.makeText(this, R.string.update_sdcard_notfound, Toast.LENGTH_SHORT).show();
        }else{
            FUpgradeTools.firmwareUpgrade(new UpgradeBean(updateFile, new IUpgrade() {
                @Override
                public void installStatus(int installStatus) {
                    Log.d(TAG, "installStatus: " + installStatus);
                }

                @Override
                public void error(String error) {
                    Log.d(TAG, "error: " + error);
                }

                @Override
                public void warning(String warning) {
                    Log.d(TAG, "warning: " + warning);
                }
            }));
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}