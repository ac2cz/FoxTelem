package com.apple.eawt;

/** @since 10.6 Update 3 and 10.5 Update 8 */
public interface AppHiddenListener extends AppEventListener {

    public void appHidden(AppEvent.AppHiddenEvent e);
    public void appUnhidden(AppEvent.AppHiddenEvent e);
}
