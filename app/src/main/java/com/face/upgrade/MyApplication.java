package com.face.upgrade;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.chtj.base_framework.FBaseTools;
import com.face_chtj.base_iotutils.BaseIotUtils;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FBaseTools.instance().create(this);
        FBaseTools.enableUpgrade(true);
        BaseIotUtils.instance().create(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }
}
