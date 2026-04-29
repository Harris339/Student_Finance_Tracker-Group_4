package com.example.studentfinancetracker_wk4;

import java.util.Calendar;
import java.util.Date;

public class User {

    private String email;
    private double balance;
    private String password;
    private ExpenseList expenseList;
    public User(String email, String password){
        this.email = email;
        this.password = password;
        balance = 0.0;
        this.expenseList = new ExpenseList();

        FixedExpense expense = new FixedExpense("Housing", "My house", 13.30, "Monthly", "");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -10);
        Date tendaysago = cal.getTime();
        expense.setLastPayment(tendaysago);

        expenseList.addExpense(expense);

        FixedExpense subscription = new FixedExpense("Subscriptions", "Netflix", 15.99, "Monthly", "");
        Calendar subCal = Calendar.getInstance();
        subCal.add(Calendar.DAY_OF_MONTH, -20);
        subscription.setLastPayment(subCal.getTime());
        subscription.setTotalPaid(31.98);
        subscription.addNewHist();
        expenseList.addExpense(subscription);
    }

    public User(){
        email = "";
        balance = 0.0;
        password = "";

    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getBalance(){return balance;}

    public void setBalance(double balance){this.balance = balance;}

    public void addExpense(FixedExpense expense){
        expenseList.addExpense(expense);
        System.out.println(expenseList.toString());
    }

    public ExpenseList getExpenseList(){ return expenseList; }
}
