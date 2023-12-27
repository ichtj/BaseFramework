package com.android.upgrade;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chtj.base_framework.FStorageTools;
import com.chtj.base_framework.entity.UpgradeBean;
import com.chtj.base_framework.upgrade.FUpgradeInterface;
import com.chtj.base_framework.upgrade.FUpgradeTools;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityInfo";
    private static final int PICK_FILE_REQUEST = 1;
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
        FUpgradeTools.firmwareUpgrade(new UpgradeBean("/sdcard/update.zip", new FUpgradeInterface() {
            @Override
            public void installStatus(int installStatus) {
                Log.d(TAG, "installStatus: "+installStatus);
            }

            @Override
            public void error(String error) {
                Log.d(TAG, "error: "+error);
            }

            @Override
            public void warning(String warning) {
                Log.d(TAG, "warning: "+warning);
            }
        }));

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}