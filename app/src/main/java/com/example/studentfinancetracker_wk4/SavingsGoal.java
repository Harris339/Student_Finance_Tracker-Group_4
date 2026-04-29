package com.example.studentfinancetracker_wk4;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SavingsGoal {
    private String title;
    private String notes;
    private double targetAmount;
    private double currentAmount;
    private Date targetDate;

    private List<Double> contributionHist;
    private List<Date> contributionDates;
    private int pos = 0;

    public SavingsGoal(String title, String notes, double targetAmount, Date targetDate) {
        this.title = title;
        this.notes = notes;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.currentAmount = 0.0;
        this.contributionHist = new ArrayList<>();
        this.contributionDates = new ArrayList<>();
        contributionHist.add(0.0);
        contributionDates.add(null);
    }

    public String getTitle() { return title; }
    public String getNotes() { return notes; }
    public double getTargetAmount() { return targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public Date getTargetDate() { return targetDate; }
    public List<Double> getContributionHist() { return contributionHist; }
    public List<Date> getContributionDates() { return contributionDates; }
    public int getPos() { return pos; }

    public void setTitle(String title) { this.title = title; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public void setCurrentAmount(double amount) { this.currentAmount = amount; }
    public void setTargetDate(Date targetDate) { this.targetDate = targetDate; }

    public void addContribution(double amount) {
        this.currentAmount += amount;
        recordHist();
    }

    public void recordHist() {
        while (contributionHist.size() > pos + 1) {
            contributionHist.remove(contributionHist.size() - 1);
            contributionDates.remove(contributionDates.size() - 1);
        }
        contributionHist.add(currentAmount);
        contributionDates.add(new Date());
        pos = contributionHist.size() - 1;
    }

    public double getHistAtCurIndex(int change) {
        pos += change;
        if (pos < 0) pos = 0;
        else if (pos >= contributionHist.size()) pos = contributionHist.size() - 1;
        return contributionHist.get(pos);
    }

    public int getProgressPercent() {
        if (targetAmount <= 0) return 0;
        return (int) Math.min(100, (currentAmount / targetAmount) * 100);
    }

    public double getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }

    public boolean isCompleted() {
        return currentAmount >= targetAmount;
    }

    public int getDaysToTarget() {
        if (targetDate == null) return -1;
        long diffMillis = targetDate.getTime() - new Date().getTime();
        if (diffMillis <= 0) return 0;
        return (int) (diffMillis / (1000L * 60 * 60 * 24));
    }
}
