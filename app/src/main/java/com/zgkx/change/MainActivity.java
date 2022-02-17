package com.zgkx.change;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.entity.UpgradeBean;
import com.chtj.base_framework.upgrade.FUpgradeInterface;
import com.chtj.base_framework.upgrade.FUpgradeTools;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FBaseTools.instance().create(this.getApplication());
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

            }
        }));
    }


}