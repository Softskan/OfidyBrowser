package com.ofidy.ofidybrowser.bus;

public class SendSimplePayTokenDoneEvent {

    public final String message;

    public SendSimplePayTokenDoneEvent(String dest) {
        this.message = dest;
    }

}
