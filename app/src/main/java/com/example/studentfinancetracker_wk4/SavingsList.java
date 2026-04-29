package com.example.studentfinancetracker_wk4;

import java.util.ArrayList;
import java.util.List;

public class SavingsList {
    private List<SavingsGoal> goals;

    public SavingsList() {
        goals = new ArrayList<>();
    }

    public void addGoal(SavingsGoal goal) { goals.add(goal); }
    public void removeGoal(SavingsGoal goal) { goals.remove(goal); }
    public List<SavingsGoal> getAllGoals() { return goals; }
}
