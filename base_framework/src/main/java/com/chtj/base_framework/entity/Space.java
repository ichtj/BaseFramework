package com.chtj.base_framework.entity;

public class Space {
    private double totalSize;
    private double useSize;
    private double availableSize;

    public Space(double totalSize, double useSize, double availableSize) {
        this.totalSize = totalSize;
        this.useSize = useSize;
        this.availableSize = availableSize;
    }

    public double getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(double totalSize) {
        this.totalSize = totalSize;
    }

    public double getUseSize() {
        return useSize;
    }

    public void setUseSize(double useSize) {
        this.useSize = useSize;
    }

    public double getAvailableSize() {
        return availableSize;
    }

    public void setAvailableSize(double availableSize) {
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
