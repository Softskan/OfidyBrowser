package com.ofidy.ofidybrowser.bus;

import com.ofidy.ofidybrowser.model.Cart;

import java.util.List;

public class ShoppingCartLoadedEvent {

    public final List<Cart> cartItems;

    public ShoppingCartLoadedEvent(List<Cart> cartItems) {
        this.cartItems = cartItems;
    }

}
