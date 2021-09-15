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
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Data;

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
        acceptBeforeUnloadAlertsAutomatically();
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
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);

        web.loadUrl("http://192.168.1.156:3001");
        //web.loadUrl("https://production-frontend-fisify.herokuapp.com/");
    }

    private void acceptBeforeUnloadAlertsAutomatically()
    {
        WebChromeClient webChromeClient = new WebChromeClient() {
            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return super.onJsBeforeUnload(view, url, message, result);
            }
        };
        web.setWebChromeClient(webChromeClient);
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

                timer.postDelayed(() ->
                {
                    activity.setContentView(web);
                    window.setNavigationBarColor(getResources().getColor(R.color.fisifyBackground));
                    window.setStatusBarColor(getResources().getColor(R.color.fisifyBackground));
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
    public void showNotificationAfterSeconds(int seconds, String text)
    {
        Data data = new Data.Builder().putString("notificationText", text).build();
        WorkRequest requestNotification = new OneTimeWorkRequest.Builder(NotificationWorker.class).setInputData(data).setInitialDelay(seconds, TimeUnit.SECONDS).addTag("notification").build();
        WorkManager.getInstance(context).enqueue(requestNotification);
    }

    @JavascriptInterface
    public void cancelAllNotifications()
    {
        WorkManager.getInstance(context).cancelAllWorkByTag("notification");
    }

    @JavascriptInterface
    public void fullScreen()
    {
        runOnUiThread(() ->
        {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
        });
    }

    @JavascriptInterface
    public void normalScreen()
    {
        runOnUiThread(() ->
        {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
