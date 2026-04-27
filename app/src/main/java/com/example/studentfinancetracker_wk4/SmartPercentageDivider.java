package com.example.studentfinancetracker_wk4;

import java.util.ArrayList;
import java.util.List;

public class SmartPercentageDivider {
    private List<Category> divisions;

    public SmartPercentageDivider() {
        divisions = new ArrayList<>();
    }

    public void removeDivider(Category division) {
        divisions.remove(division);
    }

    public List<Category> getAllDividers() {
        return divisions;
    }

    public double getTotalPercentage() {
        double total = 0;
        for (Category division : divisions) {
            total += division.getPercentage();
        }
        return total;
    }

    public void addDivider(String name, double newPercent) {
        if (newPercent >= 100) {
            divisions.clear();
            divisions.add(new Category(name, 100));
            return;
        }

        double remaining = 100 - newPercent;
        double currentTotal = getTotalPercentage();

        for (Category c : divisions) {
            double adjusted = (c.getPercentage() / currentTotal) * remaining;
            c.setPercentage(adjusted);
        }

        divisions.add(new Category(name, newPercent));
    }

    public boolean updateAllPercentages(List<Double> newPercentages) {
        if (newPercentages.size() != divisions.size()) {
            return false;
        }

        double total = 0;
        for (double p : newPercentages) {
            total += p;
        }

        if (Math.abs(total - 100) > 0.01) {
            return false;
        }

        for (int i = 0; i < divisions.size(); i++) {
            divisions.get(i).setPercentage(newPercentages.get(i));
        }
        return true;
    }

    public void allocateIncome(double income) {
        for (Category c : divisions) {
            double allocation = (c.getPercentage() / 100) * income;

            c.setLastAllocation(allocation);
            c.setTotalAllocatedToDate(allocation);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Category division : divisions) {
            sb.append(division.toString()).append("\n");
        }
        return sb.toString();
    }
}
