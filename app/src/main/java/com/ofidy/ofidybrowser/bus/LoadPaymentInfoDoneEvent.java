package com.ofidy.ofidybrowser.bus;

import com.ofidy.ofidybrowser.model.OrderInvoice;

public class LoadPaymentInfoDoneEvent {

    public final OrderInvoice orderInvoice;

    public LoadPaymentInfoDoneEvent(OrderInvoice dest) {
        this.orderInvoice = dest;
    }

}
