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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationWorker extends Worker
{
	static int incrementingID = 0;

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
		String text = getInputData().getString("notificationText");
		Intent intent = new Intent(context, Main.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		Notification notification = new NotificationCompat.Builder(context, "FISIFY_CHANNEL_ID").setSmallIcon(R.drawable.notifications_logo).setContentTitle("Fisify").setContentText(text).setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(pendingIntent).setAutoCancel(true).build();
		NotificationManagerCompat.from(context).notify(createID(), notification);

		return Result.success();
	}

	public int createID()
	{
		Date now = new Date();
		int id = Integer.parseInt(new SimpleDateFormat("ddHHmmssSS", Locale.US).format(now));
		incrementingID++;
		return id + incrementingID;
	}
}
