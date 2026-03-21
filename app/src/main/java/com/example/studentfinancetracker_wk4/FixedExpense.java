package com.example.studentfinancetracker_wk4;

import java.time.LocalDate;
import java.util.Date;

public class FixedExpense {
    private String category;
    private String title;
    private double amount;
    private String frequency;
    private String notes;

    private double totalPaid;

    public FixedExpense(String category, String title, double amount, String frequency, String notes) {
        this.category = category;
        this.title = title;
        this.amount = amount;
        this.frequency = frequency;
        this.notes = notes;
        this.totalPaid = 0.0;
    }


    public FixedExpense(String category, String title, double amount, String frequency, String notes, double totalPaid) {
        this.category = category;
        this.title = title;
        this.amount = amount;
        this.frequency = frequency;
        this.notes = notes;
        this.totalPaid = totalPaid;
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

    public void setTotalPaid(double totalPaid){
        this.totalPaid = totalPaid;
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
