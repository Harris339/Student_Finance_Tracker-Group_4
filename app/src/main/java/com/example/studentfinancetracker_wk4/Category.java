package com.example.studentfinancetracker_wk4;

public class Category {

    private String category;
    private double percentage;
    private double lastAllocation;
    private double balance;

    public Category(String category, double percentage) {
        this.category = category;
        this.percentage = percentage;
        this.lastAllocation = 0;
        this.balance = 0;
    }

    public String getCategory() {
        return category;
    }

    public double getPercentage() {
        return percentage;
    }


    public double getLastAllocation() {
        return lastAllocation;
    }

    public double getBalance() {
        return balance;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }


    public void setLastAllocation(double lastAllocation) {
        this.lastAllocation = lastAllocation;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double deductFromBalance(double amount){
        if (amount > balance) return -1;
        balance -= amount;
        return amount;
    }

    @Override
    public String toString(){
        return "Category: " + category + "\n" +
                "Percentage: " + percentage + "\n" +
                "Balance for this Category: " + balance + "\n" +
                "Last Allocation: " + lastAllocation + "\n\n";
    }
}
