package com.example.studentfinancetracker_wk4;

import java.time.LocalDate;
import java.util.Date;

public class FixedExpense {
    private String category;
    private String title;
    private double amount;
    private Date date;
    private String notes;

    public FixedExpense(String category, String title, double amount,Date date, String notes) {
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

    public Date getDate() {
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

    public void setDate(Date date) {
        this.date = date;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString(){
        return "Category: " + category + "\n" +
                "Title: " + title + "\n" +
                "Amount: " + amount + "\n" +
                "Date: " + date.toString() + "\n" +
                "Notes: " + notes + "\n\n";
    }
}
