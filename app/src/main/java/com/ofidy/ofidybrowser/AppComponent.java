package com.ofidy.ofidybrowser;

import com.ofidy.ofidybrowser.constant.StartPage;
import com.ofidy.ofidybrowser.download.LightningDownloadListener;
import com.ofidy.ofidybrowser.search.SuggestionsAdapter;
import com.ofidy.ofidybrowser.ui.BrowserActivity;
import com.ofidy.ofidybrowser.ui.ReadingActivity;
import com.ofidy.ofidybrowser.ui.TabsManager;
import com.ofidy.ofidybrowser.ui.ThemableBrowserActivity;
import com.ofidy.ofidybrowser.ui.ThemableSettingsActivity;
import com.ofidy.ofidybrowser.ui.browser.BrowserPresenter;
import com.ofidy.ofidybrowser.ui.dialog.LightningDialogBuilder;
import com.ofidy.ofidybrowser.ui.fragment.BookmarkSettingsFragment;
import com.ofidy.ofidybrowser.ui.fragment.BookmarksFragment;
import com.ofidy.ofidybrowser.ui.fragment.DebugSettingsFragment;
import com.ofidy.ofidybrowser.ui.fragment.LightningPreferenceFragment;
import com.ofidy.ofidybrowser.ui.fragment.PrivacySettingsFragment;
import com.ofidy.ofidybrowser.ui.fragment.TabsFragment;
import com.ofidy.ofidybrowser.ui.view.LightningView;
import com.ofidy.ofidybrowser.ui.view.LightningWebClient;
import com.ofidy.ofidybrowser.utils.AdBlock;
import com.ofidy.ofidybrowser.utils.ProxyUtils;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(BrowserActivity activity);

    void inject(BookmarksFragment fragment);

    void inject(BookmarkSettingsFragment fragment);

    void inject(LightningDialogBuilder builder);

    void inject(TabsFragment fragment);

    void inject(LightningView lightningView);

    void inject(ThemableBrowserActivity activity);

    void inject(LightningPreferenceFragment fragment);

    void inject(BrowserApp app);

    void inject(ProxyUtils proxyUtils);

    void inject(ReadingActivity activity);

    void inject(LightningWebClient webClient);

    void inject(ThemableSettingsActivity activity);

    void inject(AdBlock adBlock);

    void inject(LightningDownloadListener listener);

    void inject(PrivacySettingsFragment fragment);

    void inject(StartPage startPage);

    void inject(BrowserPresenter presenter);

    void inject(TabsManager manager);

    void inject(DebugSettingsFragment fragment);

    void inject(SuggestionsAdapter suggestionsAdapter);

}
