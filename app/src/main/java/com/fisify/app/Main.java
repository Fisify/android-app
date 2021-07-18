package com.fisify.app;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;


import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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

        /*
        ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(new FirebaseAuthUIActivityResultContract(), new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
            @Override
            public void onActivityResult(FirebaseAuthUIAuthenticationResult result)
            {
                Log.e("Fisify", "Sign in completed");
            }
        });

        List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build(), new AuthUI.IdpConfig.GoogleBuilder().build());

        Intent signInIntent = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build();
        signInLauncher.launch(signInIntent);
        */


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        showSplashScreen();

        startLoadingWebView();
        showWebViewWhenLoaded();

        registerNotificationChannelForAndroidVersion26plus();
        listenForNotificationRequestsFromJavascript();

    }

    @Override
    public void onBackPressed()
    {
        if (Build.VERSION.SDK_INT > 19)
        {
            web.evaluateJavascript("location.href", value ->
            {
                if (value.endsWith("/login\"") || value.endsWith("/home\""))
                {
                    finish();
                }
            });
            web.evaluateJavascript("history.back()",null);
            web.evaluateJavascript("Android", value -> Log.e(value, "Fisify"));
        }
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
        //setContentView(layout);
        setContentView(R.layout.splash);

    }

    private void startLoadingWebView()
    {
        web = new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setDatabasePath(getDatabasePath("fisifyDB").getPath());
        web.getSettings().setAppCacheEnabled(true);
        web.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
        if (Build.VERSION.SDK_INT > 16)
        {
            web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        web.loadUrl("https://staging-frontend-fisify.herokuapp.com/home");
    }

    private void showWebViewWhenLoaded()
    {
        Activity activity = this;
        web.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run()
                    {
                        activity.setContentView(web);
                        getWindow().setNavigationBarColor(getResources().getColor(R.color.fisifyBackground));
                        getWindow().setStatusBarColor(getResources().getColor(R.color.fisifyBackground));
                    }
                }, 2000);
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
        if (Build.VERSION.SDK_INT > 17)
        {
            web.addJavascriptInterface(this, "Android");
        }
    }

    @JavascriptInterface
    public void showNotificationAfterSeconds(int seconds)
    {
        WorkRequest requestNotification = new OneTimeWorkRequest.Builder(NotificationWorker.class).setInitialDelay(seconds, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context).enqueue(requestNotification);
    }
}