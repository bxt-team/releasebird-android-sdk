package com.releasebird.releasebird_sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Shows the Releasebird widget in a WebView on top of the host app.
 */
public class ReleasebirdMainActivity extends AppCompatActivity {

    private static final String TAG = "RELEASEBIRD";

    public static boolean isActive = false;

    private WebView webView;

    /** Answer the WebView is waiting for while the system file picker is open. */
    private ValueCallback<Uri[]> pendingFileCallback;

    private final ActivityResultLauncher<Intent> openFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult activityResult) {
                    deliverFiles(activityResult.getResultCode() == Activity.RESULT_OK ? activityResult.getData() : null);
                }
            });

    /** Hands the picked files (or "nothing picked") back to the page – the WebView blocks further uploads until it gets an answer. */
    private void deliverFiles(Intent data) {
        ValueCallback<Uri[]> callback = pendingFileCallback;
        pendingFileCallback = null;
        if (callback == null) {
            return;
        }
        Uri[] uris = null;
        try {
            if (data != null) {
                ClipData clip = data.getClipData();
                if (clip != null && clip.getItemCount() > 0) {
                    uris = new Uri[clip.getItemCount()];
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        uris[i] = clip.getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    uris = new Uri[]{data.getData()};
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not read the selected file: " + e.getMessage());
        }
        callback.onReceiveValue(uris);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isActive = true;
        try {
            this.requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } catch (Exception ignore) {
        }
        super.onCreate(savedInstanceState);
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });
            setContentView(R.layout.activity_rbird_main);
            if (ReleasebirdConfig.getInstance().getApiKey() == null) {
                // e.g. the process was restarted with this activity on top: nothing to show without init
                finish();
                return;
            }
            webView = findViewById(R.id.releasebird_webview);
            initBrowser();
            if (savedInstanceState != null) {
                webView.restoreState(savedInstanceState);
            } else {
                webView.loadUrl(widgetUrl());
            }
        } catch (Exception ex) {
            // e.g. no WebView on this device
            Log.e(TAG, "Could not open the widget: " + ex.getMessage());
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        isActive = false;
        // an open file request must be answered, otherwise the WebView leaks it
        deliverFiles(null);
        if (webView != null) {
            webView.removeJavascriptInterface("ReleasebirdInterface");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /**
     * Address of the widget for this installation. The hash of a verified identity travels along –
     * without it the widget treats the user as unverified.
     */
    private String widgetUrl() {
        String peopleId = "";
        String hash = "null";
        JSONObject rbirdState = ReleasebirdConfig.getInstance().getRbirdState(getApplicationContext());
        if (rbirdState != null) {
            peopleId = rbirdState.optString("people", "");
            if (!rbirdState.isNull("hash") && !rbirdState.optString("hash", "").isEmpty()) {
                hash = rbirdState.optString("hash");
            }
        }
        return Uri.parse(ReleasebirdConfig.getInstance().getWidgetContentUrl() + "/widget").buildUpon()
                .appendQueryParameter("apiKey", ReleasebirdConfig.getInstance().getApiKey())
                .appendQueryParameter("ai", new AiValueManager(getApplicationContext()).getAIValue())
                .appendQueryParameter("people", peopleId)
                .appendQueryParameter("tab", "HOME")
                .appendQueryParameter("hash", hash)
                .appendQueryParameter("disableScreenCapture", "true")
                .build().toString();
    }

    private void initBrowser() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setDefaultTextEncodingName("utf-8");
        // the widget only needs the network
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        webView.setWebViewClient(new ReleasebirdWebViewClient());
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.addJavascriptInterface(new ReleasebirdInterface(this), "ReleasebirdInterface");
        webView.setWebChromeClient(new RbirdWebChromeClient());
        webView.setVisibility(View.VISIBLE);
    }

    /** True for pages of the widget itself; everything else opens outside the app. */
    private static boolean isWidgetUrl(Uri uri) {
        Uri widget = Uri.parse(ReleasebirdConfig.getInstance().getWidgetContentUrl());
        return uri != null && uri.getHost() != null && uri.getHost().equalsIgnoreCase(widget.getHost());
    }

    private void openExternally(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No app can open " + uri.getScheme() + " links");
        } catch (Exception e) {
            Log.w(TAG, "Could not open link: " + e.getMessage());
        }
    }

    private void showLoadError() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        try {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.releasebird_load_error)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        } catch (Exception e) {
            finish();
        }
    }

    private class ReleasebirdInterface {
        private final WeakReference<AppCompatActivity> mContextRef;

        public ReleasebirdInterface(AppCompatActivity c) {
            mContextRef = new WeakReference<>(c);
        }

        @JavascriptInterface
        public void callWidget(final String object) {
            final AppCompatActivity activity = this.mContextRef.get();
            if (activity == null || object == null) {
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (object.startsWith("{")) {
                            JSONObject jsonObject = new JSONObject(object);
                            if ("showImage".equals(jsonObject.optString("key"))) {
                                Uri image = Uri.parse(jsonObject.optString("url"));
                                // only real web addresses – the value comes from the page
                                if ("https".equals(image.getScheme()) || "http".equals(image.getScheme())) {
                                    Intent intent = new Intent(activity, ImageViewerActivity.class);
                                    intent.putExtra("image_url", image.toString());
                                    startActivity(intent);
                                }
                            }
                        } else if ("close".equals(object)) {
                            finish();
                        }
                    } catch (Exception err) {
                        Log.w(TAG, "Message from the widget could not be handled: " + err.getMessage());
                    }
                }
            });
        }
    }

    private class ReleasebirdWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (isWidgetUrl(uri)) {
                return false;
            }
            openExternally(uri);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            // only when the widget page itself could not be loaded (offline, server unreachable)
            if (request != null && request.isForMainFrame()) {
                showLoadError();
            }
        }
    }

    private class RbirdWebChromeClient extends WebChromeClient {

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // answer a request that is still open before starting a new one
            deliverFiles(null);
            pendingFileCallback = filePathCallback;
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] accept = fileChooserParams != null ? fileChooserParams.getAcceptTypes() : null;
                if (accept != null && accept.length > 0 && accept[0] != null && accept[0].contains("/")) {
                    if (accept.length == 1) {
                        intent.setType(accept[0]);
                    } else {
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, accept);
                    }
                }
                if (fileChooserParams != null && fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                openFileLauncher.launch(intent);
                return true;
            } catch (Exception ex) {
                Log.e(TAG, "Could not open the file picker: " + ex.getMessage());
                deliverFiles(null);
                return true;
            }
        }

        // The page waits until a dialog is answered – every path below must confirm or cancel.

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            try {
                new AlertDialog.Builder(ReleasebirdMainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                result.confirm();
                            }
                        })
                        .show();
            } catch (Exception e) {
                result.confirm();
            }
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            try {
                final boolean[] confirmed = {false};
                new AlertDialog.Builder(ReleasebirdMainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                confirmed[0] = true;
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (confirmed[0]) {
                                    result.confirm();
                                } else {
                                    result.cancel();
                                }
                            }
                        })
                        .show();
            } catch (Exception e) {
                result.cancel();
            }
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                                  final JsPromptResult result) {
            // the widget does not use prompts
            result.cancel();
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            return true;
        }
    }
}
