package com.example.studentfinancetracker_wk4;

public class Category {

    private String category;
    private double percentage;
    private double totalAllocatedToDate;
    private double lastAllocation;

    public Category(String category, double percentage) {
        this.category = category;
        this.percentage = percentage;
        this.totalAllocatedToDate = 0;
        this.lastAllocation = 0;
    }

    public String getCategory() {
        return category;
    }

    public double getPercentage() {
        return percentage;
    }

    public double getTotalAllocatedToDate() {
        return totalAllocatedToDate;
    }

    public double getLastAllocation() {
        return lastAllocation;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public void setTotalAllocatedToDate(double totalAllocatedToDate) {
        this.totalAllocatedToDate += totalAllocatedToDate;
    }

    public void setLastAllocation(double lastAllocation) {
        this.lastAllocation = lastAllocation;
    }

    @Override
    public String toString(){
        return "Category: " + category + "\n" +
                "Percentage: " + percentage + "\n" +
                "Total Allocated To Date: " + totalAllocatedToDate + "\n" +
                "Last Allocation: " + lastAllocation + "\n\n";
    }
}
