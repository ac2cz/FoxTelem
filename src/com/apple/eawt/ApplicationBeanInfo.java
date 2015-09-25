package com.apple.eawt;

import java.awt.Image;
import java.beans.SimpleBeanInfo;

public class ApplicationBeanInfo extends SimpleBeanInfo {
	
	public ApplicationBeanInfo() {}

    @Override
    public Image getIcon(int iconKind) {
        return null;
    }
}
