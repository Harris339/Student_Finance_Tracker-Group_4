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

    public void setAllDividers(List<Category> newDivisions) {
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

    public void rebalanceExcept(Category editedCategory) {
        double lockedPercent = clamp(Math.round(editedCategory.getPercentage() * 10.0) / 10.0, 0, 100);
        editedCategory.setPercentage(lockedPercent);

        double remaining = 100.0 - lockedPercent;

        List<Category> others = new ArrayList<>();
        double otherTotal = 0;
        for (Category c : divisions) {
            if (c != editedCategory) {
                others.add(c);
                otherTotal += c.getPercentage();
            }
        }

        if (others.isEmpty()) return;

        if (otherTotal == 0) {
            double split = Math.round((remaining / others.size()) * 10.0) / 10.0;
            for (Category c : others) {
                c.setPercentage(split);
            }
        } else {
            double assigned = 0;
            for (Category c : others) {
                double newPercent = Math.round((c.getPercentage() / otherTotal) * remaining * 10.0) / 10.0;
                c.setPercentage(newPercent);
                assigned += newPercent;
            }

            double leftover = Math.round((remaining - assigned) * 10.0) / 10.0;
            if (Math.abs(leftover) > 0.0) {
                Category biggest = others.get(0);
                for (Category c : others) {
                    if (c.getPercentage() > biggest.getPercentage()) {
                        biggest = c;
                    }
                }
                biggest.setPercentage(Math.round((biggest.getPercentage() + leftover) * 10.0) / 10.0);
            }
        }
    }

    public void rebalanceExceptMultiple(List<Category> editedCategories) {
        double lockedTotal = 0;
        for (Category edited : editedCategories) {
            double locked = clamp(Math.round(edited.getPercentage() * 10.0) / 10.0, 0, 100);
            edited.setPercentage(locked);
            lockedTotal += locked;
        }

        if (lockedTotal > 100) {
            for (Category edited : editedCategories) {
                edited.setPercentage(Math.round((edited.getPercentage() / lockedTotal) * 100.0 * 10.0) / 10.0);
            }
            lockedTotal = 100;
        }

        double remaining = 100.0 - lockedTotal;

        List<Category> others = new ArrayList<>();
        double otherTotal = 0;
        for (Category c : divisions) {
            if (!editedCategories.contains(c)) {
                others.add(c);
                otherTotal += c.getPercentage();
            }
        }

        if (others.isEmpty()) return;

        if (otherTotal == 0 || remaining == 0) {
            for (Category c : others) c.setPercentage(0);
            if (remaining > 0) {
                Category biggestEdited = editedCategories.get(0);
                for (Category e : editedCategories) {
                    if (e.getPercentage() > biggestEdited.getPercentage()) biggestEdited = e;
                }
                biggestEdited.setPercentage(Math.round((biggestEdited.getPercentage() + remaining) * 10.0) / 10.0);
            }
            return;
        }

        double assigned = 0;
        for (Category c : others) {
            double newPercent = Math.round((c.getPercentage() / otherTotal) * remaining * 10.0) / 10.0;
            c.setPercentage(newPercent);
            assigned += newPercent;
        }

        double leftover = Math.round((remaining - assigned) * 10.0) / 10.0;
        if (Math.abs(leftover) > 0.0) {
            Category biggest = others.get(0);
            for (Category c : others) {
                if (c.getPercentage() > biggest.getPercentage()) biggest = c;
            }
            biggest.setPercentage(Math.round((biggest.getPercentage() + leftover) * 10.0) / 10.0);
        }
    }

    public void rebalanceAll() {
        if (divisions.isEmpty()) return;

        double total = getTotalPercentage();

        if (total == 0) {
            double split = Math.round((100.0 / divisions.size()) * 10.0) / 10.0;
            for (Category c : divisions) {
                c.setPercentage(split);
            }
            double assigned = split * divisions.size();
            double leftover = Math.round((100.0 - assigned) * 10.0) / 10.0;
            if (Math.abs(leftover) > 0.0) {
                divisions.get(0).setPercentage(
                        Math.round((divisions.get(0).getPercentage() + leftover) * 10.0) / 10.0);
            }
            return;
        }

        double assigned = 0;
        for (Category c : divisions) {
            double newPercent = Math.round((c.getPercentage() / total) * 100.0 * 10.0) / 10.0;
            c.setPercentage(newPercent);
            assigned += newPercent;
        }

        double leftover = Math.round((100.0 - assigned) * 10.0) / 10.0;
        if (Math.abs(leftover) > 0.0) {
            Category biggest = divisions.get(0);
            for (Category c : divisions) {
                if (c.getPercentage() > biggest.getPercentage()) {
                    biggest = c;
                }
            }
            biggest.setPercentage(Math.round((biggest.getPercentage() + leftover) * 10.0) / 10.0);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void addDivider(String name, double newPercent) {
        if (newPercent < 0) newPercent = 1;
        if (newPercent > 100) newPercent = 100;

        Category newCategory = new Category(name, newPercent);
        divisions.add(newCategory);
        if (divisions.size() == 1) return;
        if (newPercent == 100) {
            for (Category c : divisions) {
                if (c != newCategory) {
                    c.setPercentage(0);
                }
            }
            return;
        }
        rebalanceExcept(newCategory);
    }

    public double getTotalAllocations() {
        double total = 0;
        for (Category c : divisions) {
            total += c.getLastAllocation();
        }
        return total;
    }

    public void allocateIncome(double income) {
        User user = SessionManager.getInstance().getCurrentUser();

        double otherAllocation = 0;

        if (user != null && getTotalAllocations() < user.getBalance()) {
            otherAllocation = user.getBalance() - getTotalAllocations();
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

    public void deductExpense(String category, double amount) {
        for (Category c : divisions) {
            if (c.getCategory().equals(category)) {
                c.setBalance(c.getBalance() - amount);
                return;
            }
        }
    }

    public boolean tryDeductExpense(String category, double amount) {
        for (Category c : divisions) {
            if (c.getCategory().equals(category)) {
                if (c.getBalance() < amount) return false;
                c.setBalance(c.getBalance() - amount);
                return true;
            }
        }
        return true;
    }

    public void redistributeDeletedBalance(double deletedBalance) {
        if (divisions.isEmpty() || deletedBalance <= 0) return;

        double totalPercent = getTotalPercentage();
        if (totalPercent == 0) return;

        double assigned = 0;
        for (int i = 0; i < divisions.size() - 1; i++) {
            Category c = divisions.get(i);
            double share = Math.round((c.getPercentage() / totalPercent) * deletedBalance * 100.0) / 100.0;
            c.setBalance(c.getBalance() + share);
            assigned += share;
        }

        double leftover = Math.round((deletedBalance - assigned) * 100.0) / 100.0;
        divisions.get(divisions.size() - 1).setBalance(
                divisions.get(divisions.size() - 1).getBalance() + leftover);
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
