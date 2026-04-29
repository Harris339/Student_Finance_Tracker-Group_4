package com.example.studentfinancetracker_wk4;

import java.util.ArrayList;
import java.util.List;

public class SmartPercentageDivider {
    private List<Category> divisions;
    User user = SessionManager.getInstance().getCurrentUser();

    public SmartPercentageDivider() {
        divisions = new ArrayList<>();
    }

    public void removeDivider(Category division) {
        divisions.remove(division);
    }

    public List<Category> getAllDividers() {
        return divisions;
    }

    public void setAllDividers(List<Category> newDivisions) {
        double total = 0;

        for (Category c : newDivisions) {
            total += c.getPercentage();
        }

        if (total == 0) {
            divisions.clear();
            divisions.addAll(newDivisions);
            return;
        }

        // Step 1: Normalize
        List<Double> normalized = new ArrayList<>();
        for (Category c : newDivisions) {
            normalized.add((c.getPercentage() / total) * 100);
        }

        // Step 2: Floor values and track remainders
        int[] finalInts = new int[normalized.size()];
        double[] remainders = new double[normalized.size()];
        int sum = 0;

        for (int i = 0; i < normalized.size(); i++) {
            finalInts[i] = (int) Math.floor(normalized.get(i));
            remainders[i] = normalized.get(i) - finalInts[i];
            sum += finalInts[i];
        }

        // Step 3: Distribute remaining %
        int remaining = 100 - sum;

        while (remaining > 0) {
            int maxIndex = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[maxIndex]) {
                    maxIndex = i;
                }
            }

            finalInts[maxIndex]++;
            remainders[maxIndex] = 0; // prevent re-picking
            remaining--;
        }

        // Step 4: Apply results
        for (int i = 0; i < newDivisions.size(); i++) {
            newDivisions.get(i).setPercentage(finalInts[i]);
        }

        divisions.clear();
        divisions.addAll(newDivisions);
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

    public void addDivider(Category c) {
        String name = c.getCategory();
        double newPercent = c.getPercentage();
        if (newPercent >= 100) {
            divisions.clear();
            divisions.add(new Category(name, 100));
            return;
        }

        double remaining = 100 - newPercent;
        double currentTotal = getTotalPercentage();

        for (Category cat : divisions) {
            double adjusted = (c.getPercentage() / currentTotal) * remaining;
            cat.setPercentage(adjusted);
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

    public double getTotalAllocations() {
        double total = 0;
        for (Category c : divisions) {
            total += c.getLastAllocation();
        }
        return total;
    }

    public void allocateIncome(double income) {
        double otherAllocation = 0;

        if (getTotalAllocations() < user.getBalance()) {
            otherAllocation= user.getBalance() - getTotalAllocations();
        }
        for (Category c : divisions) {
            if (c.getCategory().equals("Other")) {
                c.setBalance(c.getBalance() + otherAllocation);
            }
            double allocation = (c.getPercentage() / 100) * income;

            c.setLastAllocation(allocation);
            c.setBalance(c.getBalance() + allocation);
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
