package com.ofidy.ofidybrowser.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.ofidy.ofidybrowser.BrowserApp;
import com.ofidy.ofidybrowser.bus.AddCartItemErrorEvent;
import com.ofidy.ofidybrowser.bus.AddEditAddressEvent;
import com.ofidy.ofidybrowser.bus.AddEditAddressStatusEvent;
import com.ofidy.ofidybrowser.bus.AddGuestAddressEvent;
import com.ofidy.ofidybrowser.bus.AddressLoadedEvent;
import com.ofidy.ofidybrowser.bus.BusProvider;
import com.ofidy.ofidybrowser.bus.CartItemRemovedEvent;
import com.ofidy.ofidybrowser.bus.CartItemUpdatedEvent;
import com.ofidy.ofidybrowser.bus.DeleteCartItemErrorEvent;
import com.ofidy.ofidybrowser.bus.DeleteCartItemEvent;
import com.ofidy.ofidybrowser.bus.EmptyCartEvent;
import com.ofidy.ofidybrowser.bus.GetOrderBillDoneEvent;
import com.ofidy.ofidybrowser.bus.GetOrderBillEvent;
import com.ofidy.ofidybrowser.bus.GetOrderCostDoneEvent;
import com.ofidy.ofidybrowser.bus.GetOrderCostErrorEvent;
import com.ofidy.ofidybrowser.bus.GetOrderCostEvent;
import com.ofidy.ofidybrowser.bus.LoadCustomerAddressEvent;
import com.ofidy.ofidybrowser.bus.LoadPaymentInfoDoneEvent;
import com.ofidy.ofidybrowser.bus.LoadPaymentInfoErrorEvent;
import com.ofidy.ofidybrowser.bus.LoadPaymentInfoEvent;
import com.ofidy.ofidybrowser.bus.LoadShoppingCartEvent;
import com.ofidy.ofidybrowser.bus.LoadStateEvent;
import com.ofidy.ofidybrowser.bus.SendPaypalVerifyEvent;
import com.ofidy.ofidybrowser.bus.SendPaystackVerifyEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenDoneEvent;
import com.ofidy.ofidybrowser.bus.SendSimplePayTokenErrorEvent;
import com.ofidy.ofidybrowser.bus.ShoppingCartLoadedEvent;
import com.ofidy.ofidybrowser.bus.StatesLoadedEvent;
import com.ofidy.ofidybrowser.bus.UpdateCartItemErrorEvent;
import com.ofidy.ofidybrowser.bus.UpdateCartItemEvent;
import com.ofidy.ofidybrowser.model.Address;
import com.ofidy.ofidybrowser.model.Cart;
import com.ofidy.ofidybrowser.model.OrderInvoice;
import com.ofidy.ofidybrowser.model.State;
import com.ofidy.ofidybrowser.pref.AppState;
import com.ofidy.ofidybrowser.pref.UserPrefs;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ari on 9/12/16.
 */
public class ServerHelper {

    private static final String TAG = "ServerHelper";
    private static final int PRODUCTS_FETCH_LIMIT = 30;
    private static String URL;

    private static final String CUSTOMER_CODE = "customer";
    private static final String PRODUCT_CODE = "product";
    private static final String CATEGORY_CODE = "category";
    private static final String CART_CODE = "cart";
    private static final String ORDER_CODE = "order";
    private static final String ENV_CODE = "env";

    private Context mContext;
    private static OfidyDB mOfidy = null;
    private static Gson gson = null;
    private OkHttpClient mOkHttpClient = null;

    public ServerHelper() {
        Crashlytics.log(Log.DEBUG, TAG, "Initializing NetworkService...");
    }

    public void start(Context context, OkHttpClient okHttpClient) {
        mOkHttpClient = okHttpClient;
        getBus().register(this);
        mOfidy = OfidyDB.getInstance(context);
        gson = new Gson();
        URL = ConfigHelper.getConfigValue(context, "api_url");
        mContext = context;
    }

    @SuppressWarnings("unused")
    public void stop() {
        getBus().unregister(this);
    }

