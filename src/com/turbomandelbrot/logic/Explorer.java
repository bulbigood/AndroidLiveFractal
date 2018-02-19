package com.turbomandelbrot.logic;

public interface Explorer {
    int FPS_SHOW_LATENCY = 1000;
    float[] NULL_POS = {-Float.MAX_VALUE/2, -Float.MAX_VALUE/2, Float.MAX_VALUE/2};

    Fractal getFractal();
    float getQuality();
    void setQuality(float q);
    int getIterationsNum();

    void calculatePosition();
    float posX();
    float posY();
    float scale();
    float angle();
    float[] posValues();
    float[] prevPosValues();
    boolean isStatic();
    int antiflickeringEnabled();
    float getAntiflickeringPixelWeight();
    void stopTimer();

    /**
     * Эксплорер производит бенчмарк
     * @return 0 - нет, 1 - бенчмарк с рендером на экран, 2 - бенчмарк с рендером в буфер
     */
    boolean benchmark();
    boolean showingFPS();
    float getFPS();
    float getAverageFPS();
}
