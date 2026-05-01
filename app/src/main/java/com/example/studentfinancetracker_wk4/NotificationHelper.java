package com.example.studentfinancetracker_wk4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID       = "overspend_channel";
    private static final String CHANNEL_NAME      = "Overspending Alerts";
    private static final int    NOTIFICATION_ID   = 1001;

    private static final String BILLS_CHANNEL_ID   = "bills_channel";
    private static final String BILLS_CHANNEL_NAME = "Upcoming Bill Reminders";
    private static final int    BILLS_NOTIF_ID     = 1002;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel overspend = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            overspend.setDescription("Alerts when you overspend your income");
            manager.createNotificationChannel(overspend);

            NotificationChannel bills = new NotificationChannel(
                    BILLS_CHANNEL_ID, BILLS_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            bills.setDescription("Reminders for bills due within 3 days");
            manager.createNotificationChannel(bills);
        }
    }

    public static void notifyUpcomingBills(android.content.Context context, User user) {
        if (user == null) return;
        java.util.List<FixedExpense> urgent = new java.util.ArrayList<>();
        for (FixedExpense e : user.getExpenseList().getAllExpenses()) {
            if (e.getLastPayment() != null && e.getDaysUntilNextPayment() <= 3) {
                urgent.add(e);
            }
        }
        if (urgent.isEmpty()) return;

        String title, body;
        if (urgent.size() == 1) {
            FixedExpense e = urgent.get(0);
            int days = e.getDaysUntilNextPayment();
            title = "Bill due " + (days == 0 ? "today" : "in " + days + " day" + (days == 1 ? "" : "s"));
            body  = e.getTitle() + " — " + String.format(java.util.Locale.getDefault(), "€%.2f", e.getAmount());
        } else {
            title = urgent.size() + " bills due soon";
            StringBuilder sb = new StringBuilder();
            for (FixedExpense e : urgent) sb.append(e.getTitle()).append(", ");
            body = sb.substring(0, sb.length() - 2);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, BILLS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(BILLS_NOTIF_ID, builder.build());
    }

    public static void sendOverspendNotification(Context context, double balance) {
        String message = String.format(
                "Your balance is now €%.2f. You are overspending!", balance);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Overspending Alert!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}