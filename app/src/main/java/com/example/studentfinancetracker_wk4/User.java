package com.example.studentfinancetracker_wk4;

import java.util.Calendar;
import java.util.Date;

public class User {

    private String fname;
    private String lname;
    private String email;
    private String password;
    private ExpenseList expenseList;
    public User(String fname, String lname, String email, String password){
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.password = password;
        this.expenseList = new ExpenseList();

        FixedExpense expense = new FixedExpense("Housing", "My house", 13.30, "30", "");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -10);
        Date tendaysago = cal.getTime();
        expense.setLastPayment(tendaysago);

        expenseList.addExpense(expense);
    }

    public User(){
        fname = "";
        lname = "";
        email = "";
        password = "";

    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
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

    public void addExpense(FixedExpense expense){
        expenseList.addExpense(expense);
        System.out.println(expenseList.toString());
    }

    public ExpenseList getExpenseList(){ return expenseList; }
}
