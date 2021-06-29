package com.fisify.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker
{
    Context context;
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Intent intent = new Intent(context, Main.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(context, "FISIFY_CHANNEL_ID").setSmallIcon(R.drawable.logo_transparent).setContentTitle("Fisify").setContentText(context.getString(R.string.notification_text)).setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(pendingIntent).setAutoCancel(true).build();
        NotificationManagerCompat.from(context).notify(1, notification);

        return Result.success();
    }
}