package com.chtj.base_framework.entity;

public class RomSpace extends Space {
    private String availableCounts;
    private String totalCounts;
    private String eachBlockSize;

    public RomSpace(String totalSize, String useSize, String availableSize, String availableCounts, String totalCounts, String eachBlockSize) {
        super(totalSize, useSize, availableSize);
        this.availableCounts = availableCounts;
        this.totalCounts = totalCounts;
        this.eachBlockSize = eachBlockSize;
    }

    public String getAvailableCounts() {
        return availableCounts;
    }

    public void setAvailableCounts(String availableCounts) {
        this.availableCounts = availableCounts;
    }

    public String getTotalCounts() {
        return totalCounts;
    }

    public void setTotalCounts(String totalCounts) {
        this.totalCounts = totalCounts;
    }

    public String getEachBlockSize() {
        return eachBlockSize;
    }

    public void setEachBlockSize(String eachBlockSize) {
        this.eachBlockSize = eachBlockSize;
    }
}
