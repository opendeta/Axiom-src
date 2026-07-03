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
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    
    private MainBinding binding;
    private ValueCallback<Uri[]> mUploadMessageArr;
    
    private final int FILECHOOSER_RESULTCODE = 1001;
    private final String BOT_CODE_KEY = "saved_bot_script";
    private final String CHROME_UA = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Mobile Safari/537.36";
    
    private boolean isAdBlockEnabled = true;
    private boolean isRefreshModeEnabled = true;
    private boolean isZoomEnabled = false; 
    private boolean isDesktopModeEnabled = false;
    private boolean isBotRunning = false;
    private boolean isBotDialogShowing = false;
    private boolean isCleanerDialogShowing = false;
    private boolean isSingleSearchAction = false;
    private String lastInjectedUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeViews();
        setupDefaultBrowserAndHistory();
    }

    private void initializeViews() {
        WebView myBrowser = binding.browserWebview;
        EditText urlInput = findViewById(R.id.url_input);
        ImageView btnClear = findViewById(R.id.btn_clear_text);
        TextView btnMenu = findViewById(R.id.btn_menu);
        SwipeRefreshLayout swipe = findViewById(R.id.swipe_refresh);
        ProgressBar webProgress = findViewById(R.id.web_progress);

        // Progress Bar Setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webProgress.setProgressTintList(ColorStateList.valueOf(0xFFFF0000));
        } else {
            webProgress.getProgressDrawable().setColorFilter(0xFFFF0000, PorterDuff.Mode.SRC_IN);
        }

        btnClear.setOnClickListener(v -> urlInput.setText(""));

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || 
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String u = urlInput.getText().toString().trim();
                if (!u.isEmpty()) {
                    if (!checkAndLoadSource(myBrowser, u)) {
                        if (!u.startsWith("http://") && !u.startsWith("https://")) {
                            u = "https://www.google.com/search?q=" + u;
                        }
                        myBrowser.loadUrl(u);
                    }
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
                    }
                }
                return true;
            }
            return false;
        });

        // WebSettings Setup
        WebSettings s = myBrowser.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setUserAgentString(CHROME_UA);
        s.setSupportZoom(isZoomEnabled);
        s.setBuiltInZoomControls(isZoomEnabled);
        s.setDisplayZoomControls(false);

        myBrowser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    webProgress.setVisibility(View.GONE);
                } else {
                    webProgress.setVisibility(View.VISIBLE);
                    webProgress.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(view); 
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> f, FileChooserParams p) {
                if (mUploadMessageArr != null) mUploadMessageArr.onReceiveValue(null);
                mUploadMessageArr = f;
                startActivityForResult(p.createIntent(), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        myBrowser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleCustomUri(view, request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleCustomUri(view, url);
            }

            private boolean handleCustomUri(WebView view, String url) {
                if (url == null) return false;
                if (url.startsWith("http://") || url.startsWith("https://") ||
                    url.startsWith("file://") || url.startsWith("javascript:") || url.startsWith("about:")) {
                    return false; 
                }
                try {
                    Context context = view.getContext();
                    Intent intent;
                    if (url.startsWith("intent://")) {
                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME | Intent.URI_ANDROID_APP_SCHEME);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            context.startActivity(intent);
                            return true;
                        } catch (ActivityNotFoundException e) {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                view.loadUrl(fallbackUrl);
                                return true;
                            }
                            String packageName = intent.getPackage();
                            if (packageName != null) {
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(marketIntent);
                                return true;
                            }
                        }
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { context.startActivity(intent); } catch (Exception e) {}
                        return true;
                    }
                } catch (Exception e) { return true; }
                return true; 
            }

            @Override 
            public void onPageFinished(WebView v, String u) {
                urlInput.setText(u);
                swipe.setRefreshing(false);

                if (isAdBlockEnabled) {
                    v.loadUrl("javascript:(function(){ document.querySelectorAll('.adsbygoogle, ins').forEach(e => e.remove()); })();");
                }

                if (isZoomEnabled) {
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

        btnMenu.setOnClickListener(v -> showNavigationMenu(myBrowser, swipe));
        
        // Polling background tasks initialization
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setupDownloadSystem(myBrowser);
            activateUltraAdBlock(myBrowser);
            injectNativeBotEngine(myBrowser);
            startMasterBot(myBrowser);
            setupEdgeSwipe(swipe, myBrowser);
            setupLongPressDialog(myBrowser);
            setupBotAPI(myBrowser);
            setupScrollHidingToolbar(myBrowser);
            setupCleanerAPI(myBrowser);
            fixRefreshLogicSmartly(swipe, myBrowser);
        }, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mUploadMessageArr == null) return;
            mUploadMessageArr.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            mUploadMessageArr = null;
        }
    }

    private CardView createCardButton(String text, int color, int textColor, boolean isChecked, boolean showCheckBox) {
        CardView card = new CardView(this);
        card.setCardBackgroundColor(color);
        card.setRadius(25f);
        card.setCardElevation(12f);
        card.setUseCompatPadding(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(40, 30, 40, 30);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        tv.setTypeface(null, Typeface.BOLD);
        container.addView(tv);

        if (showCheckBox) {
            CheckBox cb = new CheckBox(this);
            cb.setChecked(isChecked);
            cb.setClickable(false);
            int[][] states = new int[][] { new int[] { android.R.attr.state_checked }, new int[] { -android.R.attr.state_checked } };
            int[] colors = new int[] { 0xFF2196F3, 0xFFBDBDBD };
            cb.setButtonTintList(new ColorStateList(states, colors));
            container.addView(cb);
        }

        card.addView(container);
        return card;
    }

    private CardView createStopButton() {
        CardView card = new CardView(this);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setRadius(80f);
        card.setCardElevation(15f);
        card.setUseCompatPadding(true);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(30, 15, 45, 15);
        container.setGravity(Gravity.CENTER);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        pb.setLayoutParams(new LinearLayout.LayoutParams(50, 50));

        TextView tv = new TextView(this);
        tv.setText("  Stop");
        tv.setTextSize(18);
        tv.setTextColor(0xFF000000);
        tv.setTypeface(null, Typeface.BOLD);

        container.addView(pb);
        container.addView(tv);
        card.addView(container);
        return card;
    }

    private void showStatus(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showBotEditor(final WebView webview) {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        final SharedPreferences pref = getSharedPreferences("BotPrefs", Context.MODE_PRIVATE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(50, 50, 50, 50);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(35f);
        bg.setColor(0xFFFFFFFF);
        root.setBackground(bg);

        TextView header = new TextView(this);
        header.setText("Code Editor");
        header.setTextSize(22);
        header.setTextColor(0xFF333333);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, 0, 0, 40);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        
        final EditText editor = new EditText(this);
        editor.setText(pref.getString(BOT_CODE_KEY, ""));
        editor.setGravity(Gravity.TOP);
        editor.setHint("Write JavaScript code here...");
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setBackgroundResource(android.R.drawable.editbox_background_normal);
        scrollView.addView(editor);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.RIGHT);
        btnLayout.setPadding(0, 30, 0, 0);

        CardView cardCancel = createCardButton("Cancel", 0xFFE3F2FD, 0xFF2196F3, false, false);
        CardView cardRun = createCardButton("Run", 0xFFFFEBEE, 0xFFE57373, false, false);
        btnLayout.addView(cardCancel);
        btnLayout.addView(cardRun);

        root.addView(header);
        root.addView(scrollView);
        root.addView(btnLayout);
        dialog.setView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        cardCancel.setOnClickListener(v -> dialog.dismiss());
        cardRun.setOnClickListener(v -> {
            String code = editor.getText().toString();
            pref.edit().putString(BOT_CODE_KEY, code).apply();
            webview.evaluateJavascript("(function(){ try{" + code + "}catch(e){alert(e.message);} })();", null);
            isBotRunning = true;
            showStatus("Bot Activated!");
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showNavigationMenu(WebView myBrowser, SwipeRefreshLayout swipe) {
        final Dialog menuDlg = new Dialog(MainActivity.this);
        LinearLayout menuRoot = new LinearLayout(MainActivity.this);
        menuRoot.setOrientation(LinearLayout.VERTICAL);
        menuRoot.setPadding(30, 30, 30, 30);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(35f);
        bg.setColor(0xFFFFFFFF);
        menuRoot.setBackground(bg);

        CardView m1 = createCardButton("WORK WITH BOT", 0xFFFFFFFF, 0xFF2196F3, false, false);
        menuRoot.addView(m1);

        if (isBotRunning) {
            CardView stopBtn = createStopButton();
            menuRoot.addView(stopBtn);
            stopBtn.setOnClickListener(v1 -> {
                myBrowser.reload();
                isBotRunning = false;
                showStatus("Bot Stopped");
                menuDlg.dismiss();
            });
        }

        CardView m2 = createCardButton("BLOCK ADS", 0xFFFFFFFF, 0xFF333333, isAdBlockEnabled, true);
        CardView m3 = createCardButton("REFRESH MODE", 0xFFFFFFFF, 0xFF333333, isRefreshModeEnabled, true);
        CardView mZoom = createCardButton("ZOOM MODE", 0xFFFFFFFF, 0xFF333333, isZoomEnabled, true);
        CardView m4 = createCardButton("DESKTOP MODE", 0xFFFFFFFF, 0xFF333333, isDesktopModeEnabled, true);

        menuRoot.addView(m2); 
        menuRoot.addView(m3); 
        menuRoot.addView(mZoom); 
        menuRoot.addView(m4);
        menuDlg.setContentView(menuRoot);

        if (menuDlg.getWindow() != null) {
            menuDlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        m1.setOnClickListener(v1 -> { menuDlg.dismiss(); showBotEditor(myBrowser); });
        m2.setOnClickListener(v1 -> { isAdBlockEnabled = !isAdBlockEnabled; myBrowser.reload(); menuDlg.dismiss(); });
        m3.setOnClickListener(v1 -> { isRefreshModeEnabled = !isRefreshModeEnabled; swipe.setEnabled(isRefreshModeEnabled); menuDlg.dismiss(); });
        mZoom.setOnClickListener(v1 -> {
            isZoomEnabled = !isZoomEnabled;
            myBrowser.getSettings().setSupportZoom(isZoomEnabled);
            myBrowser.getSettings().setBuiltInZoomControls(isZoomEnabled);
            myBrowser.getSettings().setDisplayZoomControls(false);
            myBrowser.reload(); 
            menuDlg.dismiss();
        });
        m4.setOnClickListener(v1 -> {
            isDesktopModeEnabled = !isDesktopModeEnabled;
            myBrowser.getSettings().setUserAgentString(isDesktopModeEnabled ? "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36" : CHROME_UA);
            myBrowser.getSettings().setUseWideViewPort(isDesktopModeEnabled);
            myBrowser.reload(); 
            menuDlg.dismiss();
        });
        menuDlg.show();
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        } catch (Exception e) { 
            return true; 
        }
    }

    private void showOfflineLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.WHITE);

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_dialog_alert);
        icon.setLayoutParams(new LinearLayout.LayoutParams(250, 250));

        TextView msg = new TextView(this);
        msg.setText("No internet connection\\nCheck your connection and try again");
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 50, 0, 50);
        msg.setTextColor(0xFF333333);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                recreate();
            } else {
                Toast.makeText(this, "You're Still Offline!", Toast.LENGTH_SHORT).show();
            }
        });

        root.addView(icon);
        root.addView(msg);
        root.addView(retry);
        setContentView(root);
    }

