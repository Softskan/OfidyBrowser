package com.ofidy.ofidybrowser.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.ofidy.ofidybrowser.BrowserApp;
import com.ofidy.ofidybrowser.R;
import com.ofidy.ofidybrowser.bus.CartItemRemovedEvent;
import com.ofidy.ofidybrowser.bus.CartItemUpdatedEvent;
import com.ofidy.ofidybrowser.bus.DeleteCartItemErrorEvent;
import com.ofidy.ofidybrowser.bus.DeleteCartItemEvent;
import com.ofidy.ofidybrowser.bus.EmptyCartEvent;
import com.ofidy.ofidybrowser.bus.LoadShoppingCartEvent;
import com.ofidy.ofidybrowser.bus.ShoppingCartLoadedEvent;
import com.ofidy.ofidybrowser.bus.UpdateCartItemErrorEvent;
import com.ofidy.ofidybrowser.bus.UpdateCartItemEvent;
import com.ofidy.ofidybrowser.model.Cart;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

import static com.ofidy.ofidybrowser.bus.BusProvider.getBus;

public class CartActivity extends BaseActivity {

    @Bind(R.id.list)
    RecyclerView mPostList;
    @Bind(R.id.total)
    TextView mTotal;
    private CartAdapter adapter;
    private List<Cart> mCarts;
    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.activity_cart);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCarts = BrowserApp.cartItems;

        adapter = new CartAdapter(this, mCarts, R.layout.cart_list_item2, view -> {
            int pos = mPostList.getChildLayoutPosition(view);
        });
        mPostList.setAdapter(adapter);
        mPostList.setLayoutManager(new LinearLayoutManager(this));
        mPostList.setItemAnimator(new DefaultItemAnimator());
        setTotal();
        getBus().post(new LoadShoppingCartEvent());
    }

    private void setTotal(){
        double t = 0;
        for(Cart c : mCarts){
            t += c.getTotalPrice();
        }
        DecimalFormat precision = new DecimalFormat("0.00");
        mTotal.setText(precision.format(t));
    }

    @OnClick(R.id.checkout)
    protected void onCheckoutClicked() {
        double t = 0;
        for(Cart c : mCarts){
            t += c.getTotalPrice();
        }
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(BundleKeys.CART_TOTAL, t);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cart, menu);
        inflateCommonMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_clear){
            final AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Clear shopping cart?")
                    .setMessage("Clearing the shopping cart will permanently remove all items from cart")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        getBus().post(new EmptyCartEvent());
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .create();
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onUpdateCartItemEvent(final UpdateCartItemEvent event) {
        progress = ProgressDialog.show(this, "Updating....",
                "Please wait", true);
    }

    @Subscribe
    public void onDeleteCartItemEvent(final DeleteCartItemEvent event) {
        progress = ProgressDialog.show(this, "Deleting....",
                "Please wait", true);
    }

    @Subscribe
    public void onDeleteCartItemEvent(final EmptyCartEvent event) {
        progress = ProgressDialog.show(this, "Updating....",
                "Please wait", true);
    }

    @Subscribe
    public void onCartsLoadedEvent(ShoppingCartLoadedEvent event) {
        if(progress != null)
            progress.dismiss();
        mCarts = BrowserApp.cartItems;
        adapter.notifyDataSetChanged();
        setTotal();
    }

    @Subscribe
    public void onCartsLoadedEvent(CartItemUpdatedEvent event) {
        if(progress != null)
            progress.dismiss();
        mCarts = BrowserApp.cartItems;
        adapter.notifyDataSetChanged();
        setTotal();
        Snackbar.make(mTotal, "Cart item updated successfully", Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onCartsLoadedEvent(CartItemRemovedEvent event) {
        if(progress != null)
            progress.dismiss();
        mCarts = BrowserApp.cartItems;
        adapter.notifyDataSetChanged();
        setTotal();
        Snackbar.make(mTotal, "Cart item(s) removed successfully", Snackbar.LENGTH_SHORT).show();
        if(mCarts.isEmpty())
            new Handler().postDelayed(
                    () -> finish(), 1000);
    }

    @Subscribe
    public void onUpdateCartItemErrorEvent(UpdateCartItemErrorEvent event) {
        if(progress != null)
            progress.dismiss();
        Snackbar.make(mTotal, event.message, Snackbar.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onDeleteCartItemErrorEvent(DeleteCartItemErrorEvent event) {
        if(progress != null)
            progress.dismiss();
        Snackbar.make(mTotal, event.message, Snackbar.LENGTH_SHORT).show();
    }

}
