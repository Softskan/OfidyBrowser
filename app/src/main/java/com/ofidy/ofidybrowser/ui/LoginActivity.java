package com.ofidy.ofidybrowser.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;

import com.ofidy.ofidybrowser.BrowserApp;
import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.bus.LoadCustomerAddressEvent;
import com.ofidy.ofidybrowser.bus.LoadShoppingCartEvent;
import com.ofidy.ofidybrowser.pref.AppState;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.ofidy.ofidybrowser.utils.ConfigHelper;

import org.json.JSONObject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.ofidy.ofidybrowser.bus.BusProvider.getBus;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references.
    @Bind(R.id.email)
    AutoCompleteTextView mEmailView;
    @Bind(R.id.password)
    EditText mPasswordView;
    @Bind(R.id.login_progress)
    View mProgressView;
    @Bind(R.id.login_form)
    View mLoginFormView;
    @Bind(R.id.email_layout)
    TextInputLayout inputLayoutEmail;
    @Bind(R.id.password_layout)
    TextInputLayout inputLayoutPassword;
    @Bind(R.id.image)
    ImageView imageView;
    String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });
        if(!TextUtils.isEmpty(UserPrefs.getInstance(this).getString(UserPrefs.Key.UNAME)))
            mEmailView.setText(UserPrefs.getInstance(this).getString(UserPrefs.Key.UNAME));
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @OnClick(R.id.sign_up)
    protected void haveAccount() {
        Intent i = new Intent(this, RegisterActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //i.putExtra(BundleKeys.FROM_ACTIVITY, fromActivity);
        startActivity(i);
        finish();
    }

    @OnClick(R.id.forgot_password)
    protected void forgotPassword() {
        Intent i = new Intent(this, ForgotActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }


    @OnClick(R.id.sign_in_button)
    protected void attemptLogin() {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!validatePassword(password)) {
            focusView = mPasswordView;
            cancel = true;
        }

        if (!validateEmail(email)) {
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            requestFocus(focusView);//focusView.requestFocus();
        } else {
            showProgress(true);
            new LoginTask().execute(email, password);
        }
    }

    @OnClick(R.id.guest)
    protected void guestLogin() {
        showProgress(true);
        new GuestLoginTask().execute();
    }

    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email);// && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty() || !isValidEmail(email)) {
            inputLayoutEmail.setError(getString(R.string.err_msg_email));
            return false;
        } else {
            inputLayoutEmail.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty() || password.length() < 6) {
            inputLayoutPassword.setError(getString(R.string.err_msg_password));
            return false;
        } else {
            inputLayoutPassword.setErrorEnabled(false);
        }

        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    class LoginTask extends AsyncTask<String, String, String>{

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected String doInBackground(final String... params) {
            result = null;
            final String action = "login";
            try {
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", "customer")
                        .add("email", params[0])
                        .add("password", params[1])
                        .build();
                Request request = new Request.Builder()
                        .url(ConfigHelper.getConfigValue(LoginActivity.this, "api_url"))
                        .post(formBody)
                        .build();
                Response response = BrowserApp.getOkHttpClient().newCall(request).execute();
                result = response.body().string();
                UserPrefs.getInstance(LoginActivity.this).setString(UserPrefs.Key.UNAME, params[0]);
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            showProgress(false);
            if(TextUtils.isEmpty(result)){
                Snackbar.make(mEmailView, "Network error", Snackbar.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject json = new JSONObject(result);
                if (!json.getBoolean("error")) {
                    AppState.getInstance(LoginActivity.this).setBoolean(AppState.Key.LOGGED_IN, true);
                    UserPrefs prefs = UserPrefs.getInstance(LoginActivity.this);
                    prefs.setString(UserPrefs.Key.ID, json.getString("id"));
                    prefs.setString(UserPrefs.Key.EMAIL, json.getString("email"));
                    prefs.setString(UserPrefs.Key.PASSWORD, mPasswordView.getText().toString());
                    prefs.setString(UserPrefs.Key.SID, json.getString("sid"));
                    prefs.setString(UserPrefs.Key.CURRENCY, json.getString("currency"));
                    prefs.setString(UserPrefs.Key.FIRST_NAME, json.getString("firstName"));
                    prefs.setString(UserPrefs.Key.LAST_NAME, json.getString("lastName"));
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    getBus().post(new LoadShoppingCartEvent());
                    getBus().post(new LoadCustomerAddressEvent(true));
                    finish();
                } else {
                    showProgress(false);
                    EditText errorView = mPasswordView;
                    TextInputLayout errorLayout = inputLayoutPassword;
                    errorView.requestFocus();
                    errorLayout.setError(json.getString("message"));
                }
            }catch (Exception e){
                Snackbar.make(mEmailView, "Internal application error", Snackbar.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    class GuestLoginTask extends AsyncTask<String, String, String>{

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected String doInBackground(final String... params) {
            result = null;
            final String action = "guest";
            try {
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", "customer")
                        .add("prf", "USD")
                        .build();
                Request request = new Request.Builder()
                        .url(ConfigHelper.getConfigValue(LoginActivity.this, "api_url"))
                        .post(formBody)
                        .build();
                Response response = BrowserApp.getOkHttpClient().newCall(request).execute();
                result = response.body().string();
                System.out.println("...........................................guest = "+result);
                UserPrefs.getInstance(LoginActivity.this).setString(UserPrefs.Key.UNAME, params[0]);
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            showProgress(false);
            if(TextUtils.isEmpty(result)){
                Snackbar.make(mEmailView, "Network error", Snackbar.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject json = new JSONObject(result);
                if (!json.getBoolean("error")) {
                    AppState.getInstance(LoginActivity.this).setBoolean(AppState.Key.LOGGED_IN, true);
                    UserPrefs prefs = UserPrefs.getInstance(LoginActivity.this);
                    AppState.getInstance(LoginActivity.this).setBoolean(AppState.Key.GUEST, true);
                    prefs.setString(UserPrefs.Key.ID, json.getString("id"));
                    prefs.setString(UserPrefs.Key.SID, json.getString("sid"));
                    prefs.setString(UserPrefs.Key.CURRENCY, json.getString("currency"));
                    prefs.setString(UserPrefs.Key.EMAIL, "guest");
                    prefs.setString(UserPrefs.Key.FIRST_NAME, "Guest");
                    prefs.setString(UserPrefs.Key.LAST_NAME, "");
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    showProgress(false);
                    EditText errorView = mPasswordView;
                    TextInputLayout errorLayout = inputLayoutPassword;
                    errorView.requestFocus();
                    errorLayout.setError(json.getString("message"));
                }
            }catch (Exception e){
                Snackbar.make(mEmailView, "Internal application error", Snackbar.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}

