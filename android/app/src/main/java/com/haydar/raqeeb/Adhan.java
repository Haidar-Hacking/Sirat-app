package com.haydar.raqeeb;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import com.haydar.sirat.R;
import org.json.JSONArray;

public class Adhan {
    static final String CH = "adhan_v2";
    static final int MAX = 200;

    static Uri soundUri(Context context) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.adhan);
    }

    static AudioAttributes attrs() {
        return new AudioAttributes.Builder().setUsage(4).setContentType(4).build();
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            try {
                notificationManager.deleteNotificationChannel("adhan_v1");
            } catch (Exception e) {
            }
            if (notificationManager.getNotificationChannel(CH) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(CH, "الأذان", 4);
                notificationChannel.setDescription("صوت الأذان الكامل عند دخول وقت الصلاة");
                notificationChannel.setSound(soundUri(context), attrs());
                notificationChannel.enableVibration(true);
                notificationChannel.setLockscreenVisibility(1);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    static void show(Context context, String str, String str2, int i) {
        Notification.Builder builder;
        ensureChannel(context);
        Intent intent = new Intent(context, (Class<?>) MainActivity.class);
        intent.setFlags(872415232);
        PendingIntent activity = PendingIntent.getActivity(context, i, intent, 201326592);
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(context, CH);
        } else {
            Notification.Builder builder2 = new Notification.Builder(context);
            builder2.setSound(soundUri(context), attrs());
            builder2.setPriority(2);
            builder2.setDefaults(2);
            builder = builder2;
        }
        builder.setSmallIcon(R.drawable.ic_stat).setContentTitle(str).setContentText(str2).setStyle(new Notification.BigTextStyle().bigText(str2)).setAutoCancel(true).setContentIntent(activity).setCategory("alarm").setVisibility(1).setColor(-15253656);
        Intent intent2 = new Intent(context, (Class<?>) AlarmReceiver.class);
        intent2.setAction("com.haydar.raqeeb.STOP");
        intent2.putExtra("nid", i);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 50000 + i, intent2, 201326592);
        builder.addAction(new Notification.Action.Builder((Icon) null, "إيقاف الأذان", broadcast).build());
        builder.setDeleteIntent(broadcast);
        try {
            ((NotificationManager) context.getSystemService("notification")).notify(i, builder.build());
        } catch (Exception e) {
        }
    }

    static PendingIntent alarmIntent(Context context, int i, String str, String str2) {
        Intent intent = new Intent(context, (Class<?>) AlarmReceiver.class);
        intent.setAction("com.haydar.raqeeb.ADHAN");
        intent.putExtra("id", i);
        if (str != null) {
            intent.putExtra("t", str);
            intent.putExtra("b", str2);
        }
        return PendingIntent.getBroadcast(context, i, intent, 201326592);
    }

    static void setAlarm(AlarmManager alarmManager, long j, PendingIntent pendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(0, j, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(0, j, pendingIntent);
            } else {
                alarmManager.setExact(0, j, pendingIntent);
            }
        } catch (SecurityException e) {
            try {
                alarmManager.set(0, j, pendingIntent);
            } catch (Exception e2) {
            }
        }
    }

    static synchronized void scheduleAll(Context context, String str) {
        int i;
        synchronized (Adhan.class) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            SharedPreferences sharedPreferences = context.getSharedPreferences("adhan", 0);
            int i2 = sharedPreferences.getInt("n", 0);
            for (int i3 = 0; i3 < i2; i3++) {
                try {
                    alarmManager.cancel(alarmIntent(context, i3, null, null));
                } catch (Exception e) {
                }
            }
            try {
                if (str == null) {
                    str = "[]";
                }
                JSONArray jSONArray = new JSONArray(str);
                long currentTimeMillis = System.currentTimeMillis();
                i = 0;
                for (int i4 = 0; i4 < jSONArray.length() && i < MAX; i4++) {
                    try {
                        JSONArray jSONArray2 = jSONArray.getJSONArray(i4);
                        long j = jSONArray2.getLong(0);
                        if (j > currentTimeMillis) {
                            setAlarm(alarmManager, j, alarmIntent(context, i, jSONArray2.getString(1), jSONArray2.getString(2)));
                            i++;
                        }
                    } catch (Exception e2) {
                    }
                }
            } catch (Exception e3) {
                i = 0;
            }
            sharedPreferences.edit().putInt("n", i).apply();
        }
    }

    static void save(Context context, String str) {
        context.getSharedPreferences("adhan", 0).edit().putString("json", str).apply();
    }

    static void rescheduleSaved(Context context) {
        String string = context.getSharedPreferences("adhan", 0).getString("json", null);
        if (string != null) {
            scheduleAll(context, string);
        }
    }
}
