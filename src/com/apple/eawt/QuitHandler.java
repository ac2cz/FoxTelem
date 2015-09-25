package com.apple.eawt;

/** @since 10.6 Update 3 and 10.5 Update 8 */
public interface QuitHandler {
    public void handleQuitRequestWith(AppEvent.QuitEvent e, QuitResponse response);
}
