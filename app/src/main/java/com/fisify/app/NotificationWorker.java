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
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        String text = getInputData().getString("notificationText");
        Intent intent = new Intent(this.context, Main.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this.context, "FISIFY_CHANNEL_ID").setSmallIcon(R.drawable.notifications_logo).setContentTitle("Fisify").setContentText(text).setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(pendingIntent).setAutoCancel(true).build();
        NotificationManagerCompat.from(this.context).notify(1, notification);

        return Result.success();
    }
}
