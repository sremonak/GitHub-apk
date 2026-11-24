package dev.octohub.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** A small native shell. GitHub, not this app, renders and authenticates every page. */
public class MainActivity extends Activity {
    private static final String HOME = "https://github.com/";
    private static final int FILE_PICKER = 31;
    private WebView web;
    private ProgressBar progress;
    private TextView address;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private String lastPage = HOME;
    private boolean desktop;
    private boolean pageFailed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(13, 17, 23));
        getWindow().setNavigationBarColor(Color.rgb(13, 17, 23));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(13, 17, 23));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            // Also handles Android 15's enforced edge-to-edge and the keyboard.
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(4), dp(8), dp(4));
        address = new TextView(this);
        address.setTextColor(Color.rgb(230, 237, 243));
        address.setTextSize(15);
        address.setText("OctoHub · github.com");
        address.setContentDescription("Current website");
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(44), 1));
        address.setGravity(Gravity.CENTER_VERTICAL);
        Button menu = button("•••", "App menu");
        menu.setOnClickListener(this::showMenu);
        bar.addView(menu);
        root.addView(bar);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));

        android.widget.FrameLayout content = new android.widget.FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(13, 17, 23));
        content.addView(web, new android.widget.FrameLayout.LayoutParams(-1, -1));
        setupErrorPanel(content);
        configureWebView();

        LinearLayout navigation = new LinearLayout(this);
        navigation.setGravity(Gravity.CENTER);
        addNavigation(navigation, "‹", "Go back", () -> { if (web.canGoBack()) web.goBack(); });
        addNavigation(navigation, "›", "Go forward", () -> { if (web.canGoForward()) web.goForward(); });
        addNavigation(navigation, "⌂", "GitHub home", () -> web.loadUrl(HOME));
        addNavigation(navigation, "↻", "Reload page", this::retry);
        root.addView(navigation);
        if (state != null) {
            desktop = state.getBoolean("desktop");
            lastPage = state.getString("lastPage", HOME);
            applyUserAgent();
            if (web.restoreState(state) == null) web.loadUrl(HOME);
        } else web.loadUrl(HOME);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private Button button(String text, String description) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(22);
        b.setTextColor(Color.rgb(201, 209, 217));
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setContentDescription(description);
        b.setMinWidth(dp(48));
        b.setMinimumWidth(dp(48));
        b.setMinimumHeight(dp(48));
        return b;
    }

    private void addNavigation(LinearLayout row, String text, String description, Runnable action) {
        Button b = button(text, description);
        b.setOnClickListener(v -> action.run());
        row.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    }

    private void setupErrorPanel(android.widget.FrameLayout content) {
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorPanel.setBackgroundColor(Color.rgb(13, 17, 23));
        TextView title = new TextView(this);
        title.setText("GitHub couldn’t be loaded");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        TextView hint = new TextView(this);
        hint.setText("Check your internet connection and try again.\nইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।");
        hint.setTextColor(Color.LTGRAY);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(16), 0, dp(24));
        Button retry = button("Try again", "Try loading GitHub again");
        retry.setTextSize(16);
        retry.setOnClickListener(v -> retry());
        errorPanel.addView(title);
        errorPanel.addView(hint);
        errorPanel.addView(retry);
        errorPanel.setVisibility(View.GONE);
        content.addView(errorPanel, new android.widget.FrameLayout.LayoutParams(-1, -1));
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        applyUserAgent();
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl());
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                pageFailed = false;
                errorPanel.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                if (NavigationPolicy.isInternal(url)) lastPage = url;
                Uri uri = Uri.parse(url);
                address.setText("OctoHub · " + (uri.getHost() == null ? "GitHub" : uri.getHost()));
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                if (!pageFailed) errorPanel.setVisibility(View.GONE);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                // Keep GitHub's own 404/403 pages; they include useful account and repo context.
                if (request.isForMainFrame() && response.getStatusCode() >= 500) showError();
            }
            // TLS errors deliberately use WebView's default cancellation. Never bypass certificate validation.
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) { progress.setProgress(value); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_PICKER);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    toast("No file picker is installed on this device.");
                    return false;
                }
            }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message result) {
                if (!userGesture) return false;
                WebView popup = new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView child, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        if (NavigationPolicy.isInternal(uri.toString())) web.loadUrl(uri.toString());
                        else openExternal(uri);
                        child.destroy();
                        return true;
                    }
                });
                ((WebView.WebViewTransport) result.obj).setWebView(popup);
                result.sendToTarget();
                return true;
            }
        });
        web.setDownloadListener((url, agent, disposition, mime, length) -> download(url, agent, disposition, mime));
    }

    private void applyUserAgent() {
        String ua = WebSettings.getDefaultUserAgent(this);
        if (desktop) ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
        web.getSettings().setUserAgentString(ua);
    }

    private boolean route(Uri uri) {
        if (NavigationPolicy.isInternal(uri.toString())) return false;
        openExternal(uri);
        return true;
    }

    private void openExternal(Uri uri) {
        String scheme = uri.getScheme();
        // Never launch arbitrary intent://, file:// or javascript: URLs supplied by web content.
        if (!("https".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme))) {
            toast("This link type is not supported. Use your browser if needed.");
            return;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
        catch (ActivityNotFoundException e) { toast("No app can open this link."); }
    }

    private void download(String url, String agent, String disposition, String mime) {
        if (!NavigationPolicy.isDownloadAllowed(url)) {
            if (url.startsWith("blob:")) toast("This download needs a browser. Choose Open in browser from the menu.");
            else openExternal(Uri.parse(url));
            return;
        }
        String filename = android.webkit.URLUtil.guessFileName(url, disposition, mime);
        new AlertDialog.Builder(this).setTitle("Download file?")
                .setMessage(filename + "\n\nSave to your Downloads folder. Only install files you trust.")
                .setNegativeButton("Cancel", null).setPositiveButton("Download", (dialog, which) -> {
                    try {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setTitle(filename);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                        if (mime != null) request.setMimeType(mime);
                        // Ask the cookie jar for this exact URL, never forward github.com cookies to another host.
                        String cookie = CookieManager.getInstance().getCookie(url);
                        if (cookie != null) request.addRequestHeader("Cookie", cookie);
                        request.addRequestHeader("User-Agent", agent);
                        ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                        toast("Download started. Check your Downloads folder.");
                    } catch (RuntimeException e) { toast("Download could not start. Try opening it in your browser."); }
                }).show();
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Open in browser");
        menu.getMenu().add(0, 2, 1, "Share page");
        menu.getMenu().add(0, 3, 2, desktop ? "Mobile site" : "Desktop site");
        menu.getMenu().add(0, 4, 3, "Clear session & local data");
        menu.getMenu().add(0, 5, 4, "About OctoHub");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: openExternal(Uri.parse(lastPage)); break;
                case 2:
                    Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, lastPage);
                    startActivity(Intent.createChooser(share, "Share GitHub page")); break;
                case 3: desktop = !desktop; applyUserAgent(); web.reload(); break;
                case 4: confirmClearSession(); break;
                case 5: new AlertDialog.Builder(this).setTitle("OctoHub")
                        .setMessage("An unofficial Android window to GitHub.\n\nAll pages, account actions, and sign-in are served by GitHub. This app does not read your password or inject page scripts.\n\nSome features (passkeys, social login, device integrations, or blob downloads) may require an external browser. Browser and app sessions are separate.\n\nNot affiliated with or endorsed by GitHub, Inc.")
                        .setPositiveButton("OK", null).show(); break;
                default: return false;
            }
            return true;
        });
        menu.show();
    }

    private void confirmClearSession() {
        new AlertDialog.Builder(this).setTitle("Clear local session?")
                .setMessage("This signs you out in this app and removes its cookies, cache and website storage. Your GitHub account and repositories will not be deleted.")
                .setNegativeButton("Cancel", null).setPositiveButton("Clear", (dialog, which) -> {
                    web.stopLoading();
                    android.webkit.WebStorage.getInstance().deleteAllData();
                    web.clearCache(true);
                    web.clearFormData();
                    CookieManager.getInstance().removeAllCookies(success -> {
                        CookieManager.getInstance().flush();
                        web.clearHistory();
                        web.loadUrl(HOME);
                        toast("Local session cleared.");
                    });
                }).show();
    }

    private void showError() {
        pageFailed = true;
        progress.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }
    private void retry() { web.loadUrl(lastPage); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        web.saveState(state);
        state.putBoolean("desktop", desktop);
        state.putString("lastPage", lastPage);
        super.onSaveInstanceState(state);
    }
    @Override protected void onPause() {
        web.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        web.stopLoading();
        ((android.view.ViewGroup) web.getParent()).removeView(web);
        web.destroy();
        super.onDestroy();
    }
}
