package com.quickspend.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationManagerCompat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PhonePeNotificationListener extends NotificationListenerService {
    static boolean enabled(Context context) {
        return context.getSharedPreferences("qs_float", MODE_PRIVATE).getBoolean("phonepe_capture", false);
    }

    static boolean setEnabled(Context context, boolean enabled) {
        return context.getSharedPreferences("qs_float", MODE_PRIVATE).edit()
                .putBoolean("phonepe_capture", enabled).commit();
    }

    static boolean allowed(Context context) {
        if (Build.VERSION.SDK_INT >= 27) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            return manager != null && manager.isNotificationListenerAccessGranted(
                    new ComponentName(context, PhonePeNotificationListener.class));
        }
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName());
    }

    private static String text(Bundle extras, String key) {
        CharSequence value = extras.getCharSequence(key);
        return value == null ? "" : value.toString();
    }

    @Override public void onNotificationPosted(StatusBarNotification posted) {
        if (posted == null || !PhonePePayment.PACKAGE.equals(posted.getPackageName()) || !enabled(this)) return;
        Notification notice = posted.getNotification();
        if (notice == null || notice.extras == null || (notice.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        try {
            String title = text(notice.extras, Notification.EXTRA_TITLE);
            PhonePePayment payment = PhonePePayment.parse(posted.getPackageName(), title,
                    text(notice.extras, Notification.EXTRA_BIG_TEXT));
            if (payment == null) payment = PhonePePayment.parse(posted.getPackageName(), title,
                    text(notice.extras, Notification.EXTRA_TEXT));
            if (payment == null) return;
            long stamp = notice.when > 0 ? notice.when : posted.getPostTime();
            String identity = posted.getKey() + "|" + stamp + "|" + payment.amount + "|" + payment.sender + "|" + payment.account;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder(PhonePePayment.KEY_PREFIX);
            for (byte value : digest) {
                key.append(Character.forDigit((value >>> 4) & 15, 16));
                key.append(Character.forDigit(value & 15, 16));
            }
            SmsStore.enqueuePhonePe(this, payment, key.toString(), posted.getPostTime());
        } catch (Exception failure) {
            android.util.Log.w("QuickSpend", "Could not queue PhonePe payment");
        }
    }
}
