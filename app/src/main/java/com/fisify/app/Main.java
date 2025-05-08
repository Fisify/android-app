package com.fisify.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fisify.app.interfaces.IVersionCallback;
import com.fisify.app.security.InstrumentationDetector;
import com.fisify.app.security.RootChecker;
import com.fisify.app.security.SSLPinning;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends AppCompatActivity
{
	private static final String TAG = "Main";

	Context context;
	Window window;
	WebView web;

	private static final int RC_SIGN_IN = 9001;

	private GoogleSignInClient mGoogleSignInClient;
	private int SPLASH_SCREEN_TIMEOUT = 2000;

	private final String WEBVIEW_URL = BuildConfig.WEBVIEW_URL;
	private final String VERSION_URL = BuildConfig.VERSION_CHECK_ENDPOINT_URL;
	private final String NOTIFICATIONS_URL = BuildConfig.NOTIFICATION_DEVICES_URL;
	private final String CLIENT_ID = BuildConfig.FIREBASE_CLIENT_ID;

	private static final int FILE_CHOOSER_REQUEST_CODE = 100;
	private ValueCallback<Uri[]> fileChooserCallback;

	private final AtomicBoolean isSecurityCheckValid = new AtomicBoolean(false);
	private FrameLayout rootLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Security checks
		InstrumentationDetector.performSecurityChecks(this);
		checkRootAndShowDialog();
		checkSSLPinning();

		// Force to use Light Mode always
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		context = getApplicationContext();
		window = getWindow();

		int MY_PERMISSIONS_REQUEST_CAMERA = 0;
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
		}

		showSplashScreen();
		initializeFirebaseAuthentication();
		View mainView = loadWebViewOnBackground();

		rootLayout = findViewById(android.R.id.content);

		rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
			Rect r = new Rect();
			rootLayout.getWindowVisibleDisplayFrame(r);
			int screenHeight = rootLayout.getRootView().getHeight();
			int heightDiff = screenHeight - r.height();

			boolean isKeyboardVisible = heightDiff > screenHeight * 0.15; // Umbral aproximado

			if (isKeyboardVisible) {
				showSystemUI();
			} else {
				hideSystemUI();
			}
		});

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				setContentView(mainView);
				web.setVisibility(View.VISIBLE);
				hideSystemUI();
			}
		}, SPLASH_SCREEN_TIMEOUT);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void checkSSLPinning() {
		// Check SSL pinning for security between APP and Webview
		new Thread(() -> {
			try {
				SSLPinning.makeRequestWithPinning();
				isSecurityCheckValid.set(true);
			} catch (Exception e) {
				runOnUiThread(() -> {
					abortWebViewLoading();
					showErrorDialog("Security error", "Invalid certificate");
				});
			}
		}).start();
	}

	protected void checkRootAndShowDialog() {
		// Checks if device is running on root mode and shows an alert in that case
		if (RootChecker.isDeviceRooted()) {
			showErrorDialog("Unsupported device", "This app cannot run on rooted devices for security reasons.");
			isSecurityCheckValid.set(false);
			abortWebViewLoading();
			return;
		}

		isSecurityCheckValid.set(true);
	}

	private void showErrorDialog(String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title)
			.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("Close", (dialog, which) -> {
				finishActivity(0);
				System.exit(0);
			})
			.show();
	}

	private void abortWebViewLoading() {
		if (web != null) {
			web.stopLoading();
			web.loadUrl("about:blank");
			web.destroy();
		}
	}

	protected void initializeFirebaseAuthentication() {
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(CLIENT_ID)
				.requestEmail()
				.build();
		mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
	}

	protected View loadWebViewOnBackground() {
		// Inicializar el WebView en segundo plano pero sin mostrarlo aún
		View mainView = getLayoutInflater().inflate(R.layout.layout, null);
		web = mainView.findViewById(R.id.webView);
		web.setVisibility(View.GONE); // Ocultar inicialmente

		// Configurar el WebView
		startLoadingWebView();
		acceptBeforeUnloadAlertsAutomatically();
		showWebViewWhenLoaded();
		listenForNotificationRequestsFromJavascript();

		return mainView;
	}

	@SuppressLint("NewApi")
	private void hideSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Android 11+ - Modo inmersivo pero permitiendo ajuste de teclado
			getWindow().setDecorFitsSystemWindows(false);
		} else {
			// Versiones anteriores - Modo inmersivo mejorado
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			);
		}
		// Hacer la barra de estado transparente
		getWindow().setStatusBarColor(Color.TRANSPARENT);
	}

	@SuppressLint("NewApi")
	private void showSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(true);
		} else {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		}
		// Restaurar el color de la barra de estado (puedes cambiarlo si lo necesitas)
		getWindow().setStatusBarColor(Color.TRANSPARENT);
	}

	@JavascriptInterface
	public void showGoogleSignIn () {
		Intent signInIntent = mGoogleSignInClient.getSignInIntent();
		startActivityForResult(signInIntent, RC_SIGN_IN);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RC_SIGN_IN) {
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				// Google Sign In was successful, authenticate with Firebase
				GoogleSignInAccount account = task.getResult(ApiException.class);
				String idToken = account.getIdToken();

				String javascriptCode = String.format("window.handleGoogleSignInFromAndroid('%s')", idToken);
				web.evaluateJavascript(javascriptCode,null);

				// https://stackoverflow.com/questions/38707133/google-firebase-sign-out-and-forget-user-in-android-app
				this.mGoogleSignInClient.signOut();
			} catch (ApiException e) {
				// Google Sign In failed, update UI appropriately
				Log.w(TAG, "Google sign in failed", e);
			}
		}

		if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
			if (fileChooserCallback != null) {
				Uri[] result = (resultCode == RESULT_OK && data != null)
						? new Uri[]{data.getData()}
						: null;
				fileChooserCallback.onReceiveValue(result);
				fileChooserCallback = null;
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		web.evaluateJavascript("location.href", value -> {
			String[] paths = {"/login", "/home", "/wellness\"", "/education\"", "/habit\"", "/stats\""};

			boolean shouldFinishApp = false;
			for (String path : paths) {
				if (value.contains(path)) {
					shouldFinishApp = true;
					break;
				}
			}
			if (shouldFinishApp) {
				finish();
			} else {
				web.evaluateJavascript("history.back()",null);
			}
		});
	}

	private void showSplashScreen() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_FULLSCREEN |
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
		);
		setContentView(R.layout.splash);
	}

	private void getVersion(final IVersionCallback callback) {
		final RequestQueue queue = Volley.newRequestQueue(Main.this);

		final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, VERSION_URL, null,
				response -> {
					try {
						String version = response.getString("version");
						callback.onSuccess(version);
					} catch (Exception e) {
						e.printStackTrace();
						callback.onError(new VolleyError("Error al obtener la versión"));
					}
				},
				error -> callback.onError(error));

		queue.add(jsonObjectRequest);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void startLoadingWebView() {
		WebView.setWebContentsDebuggingEnabled(true);

		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setDomStorageEnabled(true);
		web.getSettings().setAllowContentAccess(true);
		web.getSettings().setAllowFileAccess(true);
		web.getSettings().setMediaPlaybackRequiresUserGesture(false);

		final long systemMillis = System.currentTimeMillis() / 1000;
		final String timestamp = Long.toString(systemMillis);

		getVersion(new IVersionCallback() {
			@Override
			public void onSuccess(String version) {
				final String webviewUrl = WEBVIEW_URL + "?version=" + version;
				Log.d(TAG, webviewUrl);
				web.loadUrl(webviewUrl);
			}

			@Override
			public void onError(VolleyError error) {
				final String webviewUrl = WEBVIEW_URL + "?timestamp=" + timestamp;
				Log.e(TAG, error.toString());
				Log.d(TAG, webviewUrl);
				web.loadUrl(webviewUrl);
			}
		});
	}

	private void acceptBeforeUnloadAlertsAutomatically() {
		WebChromeClient webChromeClient = new WebChromeClient() {
			@Override
			public void onPermissionRequest(PermissionRequest request) {
				runOnUiThread(() -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						String[] PERMISSIONS = {
								PermissionRequest.RESOURCE_AUDIO_CAPTURE,
								PermissionRequest.RESOURCE_VIDEO_CAPTURE,
						};
						request.grant(PERMISSIONS);
					}
				});
			}
			@Override
			public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
				result.confirm();
				return super.onJsBeforeUnload(view, url, message, result);
			}

			@Override
			public Bitmap getDefaultVideoPoster() {
				return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
			}

			@Override
			public boolean onShowFileChooser(
					WebView webView,
					ValueCallback<Uri[]> filePathCallback,
					FileChooserParams fileChooserParams
			) {
				if (fileChooserCallback != null) {
					fileChooserCallback.onReceiveValue(null);
				}
				fileChooserCallback = filePathCallback;

				Intent intent = fileChooserParams.createIntent();
				try {
					startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					fileChooserCallback = null;
					return false;
				}
				return true;
			}
		};
		web.setWebChromeClient(webChromeClient);
	}

	private void showWebViewWhenLoaded() {
		web.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (!isSecurityCheckValid.get()) {
					view.stopLoading();
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				runOnUiThread(() -> {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Bloquear de nuevo en portrait
					new Handler().postDelayed(() -> {
						String js = "(function() {" +
								"var inputs = document.querySelectorAll('input, textarea');" +
								"for (var i = 0; i < inputs.length; i++) {" +
								"inputs[i].addEventListener('focus', function() {" +
								"this.scrollIntoView({ behavior: 'smooth', block: 'nearest' });" +
								"});" +
								"}" +
								"})();";
						view.evaluateJavascript(js, null);
					}, 500); // Retraso de 500 milisegundos (ajusta si es necesario)
				});
			}
		});
	}

	private void listenForNotificationRequestsFromJavascript() {
		web.addJavascriptInterface(this, "Android");
	}

	// https://nabeelj.medium.com/making-a-simple-get-and-post-request-using-volley-beginners-guide-ee608f10c0a9
	@JavascriptInterface
	public void sendDeviceAndroid(String uid) {
		FirebaseMessaging.getInstance().getToken()
				.addOnCompleteListener(task -> {
					if (!task.isSuccessful()) {
						Log.w(TAG, "Fetching FCM registration token failed", task.getException());
						return;
					}
					// Get new FCM registration token
					String token = task.getResult();
					Locale defaultLocale = Locale.getDefault();
					String locale = defaultLocale.getLanguage().substring(0, 2);
					RequestQueue queue = Volley.newRequestQueue(Main.this);

					JSONObject jsonBody = new JSONObject();
					try {
						jsonBody.put("firebaseUID", uid);
						jsonBody.put("token", token);
						jsonBody.put("lang", locale);
						jsonBody.put("platform", "android");
					} catch (JSONException e) {
						e.printStackTrace();
					}

					JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
							Request.Method.POST,
							NOTIFICATIONS_URL,
							jsonBody,
							response -> Log.d(TAG, response.toString()),
							error -> Log.e(TAG, "Known error because backend returns an empty JSON response.")
					);

					// https://stackoverflow.com/questions/27873001/android-volley-sending-data-twice
					jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

					// Add the request to the RequestQueue.
					queue.add(jsonObjectRequest);
				});
	}

	/**
	 * This functions leaves the orientation control to the web application
	 */
	@JavascriptInterface
	public void fullScreen() {
		runOnUiThread(() -> {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // Permitir cambios temporales
		});
	}

	@JavascriptInterface
	public void normalScreen() {
		runOnUiThread(() -> {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Bloquear de nuevo en portrait
		});
	}

	@JavascriptInterface
	public void landscape() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	@JavascriptInterface
	public void portrait() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@JavascriptInterface
	public void openExternalUrl(String url) {
		runOnUiThread(() -> {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// https://stackoverflow.com/questions/42900826/how-can-i-show-an-image-from-link-in-android-push-notification
	public Bitmap getBitmapFromURL(String strURL) {
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return myBitmap;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
