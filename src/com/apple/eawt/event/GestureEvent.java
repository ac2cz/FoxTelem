package com.apple.eawt.event;

/** 
 * @since 10.5 Update 7 and 10.6 Update 2
 */
public abstract class GestureEvent {
    
    public void consume() {}

    protected boolean isConsumed() {
        return false;
    }
}
