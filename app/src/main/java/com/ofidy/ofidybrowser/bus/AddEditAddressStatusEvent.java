package com.ofidy.ofidybrowser.bus;

public class AddEditAddressStatusEvent {

    public final String message;
    public final boolean success;

    public AddEditAddressStatusEvent(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

}
