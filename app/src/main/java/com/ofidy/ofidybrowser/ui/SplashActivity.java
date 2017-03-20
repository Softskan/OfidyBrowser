package com.ofidy.ofidybrowser.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;

import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.pref.AppState;
import com.ofidy.ofidybrowser.pref.Intervals;
import com.ofidy.ofidybrowser.pref.UserPrefs;

/**
 * Created by ari on 1/11/16.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_splash);
        new Handler().postDelayed(() -> {
            Intent i;
            if(! AppState.getInstance(SplashActivity.this).getBoolean(AppState.Key.LOGGED_IN)){
                i = new Intent(SplashActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
            else{
                i = new Intent(SplashActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                refreshSession();
            }

            finish();
        }, 2000);
    }

    protected void refreshSession(){
        //if(NetworkUtils.isConnected(this)){
        /*long v = System.currentTimeMillis() - UserPrefs.getInstance(this).getLong(UserPrefs.Key.LAST_LOGIN);
        if(v >= Intervals.SESSION_INTERVAL && AppState.getInstance(this).getBoolean(AppState.Key.LOGGED_IN)){
            getBus().post(new LoginStartEvent(UserPrefs.getInstance(this).getString(UserPrefs.Key.EMAIL),
                    UserPrefs.getInstance(this).getString(UserPrefs.Key.PASSWORD), false));
        }*/
        //}
    }
}
