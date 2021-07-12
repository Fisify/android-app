package com.fisify.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

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
        listenForNotificationRequestsFromJavascript();

        showNotificationAfterSeconds(5);
    }

    private void showSplashScreen()
    {
        splash = new ImageView(context);
        splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
        splash.setImageResource(R.drawable.splashscreen);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        splash.setLayoutParams(params);

        RelativeLayout layout = new RelativeLayout(context);
        layout.addView(splash);
        setContentView(layout);
    }

    private void startLoadingWebView()
    {
        web = new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
        int SDK_INT = Build.VERSION.SDK_INT;
        if (SDK_INT > 16)
        {
            web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        web.loadUrl("https://staging-frontend-fisify.herokuapp.com");
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

    private void listenForNotificationRequestsFromJavascript()
    {
        web.addJavascriptInterface(this, "Android");
    }

    @JavascriptInterface
    public void showNotificationAfterSeconds(int seconds)
    {
        WorkRequest requestNotification = new OneTimeWorkRequest.Builder(NotificationWorker.class).setInitialDelay(seconds, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context).enqueue(requestNotification);
    }
}