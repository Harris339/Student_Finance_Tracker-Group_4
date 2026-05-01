package com.example.studentfinancetracker_wk4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.List;

public class NotificationHelper {

    private static final String CHANNEL_ID = "tracker_alerts";
    private static final int    NOTIF_ID   = 1001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Tracker Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Upcoming bill reminders");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void notifyUpcomingBills(Context context, List<FixedExpense> expenses) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
        }

        List<FixedExpense> dueSoon = new ArrayList<>();
        for (FixedExpense e : expenses) {
            if (e.getLastPayment() != null && isDueSoon(e)) dueSoon.add(e);
        }
        if (dueSoon.isEmpty()) return;

        String title, text;
        if (dueSoon.size() == 1) {
            FixedExpense e = dueSoon.get(0);
            title = e.getTitle() + " due " + dueLabel(e);
            text  = String.format("€%.2f — %s", e.getAmount(), e.getCategory());
        } else {
            title = dueSoon.size() + " bills due soon";
            StringBuilder sb = new StringBuilder();
            for (FixedExpense e : dueSoon) sb.append(e.getTitle()).append(", ");
            text = sb.substring(0, sb.length() - 2);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build());
    }

    private static boolean isDueSoon(FixedExpense e) {
        int days = e.getDaysUntilNextPayment();
        switch (e.getFrequency()) {
            case "Daily":   return days <= 1;
            case "Weekly":  return days <= 3;
            case "Monthly": return days <= 7;
            case "Yearly":  return days <= 30;
            default:        return false;
        }
    }

    private static String dueLabel(FixedExpense e) {
        int days = e.getDaysUntilNextPayment();
        if (days == 0) return "today";
        if (days == 1) return "tomorrow";
        return "in " + days + " days";
    }
}
