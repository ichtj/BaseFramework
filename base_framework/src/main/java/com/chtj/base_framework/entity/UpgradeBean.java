package com.chtj.base_framework.entity;

import com.chtj.base_framework.upgrade.IUpgrade;

public class UpgradeBean {
    private String filePath;
    private IUpgrade iUpgrade;

    public UpgradeBean(String filePath, IUpgrade iUpgrade) {
        this.filePath = filePath;
        this.iUpgrade = iUpgrade;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public IUpgrade getiUpgrade() {
        return iUpgrade;
    }

    public void setiUpgrade(IUpgrade iUpgrade) {
        this.iUpgrade = iUpgrade;
    }
}
