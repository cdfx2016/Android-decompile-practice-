package com.fanyu.boundless.widget.imagepager.photoview.gestures;

import android.view.MotionEvent;

public interface GestureDetector {
    boolean isScaling();

    boolean onTouchEvent(MotionEvent motionEvent);

    void setOnGestureListener(OnGestureListener onGestureListener);
}
