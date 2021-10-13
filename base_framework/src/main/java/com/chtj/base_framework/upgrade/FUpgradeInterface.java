package com.chtj.base_framework.upgrade;

public interface FUpgradeInterface {
    /**
     * 固件安装过程
     *
     * @param installStatus FUpgradeTools {I_CHECK,I_COPY,I_INSTALLING}
     */
    void installStatus(int installStatus);

    /**
     * 固件安装前的过程执行失败
     */
    void error(String error);

    /**
     * 警告信息
     */
    void warning(String warning);
}
