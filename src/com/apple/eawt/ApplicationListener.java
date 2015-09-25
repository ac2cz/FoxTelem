package com.apple.eawt;

import java.util.EventListener;

@Deprecated
public interface ApplicationListener extends EventListener {

    @Deprecated
    public void handleAbout(ApplicationEvent event);
    @Deprecated
    public void handleOpenApplication(ApplicationEvent event);
    @Deprecated
    public void handleOpenFile(ApplicationEvent event);
    @Deprecated
    public void handlePreferences(ApplicationEvent event);
    @Deprecated
    public void handlePrintFile(ApplicationEvent event);
    @Deprecated
    public void handleQuit(ApplicationEvent event);
    @Deprecated
    public void handleReOpenApplication(ApplicationEvent event);
}