    @Subscribe
    public void onLoadStateEvent(final LoadStateEvent event) {
        final String action = "getStates";
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("action", action)
                    .add("code", ENV_CODE)
                    .add("region", event.region)
                    .build();
            Request request = new Request.Builder()
                    .url(URL)
                    .post(formBody)
                    .build();
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                    getBus().post(new StatesLoadedEvent(null, true, e.getMessage()));
                    flushApiEventQueue(true);
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        } else {
                            String s = response.body().string();
                            if (!TextUtils.isEmpty(s)) {
                                JSONObject json = new JSONObject(s);
                                if(!json.getBoolean("error")) {
                                    ArrayList<State> states = new ArrayList<State>();
                                    JSONArray array = json.getJSONArray("data");
                                    for(int i = 0; i < array.length(); i++){
                                        JSONObject ob = array.getJSONObject(i);
                                        State p = gson.fromJson(ob.toString(), State.class);
                                        if(p != null) {
                                            states.add(p);
                                        }
                                    }
                                    getBus().post(new StatesLoadedEvent(states, false, null));
                                }
                                else
                                    getBus().post(new StatesLoadedEvent(null, true, json.getString("message")));
                            }
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                    }
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("APIPlug", "Error Occurred: " + e.getMessage());
            getBus().post(new StatesLoadedEvent(null, true, e.getMessage()));
            flushApiEventQueue(true);
        }
    }

    @Subscribe
    public void onGetOrderCostEvent(final GetOrderCostEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                System.out.println(".......................................... order called");
                final String action = "getCost";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", ORDER_CODE)
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .add("dest", event.dest)
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new GetOrderCostErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    System.out.println("....................................cost "+s);
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        List<String> ss = new ArrayList<>();
                                        List<Double> dd = new ArrayList<>();
                                        boolean pod = false;
                                        boolean simplepay = false;
                                        JSONArray ar = json.getJSONArray("data");
                                        if(ar.length() > 0){
                                            for(int i = 0; i < 3; i++){
                                                StringBuilder sj = new StringBuilder();
                                                switch (i){
                                                    case 0:
                                                        sj.append("Super Fast shipping: ");
                                                        break;
                                                    case 1:
                                                        sj.append("Fast shipping: ");
                                                        break;
                                                    case 2:
                                                        sj.append("Normal shipping: ");
                                                        break;
                                                }
                                                sj.append(BrowserApp.getInstance().getCurrency()).append(ar.getString(i));
                                                if((i == 0 || ar.getDouble(0) != ar.getDouble(i)) && ar.getDouble(i) > 0) {
                                                    ss.add(sj.toString());
                                                    dd.add(ar.getDouble(i));
                                                }
                                            }
                                            if(ss.isEmpty()){
                                                getBus().post(new GetOrderCostErrorEvent("We're sorry, we couldn't find any " +
                                                        "shipping options to your location. There may be an error connecting to " +
                                                        "our database. Please try again later."));
                                                return;
                                            }
                                            if(ar.getInt(3) == 1){
                                                pod = true;
                                            }
                                            else if(ar.getInt(3) != 1){
                                                pod = false;
                                            }
                                            if(ar.isNull(4)){
                                                simplepay = false;
                                            }
                                            else if(ar.getInt(4) == 1){
                                                simplepay = true;
                                            }
                                            else if(ar.getInt(4) != 1){
                                                simplepay = false;
                                            }
                                        }
                                        getBus().post(new GetOrderCostDoneEvent(ss, dd, pod, simplepay));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new GetOrderCostErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new GetOrderCostErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onGetOrderBillEvent(final GetOrderBillEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "getBill";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", ORDER_CODE)
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        //getBus().post(new GetOrderCostErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        String ar = json.getString("data");
                                        if(!TextUtils.isEmpty(ar)){
                                            String[] tt = ar.split(":");
                                            if(tt.length > 1 && tt[1].equals("0")) {
                                                getBus().post(new GetOrderBillDoneEvent(tt[0]));
                                            }
                                            else if(tt.length > 0)
                                                getBus().post(new GetOrderBillDoneEvent(ar));
                                            else
                                                getBus().post(new GetOrderCostErrorEvent(json.getString("message")));
                                        }
                                    }
                                    else{
                                        getBus().post(new GetOrderCostErrorEvent(json.getString("message")));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    //getBus().post(new GetOrderCostErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onLoadPaymentInfoEvent(final LoadPaymentInfoEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "paymentInfo";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", ORDER_CODE)
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .add("bill_addr", event.billAdd)
                        .add("ship_addr", event.shipAdd)
                        .add("shipmethod", String.valueOf(event.shipMethod))
                        .add("shipred", event.shipReq)
                        .add("paymethod", event.payMethod)
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new LoadPaymentInfoErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    System.out.println("............................................pay = "+s);
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        JSONObject ar = json.getJSONObject("data");
                                        OrderInvoice orderInvoice = gson.fromJson(ar.toString(), OrderInvoice.class);
                                        getBus().post(new LoadPaymentInfoDoneEvent(orderInvoice));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new LoadPaymentInfoErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new LoadPaymentInfoErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onSendPaystackVerifyEvent(final SendPaystackVerifyEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "paystackVerify";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", ORDER_CODE)
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("ref", event.ref)
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new SendSimplePayTokenErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            System.out.println(".........................................................ref server"+s);
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        String ar = json.getString("message");
                                        getBus().post(new SendSimplePayTokenDoneEvent(ar));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new SendSimplePayTokenErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }
            catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new SendSimplePayTokenErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onSendPaystackVerifyEvent(final SendPaypalVerifyEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "paypalVerify";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", ORDER_CODE)
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new SendSimplePayTokenErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        String ar = json.getString("message");
                                        if(ar.contains(":")){
                                            String[] ss = ar.split(":");
                                            if(ss.length > 1){
                                                ar = ss[0];
                                                UserPrefs.getInstance(mContext).setString(UserPrefs.Key.SID, ss[1]);
                                            }
                                        }
                                        getBus().post(new SendSimplePayTokenDoneEvent(ar));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new SendSimplePayTokenErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }
            catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new SendSimplePayTokenErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onLoadCustomerAddressEvent(final LoadCustomerAddressEvent event) {
        final String action = "getAddress";
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(URL).newBuilder();
            urlBuilder.addQueryParameter("action", action);
            urlBuilder.addQueryParameter("code", CUSTOMER_CODE);
            urlBuilder.addQueryParameter("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID));
            String url = urlBuilder.build().toString();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                    flushApiEventQueue(true);
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (response.isSuccessful()) {
                        List<Address> products = new ArrayList<>();
                        String s = response.body().string();
                        if(!TextUtils.isEmpty(s)) {
                            try {
                                System.out.println("....................................................address = "+s);
                                JSONObject json = new JSONObject(s);
                                if(!json.getBoolean("error")){
                                    mOfidy.emptyAddress();
                                    JSONArray array = json.getJSONArray("data");
                                    for(int i = 0; i < array.length(); i++){
                                        JSONObject ob = array.getJSONObject(i);
                                        Address p = gson.fromJson(ob.toString(), Address.class);
                                        if(p != null) {
                                            mOfidy.insertAddress(p);
                                            products.add(p);
                                        }
                                    }
                                    getBus().post(new AddressLoadedEvent(products));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("APIPlug", "Error Occurred: " + e.getMessage());
            flushApiEventQueue(true);
        }
    }

    /**
     * Adds new address or updates existing address
     */
    @Subscribe
    public void onAddEditAddressEventEvent(final AddEditAddressEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                String action = "addAddress";
                if(event.edit)
                    action = "editAddress";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", CUSTOMER_CODE)
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("addr1", event.addrLine1)
                        .add("addr2", event.addrLine2)
                        .add("addr3", event.area)
                        .add("addrdesc", event.desc)
                        .add("city", event.city)
                        .add("state", event.state)
                        .add("country", event.country)
                        .add("postcode", event.postcode)
                        .add("deladdr", event.del)
                        .add("coraddr", event.cor)
                        .add("addrtype", String.valueOf(event.addressType))
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new AddEditAddressStatusEvent(e.getMessage(), false));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            System.out.println(".....................................................address ish = "+s);
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    String id = null;
                                    if(!json.getBoolean("error")){
                                        mOfidy.emptyAddress();
                                        List<Address> products = new ArrayList<>();
                                        JSONArray array = json.getJSONArray("data");
                                        for(int i = 0; i < array.length(); i++){
                                            JSONObject ob = array.getJSONObject(i);
                                            Address p = gson.fromJson(ob.toString(), Address.class);
                                            id = ob.getString("id");
                                            if(p != null && !TextUtils.isEmpty(p.getAddressLine1())) {
                                                mOfidy.insertAddress(p);
                                                products.add(p);
                                            }
                                        }
                                        if(!AppState.getInstance(mContext).getBoolean(AppState.Key.GUEST)) {
                                            getBus().post(new AddressLoadedEvent(products));
                                            getBus().post(new AddEditAddressStatusEvent("Address added successfully", true));
                                            getBus().post(new LoadCustomerAddressEvent(true));
                                        }
                                        else if(!TextUtils.isEmpty(id)){
                                            Address ad = new Address();
                                            ad.setAddressLine1(event.addrLine1);
                                            ad.setAddressLine2(event.addrLine2);
                                            ad.setAddressLine3(event.area);
                                            ad.setCity(event.city);
                                            ad.setState(event.state);
                                            ad.setCountry(event.country);
                                            ad.setPostcode(event.postcode);
                                            ad.setId(id);
                                            mOfidy.insertAddress(ad);
                                            getBus().post(new AddressLoadedEvent(products));
                                            getBus().post(new AddEditAddressStatusEvent("Address added successfully", true));
                                            getBus().post(new LoadCustomerAddressEvent(true));
                                        }
                                    }
                                    else{
                                        getBus().post(new AddEditAddressStatusEvent(json.getString("message"), false));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new AddEditAddressStatusEvent(e.getMessage(), false));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new AddEditAddressStatusEvent(e.getMessage(), false));
                flushApiEventQueue(true);
            }
    }

    /**
     * Adds address to server by guest
     */
    @Subscribe
    public void onAddGuestAddressEventEvent(final AddGuestAddressEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                String action = "addGuestAddress";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", CUSTOMER_CODE)
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("addr1", event.addrLine1)
                        .add("addr2", event.addrLine2)
                        .add("addr3", event.area)
                        .add("addrdesc", event.desc)
                        .add("city", event.city)
                        .add("state", event.state)
                        .add("country", event.country)
                        .add("postcode", event.postcode)
                        .add("addrtype", String.valueOf(event.addressType))
                        .add("email", event.email)
                        .add("fname", event.fname)
                        .add("lname", event.lname)
                        .add("phone", event.phone)
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new AddEditAddressStatusEvent(e.getMessage(), false));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            System.out.println(".....................................................guest address ish = "+s);
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    String id = null;
                                    if(!json.getBoolean("error")){
                                        mOfidy.emptyAddress();
                                        List<Address> products = new ArrayList<>();
                                        JSONArray array = json.getJSONArray("data");
                                        for(int i = 0; i < array.length(); i++){
                                            JSONObject ob = array.getJSONObject(i);
                                            Address p = gson.fromJson(ob.toString(), Address.class);
                                            id = ob.getString("id");
                                            if(p != null && !TextUtils.isEmpty(p.getAddressLine1())) {
                                                mOfidy.insertAddress(p);
                                                products.add(p);
                                            }
                                        }
                                        if(!AppState.getInstance(mContext).getBoolean(AppState.Key.GUEST)) {
                                            getBus().post(new AddressLoadedEvent(products));
                                            getBus().post(new AddEditAddressStatusEvent("Address added successfully", true));
                                            getBus().post(new LoadCustomerAddressEvent(true));
                                        }
                                        else if(!TextUtils.isEmpty(id)){
                                            Address ad = new Address();
                                            ad.setAddressLine1(event.addrLine1);
                                            ad.setAddressLine2(event.addrLine2);
                                            ad.setAddressLine3(event.area);
                                            ad.setCity(event.city);
                                            ad.setState(event.state);
                                            ad.setCountry(event.country);
                                            ad.setPostcode(event.postcode);
                                            ad.setId(id);
                                            mOfidy.insertAddress(ad);
                                            getBus().post(new AddressLoadedEvent(products));
                                            getBus().post(new AddEditAddressStatusEvent("Address added successfully", true));
                                            getBus().post(new LoadCustomerAddressEvent(true));
                                        }
                                    }
                                    else{
                                        getBus().post(new AddEditAddressStatusEvent(json.getString("message"), false));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new AddEditAddressStatusEvent(e.getMessage(), false));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new AddEditAddressStatusEvent(e.getMessage(), false));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onUpdateCartItemEvent(final UpdateCartItemEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "edit";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", CART_CODE)
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .add("tid", event.tid)
                        .add("promo", event.promoCode)
                        .add("qty", String.valueOf(event.quantity))
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new UpdateCartItemErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            System.out.println("............................................s "+s);
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        getBus().post(new CartItemUpdatedEvent());
                                        BrowserApp.cartItems = new ArrayList<>();
                                        JSONArray array = json.getJSONArray("data");
                                        for(int i = 0; i < array.length(); i++){
                                            JSONObject ob = array.getJSONObject(i);
                                            Cart p = gson.fromJson(ob.toString(), Cart.class);
                                            if(p != null) {
                                                BrowserApp.cartItems.add(p);
                                            }
                                        }
                                        getBus().post(new ShoppingCartLoadedEvent(BrowserApp.cartItems));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new UpdateCartItemErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new UpdateCartItemErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onDeleteCartItemEvent(final DeleteCartItemEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "delete";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", CART_CODE)
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .add("tid", event.tid)
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new DeleteCartItemErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            System.out.println("............................................delete cart "+s);
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        getBus().post(new CartItemRemovedEvent());
                                        BrowserApp.cartItems = new ArrayList<>();
                                        JSONArray array = json.getJSONArray("data");
                                        for(int i = 0; i < array.length(); i++){
                                            JSONObject ob = array.getJSONObject(i);
                                            Cart p = gson.fromJson(ob.toString(), Cart.class);
                                            if(p != null) {
                                                BrowserApp.cartItems.add(p);
                                            }
                                        }
                                        getBus().post(new ShoppingCartLoadedEvent(BrowserApp.cartItems));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new DeleteCartItemErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new DeleteCartItemErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onEmptyCartEvent(final EmptyCartEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "empty";
                RequestBody formBody = new FormBody.Builder()
                        .add("action", action)
                        .add("code", CART_CODE)
                        .add("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID))
                        .add("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID))
                        .add("prf", BrowserApp.getInstance().getCurrency())
                        .build();
                Request request = new Request.Builder()
                        .url(URL)
                        .post(formBody)
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        getBus().post(new DeleteCartItemErrorEvent(e.getMessage()));
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            System.out.println("............................................empty "+s);
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        getBus().post(new CartItemRemovedEvent());
                                        BrowserApp.cartItems = new ArrayList<>();
                                        JSONArray array = json.getJSONArray("data");
                                        for(int i = 0; i < array.length(); i++){
                                            JSONObject ob = array.getJSONObject(i);
                                            Cart p = gson.fromJson(ob.toString(), Cart.class);
                                            if(p != null) {
                                                BrowserApp.cartItems.add(p);
                                            }
                                        }
                                        getBus().post(new ShoppingCartLoadedEvent(BrowserApp.cartItems));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new DeleteCartItemErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new DeleteCartItemErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    @Subscribe
    public void onLoadShoppingCartEvent(final LoadShoppingCartEvent event) {
        if(AppState.getInstance(mContext).getBoolean(AppState.Key.LOGGED_IN))
            try {
                final String action = "get";
                HttpUrl.Builder urlBuilder = HttpUrl.parse(URL).newBuilder();
                urlBuilder.addQueryParameter("action", action);
                urlBuilder.addQueryParameter("code", CART_CODE);
                urlBuilder.addQueryParameter("id", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.ID));
                urlBuilder.addQueryParameter("sid", UserPrefs.getInstance(mContext).getString(UserPrefs.Key.SID));
                urlBuilder.addQueryParameter("prf", BrowserApp.getInstance().getCurrency());
                String url = urlBuilder.build().toString();
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                        flushApiEventQueue(true);
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String s = response.body().string();
                            if(!TextUtils.isEmpty(s)) {
                                try {
                                    System.out.println(".......................................... cart "+s);
                                    JSONObject json = new JSONObject(s);
                                    if(!json.getBoolean("error")){
                                        BrowserApp.cartItems = new ArrayList<>();
                                        JSONArray array = json.getJSONArray("data");
                                        for(int i = 0; i < array.length(); i++){
                                            JSONObject ob = array.getJSONObject(i);
                                            Cart p = gson.fromJson(ob.toString(), Cart.class);
                                            if(p != null) {
                                                BrowserApp.cartItems.add(p);
                                            }
                                        }
                                        getBus().post(new ShoppingCartLoadedEvent(BrowserApp.cartItems));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    getBus().post(new AddCartItemErrorEvent(e.getMessage()));
                                }
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.d("APIPlug", "Error Occurred: " + e.getMessage());
                getBus().post(new AddCartItemErrorEvent(e.getMessage()));
                flushApiEventQueue(true);
            }
    }

    private Bus getBus() {
        return BusProvider.getBus();
    }

    private void flushApiEventQueue(boolean loadCachedData) {
        Bus bus = getBus();
        boolean isQueueEmpty;
    }
}
