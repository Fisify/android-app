package com.fisify.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Main extends AppCompatActivity
{
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        showSplashScreen();


        /*
        WebView web = new WebView(getApplicationContext());

        web.getSettings().setJavaScriptEnabled(true);
        web.addJavascriptInterface(new WebAppInterface(this), "Android");

        web.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.e("Martin", "finished");
                setContentView(web);
            }
        });

        web.loadUrl("https://app.fisify.com/?id=601988a64c97b4000440a242");

        // Test webpage hosted in Gitlab to test JS to Java communication
        //web.loadUrl("https://martin-azpillaga.gitlab.io/web-experiments");

        // Notifications
        // createNotificationChannel();
        /*
        WorkRequest requestNotification = new OneTimeWorkRequest.Builder(NotificationWorker.class).setInitialDelay(5, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context).enqueue(requestNotification);


        WorkRequest longRequestNotification = new OneTimeWorkRequest.Builder(NotificationWorker.class).setInitialDelay(50, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context).enqueue(longRequestNotification);


        WorkRequest minutesNotification = new OneTimeWorkRequest.Builder(NotificationWorker.class).setInitialDelay(10, TimeUnit.MINUTES).build();
        WorkManager.getInstance(context).enqueue(minutesNotification);
        */
    }

    private void showSplashScreen()
    {
        ImageView splash = new ImageView(context);
        splash.setImageResource(R.drawable.logo);
        setContentView(splash);
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FISIFY_CHANNEL";
            String description = "FISIFY_NOTIFICATIONS";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("FISIFY_CHANNEL_ID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}