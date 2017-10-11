package com.turbomandelbrot.logic;

public class DrawTimer {

    private boolean stopped;

    private long lastTime;
    private int deltaTime;

    public DrawTimer(){
        lastTime = time();
        deltaTime = 0;
        stopped = true;
    }

    public void start(){
        lastTime = time();
        stopped = false;
    }

    public void stop(){
        stopped = true;
    }

    public boolean stopped(){
        return stopped;
    }

    public void pressTimer(){
        if(!stopped) {
            long vr = time();
            deltaTime = (int) (vr - lastTime);
            lastTime = vr;
        }
    }

    public int getDeltaTime(){
        return deltaTime;
    }

    private long time(){
        return System.currentTimeMillis();
    }
}
