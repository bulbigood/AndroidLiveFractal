package com.turbomandelbrot.logic.touch_listeners;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import com.turbomandelbrot.logic.ScreenMatrix;

public class TouchExplorerListener implements View.OnTouchListener {

    private enum TouchMode {
        NONE, ZOOM, DRAG
    }

    private TouchMode mode;
    private ScreenMatrix screenMatrix;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float[] lastEvent = new float[4];
    private float oldDist = 0;
    private float d = 0;
    private float newRot = 0;

    public TouchExplorerListener(ScreenMatrix mat){
        screenMatrix = mat;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // handle touch events here
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                start.set(event.getX(), event.getY());
                mode = TouchMode.DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f)
                    mode = TouchMode.ZOOM;

                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = TouchMode.NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == TouchMode.DRAG) {
                    float dx = start.x - event.getX();
                    float dy = event.getY() - start.y;
                    start.set(event.getX(), event.getY());
                    screenMatrix.translate(dx, dy);
                } else if (mode == TouchMode.ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        float scale = (newDist / oldDist);
                        oldDist = newDist;
                        midPoint(mid, event);
                        screenMatrix.scale(scale, scale,  v.getMeasuredWidth()/2 - mid.x, mid.y - v.getMeasuredHeight()/2);
                    }
                    /*
                    if (lastEvent != null && event.getPointerCount() == 3) {
                        newRot = rotation(event);
                        float r = newRot - d;
                        float[] values = screenMatrix.getValues();
                        float tx = values[2];
                        float ty = values[5];
                        float sx = values[0];
                        float xc = (WIDTH / 2) * sx;
                        float yc = (HEIGHT / 2) * sx;
                        screenMatrix.rotate(r, tx + xc, ty + yc);
                    }*/
                }
                break;
        }
        return true;
    }

    //Determine the space between the first two fingers
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    //Calculate the mid point of the first two fingers
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    //Calculate the degree to be rotated by.
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }
}
