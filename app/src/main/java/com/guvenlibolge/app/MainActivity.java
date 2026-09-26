package com.guvenlibolge.app;

import android.app.Activity;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private static final String GAME_URL = "https://gkh4n.github.io/guvenli-bolge/";
    private static final int DARK_BG = Color.rgb(7, 10, 16);
    private static final int LIGHT_BG = Color.rgb(238, 243, 247);
    private WebView webView;
    private FrameLayout root;
    private boolean lightTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        lightTheme = getSharedPreferences("safe_zone_prefs", MODE_PRIVATE)
                .getBoolean("light_theme", false);
        applySystemBars(lightTheme);

        root = new FrameLayout(this);
        root.setBackgroundColor(lightTheme ? LIGHT_BG : DARK_BG);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });

        webView = new WebView(this);
        webView.setBackgroundColor(lightTheme ? LIGHT_BG : DARK_BG);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalFadingEdgeEnabled(false);
        webView.setHorizontalFadingEdgeEnabled(false);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString(settings.getUserAgentString() + " GuvenliBolgeApp/2");
        webView.addJavascriptInterface(new ThemeBridge(), "SafeZoneNative");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                if ("https".equals(scheme) || "http".equals(scheme)) {
                    return false;
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) showOfflinePage();
            }
        });

        webView.setOnLongClickListener(v -> true);
        webView.setHapticFeedbackEnabled(false);
        loadGame();
    }

    private void applySystemBars(boolean light) {
        int bg = light ? LIGHT_BG : DARK_BG;
        Window window = getWindow();
        window.setStatusBarColor(bg);
        window.setNavigationBarColor(bg);

        int flags = window.getDecorView().getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (light) flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (light) flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            else flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);

        if (root != null) root.setBackgroundColor(bg);
        if (webView != null) webView.setBackgroundColor(bg);
    }

    private class ThemeBridge {
        @JavascriptInterface
        public void setTheme(String theme) {
            final boolean useLight = "light".equals(theme);
            runOnUiThread(() -> {
                lightTheme = useLight;
                getSharedPreferences("safe_zone_prefs", MODE_PRIVATE)
                        .edit().putBoolean("light_theme", useLight).apply();
                applySystemBars(useLight);
            });
        }
    }

    private boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void loadGame() {
        if (hasNetwork()) webView.loadUrl(GAME_URL);
        else showOfflinePage();
    }

    private void showOfflinePage() {
        String bg = lightTheme ? "#eef3f7" : "#070a10";
        String text = lightTheme ? "#0d1823" : "#f5f7fb";
        String muted = lightTheme ? "#667789" : "#8c99ab";
        String button = lightTheme ? "#0aa9c4" : "#92f3ff";
        String buttonText = lightTheme ? "#ffffff" : "#081017";
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{margin:0;background:" + bg + ";color:" + text + ";font-family:sans-serif;display:grid;place-items:center;min-height:100vh;text-align:center;padding:28px;box-sizing:border-box}"
                + "h1{font-size:28px;margin:0 0 10px}p{color:" + muted + ";line-height:1.6}button{margin-top:16px;border:0;border-radius:14px;padding:14px 22px;font-weight:800;background:" + button + ";color:" + buttonText + "}</style></head>"
                + "<body><main><h1>Güvenli Bölge</h1><p>Oyuna bağlanmak için internet bağlantısı gerekiyor.</p>"
                + "<button onclick=\"location.href='" + GAME_URL + "'\">Tekrar Dene</button></main></body></html>";
        webView.loadDataWithBaseURL(GAME_URL, html, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }

        webView.evaluateJavascript(
                "(window.safeZoneHandleBack ? window.safeZoneHandleBack() : false)",
                value -> {
                    if (!"true".equals(value)) {
                        if (webView.canGoBack()) webView.goBack();
                        else MainActivity.super.onBackPressed();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
