package com.ofidy.ofidybrowser;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import com.crashlytics.android.Crashlytics;
import com.ofidy.ofidybrowser.model.Cart;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.ofidy.ofidybrowser.preference.PreferenceManager;
import com.ofidy.ofidybrowser.utils.FileUtils;
import com.ofidy.ofidybrowser.utils.MemoryLeakUtils;
import com.ofidy.ofidybrowser.utils.ServerHelper;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import io.fabric.sdk.android.Fabric;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.OkHttpClient;


public class BrowserApp extends Application {

    private static final String TAG = BrowserApp.class.getSimpleName();

    private static AppComponent mAppComponent;
    private static final Executor mIOThread = Executors.newSingleThreadExecutor();
    private static final Executor mTaskThread = Executors.newCachedThreadPool();

    private static final int CONNECTION_TIMEOUT = 20 * 1000;            // in milliseconds

    @Inject
    Bus mBus;
    @Inject
    PreferenceManager mPreferenceManager;
    private static OkHttpClient mOkHttpClient;
    private String currency;
    public static ArrayList<Cart> cartItems;
    private static BrowserApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Fabric.with(this, new Crashlytics());
        cartItems = new ArrayList<>();
        new ServerHelper().start(this, getOkHttpClient());
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        }

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {

                if (BuildConfig.DEBUG) {
                    FileUtils.writeCrashToStorage(ex);
                }

                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                } else {
                    System.exit(2);
                }
            }
        });

        mAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        mAppComponent.inject(this);

        if (mPreferenceManager.getUseLeakCanary() && !isRelease()) {
            LeakCanary.install(this);
        }
        if (!isRelease() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        registerActivityLifecycleCallbacks(new MemoryLeakUtils.LifecycleAdapter() {
            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "Cleaning up after the Android framework");
                MemoryLeakUtils.clearNextServedView(activity, BrowserApp.this);
            }
        });
    }

    public static BrowserApp getInstance() {
        return sInstance;
    }

    public String getCurrency(){
        if(TextUtils.isEmpty(currency))
            return UserPrefs.getInstance(this).getString(UserPrefs.Key.CURRENCY);
        return currency;
    }

    public void setCurrency(String c){
        currency = c;
    }

    @NonNull
    public static BrowserApp get(@NonNull Context context) {
        return (BrowserApp) context.getApplicationContext();
    }

    public static AppComponent getAppComponent() {
        return mAppComponent;
    }

    @NonNull
    public static Executor getIOThread() {
        return mIOThread;
    }

    @NonNull
    public static Executor getTaskThread() {
        return mTaskThread;
    }

    public static Bus getBus(@NonNull Context context) {
        return get(context).mBus;
    }

    /**
     * Determines whether this is a release build.
     *
     * @return true if this is a release build, false otherwise.
     */
    public static boolean isRelease() {
        return !BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.toLowerCase().equals("release");
    }

    public static void copyToClipboard(@NonNull Context context, @NonNull String string) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", string);
        clipboard.setPrimaryClip(clip);
    }

    public static OkHttpClient getOkHttpClient() {
        if (mOkHttpClient != null) {
            return mOkHttpClient;
        }
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        return mOkHttpClient;
    }

}