//  Bot.search(), Bot.setstatus(),… 

    public class BotInterface {
        @JavascriptInterface
        public void search(final String newUrl) {
            runOnUiThread(() -> {
                EditText urlInput = findViewById(R.id.url_input);
                WebView myBrowser = binding.browserWebview;
                if (urlInput != null && myBrowser != null) {
                    urlInput.setText(newUrl);

                    String currentCode = getSharedPreferences("BotPrefs", 0).getString(BOT_CODE_KEY, "");
                    if (!currentCode.contains("Bot.setStatus")) {
                        isSingleSearchAction = true; 
                    }

                    String finalUrl = newUrl.trim();
                    if (!finalUrl.startsWith("http")) finalUrl = "https://www.google.com/search?q=" + finalUrl;
                    myBrowser.loadUrl(finalUrl);
                }
            });
        }

        @JavascriptInterface
        public void setStatus(String status) {
            isSingleSearchAction = false; 
            getSharedPreferences("BotSystem", 0).edit().putString("step_status", status).apply();
        }

        @JavascriptInterface
        public String getStatus() {
            return getSharedPreferences("BotSystem", 0).getString("step_status", "idle");
        }
    }
  
    private void startMasterBot(final WebView webview) {
        webview.addJavascriptInterface(new BotInterface(), "Bot");

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isBotRunning && webview.getProgress() == 100) {
                    String currentUrl = webview.getUrl();

                    if (currentUrl != null && !currentUrl.equals(lastInjectedUrl)) {
                        lastInjectedUrl = currentUrl;

                        EditText urlInput = findViewById(R.id.url_input);
                        if (urlInput != null) urlInput.setText(currentUrl);

                        String savedCode = getSharedPreferences("BotPrefs", 0).getString(BOT_CODE_KEY, "");

                        if (!savedCode.isEmpty()) {
                            if (isSingleSearchAction) {
                                getSharedPreferences("BotPrefs", 0).edit().putString(BOT_CODE_KEY, "").apply();
                                isSingleSearchAction = false; 
                            } else {
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

// Local file downloader
    private void saveBase64ToFile(String base64Data, String mimeType, String fileName) {
        runOnUiThread(() -> {
            try {
                String currentName = fileName; 
                String pureBase64 = base64Data;
                if (base64Data.contains(",")) {
                    pureBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
                }

                byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, currentName);

                int count = 1;
                while (file.exists()) {
                    int dotIndex = fileName.lastIndexOf(".");
                    String namePart = dotIndex != -1 ? fileName.substring(0, dotIndex) : fileName;
                    String extPart = dotIndex != -1 ? fileName.substring(dotIndex) : "";
                    currentName = namePart + "_" + count + extPart; 
                    file = new File(path, currentName);
                    count++;
                }

                FileOutputStream os = new FileOutputStream(file);
                os.write(decodedBytes);
                os.close();

                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.addCompletedDownload(currentName, "Axiom Local Download", true, mimeType, file.getAbsolutePath(), file.length(), true);
                }
                Toast.makeText(getApplicationContext(), "Download Complete: " + currentName, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Local Download Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDownloadSystem(final WebView webview) {
        webview.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void saveBase64(String base64Data, String mimeType, String fileName) {
                saveBase64ToFile(base64Data, mimeType, fileName);
            }
        }, "AxiomBlobDownloader");

        webview.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
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
                Toast.makeText(getApplicationContext(), "Processing Local File...", Toast.LENGTH_SHORT).show();
                String js = "(function() { " +
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', '"+url+"', true);" +
                "xhr.responseType = 'blob';" +
                "xhr.onload = function() {" +
                " if (this.status == 200) { " +
                " var reader = new FileReader();" +
                " reader.readAsDataURL(this.response);" +
                " reader.onloadend = function() { " +
                " AxiomBlobDownloader.saveBase64(reader.result, '"+finalMimeType+"', '"+fileName+"');" +
                " }" +
                " }" +
                "};" +
                "xhr.send();" +
                "})();";
                webview.evaluateJavascript(js, null);
                return;
            } else if (url.startsWith("data:")) {
                Toast.makeText(getApplicationContext(), "Processing Data File...", Toast.LENGTH_SHORT).show();
                saveBase64ToFile(url, finalMimeType, fileName);
                return;
            }

            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(finalMimeType); 
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                request.setTitle(fileName);
                request.setDescription("Axiom is downloading...");

                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Download Error! Check Permission (Storage)", Toast.LENGTH_SHORT).show();
            }
        });
    }
// Ad Blocker 
    private void activateUltraAdBlock(final WebView webview) {
        Handler blockerHandler = new Handler(Looper.getMainLooper());
        blockerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdBlockEnabled) {
                    String adDestroyer = "(function() { " +
                    "var ads = document.querySelectorAll('.adsbygoogle, ins, .ad-showing, .ad-container, #player-ads, [id^=\"google_ads\"], .premium-ad'); " +
                    "for(var i=0; i<ads.length; i++) { ads[i].style.display='none'; ads[i].remove(); } " +
                    "window.open = function() { return null; }; " +
                    "var links = document.getElementsByTagName('a'); " +
                    "for(var j=0; j<links.length; j++) { if(links[j].target === '_blank') links[j].target = '_self'; } " +
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
   
// AppBot interface (Hardware level Click, Swipe/Scroll)
    private void injectNativeBotEngine(final WebView webview) {
        webview.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void click(final float x, final float y) {
                webview.post(() -> {
                    float density = webview.getResources().getDisplayMetrics().density;
                    float realX = x * density;
                    float realY = y * density;
                    long time = SystemClock.uptimeMillis();
                    MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, realX, realY, 0);
                    MotionEvent up = MotionEvent.obtain(time, time + 50, MotionEvent.ACTION_UP, realX, realY, 0);
                    webview.dispatchTouchEvent(down);
                    webview.dispatchTouchEvent(up);
                    down.recycle();
                    up.recycle();
                });
            }

            @JavascriptInterface
            public void swipe(final float startX, final float startY, final float endX, final float endY) {
                webview.post(() -> {
                    float density = webview.getResources().getDisplayMetrics().density;
                    float rx1 = startX * density;
                    float ry1 = startY * density;
                    float rx2 = endX * density;
                    float ry2 = endY * density;

                    long time = SystemClock.uptimeMillis();
                    MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, rx1, ry1, 0);
                    webview.dispatchTouchEvent(down);

                    MotionEvent move = MotionEvent.obtain(time, time + 100, MotionEvent.ACTION_MOVE, rx2, ry2, 0);
                    webview.dispatchTouchEvent(move);

                    MotionEvent up = MotionEvent.obtain(time, time + 200, MotionEvent.ACTION_UP, rx2, ry2, 0);
                    webview.dispatchTouchEvent(up);

                    down.recycle();
                    move.recycle();
                    up.recycle();
                });
            }
     
            @JavascriptInterface
            public void scrollBy(final float distanceX, final float distanceY) {
                webview.post(() -> {
                    float density = webview.getResources().getDisplayMetrics().density;
                    int scrollX = (int) (distanceX * density);
                    int scrollY = (int) (distanceY * density);
                    webview.scrollBy(scrollX, scrollY);
                });
            }
        }, "AppBot");
    }
    
