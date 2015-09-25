package com.apple.eawt;

/** @since 10.6 Update 3 and 10.5 Update 8 */
public interface UserSessionListener extends AppEventListener {
    public void userSessionActivated(AppEvent.UserSessionEvent e);
    public void userSessionDeactivated(AppEvent.UserSessionEvent e);
}
