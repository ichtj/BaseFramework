package com.zgkx.change;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.entity.UpgradeBean;
import com.chtj.base_framework.upgrade.FUpgradeInterface;
import com.chtj.base_framework.upgrade.FUpgradeTools;
import com.zgkx.change.test.ShellUtils;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityInfo";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FBaseTools.instance().create(this.getApplication());
        PermissionsUtils.with(this)
                .addPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .addPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                .initPermission();
        startService(new Intent(this, MyService.class));
        
    }


    public void updateClick(View view) {
        FUpgradeTools.firmwareUpgrade(new UpgradeBean("/sdcard/CloudCache/update.zip", new FUpgradeInterface() {
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
}