// Default Browser Action 
    private void setupDefaultBrowserAndHistory() {
        final ViewGroup rootView = findViewById(android.R.id.content);
        final View splashScreen = new View(this);
        splashScreen.setBackgroundColor(Color.WHITE);
        splashScreen.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        if (rootView != null) {
            rootView.addView(splashScreen);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (findViewById(R.id.url_input) == null || binding.browserWebview == null) {
                    if (!isNetworkAvailable()) showOfflineLayout();
                    return; 
                }

                if (!isNetworkAvailable()) {
                    showOfflineLayout();
                    return;
                }

                final WebView myBrowser = binding.browserWebview;
                final EditText urlInput = findViewById(R.id.url_input);

                Intent intent = getIntent();
                Uri intentData = intent.getData();
                SharedPreferences pref = getSharedPreferences("BrowserHistory", 0);
                String lastUrl = pref.getString("last_url", "");

                if (intentData != null) {
                    String targetUrl = intentData.toString();
                    urlInput.setText(targetUrl);
                    myBrowser.loadUrl(targetUrl);
                } else if (!lastUrl.isEmpty() && !lastUrl.equals("https://www.google.com/")) {
                    urlInput.setText(lastUrl);
                    myBrowser.loadUrl(lastUrl);
                } else {
                    myBrowser.loadUrl("https://www.google.com");
                }

                if (rootView != null && splashScreen.getParent() != null) {
                    rootView.removeView(splashScreen);
                }

                final Handler historyHandler = new Handler(Looper.getMainLooper());
                historyHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (myBrowser != null && myBrowser.getUrl() != null) {
                            String currentUrl = myBrowser.getUrl();
                            if (!currentUrl.isEmpty() && !currentUrl.startsWith("https://www.google.com/search")) {
                                getSharedPreferences("BrowserHistory", 0).edit().putString("last_url", currentUrl).apply();
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
        }, 1000);
    }

    private void fixRefreshLogicSmartly(SwipeRefreshLayout swipe, WebView myBrowser) {
        if (swipe != null && myBrowser != null) {
            isRefreshModeEnabled = true; 
            final Handler refreshHandler = new Handler(Looper.getMainLooper());
            refreshHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isRefreshModeEnabled) {
                        swipe.setEnabled(myBrowser.getScrollY() == 0);
                    } else {
                        swipe.setEnabled(false);
                        if (swipe.isRefreshing()) swipe.setRefreshing(false);
                    }
                    if (myBrowser.getProgress() >= 95 && swipe.isRefreshing()) {
                        swipe.setRefreshing(false);
                    }
                    refreshHandler.postDelayed(this, 300);
                }
            });

            swipe.setOnRefreshListener(() -> {
                if (isRefreshModeEnabled) {
                    myBrowser.reload();
                } else {
                    swipe.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        WebView myBrowser = binding.browserWebview;
        if (myBrowser != null && myBrowser.canGoBack()) {
            myBrowser.goBack();
        } else {
            super.onBackPressed();
        }
    }
// Edge-swipe 
    private void setupEdgeSwipe(SwipeRefreshLayout swipe, WebView myBrowser) {
        if (swipe == null || myBrowser == null) return;
        ViewGroup parentLayout = (ViewGroup) swipe.getParent();
        if (parentLayout == null) return;

        int swipeIndex = parentLayout.indexOfChild(swipe);
        parentLayout.removeView(swipe);

        Bitmap bmpLeft = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvasLeft = new Canvas(bmpLeft);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.WHITE);

        Path pathLeft = new Path();
        pathLeft.moveTo(60, 20); 
        pathLeft.lineTo(30, 50); 
        pathLeft.lineTo(60, 80);
        canvasLeft.drawPath(pathLeft, paint);

        final ImageView leftArrow = new ImageView(this);
        leftArrow.setImageBitmap(bmpLeft);
        leftArrow.setColorFilter(0xFF757575, PorterDuff.Mode.SRC_IN);

        final GradientDrawable bgLeft = new GradientDrawable();
        bgLeft.setShape(GradientDrawable.OVAL);
        bgLeft.setColor(0xFFEEEEEE);
        leftArrow.setBackground(bgLeft);
        leftArrow.setElevation(15f);
        leftArrow.setPadding(25, 25, 25, 25);

        FrameLayout.LayoutParams lpLeft = new FrameLayout.LayoutParams(120, 120);
        lpLeft.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        leftArrow.setLayoutParams(lpLeft);
        leftArrow.setTranslationX(-150);

        Bitmap bmpRight = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvasRight = new Canvas(bmpRight);
        Path pathRight = new Path();
        pathRight.moveTo(40, 20); 
        pathRight.lineTo(70, 50); 
        pathRight.lineTo(40, 80);
        canvasRight.drawPath(pathRight, paint);

        final ImageView rightArrow = new ImageView(this);
        rightArrow.setImageBitmap(bmpRight);
        rightArrow.setColorFilter(0xFF757575, PorterDuff.Mode.SRC_IN);

        final GradientDrawable bgRight = new GradientDrawable();
        bgRight.setShape(GradientDrawable.OVAL);
        bgRight.setColor(0xFFEEEEEE);
        rightArrow.setBackground(bgRight);
        rightArrow.setElevation(15f);
        rightArrow.setPadding(25, 25, 25, 25);

        FrameLayout.LayoutParams lpRight = new FrameLayout.LayoutParams(120, 120);
        lpRight.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        rightArrow.setLayoutParams(lpRight);
        rightArrow.setTranslationX(150);

        FrameLayout gestureContainer = new FrameLayout(this) {
            float startX = 0, startY = 0;
            boolean isEdgeSwiping = false;
            boolean isLeftEdge = false;
            final float THRESHOLD = 220f; 

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                switch(ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
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
                    case MotionEvent.ACTION_MOVE:
                        if (isEdgeSwiping) {
                            float dX = Math.abs(ev.getX() - startX);
                            float dY = Math.abs(ev.getY() - startY);
                            if (dX > 25 && dX > dY) return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isEdgeSwiping = false;
                        break;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                if (!isEdgeSwiping) return super.onTouchEvent(ev);
                float deltaX = ev.getX() - startX;
                float pullDist = Math.abs(deltaX);

                switch(ev.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if (isLeftEdge && deltaX > 0) {
                            leftArrow.setTranslationX(-150 + Math.min(pullDist, 300));
                            if (pullDist > THRESHOLD) {
                                bgLeft.setColor(0xFF2196F3); 
                                leftArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN); 
                            } else {
                                bgLeft.setColor(0xFFEEEEEE); 
                                leftArrow.setColorFilter(0xFF757575, PorterDuff.Mode.SRC_IN); 
                            }
                        } else if (!isLeftEdge && deltaX < 0) {
                            rightArrow.setTranslationX(150 - Math.min(pullDist, 300));
                            if (pullDist > THRESHOLD) {
                                bgRight.setColor(0xFF2196F3); 
                                rightArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN); 
                            } else {
                                bgRight.setColor(0xFFEEEEEE); 
                                rightArrow.setColorFilter(0xFF757575, PorterDuff.Mode.SRC_IN); 
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (pullDist > THRESHOLD && ev.getAction() == MotionEvent.ACTION_UP) {
                            if (isLeftEdge) myBrowser.goBack();
                            else myBrowser.goForward();
                        }
                        leftArrow.animate().translationX(-150).setDuration(200).start();
                        rightArrow.animate().translationX(150).setDuration(200).start();
                        isEdgeSwiping = false;
                        break;
                }
                return true;
            }
        };

        gestureContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        gestureContainer.addView(swipe, new FrameLayout.LayoutParams(-1, -1));
        gestureContainer.addView(leftArrow);
        gestureContainer.addView(rightArrow);
        parentLayout.addView(gestureContainer, swipeIndex);
    }
    
// Long Press Dialog 
    private void setupLongPressDialog(WebView myBrowser) {
        myBrowser.setOnLongClickListener(v -> {
            WebView.HitTestResult result = myBrowser.getHitTestResult();
            if (result != null) {
                int type = result.getType();
                if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    String url = result.getExtra();
                    if (url != null && !url.isEmpty()) {
                        showLinkOptionsDialog(url, myBrowser);
                        return true; 
                    }
                }
            }
            return false;
        });
    }

    private void showLinkOptionsDialog(final String url, final WebView webView) {
        final Dialog dialog = new Dialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(50, 50, 50, 50);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(35f); 
        bg.setColor(Color.WHITE);
        root.setBackground(bg);

        TextView urlText = new TextView(this);
        urlText.setText(url);
        urlText.setTextColor(0xFF1976D2); 
        urlText.setTextSize(15);
        urlText.setPadding(0, 0, 0, 40);
        urlText.setSingleLine(false); 
        urlText.setMaxLines(4);
        urlText.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(urlText);

        TextView btnCopy = createDialogButton("Copy Link");
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Link", url);
            if (clipboard != null) clipboard.setPrimaryClip(clip);
            Toast.makeText(MainActivity.this, "Link Copied!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        root.addView(btnCopy);

        TextView btnOpen = createDialogButton("Open URL");
        btnOpen.setOnClickListener(v -> {
            webView.loadUrl(url);
            dialog.dismiss();
        });
        root.addView(btnOpen);

        TextView btnShare = createDialogButton("Share URL");
        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(shareIntent, "Share Link"));
            dialog.dismiss();
        });
        root.addView(btnShare);

        dialog.setContentView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85); 
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private TextView createDialogButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(0xFF333333);
        btn.setTextSize(17);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(20, 25, 20, 25);
        btn.setBackgroundResource(android.R.drawable.list_selector_background); 
        return btn;
    }

