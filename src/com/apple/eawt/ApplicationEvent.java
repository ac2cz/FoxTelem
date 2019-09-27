package com.apple.eawt;

import java.util.EventObject;

@SuppressWarnings("serial")
@Deprecated
public class ApplicationEvent extends EventObject {

    ApplicationEvent(Object source) {
        super(source);
    }

    ApplicationEvent(Object source, String str) {
        super(source);
    }

    @Deprecated
	public String getFilename() {
        return null;
    }

    @Deprecated
    public boolean isHandled() {
        return false;
    }

    @Deprecated
    public void setHandled(boolean state) {}
}
