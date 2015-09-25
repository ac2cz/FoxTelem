package com.apple.eawt;

/** @since 10.6 Update 3 and 10.5 Update 8 */
public interface AppForegroundListener extends AppEventListener {

    public void appMovedToBackground(AppEvent.AppForegroundEvent e);
    public void appRaisedToForeground(AppEvent.AppForegroundEvent e);
}