// “view-source:” Method 
    private void handleViewSource(final WebView webview, String sourceUrl) {
        try {
            String cleanUrl = sourceUrl.substring(12).trim();
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://" + cleanUrl;
            }

            final String targetUrl = cleanUrl;
            Toast.makeText(getApplicationContext(), "Fetching Source Code...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    URL url = new URL(targetUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\\n");
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
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error Fetching Source!", Toast.LENGTH_LONG).show());
                }
            }).start();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkAndLoadSource(final WebView webview, String input) {
        if (input != null) {
            String trimmedInput = input.trim();
            if (trimmedInput.toLowerCase().startsWith("view-source:")) {
                if (trimmedInput.length() <= 12) {
                    Toast.makeText(getApplicationContext(), "Please provide a link!", Toast.LENGTH_SHORT).show();
                    return true;
                }
                handleViewSource(webview, trimmedInput);
                return true;
            }
        }
        return false; 
    }

    private void setupBotAPI(WebView myBrowser) {
        myBrowser.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void Bot() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isBotDialogShowing) return;
                    String currentUrl = myBrowser.getUrl();
                    if (currentUrl != null && !currentUrl.isEmpty()) {
                        isBotDialogShowing = true;
                        isBotRunning = false; 
                        getSharedPreferences("BotPrefs", 0).edit().putString(BOT_CODE_KEY, "").apply();
                        showBotSourceDialog(myBrowser, "view-source:" + currentUrl);
                    } else {
                        Toast.makeText(myBrowser.getContext(), "Error: No URL found!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
// Code.Bot() Method 
            @JavascriptInterface
            public void Bot(final String targetUrl) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isBotDialogShowing) return;
                    if (targetUrl != null && !targetUrl.trim().isEmpty()) {
                        isBotDialogShowing = true;
                        isBotRunning = false; 
                        getSharedPreferences("BotPrefs", 0).edit().putString(BOT_CODE_KEY, "").apply();
                        showBotSourceDialog(myBrowser, "view-source:" + targetUrl.trim());
                    } else {
                        Toast.makeText(myBrowser.getContext(), "Error: Invalid link!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, "Code");
    }

    private void showBotSourceDialog(final WebView webView, final String sourceUrl) {
        Context context = webView.getContext();
        final Dialog dialog = new Dialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 70, 60, 70);
        root.setGravity(Gravity.CENTER);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40f);
        bg.setColor(Color.WHITE);
        root.setBackground(bg);

        TextView titleText = new TextView(context);
        titleText.setText("View Page Source with Bot?");
        titleText.setTextColor(0xFF333333); 
        titleText.setTextSize(19);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 60); 
        root.addView(titleText);

        TextView btnView = new TextView(context);
        btnView.setText("View");
        btnView.setTextColor(0xFFFFEBEE); 
        btnView.setTextSize(17);
        btnView.setTypeface(null, Typeface.BOLD);
        btnView.setGravity(Gravity.CENTER);
        btnView.setPadding(0, 35, 0, 35);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(23f);
        btnBg.setColor(0xFFE57373); 
        btnView.setBackground(btnBg);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(20, 0, 20, 0); 
        btnView.setLayoutParams(btnParams);

        btnView.setOnClickListener(v -> {
            webView.loadUrl(sourceUrl); 
            dialog.dismiss();
        });
        root.addView(btnView);

        dialog.setOnDismissListener(d -> isBotDialogShowing = false);
        dialog.setContentView(root);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(context.getResources().getDisplayMetrics().widthPixels * 0.85); 
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }
    
