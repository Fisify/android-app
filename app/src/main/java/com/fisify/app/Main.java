package com.fisify.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Main extends AppCompatActivity
{
    Context context;
    ImageView splash;
    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        showSplashScreen();

        startLoadingWebView();
        showWebViewWhenLoaded();

        registerNotificationChannelForAndroidVersion26plus();
        //connectToJavascript();

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
        splash = new ImageView(context);
        splash.setImageResource(R.drawable.logo);
        setContentView(splash);
    }

    private void startLoadingWebView()
    {
        web = new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.loadUrl("https://app.fisify.com/?id=601988a64c97b4000440a242");
    }

    private void showWebViewWhenLoaded()
    {
        web.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                setContentView(web);
            }
        });
    }

    private void registerNotificationChannelForAndroidVersion26plus()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel("FISIFY_CHANNEL_ID", "FISIFY_CHANNEL", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("FISIFY_NOTIFICATIONS");

            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void connectToJavascript()
    {
        web.addJavascriptInterface(this, "Android");
    }

    @JavascriptInterface
    public void showNotificationAfterSeconds(int seconds)
    {

    }
}