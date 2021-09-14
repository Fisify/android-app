package com.fisify.app;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

public class Main extends AppCompatActivity
{
    Context context;
    Window window;
    WebView web;

    int SPLASHSCREEN_DELAY_AFTER_PAGE_LOADED = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        window = getWindow();

        showSplashScreen();

        startLoadingWebView();
        showWebViewWhenLoaded();

        registerNotificationChannelForAndroidVersion26plus();
        listenForNotificationRequestsFromJavascript();
    }

    @Override
    public void onBackPressed()
    {
        web.evaluateJavascript("location.href", value ->
        {
            if (value.endsWith("/login\"") || value.endsWith("/home\""))
            {
                finish();
            }
            else
            {
                web.evaluateJavascript("history.back()",null);
            }
        });
    }

    private void showSplashScreen()
    {
        setContentView(R.layout.splash);
    }

    private void startLoadingWebView()
    {
        WebView.setWebContentsDebuggingEnabled(true);

        web = new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setDatabasePath(getDatabasePath("fisifyDB").getPath());
        web.getSettings().setAppCacheEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);

        // mikel@production.com
        // mikelmikel
        // Necessary for firebase login with google
        //web.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");

        web.loadUrl("https://production-frontend-fisify.herokuapp.com/");
    }

    private void showWebViewWhenLoaded()
    {
        Activity activity = this;

        web.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                final Handler timer = new Handler(Looper.getMainLooper());

                timer.postDelayed(new Runnable() {
                    @Override
                    public void run()
                    {
                        activity.setContentView(web);
                        window.setNavigationBarColor(getResources().getColor(R.color.fisifyBackground));
                        window.setStatusBarColor(getResources().getColor(R.color.fisifyBackground));
                    }
                }, SPLASHSCREEN_DELAY_AFTER_PAGE_LOADED);
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

    @JavascriptInterface
    public void fullScreen()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }
        });
    }

    @JavascriptInterface
    public void normalScreen()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        });
    }

    @JavascriptInterface
    public void landscape()
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @JavascriptInterface
    public void portrait()
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}