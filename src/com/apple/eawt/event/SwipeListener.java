package com.apple.eawt.event;

/** 
 * @since 10.5 Update 7 and 10.6 Update 2
 */
public interface SwipeListener extends GestureListener {

    public void swipedDown(SwipeEvent e);
    public void swipedLeft(SwipeEvent e);
    public void swipedRight(SwipeEvent e);
    public void swipedUp(SwipeEvent e);
}
