package com.chtj.base_framework.entity;

import com.chtj.base_framework.upgrade.FUpgradeInterface;

public class UpgradeBean {
    private String filePath;
    private FUpgradeInterface upInterface;

    public UpgradeBean(String filePath, FUpgradeInterface upInterface) {
        this.filePath = filePath;
        this.upInterface = upInterface;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public FUpgradeInterface getUpInterface() {
        return upInterface;
    }

    public void setUpInterface(FUpgradeInterface upInterface) {
        this.upInterface = upInterface;
    }
}
