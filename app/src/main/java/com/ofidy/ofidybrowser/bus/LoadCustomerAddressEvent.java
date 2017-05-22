package com.ofidy.ofidybrowser.bus;

public class LoadCustomerAddressEvent {

    public boolean loadCachedData = false;
    public boolean cacheData = false;

    public LoadCustomerAddressEvent(boolean cacheData) {
        this.cacheData = cacheData;
    }

}
