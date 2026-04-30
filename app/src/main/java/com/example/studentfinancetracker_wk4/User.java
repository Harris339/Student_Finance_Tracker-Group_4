package com.example.studentfinancetracker_wk4;

public class User {

    private String email;
    private double balance;
    private String password;
    private ExpenseList expenseList;
    private SavingsList savingsList;

    private SmartPercentageDivider percDivider;

    public User(String email, String password){
        this.email = email;
        this.password = password;
        balance = 0.0;
        this.expenseList = new ExpenseList();
        this.savingsList = new SavingsList();
        this.percDivider = new SmartPercentageDivider();
    }

    public User(){
        email = "";
        balance = 0.0;
        password = "";
        this.expenseList = new ExpenseList();
        this.savingsList = new SavingsList();
        this.percDivider = new SmartPercentageDivider();
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
    public SavingsList getSavingsList(){ return savingsList; }

    public SmartPercentageDivider getPercDivider(){ return percDivider; }
}
