package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TransactionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.BackBtn).setVisibility(View.GONE);

        view.findViewById(R.id.transactions).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ViewTransactionsActivity.class)));

        view.findViewById(R.id.addIncomeBtn).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), IncomeActivity.class)));

        view.findViewById(R.id.addExpenseBtn).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExpenseActivity.class)));
    }
}
