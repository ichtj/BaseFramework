package com.chtj.base_framework.entity;

public class Space {
    private String totalSize;
    private String useSize;
    private String availableSize;

    public Space(String totalSize, String useSize, String availableSize) {
        this.totalSize = totalSize;
        this.useSize = useSize;
        this.availableSize = availableSize;
    }

    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }

    public String getUseSize() {
        return useSize;
    }

    public void setUseSize(String useSize) {
        this.useSize = useSize;
    }

    public String getAvailableSize() {
        return availableSize;
    }

    public void setAvailableSize(String availableSize) {
        this.availableSize = availableSize;
    }

    @Override
    public String toString() {
        return "Space{" +
                "totalSize=" + totalSize +
                ", useSize=" + useSize +
                ", availableSize=" + availableSize +
                '}';
    }
}
