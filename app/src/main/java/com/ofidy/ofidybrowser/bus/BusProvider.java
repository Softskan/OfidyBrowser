package com.ofidy.ofidybrowser.bus;

import com.squareup.otto.Bus;

public class BusProvider {

    private static final MainThreadBus mBus = new MainThreadBus();

    private BusProvider() {}

    public static Bus getBus() { return mBus; }

}
