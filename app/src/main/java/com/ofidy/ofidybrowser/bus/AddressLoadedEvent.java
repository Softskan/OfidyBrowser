package com.ofidy.ofidybrowser.bus;

import com.ofidy.ofidybrowser.model.Address;

import java.util.List;

public class AddressLoadedEvent {

    public final List<Address> addresses;

    public AddressLoadedEvent(List<Address> cartItems) {
        this.addresses = cartItems;
    }

}
