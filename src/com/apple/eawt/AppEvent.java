package com.apple.eawt;

import java.net.URI;
import java.util.EventObject;
import java.util.List;

/** @since 10.6 Update 3 and 10.5 Update 8 */
public abstract class AppEvent extends EventObject {

    AppEvent() {super(Application.getApplication());}

    public static class AboutEvent extends AppEvent {}
    public static class AppForegroundEvent extends AppEvent {}
    public static class AppHiddenEvent extends AppEvent {}
    public static class AppReOpenedEvent extends AppEvent {}
    public static class OpenURIEvent extends AppEvent {
        public URI getURI() {return null;}
    }
    public static class PreferencesEvent extends AppEvent {}

    public static class QuitEvent extends AppEvent {}
    public static class ScreenSleepEvent extends AppEvent {}
    public static class SystemSleepEvent extends AppEvent {}
    public static class UserSessionEvent extends AppEvent {}

    public static class FilesEvent extends AppEvent {
        public List getFiles() {return null;}
    }
    public static class PrintFilesEvent extends FilesEvent {}
    public static class OpenFilesEvent extends FilesEvent {
        public String getSearchTerm() {return null;}
    }
}

