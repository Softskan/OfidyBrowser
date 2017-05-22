package com.ofidy.ofidybrowser.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenDoneEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenErrorEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenEvent;
import com.ofidy.ofidybrowser.model.OrderInvoice;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.squareup.otto.Subscribe;

import org.json.JSONException;

import java.text.DecimalFormat;

import butterknife.Bind;
import butterknife.OnClick;
import mehdi.sakout.fancybuttons.FancyButton;

public class PayActivity extends BaseActivity {

    private static final int PAYSTACK_GATEWAY = 2;
    private static final int PAYPAL_GATEWAY = 1;
    private OrderInvoice orderInvoice;
    private ProgressDialog progress;

    @Bind(R.id.payment_method)
    TextView mText;
    @Bind(R.id.sub_total)
    TextView mSubTotal;
    @Bind(R.id.total)
    TextView mTotal;
    @Bind(R.id.shipping_cost)
    TextView mShipping;
    @Bind(R.id.cust_name)
    TextView mCustName;
    @Bind(R.id.shipping_address)
    TextView mShippingAddress;
    @Bind(R.id.billing_address)
    TextView mBillingAddress;
    @Bind(R.id.pay)
    FancyButton mPay;
    @Bind(R.id.retry)
    FancyButton mRetry;
    private String fullbill;
    private String token;
    private boolean paypalLive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_payment);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        orderInvoice = bundle.getParcelable(BundleKeys.ORDER);
        if(orderInvoice ==  null)
            finish();
        DecimalFormat precision = new DecimalFormat("0.00");
        mText.setText("Payment method is "+ orderInvoice.getPayMethod());
        mShipping.setText("Shipping = " + orderInvoice.getCurrency() + precision.format(orderInvoice.getShipBill()));
        mSubTotal.setText("Subtotal = " + orderInvoice.getCurrency() + precision.format(orderInvoice.getProductsBill()));
        mTotal.setText("Total = " + orderInvoice.getCurrency() + precision.format(orderInvoice.getFullBill()));
        mShippingAddress.setText(orderInvoice.getFullAddress());
        mCustName.setText(UserPrefs.getInstance(this).getString(UserPrefs.Key.FIRST_NAME) + " "+ UserPrefs.getInstance(this).getString(UserPrefs.Key.LAST_NAME));

    }

    @OnClick(R.id.pay)
    protected void onPay(){
        if(orderInvoice.getPayMethod().equals("PayPal"))
            payPay();
        else
            paystack();
    }

    @OnClick(R.id.retry)
    protected void onRetry(){
        if(!TextUtils.isEmpty(token)) {
            mRetry.setVisibility(View.GONE);
            progress.show();
            getBus().post(new SendSimplePayTokenEvent(token, fullbill));
        }
    }

    private void paystack(){
        Intent intent = new Intent(this, PaystackActivity.class);
        intent.putExtra(BundleKeys.ORDER, orderInvoice);
        startActivityForResult(intent, PAYSTACK_GATEWAY);
    }

    private void payPay(){
        Intent intent = new Intent(this, PaypalActivity.class);
        intent.putExtra(BundleKeys.ORDER, orderInvoice);
        startActivityForResult(intent, PAYPAL_GATEWAY);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == PAYPAL_GATEWAY) {

            }
            else{
                mPay.setVisibility(View.GONE);
                mRetry.setVisibility(View.GONE);
                //mText.setText();
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            Log.i("paymentExample", "The user canceled.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Subscribe
    public void onSendSimplePayTokenDoneEvent(final SendSimplePayTokenDoneEvent event) {
        if(progress != null && progress.isShowing())
            progress.dismiss();
        mPay.setVisibility(View.GONE);
        mRetry.setVisibility(View.GONE);
        mText.setText(event.message);
    }

    @Subscribe
    public void onLoadSendSimplePayTokenErrorEvent(final SendSimplePayTokenErrorEvent event) {
        mRetry.setVisibility(View.VISIBLE);
        if(progress != null  && progress.isShowing())
            progress.dismiss();
        Snackbar.make(mText, event.message, Snackbar.LENGTH_LONG).show();
    }
}
