package com.example.studentfinancetracker_wk4;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        User currentUser = SessionManager.getInstance().getCurrentUser();

        TextView tvUserEmail = view.findViewById(R.id.tvUserEmail);
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        renderPools(view, currentUser);

        Button btnQuickIncome = view.findViewById(R.id.btnQuickIncome);
        Button btnQuickExpense = view.findViewById(R.id.btnQuickExpense);
        ImageButton btnLogout = view.findViewById(R.id.btnLogout);

        btnQuickIncome.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), IncomeActivity.class)));
        btnQuickExpense.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExpenseActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance().setCurrentUser(null);
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            renderPools(view, SessionManager.getInstance().getCurrentUser());
        }
    }

    private void renderPools(View view, User currentUser) {
        LinearLayout poolsContainer = view.findViewById(R.id.poolsContainer);
        TextView tvNoPools = view.findViewById(R.id.tvNoPools);
        poolsContainer.removeAllViews();

        if (currentUser == null) return;

        SmartPercentageDivider divider = currentUser.getPercDivider();
        List<Category> pools = divider.getAllDividers();

        if (pools.isEmpty()) {
            tvNoPools.setVisibility(View.VISIBLE);
            return;
        }

        tvNoPools.setVisibility(View.GONE);
        for (Category c : pools) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvName = new TextView(requireContext());
            tvName.setText(c.getCategory());
            tvName.setTextColor(0xFFFFFFFF);
            tvName.setTextSize(15);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvBalance = new TextView(requireContext());
            tvBalance.setText(String.format("€%.2f  (%.0f%%)", c.getBalance(), c.getPercentage()));
            tvBalance.setTextColor(0xFF4DB6AC);
            tvBalance.setTextSize(15);

            row.addView(tvName);
            row.addView(tvBalance);
            poolsContainer.addView(row);
        }
    }
}
