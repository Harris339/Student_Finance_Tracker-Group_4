package com.example.studentfinancetracker_wk4;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class BalanceViewModel extends ViewModel {
    private final MutableLiveData<Double> balance = new MutableLiveData<>();

    public LiveData<Double> getBalance() {
        return balance;
    }

    public void setBalance(Double value) {
        balance.setValue(value);
    }
}
