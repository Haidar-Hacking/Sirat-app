package com.haydar.raqeeb;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.WebChromeClient;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.window.OnBackInvokedCallback;
import com.haydar.sirat.R;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static final String APP_HOST = "appassets.androidplatform.net";
    static final String APP_ORIGIN = "https://appassets.androidplatform.net";
    static final String APP_URL = "https://appassets.androidplatform.net/index.html";
    private static final Map<String, String> MIME = new HashMap<>();
    static final String OLD_HOST = "raqeeb-cbk9.netlify.app";
    static final String OLD_ORIGIN = "https://raqeeb-cbk9.netlify.app";
    static final String OLD_PAGE = "<!doctype html><meta charset=utf-8><title>.</title><script>addEventListener('message',function(e){if(e.data!=='sirat-port'||!e.ports||!e.ports[0])return;var o={};try{for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o[k]=localStorage.getItem(k);}}catch(x){}e.ports[0].postMessage(JSON.stringify({c:'mig',d:JSON.stringify(o)}));});</script>";
    static final String OLD_URL = "https://raqeeb-cbk9.netlify.app/__sirat_migrate?upd=1";
    static final int REQ_LOC = 2;
    static final int REQ_NOTIF = 1;
    private View bottomBar;
    private GeolocationPermissions.Callback geoCb;
    private String geoOrigin;
    private WebMessagePort port;
    private SharedPreferences prefs;
    private LinearLayout root;
    private View topBar;
    private WebView web;
    private int topColor = -15253656;
    private int bottomColor = -1;
    private boolean migrating = false;
    private boolean clearHist = false;
    private boolean connected = false;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Adhan.ensureChannel(this);
        this.prefs = getSharedPreferences("app", 0);
        this.root = new LinearLayout(this);
        this.root.setOrientation(LinearLayout.VERTICAL);
        this.root.setBackgroundColor(-264210);
        this.topBar = new View(this);
        this.bottomBar = new View(this);
        this.web = new WebView(this);
        this.root.addView(this.topBar, new LinearLayout.LayoutParams(-1, 0));
        this.root.addView(this.web, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.root.addView(this.bottomBar, new LinearLayout.LayoutParams(-1, 0));
        WebSettings settings = this.web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setTextZoom(100);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setOffscreenPreRaster(true);
        this.web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        this.web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
                Uri u = webResourceRequest.getUrl();
                if (OfflineAudio.isAudioHost(u.getHost())) {
                    // تلاوة محفوظة على الهاتف؟ تُشغَّل منه بدون إنترنت
                    return OfflineAudio.serve(MainActivity.this, u.toString(), webResourceRequest.getRequestHeaders());
                }
                return MainActivity.this.serve(u);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
                Uri url = webResourceRequest.getUrl();
                String host = url.getHost();
                if (MainActivity.APP_HOST.equals(host) || MainActivity.OLD_HOST.equals(host)) {
                    return false;
                }
                String scheme = url.getScheme();
                if ("about".equals(scheme) || "blob".equals(scheme) || "data".equals(scheme)) {
                    return false;
                }
                try {
                    Intent intent = new Intent("android.intent.action.VIEW", url);
                    intent.addFlags(268435456);
                    MainActivity.this.startActivity(intent);
                } catch (Exception e) {
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
                MainActivity.this.connected = false;
            }

            @Override
            public void onPageCommitVisible(WebView webView, String str) {
                MainActivity.this.connect(str);
            }

            @Override
            public void onPageFinished(WebView webView, String str) {
                MainActivity.this.connect(str);
                if (MainActivity.APP_HOST.equals(Uri.parse(str).getHost()) && MainActivity.this.clearHist) {
                    MainActivity.this.clearHist = false;
                    webView.clearHistory();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                ServiceWorkerController.getInstance().setServiceWorkerClient(new ServiceWorkerClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebResourceRequest webResourceRequest) {
                        return MainActivity.this.serve(webResourceRequest.getUrl());
                    }
                });
            } catch (Exception e) {
            }
        }
        this.web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String str, GeolocationPermissions.Callback callback) {
                if (MainActivity.APP_ORIGIN.equals(str) || "https://appassets.androidplatform.net/".equals(str)) {
                    if (MainActivity.this.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) {
                        MainActivity.this.geoCb = callback;
                        MainActivity.this.geoOrigin = str;
                        MainActivity.this.requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, MainActivity.REQ_LOC);
                        return;
                    }
                    callback.invoke(str, true, false);
                    return;
                }
                callback.invoke(str, false, false);
            }
        });
        setContentView(this.root);
        setupBars();
        if (bundle != null) {
            this.web.restoreState(bundle);
        }
        if (bundle == null || this.web.getUrl() == null || !APP_HOST.equals(Uri.parse(this.web.getUrl()).getHost())) {
            start();
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    MainActivity.this.goBack();
                }
            });
        }
        Adhan.rescheduleSaved(this);
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == REQ_LOC && this.geoCb != null) {
            this.geoCb.invoke(this.geoOrigin, iArr.length > 0 && iArr[0] == 0, false);
            this.geoCb = null;
        }
        if (i == REQ_NOTIF) {
            sendStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendStatus();
    }

    private void start() {
        if (!this.prefs.getBoolean("migrated", false)) {
            this.migrating = true;
            this.web.loadUrl(OLD_URL);
            this.ui.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (MainActivity.this.migrating) {
                        MainActivity.this.finishMigration(null);
                    }
                }
            }, 6000L);
            return;
        }
        openApp();
    }

    private void openApp() {
        this.clearHist = true;
        this.web.loadUrl(APP_URL);
    }

    public void finishMigration(String str) {
        if (this.migrating) {
            this.migrating = false;
            if (str != null) {
                SharedPreferences.Editor putBoolean = this.prefs.edit().putBoolean("migrated", true);
                if (str.length() > 2) {
                    putBoolean.putString("migdata", str);
                }
                putBoolean.apply();
            }
            openApp();
        }
    }

    public void connect(String str) {
        String str2;
        if (!this.connected && str != null) {
            String host = Uri.parse(str).getHost();
            if (APP_HOST.equals(host)) {
                str2 = APP_ORIGIN;
            } else if (!OLD_HOST.equals(host) || !this.migrating) {
                return;
            } else {
                str2 = OLD_ORIGIN;
            }
            try {
                WebMessagePort[] createWebMessageChannel = this.web.createWebMessageChannel();
                if (this.port != null) {
                    try {
                        this.port.close();
                    } catch (Exception e) {
                    }
                }
                this.port = createWebMessageChannel[0];
                this.port.setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                    @Override
                    public void onMessage(WebMessagePort webMessagePort, WebMessage webMessage) {
                        MainActivity.this.handle(webMessage.getData());
                    }
                }, this.ui);
                this.web.postWebMessage(new WebMessage("sirat-port", new WebMessagePort[]{createWebMessageChannel[1]}), Uri.parse(str2));
                this.connected = true;
                if (str2.equals(APP_ORIGIN)) {
                    String string = this.prefs.getString("migdata", null);
                    if (string != null) {
                        this.prefs.edit().remove("migdata").apply();
                        JSONObject jSONObject = new JSONObject();
                        jSONObject.put("t", "mig");
                        jSONObject.put("d", string);
                        send(jSONObject);
                    }
                    sendStatus();
                }
            } catch (Exception e2) {
            }
        }
    }

    private void send(JSONObject jSONObject) {
        if (this.port != null) {
            try {
                this.port.postMessage(new WebMessage(jSONObject.toString()));
            } catch (Exception e) {
            }
        }
    }

    private void sendStatus() {
        if (this.port != null && this.connected) {
            try {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("t", "status");
                jSONObject.put("n", notificationsOn());
                jSONObject.put("e", exactOn());
                jSONObject.put("c", getSharedPreferences("adhan", 0).getInt("n", 0));
                send(jSONObject);
            } catch (Exception e) {
            }
        }
    }

    public void handle(String str) {
        try {
            JSONObject jSONObject = new JSONObject(str);
            String optString = jSONObject.optString("c");
            if ("mig".equals(optString)) {
                finishMigration(jSONObject.optString("d", null));
            } else if ("schedule".equals(optString)) {
                String optString2 = jSONObject.optString("j", "[]");
                Adhan.save(this, optString2);
                Adhan.scheduleAll(this, optString2);
                sendStatus();
            } else if ("status".equals(optString)) {
                sendStatus();
            } else if ("test".equals(optString)) {
                Adhan.show(this, "حان الآن وقت الصلاة", "تجربة صوت الأذان في صراط المؤمنين", 9999);
            } else if ("settings".equals(optString)) {
                openNotificationSettings();
            } else if ("dl".equals(optString)) {
                final String url = jSONObject.optString("u");
                if (url.startsWith("https://") && OfflineAudio.isAudioHost(Uri.parse(url).getHost())) {
                    OfflineAudio.download(this, url, new OfflineAudio.Listener() {
                        @Override
                        public void progress(final String u, final int p, final boolean done, final String err) {
                            ui.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSONObject o = new JSONObject();
                                        o.put("t", "dl");
                                        o.put("u", u);
                                        o.put("p", p);
                                        o.put("done", done);
                                        if (err != null) o.put("err", err);
                                        send(o);
                                    } catch (Exception ignored) {
                                    }
                                }
                            });
                        }
                    });
                }
            } else if ("dlstat".equals(optString)) {
                org.json.JSONArray list = jSONObject.optJSONArray("l");
                org.json.JSONArray have = new org.json.JSONArray();
                org.json.JSONArray not = new org.json.JSONArray();
                if (list != null) {
                    for (int i = 0; i < list.length(); i++) {
                        String u = list.optString(i);
                        if (OfflineAudio.has(this, u)) have.put(u); else not.put(u);
                    }
                }
                JSONObject o = new JSONObject();
                o.put("t", "dlstat");
                o.put("have", have);
                o.put("not", not);
                o.put("size", OfflineAudio.totalSize(this));
                o.put("count", OfflineAudio.count(this));
                send(o);
            } else if ("dldel".equals(optString)) {
                OfflineAudio.delete(this, jSONObject.optString("u"));
                JSONObject o = new JSONObject();
                o.put("t", "dlstat");
                o.put("not", new org.json.JSONArray().put(jSONObject.optString("u")));
                o.put("size", OfflineAudio.totalSize(this));
                o.put("count", OfflineAudio.count(this));
                send(o);
            } else if ("dlclear".equals(optString)) {
                OfflineAudio.clear(this);
                JSONObject o = new JSONObject();
                o.put("t", "dlstat");
                o.put("cleared", true);
                o.put("size", 0);
                o.put("count", 0);
                send(o);
            } else if ("bars".equals(optString)) {
                setBarColors(jSONObject.optString("t"), jSONObject.optString("b"));
            }
        } catch (Exception e) {
        }
    }

    private boolean notificationsOn() {
        if (Build.VERSION.SDK_INT >= 24) {
            return ((NotificationManager) getSystemService("notification")).areNotificationsEnabled();
        }
        return true;
    }

    private boolean exactOn() {
        if (Build.VERSION.SDK_INT >= 31) {
            return ((AlarmManager) getSystemService("alarm")).canScheduleExactAlarms();
        }
        return true;
    }

    private void openNotificationSettings() {
        Intent intent;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            } else {
                intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
        } catch (Exception e) {
        }
    }

    static {
        MIME.put("html", "text/html");
        MIME.put("js", "text/javascript");
        MIME.put("css", "text/css");
        MIME.put("json", "application/json");
        MIME.put("png", "image/png");
        MIME.put("svg", "image/svg+xml");
        MIME.put("webp", "image/webp");
        MIME.put("jpg", "image/jpeg");
        MIME.put("txt", "text/plain");
        MIME.put("woff", "font/woff");
        MIME.put("woff2", "font/woff2");
    }

    public WebResourceResponse serve(Uri uri) {
        String host = uri.getHost();
        if (OLD_HOST.equals(host)) {
            return resp("text/html", 200, new ByteArrayInputStream(OLD_PAGE.getBytes()));
        }
        if (!APP_HOST.equals(host)) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }
        String substring = path.substring(1);
        if (substring.equals("adhan.mp3")) {
            try {
                return resp("audio/mpeg", 200, getResources().openRawResource(R.raw.adhan));
            } catch (Exception e) {
                return null;
            }
        }
        if (substring.contains("..")) {
            return resp("text/plain", 404, new ByteArrayInputStream(new byte[0]));
        }
        int lastIndexOf = substring.lastIndexOf('.');
        String lowerCase = lastIndexOf >= 0 ? substring.substring(lastIndexOf + 1).toLowerCase() : "";
        if (substring.equals("index.html") && "play".equals(com.haydar.sirat.BuildConfig.FLAVOR)) {
            // نسخة Google Play: إيقاف فحص التحديثات من GitHub، فالتحديث يأتي من المتجر
            try {
                String html = new String(readAll(getAssets().open("www/index.html")), "UTF-8");
                html = html.replace("var UPD_OFF=false;", "var UPD_OFF=true;");
                return resp("text/html", 200, new ByteArrayInputStream(html.getBytes("UTF-8")));
            } catch (Exception e3) {
                return resp("text/plain", 404, new ByteArrayInputStream(new byte[0]));
            }
        }
        try {
            return resp(MIME.containsKey(lowerCase) ? MIME.get(lowerCase) : "application/octet-stream", 200, getAssets().open("www/" + substring));
        } catch (Exception e2) {
            return resp("text/plain", 404, new ByteArrayInputStream(new byte[0]));
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static WebResourceResponse resp(String str, int i, InputStream inputStream) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("Cache-Control", "no-cache");
        return new WebResourceResponse(str, "utf-8", i, i == 200 ? "OK" : "Not Found", hashMap, inputStream);
    }

    private void setupBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            window.setStatusBarColor(0);
            window.setNavigationBarColor(0);
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
            this.root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    Insets insets2 = windowInsets.getInsets(WindowInsets.Type.ime());
                    MainActivity.setH(MainActivity.this.topBar, insets.top);
                    MainActivity.setH(MainActivity.this.bottomBar, Math.max(insets.bottom, insets2.bottom));
                    view.setPadding(insets.left, 0, insets.right, 0);
                    return WindowInsets.CONSUMED;
                }
            });
            this.root.requestApplyInsets();
        }
        applyBars();
    }

    public static void setH(View view, int i) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
        if (layoutParams.height != i) {
            layoutParams.height = i;
            view.setLayoutParams(layoutParams);
        }
    }

    private static boolean isLight(int i) {
        return ((0.2126d * ((double) Color.red(i))) + (0.7152d * ((double) Color.green(i)))) + (0.0722d * ((double) Color.blue(i))) > 150.0d;
    }

    private void applyBars() {
        this.topBar.setBackgroundColor(this.topColor);
        this.bottomBar.setBackgroundColor(this.bottomColor);
        this.root.setBackgroundColor(this.bottomColor);
        Window window = getWindow();
        boolean isLight = isLight(this.topColor);
        boolean isLight2 = isLight(this.bottomColor);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                insetsController.setSystemBarsAppearance((isLight2 ? 16 : 0) | (isLight ? 8 : 0), 24);
                return;
            }
            return;
        }
        window.setStatusBarColor(this.topColor);
        int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
        int i = isLight ? systemUiVisibility | 8192 : systemUiVisibility & (-8193);
        if (Build.VERSION.SDK_INT >= 26) {
            window.setNavigationBarColor(this.bottomColor);
            i = isLight2 ? i | 16 : i & (-17);
        }
        window.getDecorView().setSystemUiVisibility(i);
    }

    private void setBarColors(String str, String str2) {
        try {
            this.topColor = Color.parseColor(str);
        } catch (Exception e) {
        }
        try {
            this.bottomColor = Color.parseColor(str2);
        } catch (Exception e2) {
        }
        applyBars();
    }

    public void goBack() {
        if (this.web == null || !this.web.canGoBack()) {
            moveTaskToBack(true);
        } else {
            this.web.goBack();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.web != null) {
            this.web.saveState(bundle);
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }
}
