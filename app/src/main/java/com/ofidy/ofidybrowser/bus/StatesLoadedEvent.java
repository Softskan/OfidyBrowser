package com.ofidy.ofidybrowser.bus;

import com.ofidy.ofidybrowser.model.State;

import java.util.List;

public class StatesLoadedEvent {

    public final boolean error;
    public final List<State> states;
    public final String message;

    public StatesLoadedEvent(List<State> states, boolean error, String message) {
        this.states = states;
        this.message = message;
        this.error = error;
    }

}
