package com.ofidy.ofidybrowser.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.bus.SendPaypalVerifyEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenDoneEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenErrorEvent;
import com.ofidy.ofidybrowser.model.OrderInvoice;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;

public class PaypalActivity extends BaseActivity {

    public static final int MY_SCAN_REQUEST_CODE = 1;
    public static final String US = "93KXD9JD8JVZA";
    public static final String UK = "DGMYBJ9GE6ZVA";

    ProgressDialog dialog;
    private OrderInvoice orderInvoice;
    DecimalFormat precision = new DecimalFormat("0.00");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_paypal);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Bundle bundle = getIntent().getExtras();
        orderInvoice = bundle.getParcelable(BundleKeys.ORDER);
        if (orderInvoice == null)
            finish();

        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new MyBrowser());
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setLoadsImagesAutomatically(true);
        String postData = "cmd=_xclick" +
                "&business=" + (orderInvoice.getCurrency().equals("USD") ? US : UK) +
                "&lc=GB" +
                "&item_name\" value=\"All items in cart\">\n" +
                "&amount=" + precision.format(orderInvoice.getFullBill()) +
                "&currency_code=" + orderInvoice.getCurrency() +
                "&button_subtype=services" +
                "&no_note=1" +
                "&no_shipping=1" +
                "&shipping=" + precision.format(orderInvoice.getShipBill()) +
                "&bn=PP-BuyNowBF:btn_buynowCC_LG.gif:NonHosted" +
                "&custom=" +orderInvoice.getInvoiceId() + "|" + orderInvoice.getFullAddress()+ "|" + orderInvoice.getFullAddress() + orderInvoice.getShipBill();
        String url = "https://www.paypal.com/cgi-bin/webscr";
        myWebView.postUrl(url, postData.getBytes());
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Subscribe
    public void onSendSimplePayTokenDoneEvent(final SendSimplePayTokenDoneEvent event) {
        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
        Snackbar.make(toolbar, event.message, Snackbar.LENGTH_LONG).show();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("message",event.message);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    @Subscribe
    public void onLoadSendSimplePayTokenErrorEvent(final SendSimplePayTokenErrorEvent event) {
        //mRetry.setVisibility(View.VISIBLE);
        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
        Snackbar.make(toolbar, event.message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed(){
        getBus().post(new SendPaypalVerifyEvent());
        super.onBackPressed();
    }
}
