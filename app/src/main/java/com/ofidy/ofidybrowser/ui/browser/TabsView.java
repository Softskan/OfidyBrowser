package com.ofidy.ofidybrowser.ui.browser;

public interface TabsView {

    void tabAdded();

    void tabRemoved(int position);

    void tabChanged(int position);

    void tabsInitialized();
}
