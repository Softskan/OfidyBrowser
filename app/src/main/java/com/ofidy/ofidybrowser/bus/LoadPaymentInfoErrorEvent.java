package com.ofidy.ofidybrowser.bus;

public class LoadPaymentInfoErrorEvent {

    public final String message;

    public LoadPaymentInfoErrorEvent(String dest) {
        this.message = dest;
    }

}
