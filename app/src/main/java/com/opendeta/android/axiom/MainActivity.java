package com.opendeta.android.axiom;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import com.opendeta.android.axiom.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
	
	private MainBinding binding;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		
		binding.browserWebview.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView _param1, String _param2, Bitmap _param3) {
				final String _url = _param2;
				
				super.onPageStarted(_param1, _param2, _param3);
			}
			
			@Override
			public void onPageFinished(WebView _param1, String _param2) {
				final String _url = _param2;
				
				super.onPageFinished(_param1, _param2);
			}
		});
	}
	
	private void initializeLogic() {
	} // onCreate ক্লোজ
	
	private android.webkit.ValueCallback<android.net.Uri[]> mUploadMessageArr;
	private final int FILECHOOSER_RESULTCODE = 1001;
	private boolean isAdBlockEnabled = true;
	private boolean isRefreshModeEnabled = false;
	private boolean isZoomEnabled = false; 
	private boolean isDesktopModeEnabled = false;
	private boolean isBotRunning = false;
	private final String BOT_CODE_KEY = "saved_bot_script";
	private final String CHROME_UA = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Mobile Safari/537.36";
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == FILECHOOSER_RESULTCODE) {
			if (mUploadMessageArr == null) return;
			mUploadMessageArr.onReceiveValue(android.webkit.WebChromeClient.FileChooserParams.parseResult(resultCode, data));
			mUploadMessageArr = null;
		}
	}
	
	private androidx.cardview.widget.CardView createCardButton(String text, int color, int textColor, boolean isChecked, boolean showCheckBox) {
		androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
		card.setCardBackgroundColor(color);
		card.setRadius(25f);
		card.setCardElevation(12f);
		card.setUseCompatPadding(true);
		android.widget.LinearLayout container = new android.widget.LinearLayout(this);
		container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		container.setGravity(android.view.Gravity.CENTER_VERTICAL);
		container.setPadding(40, 30, 40, 30);
		android.widget.TextView tv = new android.widget.TextView(this);
		tv.setText(text);
		tv.setTextColor(textColor);
		tv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
		tv.setTypeface(null, android.graphics.Typeface.BOLD);
		container.addView(tv);
		if (showCheckBox) {
			android.widget.CheckBox cb = new android.widget.CheckBox(this);
			cb.setChecked(isChecked);
			cb.setClickable(false);
			int[][] states = new int[][] { new int[] { android.R.attr.state_checked }, new int[] { -android.R.attr.state_checked } };
			int[] colors = new int[] { 0xFF2196F3, 0xFFBDBDBD };
			cb.setButtonTintList(new android.content.res.ColorStateList(states, colors));
			container.addView(cb);
		}
		card.addView(container);
		return card;
	}
	
	private androidx.cardview.widget.CardView createStopButton() {
		androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
		card.setCardBackgroundColor(0xFFFFFFFF);
		card.setRadius(80f);
		card.setCardElevation(15f);
		card.setUseCompatPadding(true);
		android.widget.LinearLayout container = new android.widget.LinearLayout(this);
		container.setPadding(30, 15, 45, 15);
		container.setGravity(android.view.Gravity.CENTER);
		android.widget.ProgressBar pb = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
		pb.setLayoutParams(new android.widget.LinearLayout.LayoutParams(50, 50));
		android.widget.TextView tv = new android.widget.TextView(this);
		tv.setText("  Stop");
		tv.setTextSize(18);
		tv.setTextColor(0xFF000000);
		tv.setTypeface(null, android.graphics.Typeface.BOLD);
		container.addView(pb);
		container.addView(tv);
		card.addView(container);
		return card;
	}
	
	private void showStatus(String msg) {
		android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
	}
	
	private void showBotEditor(final android.webkit.WebView webview) {
		final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).create();
		final android.content.SharedPreferences pref = getSharedPreferences("BotPrefs", android.content.Context.MODE_PRIVATE);
		android.widget.LinearLayout root = new android.widget.LinearLayout(this);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setPadding(50, 50, 50, 50);
		
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setCornerRadius(35f);
		bg.setColor(0xFFFFFFFF);
		root.setBackground(bg);
		
		android.widget.TextView header = new android.widget.TextView(this);
		header.setText("Code Editor");
		header.setTextSize(22);
		header.setTextColor(0xFF333333);
		header.setTypeface(null, android.graphics.Typeface.BOLD);
		header.setPadding(0, 0, 0, 40);
		android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
		android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f);
		scrollView.setLayoutParams(scrollParams);
		final android.widget.EditText editor = new android.widget.EditText(this);
		editor.setText(pref.getString(BOT_CODE_KEY, ""));
		editor.setGravity(android.view.Gravity.TOP);
		editor.setHint("Write JavaScript code here...");
		editor.setTypeface(android.graphics.Typeface.MONOSPACE);
		editor.setBackgroundResource(android.R.drawable.editbox_background_normal);
		scrollView.addView(editor);
		android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(this);
		btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		btnLayout.setGravity(android.view.Gravity.RIGHT);
		btnLayout.setPadding(0, 30, 0, 0);
		final androidx.cardview.widget.CardView cardCancel = createCardButton("Cancel", 0xFFE3F2FD, 0xFF2196F3, false, false);
		final androidx.cardview.widget.CardView cardRun = createCardButton("Run", 0xFFFFEBEE, 0xFFE57373, false, false);
		btnLayout.addView(cardCancel);
		btnLayout.addView(cardRun);
		root.addView(header);
		root.addView(scrollView);
		root.addView(btnLayout);
		dialog.setView(root);
		
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
		}
		
		cardCancel.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) { dialog.dismiss(); }
		});
		cardRun.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				String code = editor.getText().toString();
				pref.edit().putString(BOT_CODE_KEY, code).apply();
				webview.evaluateJavascript("(function(){ try{"+code+"}catch(e){alert(e.message);} })();", null);
				isBotRunning = true;
				showStatus("Bot Activated!");
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	private void startMyBrowser() {
		final android.widget.EditText urlInput = findViewById(R.id.url_input);
		final android.widget.ImageView btnClear = findViewById(R.id.btn_clear_text);
		final android.widget.TextView btnMenu = findViewById(R.id.btn_menu);
		final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		final androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipe_refresh);
		
		final android.widget.ProgressBar webProgress = findViewById(R.id.web_progress);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			webProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF0000));
		} else {
			webProgress.getProgressDrawable().setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.SRC_IN);
		}
		
		btnClear.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) { urlInput.setText(""); }
		});
		
		// 🎯 মূল অপরাধী ধরা পড়েছে! এখানে ল্যাম্বডা ও ক্র্যাশ ফিক্স করা হলো!
		urlInput.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event) {
				if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
				actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
				actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
				(event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
					
					try {
						String u = urlInput.getText().toString().trim();
						if (!u.isEmpty()) {
							
							
							if (!checkAndLoadSource(myBrowser, u)) {
								
								if (!u.startsWith("http://") && !u.startsWith("https://")) {
									u = "https://www.google.com/search?q=" + u;
								}
								myBrowser.loadUrl(u);
							}
						
							android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
							if (imm != null) {
								imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
							}
						}
					} catch (Exception e) {
						showStatus("Search Error: " + e.getMessage());
					}
					return true;
				}
				return false;
			}
		});
		
		android.webkit.WebSettings s = myBrowser.getSettings();
		s.setJavaScriptEnabled(true);
		s.setDomStorageEnabled(true);
		s.setDatabaseEnabled(true);
		s.setSupportMultipleWindows(true);
		s.setJavaScriptCanOpenWindowsAutomatically(true);
		s.setAllowFileAccess(true);
		s.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		s.setLoadWithOverviewMode(true);
		s.setUseWideViewPort(true);
		s.setUserAgentString(CHROME_UA);
		
		s.setSupportZoom(isZoomEnabled);
		s.setBuiltInZoomControls(isZoomEnabled);
		s.setDisplayZoomControls(false); 
		
		// 5n Block's DownloadManager will override it,
		myBrowser.setDownloadListener(new android.webkit.DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				try {
					android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url));
					request.setMimeType(mimetype);
					String cookies = android.webkit.CookieManager.getInstance().getCookie(url);
					request.addRequestHeader("cookie", cookies);
					request.setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype));
					request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype));
					((android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
					showStatus("Downloading...");
				} catch (Exception e) { showStatus("Download Error!"); }
			}
		});
		
		myBrowser.setWebChromeClient(new android.webkit.WebChromeClient() {
			@Override
			public void onProgressChanged(android.webkit.WebView view, int newProgress) {
				if (newProgress == 100) {
					webProgress.setVisibility(android.view.View.GONE);
				} else {
					webProgress.setVisibility(android.view.View.VISIBLE);
					webProgress.setProgress(newProgress);
				}
				super.onProgressChanged(view, newProgress);
			}
			
			@Override
			public boolean onCreateWindow(android.webkit.WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
				android.webkit.WebView.WebViewTransport transport = (android.webkit.WebView.WebViewTransport) resultMsg.obj;
				transport.setWebView(view); 
				resultMsg.sendToTarget();
				return true;
			}
			
			@Override
			public boolean onShowFileChooser(android.webkit.WebView w, android.webkit.ValueCallback<android.net.Uri[]> f, android.webkit.WebChromeClient.FileChooserParams p) {
				if (mUploadMessageArr != null) mUploadMessageArr.onReceiveValue(null);
				mUploadMessageArr = f;
				startActivityForResult(p.createIntent(), FILECHOOSER_RESULTCODE);
				return true;
			}
		});
		
		myBrowser.setWebViewClient(new android.webkit.WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
				return handleCustomUri(view, request.getUrl().toString());
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
				return handleCustomUri(view, url);
			}
			
			private boolean handleCustomUri(android.webkit.WebView view, String url) {
				if (url == null) return false;
				if (url.startsWith("http://") || url.startsWith("https://") || 
				url.startsWith("file://") || url.startsWith("javascript:") || url.startsWith("about:")) {
					return false; 
				}
				try {
					android.content.Context context = view.getContext();
					android.content.Intent intent;
					if (url.startsWith("intent://")) {
						intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME | android.content.Intent.URI_ANDROID_APP_SCHEME);
						intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
						try {
							context.startActivity(intent);
							return true;
						} catch (android.content.ActivityNotFoundException e) {
							String fallbackUrl = intent.getStringExtra("browser_fallback_url");
							if (fallbackUrl != null) {
								view.loadUrl(fallbackUrl);
								return true;
							}
							String packageName = intent.getPackage();
							if (packageName != null) {
								android.content.Intent marketIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + packageName));
								marketIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(marketIntent);
								return true;
							}
						}
					} else {
						intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
						intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
						try { context.startActivity(intent); } catch (Exception e) {}
						return true;
					}
				} catch (Exception e) { return true; }
				return true; 
			}
			
			@Override 
			public void onPageFinished(android.webkit.WebView v, String u) {
				urlInput.setText(u);
				swipe.setRefreshing(false);
				
				if(isAdBlockEnabled) {
					v.loadUrl("javascript:(function(){ document.querySelectorAll('.adsbygoogle, ins').forEach(e => e.remove()); })();");
				}
				
				if(isZoomEnabled) {
					v.loadUrl("javascript:(function(){ " +
					"var metas = document.getElementsByTagName('meta'); " +
					"var found = false; " +
					"for (var i=0; i<metas.length; i++) { " +
					"  if (metas[i].getAttribute('name') === 'viewport') { " +
					"    metas[i].setAttribute('content', 'width=device-width, initial-scale=1.0, user-scalable=yes, minimum-scale=0.2, maximum-scale=10.0'); " +
					"    found = true; " +
					"  } " +
					"} " +
					"if(!found) { " +
					"  var newMeta = document.createElement('meta'); " +
					"  newMeta.name = 'viewport'; " +
					"  newMeta.content = 'width=device-width, initial-scale=1.0, user-scalable=yes, minimum-scale=0.2, maximum-scale=10.0'; " +
					"  document.head.appendChild(newMeta); " +
					"} " +
					"})();");
				}
			}
		});
		
		swipe.setOnRefreshListener(new androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() { myBrowser.reload(); }
		});
		
		btnMenu.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				final android.app.Dialog menuDlg = new android.app.Dialog(MainActivity.this);
				android.widget.LinearLayout menuRoot = new android.widget.LinearLayout(MainActivity.this);
				menuRoot.setOrientation(android.widget.LinearLayout.VERTICAL);
				menuRoot.setPadding(30, 30, 30, 30);
				
				android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
				bg.setCornerRadius(35f);
				bg.setColor(0xFFFFFFFF);
				menuRoot.setBackground(bg);
				
				androidx.cardview.widget.CardView m1 = createCardButton("WORK WITH BOT", 0xFFFFFFFF, 0xFF2196F3, false, false);
				menuRoot.addView(m1);
				
				if (isBotRunning) {
					final androidx.cardview.widget.CardView stopBtn = createStopButton();
					menuRoot.addView(stopBtn);
					stopBtn.setOnClickListener(new android.view.View.OnClickListener() {
						@Override
						public void onClick(android.view.View v1) {
							myBrowser.reload();
							isBotRunning = false;
							showStatus("Bot Stopped");
							menuDlg.dismiss();
						}
					});
				}
				
				androidx.cardview.widget.CardView m2 = createCardButton("BLOCK ADS", 0xFFFFFFFF, 0xFF333333, isAdBlockEnabled, true);
				androidx.cardview.widget.CardView m3 = createCardButton("REFRESH MODE", 0xFFFFFFFF, 0xFF333333, isRefreshModeEnabled, true);
				androidx.cardview.widget.CardView mZoom = createCardButton("ZOOM MODE", 0xFFFFFFFF, 0xFF333333, isZoomEnabled, true);
				androidx.cardview.widget.CardView m4 = createCardButton("DESKTOP MODE", 0xFFFFFFFF, 0xFF333333, isDesktopModeEnabled, true);
				
				menuRoot.addView(m2); 
				menuRoot.addView(m3); 
				menuRoot.addView(mZoom); 
				menuRoot.addView(m4);
				menuDlg.setContentView(menuRoot);
				
				if (menuDlg.getWindow() != null) {
					menuDlg.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
				}
				
				m1.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v1) { menuDlg.dismiss(); showBotEditor(myBrowser); }
				});
				m2.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v1) { isAdBlockEnabled = !isAdBlockEnabled; myBrowser.reload(); menuDlg.dismiss(); }
				});
				m3.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v1) { isRefreshModeEnabled = !isRefreshModeEnabled; swipe.setEnabled(isRefreshModeEnabled); menuDlg.dismiss(); }
				});
				mZoom.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v1) {
						isZoomEnabled = !isZoomEnabled;
						myBrowser.getSettings().setSupportZoom(isZoomEnabled);
						myBrowser.getSettings().setBuiltInZoomControls(isZoomEnabled);
						myBrowser.getSettings().setDisplayZoomControls(false);
						myBrowser.reload(); 
						menuDlg.dismiss();
					}
				});
				m4.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v1) {
						isDesktopModeEnabled = !isDesktopModeEnabled;
						myBrowser.getSettings().setUserAgentString(isDesktopModeEnabled ? "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36" : CHROME_UA);
						myBrowser.getSettings().setUseWideViewPort(isDesktopModeEnabled);
						myBrowser.reload(); menuDlg.dismiss();
					}
				});
				menuDlg.show();
			}
		});
		
		myBrowser.loadUrl("https://www.google.com");
	}
	
	private void launch_fix() {} 
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() { startMyBrowser(); }
		}, 150);
	}
	{
	} 
	
	private boolean isNetworkAvailable() {
		try {
			android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
			android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
			return netInfo != null && netInfo.isConnected();
		} catch (Exception e) { return true; } 
	}
	
	// Offline Layout
	private void showOfflineLayout() {
		android.widget.LinearLayout root = new android.widget.LinearLayout(this);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setGravity(android.view.Gravity.CENTER);
		root.setBackgroundColor(0xFFFFFFFF);
		
		android.widget.ImageView icon = new android.widget.ImageView(this);
		icon.setImageResource(android.R.drawable.ic_dialog_alert);
		icon.setLayoutParams(new android.widget.LinearLayout.LayoutParams(250, 250));
		
		android.widget.TextView msg = new android.widget.TextView(this);
		msg.setText("No internet connection\nCheck your connection and try again");
		msg.setGravity(android.view.Gravity.CENTER);
		msg.setPadding(0, 50, 0, 50);
		msg.setTextColor(0xFF333333);
		
		android.widget.Button retry = new android.widget.Button(this);
		retry.setText("Retry");
		retry.setOnClickListener(v -> {
			if (isNetworkAvailable()) {
				recreate();
			} else {
				android.widget.Toast.makeText(this, "You're Still Offline!", 0).show();
			}
		});
		
		root.addView(icon);
		root.addView(msg);
		root.addView(retry);
		setContentView(root);
	}
	
	
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			try {
				
				if (findViewById(R.id.url_input) == null || findViewById(R.id.browser_webview) == null) {
				
					return; 
				}
				
		
				if (!isNetworkAvailable()) {
					showOfflineLayout();
				}
				
			} catch (Exception e) {
				
			}
		}, 300); 
	}
	
	private void crash_fixer_final() {
	} 
	
	private boolean isSingleSearchAction = false;
	private String lastInjectedUrl = ""; // 🔁 *Loop Handler *
	
	public class BotInterface {
		@android.webkit.JavascriptInterface
		public void search(final String newUrl) {
			runOnUiThread(() -> {
				android.widget.EditText urlInput = findViewById(R.id.url_input);
				android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
				if (urlInput != null && myBrowser != null) {
					urlInput.setText(newUrl);
					
					// 📌 if   'setStatus' not found, return Single Search
					
					String currentCode = getSharedPreferences("BotPrefs", 0).getString("saved_bot_script", "");
					if (!currentCode.contains("Bot.setStatus")) {
						isSingleSearchAction = true; 
					}
					
					String finalUrl = newUrl.trim();
					if (!finalUrl.startsWith("http")) finalUrl = "https://www.google.com/search?q=" + finalUrl;
					myBrowser.loadUrl(finalUrl);
				}
			});
		}
		
		@android.webkit.JavascriptInterface
		public void setStatus(String status) {
			isSingleSearchAction = false; // immortal Bot Flag Off.
			getSharedPreferences("BotSystem", 0).edit().putString("step_status", status).apply();
		}
		
		@android.webkit.JavascriptInterface
		public String getStatus() {
			return getSharedPreferences("BotSystem", 0).getString("step_status", "idle");
		}
	}
	
	private void startMasterBot(final android.webkit.WebView webview) {
		//JavaScript injection 🔥
		webview.addJavascriptInterface(new BotInterface(), "Bot");
		
		final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				
				if (isBotRunning && webview.getProgress() == 100) {
					String currentUrl = webview.getUrl();
					
					// 🔁 Reinjection 
					if (currentUrl != null && !currentUrl.equals(lastInjectedUrl)) {
						lastInjectedUrl = currentUrl;
						
						android.widget.EditText urlInput = findViewById(R.id.url_input);
						if (urlInput != null) urlInput.setText(currentUrl);
						
						String savedCode = getSharedPreferences("BotPrefs", 0).getString("saved_bot_script", "");
						
						if (!savedCode.isEmpty()) {
							// only Bot.search("...") 
							if (isSingleSearchAction) {
								getSharedPreferences("BotPrefs", 0).edit().putString("saved_bot_script", "").apply();
								isSingleSearchAction = false; 
				
							} else {
								// 1.5s Delay for immortal Bot 
								webview.postDelayed(() -> {
									if (isBotRunning) {
										webview.evaluateJavascript("(function(){ try{ " + savedCode + " }catch(e){console.error(e);} })();", null);
									}
								}, 1500);
							}
						}
					}
				} else if (!isBotRunning) {
					lastInjectedUrl = ""; 
				}
				
				
				handler.postDelayed(this, 1000);
			}
		});
	}
	
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
			if (myBrowser != null) startMasterBot(myBrowser);
		}, 1000);
	}
	
	private void bot_logic_v4_no_client() {
	} 
	
	// 🎯 Blob & Data URL Handler 
	private void saveBase64ToFile(String base64Data, String mimeType, String fileName) {
		runOnUiThread(() -> {
			try {
			
				String currentName = fileName; 
				
				String pureBase64 = base64Data;
				if (base64Data.contains(",")) {
					pureBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
				}
				
				byte[] decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
				
				java.io.File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
				java.io.File file = new java.io.File(path, currentName);
				
				
				int count = 1;
				while(file.exists()) {
					int dotIndex = fileName.lastIndexOf(".");
					String namePart = dotIndex != -1 ? fileName.substring(0, dotIndex) : fileName;
					String extPart = dotIndex != -1 ? fileName.substring(dotIndex) : "";
					currentName = namePart + "_" + count + extPart; 
					file = new java.io.File(path, currentName);
					count++;
				}
				
				java.io.FileOutputStream os = new java.io.FileOutputStream(file);
				os.write(decodedBytes);
				os.close();
				
				android.app.DownloadManager dm = (android.app.DownloadManager) getSystemService(android.content.Context.DOWNLOAD_SERVICE);
				if (dm != null) {
					dm.addCompletedDownload(currentName, "Axiom Local Download", true, mimeType, file.getAbsolutePath(), file.length(), true);
				}
				android.widget.Toast.makeText(getApplicationContext(), "Download Complete: " + currentName, android.widget.Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				android.widget.Toast.makeText(getApplicationContext(), "Local Download Failed!", android.widget.Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private void setupDownloadSystem(final android.webkit.WebView webview) {
		
		webview.addJavascriptInterface(new Object() {
			@android.webkit.JavascriptInterface
			public void saveBase64(String base64Data, String mimeType, String fileName) {
				saveBase64ToFile(base64Data, mimeType, fileName);
			}
		}, "AxiomBlobDownloader");
		
		webview.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
			
			String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
			String finalMimeType = mimetype != null ? mimetype : "application/octet-stream"; 
			String lowerUrl = url.toLowerCase();
			
			if (lowerUrl.contains(".apk") || (mimetype != null && mimetype.contains("package-archive"))) {
				finalMimeType = "application/vnd.android.package-archive";
				if (fileName.endsWith(".bin")) fileName = fileName.replace(".bin", ".apk");
				if (!fileName.endsWith(".apk")) fileName += ".apk";
				
			} else if (lowerUrl.contains(".mp3") || lowerUrl.contains(".wav") || (mimetype != null && mimetype.contains("audio"))) {
				finalMimeType = "audio/*";
				if (fileName.endsWith(".bin")) fileName = fileName.replace(".bin", ".mp3");
				if (!fileName.endsWith(".mp3") && !fileName.endsWith(".wav")) fileName += ".mp3";
				
			} else if (lowerUrl.contains(".mp4") || lowerUrl.contains(".mkv") || (mimetype != null && mimetype.contains("video"))) {
				finalMimeType = "video/*";
				if (fileName.endsWith(".bin")) fileName = fileName.replace(".bin", ".mp4");
				if (!fileName.endsWith(".mp4") && !fileName.endsWith(".mkv")) fileName += ".mp4";
				
			} else if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || lowerUrl.contains(".png") || (mimetype != null && mimetype.contains("image"))) {
				finalMimeType = "image/*";
				if (fileName.endsWith(".bin")) fileName = fileName.replace(".bin", ".jpg");
				if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png") && !fileName.endsWith(".jpeg")) fileName += ".jpg";
			}
			
			if (url.startsWith("blob:")) {
				android.widget.Toast.makeText(getApplicationContext(), "Processing Local File...", android.widget.Toast.LENGTH_SHORT).show();
				String js = "(function() { " +
				"var xhr = new XMLHttpRequest();" +
				"xhr.open('GET', '"+url+"', true);" +
				"xhr.responseType = 'blob';" +
				"xhr.onload = function() {" +
				"   if (this.status == 200) {" +
				"       var reader = new FileReader();" +
				"       reader.readAsDataURL(this.response);" +
				"       reader.onloadend = function() {" +
				"           AxiomBlobDownloader.saveBase64(reader.result, '"+finalMimeType+"', '"+fileName+"');" +
				"       }" +
				"   }" +
				"};" +
				"xhr.send();" +
				"})();";
				webview.evaluateJavascript(js, null);
				return;
			} 
			else if (url.startsWith("data:")) {
				android.widget.Toast.makeText(getApplicationContext(), "Processing Data File...", android.widget.Toast.LENGTH_SHORT).show();
				saveBase64ToFile(url, finalMimeType, fileName);
				return;
			}
			
			try {
				android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url));
				request.setMimeType(finalMimeType); 
				request.allowScanningByMediaScanner();
				request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
				request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
				request.setTitle(fileName);
				request.setDescription("Axiom is downloading...");
				
				android.app.DownloadManager dm = (android.app.DownloadManager) getSystemService(android.content.Context.DOWNLOAD_SERVICE);
				if (dm != null) {
					dm.enqueue(request);
					android.widget.Toast.makeText(getApplicationContext(), "Downloading: " + fileName, android.widget.Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				android.widget.Toast.makeText(getApplicationContext(), "Error: Link is broken!", android.widget.Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	{
		runOnUiThread(() -> {
			new android.os.Handler().postDelayed(() -> {
				android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
				if (myBrowser != null) setupDownloadSystem(myBrowser);
			}, 1500);
		});
	}
	
	private void universal_download_fix() {
	} 
	
	private void activateUltraAdBlock(final android.webkit.WebView webview) {
		// Pop-up Blocker
		webview.getSettings().setSupportMultipleWindows(false);
		webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		
		final android.os.Handler blockerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
		blockerHandler.post(new Runnable() {
			@Override
			public void run() {
				if (isAdBlockEnabled) {
					
					String adDestroyer = "(function() { " +
					"/* Hardcore Ad Blocker*/ " +
					"var ads = document.querySelectorAll('.adsbygoogle, ins, .ad-showing, .ad-container, #player-ads, [id^=\"google_ads\"], .premium-ad'); " +
					"for(var i=0; i<ads.length; i++) { ads[i].style.display='none'; ads[i].remove(); } " +
					
					"/*Pop-up Killer (New Tab opening)*/ " +
					"window.open = function() { return null; }; " +
					"var links = document.getElementsByTagName('a'); " +
					"for(var j=0; j<links.length; j++) { if(links[j].target === '_blank') links[j].target = '_self'; } " +
					
					"/* Youtube Ad skiper*/ " +
					"var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern'); " +
					"if(skip) skip.click(); " +
					"var v = document.querySelector('video'); " +
					"if(v && document.querySelector('.ad-showing')) { v.currentTime = v.duration; } " +
					"})();";
					
					webview.evaluateJavascript(adDestroyer, null);
				}
			
				blockerHandler.postDelayed(this, 1500);
			}
		});
	}
	
	{
		
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			android.webkit.WebView myMainWeb = findViewById(R.id.browser_webview);
			if (myMainWeb != null) {
				activateUltraAdBlock(myMainWeb);
			}
		}, 1000);
	}
	
	private void block_6_final_power() {
	} 
	
	private void injectNativeBotEngine(final android.webkit.WebView webview) {
				
		webview.addJavascriptInterface(new Object() {
			
			@android.webkit.JavascriptInterface
			public void click(final float x, final float y) {
				webview.post(() -> {
				
					float density = webview.getResources().getDisplayMetrics().density;
					float realX = x * density;
					float realY = y * density;
					
					// *Human like native Tuch"*
					long time = android.os.SystemClock.uptimeMillis();
					android.view.MotionEvent down = android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, realX, realY, 0);
					android.view.MotionEvent up = android.view.MotionEvent.obtain(time, time + 50, android.view.MotionEvent.ACTION_UP, realX, realY, 0);
					
					webview.dispatchTouchEvent(down);
					webview.dispatchTouchEvent(up);
					
					down.recycle();
					up.recycle();
				});
			}
			
			// *2: Native Scroll/Swipe*
			@android.webkit.JavascriptInterface
			public void swipe(final float startX, final float startY, final float endX, final float endY) {
				webview.post(() -> {
					float density = webview.getResources().getDisplayMetrics().density;
					float rx1 = startX * density;
					float ry1 = startY * density;
					float rx2 = endX * density;
					float ry2 = endY * density;
					
					long time = android.os.SystemClock.uptimeMillis();
					android.view.MotionEvent down = android.view.MotionEvent.obtain(time, time, android.view.MotionEvent.ACTION_DOWN, rx1, ry1, 0);
					webview.dispatchTouchEvent(down);
					
					android.view.MotionEvent move = android.view.MotionEvent.obtain(time, time + 100, android.view.MotionEvent.ACTION_MOVE, rx2, ry2, 0);
					webview.dispatchTouchEvent(move);
					
					android.view.MotionEvent up = android.view.MotionEvent.obtain(time, time + 200, android.view.MotionEvent.ACTION_UP, rx2, ry2, 0);
					webview.dispatchTouchEvent(up);
					
					down.recycle();
					move.recycle();
					up.recycle();
				});
			}
			
			//Dulta Scroll 
			@android.webkit.JavascriptInterface
			public void scrollBy(final float distanceX, final float distanceY) {
				webview.post(() -> {
					float density = webview.getResources().getDisplayMetrics().density;
					int scrollX = (int) (distanceX * density);
					int scrollY = (int) (distanceY * density);
					
					webview.scrollBy(scrollX, scrollY);
				});
			}
			
		}, "AppBot"); // User will call JavaScript with *AppBot*
		
	}
	
	{
	
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			android.webkit.WebView botWeb = findViewById(R.id.browser_webview);
			if (botWeb != null) {
				injectNativeBotEngine(botWeb);
			}
		}, 1000);
	}
	
	private void native_bot_active() {
	} 
	
	private void setupDefaultBrowserAndHistory() {
		
		final android.view.ViewGroup rootView = findViewById(android.R.id.content);
		final android.view.View splashScreen = new android.view.View(this);
		splashScreen.setBackgroundColor(android.graphics.Color.WHITE);
		splashScreen.setLayoutParams(new android.view.ViewGroup.LayoutParams(-1, -1));
		
		if (rootView != null) {
			rootView.addView(splashScreen);
		}
		
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			try {
				final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
				final android.widget.EditText urlInput = findViewById(R.id.url_input);
				
				if (myBrowser != null && urlInput != null) {
					// Saved Url Check
					android.content.Intent intent = getIntent();
					android.net.Uri intentData = intent.getData();
					android.content.SharedPreferences pref = getSharedPreferences("BrowserHistory", 0);
					String lastUrl = pref.getString("last_url", "");
					
					if (intentData != null) {
						// Default Browser Logic   
						String targetUrl = intentData.toString();
						urlInput.setText(targetUrl);
						myBrowser.loadUrl(targetUrl);
					} 
					else if (!lastUrl.isEmpty() && !lastUrl.equals("https://www.google.com/")) {
						// Load Saved Url 
						urlInput.setText(lastUrl);
						myBrowser.loadUrl(lastUrl);
					}
				}
				
			
				if (rootView != null && splashScreen.getParent() != null) {
					rootView.removeView(splashScreen);
				}
			
				final android.os.Handler historyHandler = new android.os.Handler(android.os.Looper.getMainLooper());
				historyHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (myBrowser != null && myBrowser.getUrl() != null) {
							String currentUrl = myBrowser.getUrl();
							
							if (!currentUrl.isEmpty() && !currentUrl.startsWith("https://www.google.com/search")) {
								getSharedPreferences("BrowserHistory", 0).edit()
								.putString("last_url", currentUrl).apply();
							}
						}
						
						historyHandler.postDelayed(this, 2000); 
					}
				}, 4000); 
				
			} catch (Exception e) {
				
				if (rootView != null && splashScreen.getParent() != null) {
					rootView.removeView(splashScreen);
				}
			}
		}, 2000);
	}
	
	{
		
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			setupDefaultBrowserAndHistory();
		}, 20);
	}
	
	private void default_browser_and_history_active() {
	} 
	
	private void fixRefreshLogicSmartly() {
		final androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipe_refresh);
		final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		
		if (swipe != null && myBrowser != null) {
			
			isRefreshModeEnabled = true; 
			
			final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
			refreshHandler.post(new Runnable() {
				@Override
				public void run() {
					if (myBrowser != null && swipe != null) {
						
						if (isRefreshModeEnabled) {
							swipe.setEnabled(myBrowser.getScrollY() == 0);
						} else {
							swipe.setEnabled(false);
							if (swipe.isRefreshing()) swipe.setRefreshing(false);
						}
						
						
						if (myBrowser.getProgress() >= 95 && swipe.isRefreshing()) {
							swipe.setRefreshing(false);
						}
					}
					
					refreshHandler.postDelayed(this, 300);
				}
			});
			
			// Refresh Action 
			swipe.setOnRefreshListener(() -> {
				if (isRefreshModeEnabled) {
					myBrowser.reload();
				} else {
					swipe.setRefreshing(false);
				}
			});
		}
	}
	
	{
		
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
			fixRefreshLogicSmartly();
		}, 2000);
	}
	
	private void smart_refresh_watcher_active() {
	} 
	
	@Override
	public void onBackPressed() {
		android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		if (myBrowser != null && myBrowser.canGoBack()) {
			myBrowser.goBack();
		} else {
			super.onBackPressed(); 
		}
	}
	
	// 🔁 Edge-Swipe
	private void setupEdgeSwipe() {
		final androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipe_refresh);
		final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		
		if (swipe == null || myBrowser == null) return;
		
		android.view.ViewGroup parentLayout = (android.view.ViewGroup) swipe.getParent();
		if (parentLayout == null) return;
		
		int swipeIndex = parentLayout.indexOfChild(swipe);
		parentLayout.removeView(swipe);
		
		android.graphics.Bitmap bmpLeft = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888);
		android.graphics.Canvas canvasLeft = new android.graphics.Canvas(bmpLeft);
		android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(android.graphics.Paint.Style.STROKE);
		paint.setStrokeWidth(10f);
		paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
		paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
		paint.setColor(0xFFFFFFFF); 
		
		android.graphics.Path pathLeft = new android.graphics.Path();
		pathLeft.moveTo(60, 20); 
		pathLeft.lineTo(30, 50); 
		pathLeft.lineTo(60, 80);
		canvasLeft.drawPath(pathLeft, paint);
		
		final android.widget.ImageView leftArrow = new android.widget.ImageView(this);
		leftArrow.setImageBitmap(bmpLeft);
		leftArrow.setColorFilter(0xFF757575, android.graphics.PorterDuff.Mode.SRC_IN); 
		
		final android.graphics.drawable.GradientDrawable bgLeft = new android.graphics.drawable.GradientDrawable();
		bgLeft.setShape(android.graphics.drawable.GradientDrawable.OVAL);
		bgLeft.setColor(0xFFEEEEEE);
		leftArrow.setBackground(bgLeft);
		leftArrow.setElevation(15f);
		leftArrow.setPadding(25, 25, 25, 25);
		
		android.widget.FrameLayout.LayoutParams lpLeft = new android.widget.FrameLayout.LayoutParams(120, 120);
		lpLeft.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
		leftArrow.setLayoutParams(lpLeft);
		leftArrow.setTranslationX(-150);
		
		//Forward Arrow
		android.graphics.Bitmap bmpRight = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888);
		android.graphics.Canvas canvasRight = new android.graphics.Canvas(bmpRight);
		
		android.graphics.Path pathRight = new android.graphics.Path();
		pathRight.moveTo(40, 20); 
		pathRight.lineTo(70, 50); 
		pathRight.lineTo(40, 80);
		canvasRight.drawPath(pathRight, paint);
		
		final android.widget.ImageView rightArrow = new android.widget.ImageView(this);
		rightArrow.setImageBitmap(bmpRight);
		rightArrow.setColorFilter(0xFF757575, android.graphics.PorterDuff.Mode.SRC_IN);
		
		final android.graphics.drawable.GradientDrawable bgRight = new android.graphics.drawable.GradientDrawable();
		bgRight.setShape(android.graphics.drawable.GradientDrawable.OVAL);
		bgRight.setColor(0xFFEEEEEE);
		rightArrow.setBackground(bgRight);
		rightArrow.setElevation(15f);
		rightArrow.setPadding(25, 25, 25, 25);
		
		android.widget.FrameLayout.LayoutParams lpRight = new android.widget.FrameLayout.LayoutParams(120, 120);
		lpRight.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;
		rightArrow.setLayoutParams(lpRight);
		rightArrow.setTranslationX(150);
				
		
		android.widget.FrameLayout gestureContainer = new android.widget.FrameLayout(this) {
			float startX = 0, startY = 0;
			boolean isEdgeSwiping = false;
			boolean isLeftEdge = false;
			final float THRESHOLD = 220f; 
			
			@Override
			public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
				switch(ev.getAction()) {
					case android.view.MotionEvent.ACTION_DOWN:
					startX = ev.getX();
					startY = ev.getY();
					
					if (startX < 80 && myBrowser.canGoBack()) {
						isEdgeSwiping = true;
						isLeftEdge = true;
					} else if (startX > getWidth() - 80 && myBrowser.canGoForward()) {
						isEdgeSwiping = true;
						isLeftEdge = false;
					} else {
						isEdgeSwiping = false;
					}
					break;
					case android.view.MotionEvent.ACTION_MOVE:
					if (isEdgeSwiping) {
						float dX = Math.abs(ev.getX() - startX);
						float dY = Math.abs(ev.getY() - startY);
						if (dX > 25 && dX > dY) return true;
					}
					break;
					case android.view.MotionEvent.ACTION_UP:
					case android.view.MotionEvent.ACTION_CANCEL:
					isEdgeSwiping = false;
					break;
				}
				return super.onInterceptTouchEvent(ev);
			}
			
			@Override
			public boolean onTouchEvent(android.view.MotionEvent ev) {
				if (!isEdgeSwiping) return super.onTouchEvent(ev);
				float deltaX = ev.getX() - startX;
				float pullDist = Math.abs(deltaX);
				
				switch(ev.getAction()) {
					case android.view.MotionEvent.ACTION_MOVE:
					if (isLeftEdge && deltaX > 0) {
						leftArrow.setTranslationX(-150 + Math.min(pullDist, 300));
						if (pullDist > THRESHOLD) {
							bgLeft.setColor(0xFF2196F3); 
							leftArrow.setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
						} else {
							bgLeft.setColor(0xFFEEEEEE); 
							leftArrow.setColorFilter(0xFF757575, android.graphics.PorterDuff.Mode.SRC_IN); 
						}
					} 
					else if (!isLeftEdge && deltaX < 0) {
						rightArrow.setTranslationX(150 - Math.min(pullDist, 300));
						if (pullDist > THRESHOLD) {
							bgRight.setColor(0xFF2196F3); 
							rightArrow.setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN); 
						} else {
							bgRight.setColor(0xFFEEEEEE); 
							rightArrow.setColorFilter(0xFF757575, android.graphics.PorterDuff.Mode.SRC_IN); 
						}
					}
					break;
					case android.view.MotionEvent.ACTION_UP:
					if (pullDist > THRESHOLD) {
						if (isLeftEdge) myBrowser.goBack();
						else myBrowser.goForward();
					}
					
					leftArrow.animate().translationX(-150).setDuration(200).start();
					rightArrow.animate().translationX(150).setDuration(200).start();
					isEdgeSwiping = false;
					break;
					case android.view.MotionEvent.ACTION_CANCEL:
					leftArrow.animate().translationX(-150).setDuration(200).start();
					rightArrow.animate().translationX(150).setDuration(200).start();
					isEdgeSwiping = false;
					break;
				}
				return true;
			}
		};
		
		gestureContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -1));
		gestureContainer.addView(swipe, new android.widget.FrameLayout.LayoutParams(-1, -1));
		gestureContainer.addView(leftArrow);
		gestureContainer.addView(rightArrow);
		parentLayout.addView(gestureContainer, swipeIndex);
	}
	
	
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				setupEdgeSwipe();
			}
		}, 300);
	}
	
	
	private void dummy() {
	} 
	
	// Long Press invent listener
	private void setupLongPressDialog() {
		final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		if (myBrowser == null) return;
		
		myBrowser.setOnLongClickListener(new android.view.View.OnLongClickListener() {
			@Override
			public boolean onLongClick(android.view.View v) {
				android.webkit.WebView.HitTestResult result = myBrowser.getHitTestResult();
				if (result != null) {
					int type = result.getType();
					
					if (type == android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE || 
					type == android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
						
						String url = result.getExtra();
						if (url != null && !url.isEmpty()) {
							showLinkOptionsDialog(url, myBrowser);
							return true; 
						}
					}
				}
				return false;
			}
		});
	}
	
	private void showLinkOptionsDialog(final String url, final android.webkit.WebView webView) {
		final android.app.Dialog dialog = new android.app.Dialog(this);
		
		android.widget.LinearLayout root = new android.widget.LinearLayout(this);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setPadding(50, 50, 50, 50);
		
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setCornerRadius(35f); 
		bg.setColor(0xFFFFFFFF);
		root.setBackground(bg);
		
		android.widget.TextView urlText = new android.widget.TextView(this);
		urlText.setText(url);
		urlText.setTextColor(0xFF1976D2); 
		urlText.setTextSize(15);
		urlText.setPadding(0, 0, 0, 40);
		urlText.setSingleLine(false); 
		urlText.setMaxLines(4);
		urlText.setEllipsize(android.text.TextUtils.TruncateAt.END);
		root.addView(urlText);
		
		android.widget.TextView btnCopy = createDialogButton("Copy Link");
		btnCopy.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Link", url);
				if (clipboard != null) clipboard.setPrimaryClip(clip);
				android.widget.Toast.makeText(MainActivity.this, "Link Copied!", android.widget.Toast.LENGTH_SHORT).show();
				dialog.dismiss();
			}
		});
		root.addView(btnCopy);
		
		// 3. Open URL Button 
		android.widget.TextView btnOpen = createDialogButton("Open URL");
		btnOpen.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				webView.loadUrl(url);
				dialog.dismiss();
			}
		});
		root.addView(btnOpen);
		
		// 4. Share URL Button 
		android.widget.TextView btnShare = createDialogButton("Share URL");
		btnShare.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
				startActivity(android.content.Intent.createChooser(shareIntent, "Share Link"));
				dialog.dismiss();
			}
		});
		root.addView(btnShare);
		
		dialog.setContentView(root);
	
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
			int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85); 
			dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		
		dialog.show();
	}
	
	private android.widget.TextView createDialogButton(String text) {
		android.widget.TextView btn = new android.widget.TextView(this);
		btn.setText(text);
		btn.setTextColor(0xFF333333);
		btn.setTextSize(17);
		btn.setTypeface(null, android.graphics.Typeface.BOLD);
		btn.setPadding(20, 25, 20, 25);
		btn.setBackgroundResource(android.R.drawable.list_selector_background); 
		return btn;
	}
	
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				setupLongPressDialog();
			}
		}, 500);
	}
	
	private void dummy12() {
	} 
	
	// 🎯 *view-source:* Handler
	private void handleViewSource(final android.webkit.WebView webview, String sourceUrl) {
		try {
			String cleanUrl = sourceUrl.substring(12).trim();
			
			if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
				cleanUrl = "https://" + cleanUrl;
			}
			
			final String targetUrl = cleanUrl;
			android.widget.Toast.makeText(getApplicationContext(), "Fetching Source Code...", android.widget.Toast.LENGTH_SHORT).show();
			
			new Thread(() -> {
				try {
					java.net.URL url = new java.net.URL(targetUrl);
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(10000);
					conn.setReadTimeout(10000);
					conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
					
					java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
					}
					reader.close();
					
					final String htmlSource = sb.toString();
					
					final String formattedHtml = "<html><head><style>" +
					"body { font-family: 'Courier New', monospace; font-size: 13px; background-color: #f5f5f5; color: #222222; padding: 15px; " +
					"white-space: pre-wrap !important; word-wrap: break-word !important; word-break: break-all !important; " +
					"line-height: 1.5; }" +
					"</style></head><body>" +
					htmlSource.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") +
					"</body></html>";
					
					runOnUiThread(() -> {
						if (webview != null) {
							webview.loadDataWithBaseURL(null, formattedHtml, "text/html", "UTF-8", null);
						}
					});
					
				} catch (Exception e) {
					runOnUiThread(() -> {
						android.widget.Toast.makeText(getApplicationContext(), "Error: Invalid Link or No Internet!", android.widget.Toast.LENGTH_LONG).show();
					});
				}
			}).start();
			
		} catch (Exception e) {
			android.widget.Toast.makeText(getApplicationContext(), "Something went wrong!", android.widget.Toast.LENGTH_SHORT).show();
		}
	}

	public boolean checkAndLoadSource(final android.webkit.WebView webview, String input) {
		try {
			if (input != null) {
				String trimmedInput = input.trim();
				if (trimmedInput.toLowerCase().startsWith("view-source:")) {
					if (trimmedInput.length() <= 12) {
						
						android.widget.Toast.makeText(getApplicationContext(), "Please provide a link!", android.widget.Toast.LENGTH_SHORT).show();
						return true;
					}
					handleViewSource(webview, trimmedInput);
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false; 
	}
	
	private void view_source_final_fix() {
	} 
	
	private boolean isBotDialogShowing = false;
	
	// 🎯 13. Code.Bot() Method (Anti-Loop & Anti-Duplicate Fix)
	private void setupBotAPI() {
		final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		if (myBrowser == null) return;
		
		//JavaScript interface 
		myBrowser.addJavascriptInterface(new Object() {
			
			@android.webkit.JavascriptInterface
			public void Bot() {
				new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						
						if (isBotDialogShowing) return;
						
						String currentUrl = myBrowser.getUrl();
						if (currentUrl != null && !currentUrl.isEmpty()) {
							isBotDialogShowing = true;
				
							isBotRunning = false; 
							getSharedPreferences("BotPrefs", 0).edit().putString("saved_bot_script", "").apply();
							
							showBotSourceDialog(myBrowser, "view-source:" + currentUrl);
						} else {
							android.widget.Toast.makeText(myBrowser.getContext(), "Error: No URL found to view source!", 0).show();
						}
					}
				});
			}
			
		
			@android.webkit.JavascriptInterface
			public void Bot(final String targetUrl) {
				new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						if (isBotDialogShowing) return;
						
						if (targetUrl != null && !targetUrl.trim().isEmpty()) {
							isBotDialogShowing = true;
							
					
							isBotRunning = false; 
							getSharedPreferences("BotPrefs", 0).edit().putString("saved_bot_script", "").apply();
							
							showBotSourceDialog(myBrowser, "view-source:" + targetUrl.trim());
						} else {
							android.widget.Toast.makeText(myBrowser.getContext(), "Error: Invalid link!", 0).show();
						}
					}
				});
			}
		}, "Code");
	}
	
	private void showBotSourceDialog(final android.webkit.WebView webView, final String sourceUrl) {
		android.content.Context context = webView.getContext();
		final android.app.Dialog dialog = new android.app.Dialog(context);
		
		
		android.widget.LinearLayout root = new android.widget.LinearLayout(context);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setPadding(60, 70, 60, 70);
		root.setGravity(android.view.Gravity.CENTER);
		
		
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setCornerRadius(40f);
		bg.setColor(0xFFFFFFFF);
		root.setBackground(bg);
		
		android.widget.TextView titleText = new android.widget.TextView(context);
		titleText.setText("View Page Source with Bot?");
		titleText.setTextColor(0xFF333333); 
		titleText.setTextSize(19);
		titleText.setTypeface(null, android.graphics.Typeface.BOLD);
		titleText.setGravity(android.view.Gravity.CENTER);
		titleText.setPadding(0, 0, 0, 60); 
		root.addView(titleText);
		
		android.widget.TextView btnView = new android.widget.TextView(context);
		btnView.setText("View");
		btnView.setTextColor(0xFFFFEBEE); 
		btnView.setTextSize(17);
		btnView.setTypeface(null, android.graphics.Typeface.BOLD);
		btnView.setGravity(android.view.Gravity.CENTER);
		btnView.setPadding(0, 35, 0, 35);
		
		android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
		btnBg.setCornerRadius(23f);
		btnBg.setColor(0xFFE57373); 
		btnView.setBackground(btnBg);
		
		android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
		android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
		android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
		);
		btnParams.setMargins(20, 0, 20, 0); 
		btnView.setLayoutParams(btnParams);
		
		btnView.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				checkAndLoadSource(webView, sourceUrl);
				dialog.dismiss(); 
			}
		});
		root.addView(btnView);
		
		dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(android.content.DialogInterface d) {
				isBotDialogShowing = false; 
			}
		});
		
		dialog.setContentView(root);
		
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
			int width = (int)(context.getResources().getDisplayMetrics().widthPixels * 0.85); 
			dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		
		dialog.show();
	}
	
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				setupBotAPI(); 
			}
		}, 500);
	}
	
	private void dummybotopen() {
	} 
	
	// 🎯 ১৩. URL Bar Auto Hide/Show on Scroll (Smooth & Anti-Jitter/Hang Fix)
	private void setupScrollHidingToolbar() {
		final android.webkit.WebView myBrowser = findViewById(R.id.browser_webview);
		final android.view.View toolbar = findViewById(R.id.linear3);
		
		if (myBrowser == null || toolbar == null) return;
		
		final android.animation.ValueAnimator[] currentAnim = {null};
		final boolean[] isHidden = {false};
		
		if (android.os.Build.VERSION.SDK_INT >= 23) {
			myBrowser.setOnScrollChangeListener(new android.view.View.OnScrollChangeListener() {
				@Override
				public void onScrollChange(android.view.View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
					
					int dy = scrollY - oldScrollY; 
					final int tHeight = toolbar.getHeight();
					
					if (tHeight == 0) return; 
				
					android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) toolbar.getLayoutParams();
					int currentMargin = params.topMargin;
					
					if (dy > 30 && scrollY > 100 && !isHidden[0]) {
						isHidden[0] = true; 
						
						if (currentAnim[0] != null) currentAnim[0].cancel(); 
						
						currentAnim[0] = android.animation.ValueAnimator.ofInt(currentMargin, -tHeight);
						currentAnim[0].setDuration(250);
						currentAnim[0].addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(android.animation.ValueAnimator animation) {
								params.topMargin = (int) animation.getAnimatedValue();
								toolbar.setLayoutParams(params);
							}
						});
						currentAnim[0].start();
					}
				
					else if (dy < -30 && isHidden[0]) {
						isHidden[0] = false;
						
						if (currentAnim[0] != null) currentAnim[0].cancel(); 
						
						currentAnim[0] = android.animation.ValueAnimator.ofInt(currentMargin, 0);
						currentAnim[0].setDuration(250);
						currentAnim[0].addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(android.animation.ValueAnimator animation) {
								params.topMargin = (int) animation.getAnimatedValue();
								toolbar.setLayoutParams(params);
							}
						});
						currentAnim[0].start();
					}
				}
			});
		}
	}
	
	{
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				setupScrollHidingToolbar(); 
			}
		}, 600);
	}
	
	private void dummy13() {
	}
	
}
