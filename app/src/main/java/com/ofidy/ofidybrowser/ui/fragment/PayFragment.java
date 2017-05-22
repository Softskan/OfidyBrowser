package com.ofidy.ofidybrowser.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.bus.GetOrderCostDoneEvent;
import com.ofidy.ofidybrowser.bus.GetOrderCostErrorEvent;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.ofidy.ofidybrowser.ui.CheckoutActivity;
import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PayFragment extends BaseFragment {

    @Bind(R.id.paystack_payment)
    RadioButton mPaystackPayment;
    @Bind(R.id.paypal_payment)
    RadioButton mPaypalPayment;
    @Bind(R.id.pod_payment)
    RadioButton mPODPayment;
    @Bind(R.id.simplepay_layout)
    View mSimplepayLayout;
    @Bind(R.id.paypal_layout)
    View mPaypalLayout;
    @Bind(R.id.pod_layout)
    View mPODLayout;

    public PayFragment() {
        // Required empty public constructor
    }

    public static PayFragment newInstance() {
        PayFragment fragment = new PayFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_payment, container, false);
        ButterKnife.bind(this, view);
        if(((CheckoutActivity)getActivity()).paymentMethodSelected > -1)
            switch (((CheckoutActivity)getActivity()).paymentMethodSelected){
                case 0:
                    mPaypalPayment.setChecked(true);
                    mPaystackPayment.setChecked(false);
                    mPODPayment.setChecked(false);
                    break;
                case 1:
                    mPaystackPayment.setChecked(true);
                    mPaypalPayment.setChecked(false);
                    mPODPayment.setChecked(false);
                    break;
                case 2:
                    mPODPayment.setChecked(true);
                    mPaystackPayment.setChecked(false);
                    mPaypalPayment.setChecked(false);
                    break;
            }
        if( UserPrefs.getInstance(getContext()).getString(UserPrefs.Key.CURRENCY).equals("NGN")){
            mPaypalPayment.setVisibility(View.GONE);
        }
        else{
            mSimplepayLayout.setVisibility(View.GONE);
        }
        return view;
    }

    @OnClick(R.id.next)
    protected void onNextClicked(){
        ((CheckoutActivity)getActivity()).changeTab(2);
    }

    @OnClick({R.id.paystack_payment, R.id.paypal_payment, R.id.pod_payment})
    public void radioGroupUpdate() {
        if(mPaypalPayment.isChecked()) {
            ((CheckoutActivity) getActivity()).paymentMethodSelected = 0;
            mPaystackPayment.setChecked(false);
            mPODPayment.setChecked(false);
        }
        else if(mPaystackPayment.isChecked()) {
            ((CheckoutActivity) getActivity()).paymentMethodSelected = 1;
            mPaypalPayment.setChecked(false);
            mPODPayment.setChecked(false);
        }
        else if(mPODPayment.isChecked()) {
            ((CheckoutActivity) getActivity()).paymentMethodSelected = 2;
            mPaystackPayment.setChecked(false);
            mPaypalPayment.setChecked(false);
        }
        else
            ((CheckoutActivity)getActivity()).paymentMethodSelected = -1;
    }

    @Subscribe
    public void onGetOrderCostDoneEvent(final GetOrderCostDoneEvent event) {
        if(!event.pod) {
            mPODLayout.setVisibility(View.GONE);
            mPODPayment.setChecked(false);
        }
        else{
            mPODLayout.setVisibility(View.VISIBLE);
        }
//        if(!event.simplepay) {
//            mSimplepayLayout.setVisibility(View.GONE);
//            mPaystackPayment.setChecked(false);
//        }
//        else
            mSimplepayLayout.setVisibility(View.VISIBLE);
    }

    @Subscribe
    public void onGetOrderCostErrorEvent(final GetOrderCostErrorEvent event) {
        mPODPayment.setEnabled(true);
        mPaystackPayment.setEnabled(true);
    }

}
