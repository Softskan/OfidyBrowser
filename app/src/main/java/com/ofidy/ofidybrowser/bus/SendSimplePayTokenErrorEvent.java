package com.ofidy.ofidybrowser.bus;

public class SendSimplePayTokenErrorEvent {

    public final String message;

    public SendSimplePayTokenErrorEvent(String dest) {
        this.message = dest;
    }

}
