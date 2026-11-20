package com.productionincidentlog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class IncidentReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL="incident_reminders";
    @Override public void onReceive(Context context,Intent intent) {
        if(!IncidentReminderScheduler.isEnabled(context)) return;
        IncidentReminderScheduler.schedule(context);
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        IncidentDb db=new IncidentDb(context);
        try {
            int overdue=db.countOverdue(today);
            List<String[]> upcoming=db.incidentsDue(today,7,false);
            int due=upcoming.size();
            if(overdue==0 && due==0) return;
            NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26) {
                NotificationChannel channel=new NotificationChannel(CHANNEL,"整改期限提醒",NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("生产事故整改逾期及到期提醒");
                manager.createNotificationChannel(channel);
            }
            Intent open=new Intent(context,MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent content=PendingIntent.getActivity(context,7302,open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            String message="逾期未完成："+overdue+" 起；未来 7 天到期："+due+" 起";
            NotificationCompat.Builder builder=new NotificationCompat.Builder(context,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("生产事故整改提醒")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(content);
            manager.notify(7303,builder.build());
        } finally { db.close(); }
    }
}
