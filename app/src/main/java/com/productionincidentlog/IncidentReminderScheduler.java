package com.productionincidentlog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class IncidentReminderScheduler {
    private IncidentReminderScheduler() {}
    public static void schedule(Context context) {
        AlarmManager alarm=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,IncidentReminderReceiver.class);
        PendingIntent pending=PendingIntent.getBroadcast(context,7301,intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar next=Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY,9);
        next.set(Calendar.MINUTE,0);
        next.set(Calendar.SECOND,0);
        next.set(Calendar.MILLISECOND,0);
        if(next.getTimeInMillis()<=System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR,1);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,pending);
    }
}
