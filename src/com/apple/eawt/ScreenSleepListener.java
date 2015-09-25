package com.apple.eawt;

/** @since 10.6 Update 3 and 10.5 Update 8 */
public interface ScreenSleepListener extends AppEventListener {
    public void screenAboutToSleep(AppEvent.ScreenSleepEvent e);
    public void screenAwoke(AppEvent.ScreenSleepEvent e);
}
