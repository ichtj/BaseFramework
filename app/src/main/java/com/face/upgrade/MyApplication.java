package com.face.upgrade;

import android.app.Application;

import com.chtj.base_framework.FBaseTools;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FBaseTools.instance().create(this);
        FBaseTools.enableUpgrade(true);
    }
}
