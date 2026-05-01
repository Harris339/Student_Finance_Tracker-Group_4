package com.example.studentfinancetracker_wk4;

import java.util.Calendar;
import java.util.Date;

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

        Calendar cal = Calendar.getInstance();

        // Large amount — likely exceeds any pool balance, triggers the warning
        cal.add(Calendar.DAY_OF_MONTH, -5);
        FixedExpense rent = new FixedExpense("Housing", "Rent", 1200.00, "Monthly", "");
        rent.setLastPayment(cal.getTime());
        expenseList.addExpense(rent);

        // Small amount — less likely to trigger the warning
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -12);
        FixedExpense spotify = new FixedExpense("Subscriptions", "Spotify", 10.99, "Monthly", "");
        spotify.setLastPayment(cal.getTime());
        expenseList.addExpense(spotify);

        // Savings goal to test that section
        SavingsGoal goal = new SavingsGoal("Emergency Fund", "3 months expenses", 3000.00, null);
        goal.addContribution(150.00);
        savingsList.addGoal(goal);
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
