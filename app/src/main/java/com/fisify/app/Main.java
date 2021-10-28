package com.fisify.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class Main extends AppCompatActivity
{
	String TAG = "Main";

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
	protected void onResume()
	{
		super.onResume();
		web.evaluateJavascript("window.initializeBeacon()",null);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		web.evaluateJavascript("window.sendBeacon()",null);
	}

	@Override
	protected void onUserLeaveHint()
	{
		super.onUserLeaveHint();
	}

	@Override
	protected void onStop()
	{
		KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		if( myKM.inKeyguardRestrictedInputMode()) {
			Log.d("Locked","l");
		} else {
			super.finishAndRemoveTask();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
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

		web.loadUrl("https://production-frontend-fisify.herokuapp.com/");
		// TODO: delete this
		// web.loadUrl("https://staging-frontend-fisify.herokuapp.com");
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
					web.evaluateJavascript("window.initializeBeacon()",null);
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

	// https://nabeelj.medium.com/making-a-simple-get-and-post-request-using-volley-beginners-guide-ee608f10c0a9
	@JavascriptInterface
	public void sendDeviceAndroid(String uid)
	{
		FirebaseMessaging.getInstance().getToken()
				.addOnCompleteListener(task -> {
					if (!task.isSuccessful()) {
						Log.w(TAG, "Fetching FCM registration token failed", task.getException());
						return;
					}
					// Get new FCM registration token
					String token = task.getResult();

					/*
					Toast.makeText(Main.this, token, Toast.LENGTH_SHORT).show();
					if (uid != null) {
						Toast.makeText(Main.this, uid, Toast.LENGTH_SHORT).show();
					} */

					RequestQueue queue = Volley.newRequestQueue(Main.this);
					String url = "https://staging-backend-fisify.herokuapp.com/api/devices";

					JSONObject jsonBody = new JSONObject();
					try {
						jsonBody.put("firebaseUID", uid);
						jsonBody.put("token", token);
						jsonBody.put("platform", "android");
					} catch (JSONException e) {
						e.printStackTrace();
					}

					JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
							Request.Method.POST,
							url,
							jsonBody,
							response -> Log.d(TAG, response.toString()),
							error -> Log.e(TAG, "Known error because backend returns an empty JSON response.")
					);

					// Add the request to the RequestQueue.
					queue.add(jsonObjectRequest);

					/* Long version without errors

					final String requestBody = jsonBody.toString();

					StringRequest stringRequest = new StringRequest(
							Request.Method.POST,
							url,
							response -> Log.i("LOG_RESPONSE", response),
							error -> Log.e("LOG_RESPONSE", error.toString())
					) {
						@Override
						public String getBodyContentType() {
							return "application/json; charset=utf-8";
						}

						@Override
						public byte[] getBody() throws AuthFailureError {
							try {
								return requestBody == null ? null : requestBody.getBytes("utf-8");
							} catch (UnsupportedEncodingException uee) {
								return null;
							}
						}

						@Override
						protected Response<String> parseNetworkResponse(NetworkResponse response) {
							String responseString = "";
							if (response != null) {
								responseString = String.valueOf(response.statusCode);
							}
							return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
						}
					}; */
				});
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
