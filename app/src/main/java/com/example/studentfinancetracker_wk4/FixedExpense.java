package com.example.studentfinancetracker_wk4;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixedExpense {
    private String category;
    private String title;
    private double amount;
    private String frequency;
    private String notes;

    private double totalPaid;
    private Date lastPayment;

    private List<Double> paymentHist;
    private List<Date> paymentDates;

    private int pos = 0;


    public FixedExpense(String category, String title, double amount, String frequency, String notes) {
        this.category = category;
        this.title = title;
        this.amount = amount;
        this.frequency = frequency;
        this.notes = notes;
        this.totalPaid = 0.0;
        this.lastPayment = null;
        this.paymentHist = new ArrayList<>();
        this.paymentDates = new ArrayList<>();
        paymentHist.add(totalPaid);
        paymentDates.add(null);
    }

    public FixedExpense(String category, String title, double amount, String frequency, String notes, double totalPaid) {
        this.category = category;
        this.title = title;
        this.amount = amount;
        this.frequency = frequency;
        this.notes = notes;
        this.totalPaid = totalPaid;
        this.paymentHist = new ArrayList<>();
        this.paymentDates = new ArrayList<>();
    }

    public String getAllHistory(){
        String s = "[";

        for (double i : paymentHist) {
            s += ", " + i;
        }

        return s + "]";
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getNotes() {
        return notes;
    }

    public double getTotalPaid(){
        return totalPaid;
    }

    public Date getLastPayment() { return lastPayment; }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setTotalPaid(double totalPaid){ this.totalPaid = totalPaid; }

    public void setLastPayment(Date lastPayment) { this.lastPayment = lastPayment; }

    public void addPayment(double amount){
        totalPaid += amount;
        addNewHist();
    }

    public void addNewHist(){
        paymentHist.add(totalPaid);
        paymentDates.add(new Date());
        pos = paymentHist.size() - 1;
    }

    public int getPos() {
        return pos;
    }

    public List<Double> getPaymentHist() {
        return paymentHist;
    }

    public List<Date> getPaymentDates() {
        return paymentDates;
    }

    public double getAnnualCost() {
        Map<String, Double> multipliers = new HashMap<>();
        multipliers.put("Daily", 365.0);
        multipliers.put("Weekly", 52.0);
        multipliers.put("Monthly", 12.0);
        multipliers.put("Yearly", 1.0);
        return amount * multipliers.getOrDefault(frequency, 0.0);
    }

    public int getDaysUntilNextPayment() {
        if (frequency == null || lastPayment == null) {
            return 0;
        }

        Map<String, Integer> freqMap = new HashMap<>();

        freqMap.put("Daily", 1);
        freqMap.put("Weekly", 7);
        freqMap.put("Monthly", 30);
        freqMap.put("Yearly", 365);

        int cycleDays = freqMap.getOrDefault(frequency, 0);

        long diffMillis = new Date().getTime() - lastPayment.getTime();
        long daysPassed = diffMillis / (1000 * 60 * 60 * 24);

        int daysLeft = (int) (cycleDays - daysPassed);

        return Math.max(daysLeft, 0);
    }

    public int processCompletedCycles() {
        if (lastPayment == null || frequency == null) return 0;

        Map<String, Integer> freqMap = new HashMap<>();
        freqMap.put("Daily", 1);
        freqMap.put("Weekly", 7);
        freqMap.put("Monthly", 30);
        freqMap.put("Yearly", 365);

        int cycleDays = freqMap.getOrDefault(frequency, 0);
        if (cycleDays == 0) return 0;

        long diffMillis = new Date().getTime() - lastPayment.getTime();
        long daysPassed = diffMillis / (1000 * 60 * 60 * 24);
        int cyclesCompleted = (int) (daysPassed / cycleDays);

        for (int i = 0; i < cyclesCompleted; i++) {
            totalPaid += amount;
            addNewHist();
        }

        if (cyclesCompleted > 0) {
            lastPayment = new Date(lastPayment.getTime()
                    + (long) cyclesCompleted * cycleDays * 24L * 60 * 60 * 1000);
        }

        return cyclesCompleted;
    }

    public double getHistAtCurIndex(int change){
        pos += change;

        if (pos < 0) {
            pos = 0;
        } else if (pos >= paymentHist.size()) {
            pos = paymentHist.size() - 1;
        }

        return paymentHist.get(pos);
    }

    @Override
    public String toString(){
        return "Category: " + category + "\n" +
                "Title: " + title + "\n" +
                "Amount: " + amount + "\n" +
                "Frequenct=y: " + frequency + "\n" +
                "Notes: " + notes + "\n\n";
    }
}
