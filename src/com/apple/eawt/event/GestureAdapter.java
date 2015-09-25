package com.apple.eawt.event;

/** 
 * @since 10.5 Update 7 and 10.6 Update 2
 */
public abstract class GestureAdapter implements GesturePhaseListener, MagnificationListener, RotationListener, SwipeListener {
    
    public GestureAdapter() {}

    public void gestureBegan(GesturePhaseEvent e) {}
    public void gestureEnded(GesturePhaseEvent e) {}
    public void magnify(MagnificationEvent e) {}
    public void rotate(RotationEvent e) {}
    public void swipedDown(SwipeEvent e) {}
    public void swipedLeft(SwipeEvent e) {}
    public void swipedRight(SwipeEvent e) {}
    public void swipedUp(SwipeEvent e) {}    
}
