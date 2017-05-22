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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.ofidy.ofidybrowser.BrowserApp;
import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.constant.Currency;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.ofidy.ofidybrowser.utils.ConfigHelper;

import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends BaseActivity{

    // UI references.
    @Bind(R.id.email)
    AutoCompleteTextView mEmailView;
    @Bind(R.id.password)
    EditText mPasswordView;
    @Bind(R.id.username)
    EditText mUsernameView;
    @Bind(R.id.first_name)
    EditText mFirstNameView;
    @Bind(R.id.last_name)
    EditText mLastNameView;
    @Bind(R.id.mobile)
    EditText mMobileView;
    @Bind(R.id.terms)
    CheckBox mTermsView;
    @Bind(R.id.email_layout)
    TextInputLayout inputLayoutEmail;
    @Bind(R.id.password_layout)
    TextInputLayout inputLayoutPassword;
    @Bind(R.id.lname_layout)
    TextInputLayout inputLayoutLastName;
    @Bind(R.id.fname_layout)
    TextInputLayout inputLayoutFirstName;
    @Bind(R.id.uname_layout)
    TextInputLayout inputLayoutUsername;
    @Bind(R.id.mobile_layout)
    TextInputLayout inputLayoutMobile;
    @Bind(R.id.login_progress)
    View mProgressView;
    @Bind(R.id.login_form)
    View mLoginFormView;
    @Bind(R.id.gender_male)
    RadioButton mMale;
    @Bind(R.id.gender_female)
    RadioButton mFemale;
    @Bind(R.id.currency)
    Spinner mCurrency;
    private boolean fromActivity;
    String gender = "M";
    private ArrayList<Currency> currencies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if(getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            fromActivity = bundle.getBoolean(BundleKeys.FROM_ACTIVITY);
        }
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptRegister();
                return true;
            }
            return false;
        });
        mMale.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
                gender = "M";
        });
        mFemale.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
                gender = "F";
        });

        currencies = new ArrayList<>();
            String[] curs = getResources().getStringArray(R.array.currency);
            String[] curs_val = getResources().getStringArray(R.array.currency_value);
            int i = 0;
            for(String s: curs){
                currencies.add(new Currency("0", s, curs_val[i]));
                i++;
            }

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @OnClick(R.id.sign_in)
    protected void haveAccount() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @OnClick(R.id.sign_up_button)
    protected void attemptRegister() {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String fname = mFirstNameView.getText().toString();
        String lname = mLastNameView.getText().toString();
        String uname = mUsernameView.getText().toString();
        String mobile = mMobileView.getText().toString();
        int cur = mCurrency.getSelectedItemPosition();
        String curr = "USD";
        if(currencies != null && !currencies.isEmpty()){
            curr = currencies.get(cur).getCode();
        }

        boolean cancel = false;
        View focusView = null;

        if(!mTermsView.isChecked()){
            focusView = mTermsView;
            cancel = true;
            Snackbar.make(mFirstNameView, "You have to accept our terms and conditions to continue", Snackbar.LENGTH_SHORT).show();
        }
        if (!validatePassword(password)) {
            focusView = mPasswordView;
            cancel = true;
        }
        if (!validateEmail(email)) {
            focusView = mEmailView;
            cancel = true;
        }
        if (!validateEmail(email)) {
            focusView = mEmailView;
            cancel = true;
        }
        if(TextUtils.isEmpty(uname)){
            focusView = mUsernameView;
            inputLayoutUsername.setError("You must enter a username");
            cancel = true;
        }
        if(TextUtils.isEmpty(fname)){
            focusView = mFirstNameView;
            inputLayoutFirstName.setError("Please enter your first name");
            cancel = true;
        }
        if(TextUtils.isEmpty(lname)){
            focusView = mLastNameView;
            inputLayoutLastName.setError("Please enter your last name");
            cancel = true;
        }
        if (cancel) {
            requestFocus(focusView);
        } else {
            showProgress(true);
            sendRegisterRequest(email, password, fname, lname, uname, gender, mobile, curr);
        }
    }

    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
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
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void sendRegisterRequest(String email, String password,  String fname, String lname, String uname,
                                     String sex, String mobile, String currency) {
        new RegisterTask().execute(email, password, fname, lname, uname, "ofidy", "1", "1" ,"1990", sex,
                "", mobile, currency);
    }

    class RegisterTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected String doInBackground(final String... params) {
            String result = null;
            final String action = "register";
            try {
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", "customer")
                        .add("email", params[0])
                        .add("pwd", params[1])
                        .add("fname", params[2])
                        .add("lname", params[3])
                        .add("uname", params[4])
                        .add("memword", params[5])
                        .add("day", params[6])
                        .add("month", params[7])
                        .add("year", params[8])
                        .add("gender", params[9])
                        .add("telephone", params[10])
                        .add("mobile", params[11])
                        .add("prf", params[12])
                        .build();
                Request request = new Request.Builder()
                        .url(ConfigHelper.getConfigValue(RegisterActivity.this, "api_url"))
                        .post(formBody)
                        .build();
                Response response = BrowserApp.getOkHttpClient().newCall(request).execute();
                result = response.body().string();
                UserPrefs.getInstance(RegisterActivity.this).setString(UserPrefs.Key.UNAME, params[0]);
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
                    //AppState.getInstance(LoginActivity.this).setBoolean(AppState.Key.LOGGED_IN, true);
                    UserPrefs prefs = UserPrefs.getInstance(RegisterActivity.this);
                    prefs.setString(UserPrefs.Key.ID, json.getString("id"));
                    prefs.setString(UserPrefs.Key.EMAIL, json.getString("email"));
                    prefs.setString(UserPrefs.Key.PASSWORD, mPasswordView.getText().toString());
                    prefs.setString(UserPrefs.Key.SID, json.getString("sid"));
                    prefs.setString(UserPrefs.Key.CURRENCY, json.getString("currency"));
                    prefs.setString(UserPrefs.Key.FIRST_NAME, json.getString("firstName"));
                    prefs.setString(UserPrefs.Key.LAST_NAME, json.getString("lastName"));
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
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