// Pull to UrlBar Hiding 
    private void setupScrollHidingToolbar(WebView myBrowser) {
        final View toolbar = findViewById(R.id.linear3); 
        if (myBrowser == null || toolbar == null) return;

        final ValueAnimator[] currentAnim = {null};
        final boolean[] isHidden = {false};

        if (Build.VERSION.SDK_INT >= 23) {
            myBrowser.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                int dy = scrollY - oldScrollY; 
                final int tHeight = toolbar.getHeight();
                if (tHeight == 0) return; 

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
                int currentMargin = params.topMargin;

                if (dy > 30 && scrollY > 100 && !isHidden[0]) {
                    isHidden[0] = true; 
                    if (currentAnim[0] != null) currentAnim[0].cancel(); 
                    currentAnim[0] = ValueAnimator.ofInt(currentMargin, -tHeight);
                    currentAnim[0].setDuration(250);
                    currentAnim[0].addUpdateListener(animation -> {
                        params.topMargin = (int) animation.getAnimatedValue();
                        toolbar.setLayoutParams(params);
                    });
                    currentAnim[0].start();
                } else if (dy < -30 && isHidden[0]) {
                    isHidden[0] = false;
                    if (currentAnim[0] != null) currentAnim[0].cancel(); 
                    currentAnim[0] = ValueAnimator.ofInt(currentMargin, 0);
                    currentAnim[0].setDuration(250);
                    currentAnim[0].addUpdateListener(animation -> {
                        params.topMargin = (int) animation.getAnimatedValue();
                        toolbar.setLayoutParams(params);
                    });
                    currentAnim[0].start();
                }
            });
        }
    }
    
