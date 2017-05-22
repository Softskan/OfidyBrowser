package com.ofidy.ofidybrowser.bus;

public class InterntStatusChangedEvent {

    public final boolean connected;

    public InterntStatusChangedEvent(boolean connected) {
        this.connected = connected;
    }

}
