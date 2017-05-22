package com.ofidy.ofidybrowser.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

import com.cardform.CardForm;
import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.bus.SendPaystackVerifyEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenDoneEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenErrorEvent;
import com.ofidy.ofidybrowser.model.OrderInvoice;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;

import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.Transaction;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;

public class PaystackActivity extends BaseActivity {

    public static final int MY_SCAN_REQUEST_CODE = 1;

    ProgressDialog dialog;
    private Charge charge;
    private Transaction transaction = null;
    private CardForm cardForm;
    private OrderInvoice orderInvoice;
    DecimalFormat precision = new DecimalFormat("0.00");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_paystack);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Bundle bundle = getIntent().getExtras();
        orderInvoice = bundle.getParcelable(BundleKeys.ORDER);
        if (orderInvoice == null)
            finish();

        //initialize sdk
        PaystackSdk.initialize(getApplicationContext());

        cardForm = (CardForm) findViewById(R.id.card_form);
        cardForm.setAmount(String.format(orderInvoice.getCurrency()+"%s", precision.format(orderInvoice.getFullBill())));
        //cardForm.
        cardForm.setPayBtnClickListner(card1 -> {
            if (card1 != null) {
                try {
                    if(transaction != null){
                        dialog = new ProgressDialog(PaystackActivity.this);
                        dialog.setOwnerActivity(PaystackActivity.this);
                        dialog.setMessage("Verifying payment details....");

                        dialog.show();

                        getBus().post(new SendPaystackVerifyEvent(transaction.getReference()));

                    }
                    else {
                        dialog = new ProgressDialog(PaystackActivity.this);
                        dialog.setOwnerActivity(PaystackActivity.this);
                        dialog.setMessage("Performing transaction... please wait");
                        dialog.setCancelable(false);
                        dialog.setCanceledOnTouchOutside(false);

                        dialog.show();

                        String fullbill = precision.format(orderInvoice.getFullBill());
                        fullbill = fullbill.replace(".", "");
                        charge = new Charge();
                        charge.setCard(new Card.Builder(card1.getNumber(), card1.getExpMonth(),
                                card1.getExpYear(), card1.getCVC()).build());
                        charge.setEmail(UserPrefs.getInstance(this).getString(UserPrefs.Key.EMAIL));
                        charge.setCurrency(orderInvoice.getCurrency());
                        charge.putMetadata("invoiceId", orderInvoice.getInvoiceId());
                        //charge.putCustomField("Paid Via", "Android SDK");
                        charge.putMetadata("address", orderInvoice.getFullAddress());
                        charge.putMetadata("country", orderInvoice.getCountrySp());
                        charge.setAmount(Integer.parseInt(fullbill));
                        //charge.setReference(orderInvoice.getInvoiceId());
                        PaystackSdk.chargeCard(PaystackActivity.this, charge, new Paystack.TransactionCallback() {
                            @Override
                            public void onSuccess(Transaction transaction1) {
                                if ((dialog != null) && dialog.isShowing()) {
                                    //dialog.dismiss();
                                    dialog.setMessage("Verifying payment details....");
                                }
                                PaystackActivity.this.transaction = transaction1;
                                System.out.println("..............................................trans = " + transaction1.getReference());
                                getBus().post(new SendPaystackVerifyEvent(transaction1.getReference()));
                            }

                            @Override
                            public void beforeValidate(Transaction transaction1) {
                                // This is called only before requesting OTP
                                // Save reference so you may send to server. If
                                // error occurs with OTP, you should still verify on server
                            }

                            @Override
                            public void onError(Throwable error, Transaction transaction) {
                                if ((dialog != null) && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                                if (PaystackActivity.this.transaction == null) {
                                    Toast.makeText(PaystackActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                                    //mTextReference.setText(String.format("Error: %s", error.getMessage()));
                                } else {
                                    Toast.makeText(PaystackActivity.this, transaction.getReference() + " concluded with error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    //mTextCard.setText(String.format("%s  concluded with error: %s", transaction.reference, error.getMessage()));
                                }
                                transaction = null;
                            }

                        });
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_SCAN_REQUEST_CODE) {
            String resultDisplayStr;
//            if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
//                CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
//                cardForm.setDetails(scanResult);
//            } else {
//                Toast.makeText(this, "Scan was canceled.", Toast.LENGTH_SHORT).show();
//            }
        }
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
}
