package com.haydar.raqeeb;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.haydar.raqeeb.STOP".equals(intent.getAction())) {
            try {
                ((NotificationManager) context.getSystemService("notification")).cancel(intent.getIntExtra("nid", 0));
                return;
            } catch (Exception e) {
                return;
            }
        }
        String stringExtra = intent.getStringExtra("t");
        String stringExtra2 = intent.getStringExtra("b");
        if (stringExtra == null) {
            stringExtra = "حان الآن وقت الصلاة";
        }
        if (stringExtra2 == null) {
            stringExtra2 = "";
        }
        Adhan.show(context, stringExtra, stringExtra2, intent.getIntExtra("id", 0) + 1000);
    }
}
