package com.example.studentfinancetracker_wk4;

import java.util.ArrayList;
import java.util.List;

public class ExpenseList {
    private List<FixedExpense> expenses;

    public ExpenseList(){ expenses = new ArrayList<>(); }

    public void addExpense(FixedExpense expense){ expenses.add(expense); }

    public void removeExpense(FixedExpense expense){ expenses.remove(expense); }

    public List<FixedExpense> getAllExpenses(){ return expenses; }

    public double getTotalExpenses() {
        double total = 0;
        for (FixedExpense expense : expenses) {
            total += expense.getAmount();
        }
        return total;
    }
}
