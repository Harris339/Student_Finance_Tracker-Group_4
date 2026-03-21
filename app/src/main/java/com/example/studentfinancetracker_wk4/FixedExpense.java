package com.example.studentfinancetracker_wk4;

import java.time.LocalDate;

public class FixedExpense {
    private String category;
    private String title;
    private double amount;
    private LocalDate date;
    private String notes;

    public FixedExpense(String category, String title, double amount, LocalDate date, String notes) {
        this.category = category;
        this.title = title;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
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

    public LocalDate getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
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

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
