package com.ofidy.ofidybrowser.bus;

public class GetOrderBillDoneEvent {

    public final String bill;

    public GetOrderBillDoneEvent(String bill) {
        this.bill = bill;
    }

}