//Cleaner.Bot() Method 
    private void setupCleanerAPI(WebView myBrowser) {
        myBrowser.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void Bot() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isCleanerDialogShowing) return;
                    String currentUrl = myBrowser.getUrl();
                    if (currentUrl != null && !currentUrl.isEmpty()) {
                        isCleanerDialogShowing = true;
                        isBotRunning = false; 
                        getSharedPreferences("BotPrefs", 0).edit().putString(BOT_CODE_KEY, "").apply();
                        try {
                            Uri uri = Uri.parse(currentUrl);
                            String baseDomain = uri.getScheme() + "://" + uri.getHost();
                            showCleanDataDialog(myBrowser, baseDomain);
                        } catch (Exception e) {
                            Toast.makeText(myBrowser.getContext(), "Error parsing URL!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(myBrowser.getContext(), "No active site to clean!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, "Cleaner");
    }

    private void showCleanDataDialog(final WebView webView, final String baseDomain) {
        Context context = webView.getContext();
        final Dialog dialog = new Dialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 70, 60, 70);
        root.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40f);
        bg.setColor(Color.WHITE);
        root.setBackground(bg);

        TextView msgText1 = new TextView(context);
        msgText1.setText("Do you want to delete all data from this site?");
        msgText1.setTextColor(0xFF333333); 
        msgText1.setTextSize(17);
        msgText1.setTypeface(null, Typeface.BOLD);
        msgText1.setGravity(Gravity.CENTER);
        msgText1.setPadding(0, 0, 0, 5); 
        root.addView(msgText1);

        TextView msgText2 = new TextView(context);
        msgText2.setText("This may sign you out of this site.");
        msgText2.setTextColor(0xFF555555); 
        msgText2.setTextSize(14);
        msgText2.setGravity(Gravity.CENTER);
        msgText2.setPadding(0, 0, 0, 20); 
        root.addView(msgText2);

        TextView urlText = new TextView(context);
        urlText.setText(baseDomain);
        urlText.setTextColor(0xFF1976D2); 
        urlText.setTextSize(15);
        urlText.setGravity(Gravity.CENTER);
        urlText.setPadding(0, 0, 0, 60); 
        root.addView(urlText);

        LinearLayout btnLayout = new LinearLayout(context);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(0xFF757575); 
        btnCancel.setTextSize(16);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(0, 30, 0, 30);

        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setCornerRadius(20f);
        cancelBg.setColor(0xFFEEEEEE); 
        btnCancel.setBackground(cancelBg);

        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        cancelParams.setMargins(0, 0, 15, 0); 
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnLayout.addView(btnCancel);

        TextView btnDelete = new TextView(context);
        btnDelete.setText("Delete");
        btnDelete.setTextColor(Color.WHITE); 
        btnDelete.setTextSize(16);
        btnDelete.setTypeface(null, Typeface.BOLD);
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setPadding(0, 30, 0, 30);

        GradientDrawable deleteBg = new GradientDrawable();
        deleteBg.setCornerRadius(20f);
        deleteBg.setColor(0xFFE53935); 
        btnDelete.setBackground(deleteBg);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        deleteParams.setMargins(15, 0, 0, 0); 
        btnDelete.setLayoutParams(deleteParams);

        btnDelete.setOnClickListener(v -> {
            WebStorage.getInstance().deleteOrigin(baseDomain);
            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(baseDomain);
            if (cookies != null) {
                Uri uri = Uri.parse(baseDomain);
                String host = uri.getHost();
                String bareHost = (host != null && host.startsWith("www.")) ? host.substring(4) : host;

                String[] temp = cookies.split(";");
                for (String ar1 : temp) {
                    String cookieName = ar1.split("=")[0].trim();
                    if (host != null) {
                        cookieManager.setCookie(baseDomain, cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=" + host);
                        cookieManager.setCookie(baseDomain, cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=." + bareHost);
                        cookieManager.setCookie(baseDomain, cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=" + bareHost);
                    }
                    cookieManager.setCookie(baseDomain, cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/");
                }
                cookieManager.flush();
            }

            String jsClear = "localStorage.clear(); sessionStorage.clear(); " +
            "document.cookie.split(';').forEach(function(c) { document.cookie = c.replace(/^ +/, '').replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/'); });";

            webView.evaluateJavascript(jsClear, value -> {
                Toast.makeText(webView.getContext(), "Site data cleared successfully!", Toast.LENGTH_SHORT).show();
                webView.reload(); 
            });
            dialog.dismiss();
        });
        btnLayout.addView(btnDelete);
        root.addView(btnLayout);

        dialog.setOnDismissListener(d -> isCleanerDialogShowing = false);
        dialog.setContentView(root);

        if (d

// Axiom Force Sync: 2026-07-03T09:50:43.775Z