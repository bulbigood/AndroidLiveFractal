package com.turbomandelbrot.logic;

import android.content.SharedPreferences;
import android.view.View;
import com.turbomandelbrot.LiveWallpaper;
import com.turbomandelbrot.ui.WallpaperPreferencesActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class TouchExplorer implements Explorer {
    ScreenMatrix screenMatrix;
    DrawTimer drawTimer;
    Fractal fractal;

    boolean showFPS;
    private float fps = LiveWallpaper.FPS_CAP;
    private int fpsCycleTime = 0;

    /**
     * Подавление мерцания (TAA - Temporal anti-aliasing). Для ручного эксплорера не годится. Состояния 0 и 1
     */
    private int antiflickering_mode;
    /**
     * Вес "нового" пикселя при расчете изображения. Под "старым" пикселем понимается взятый из предыдущего изображения.
     * Нужен для подавления мерцания (TAA).
     * Чем меньше, тем смазаннее и плавнее движущаяся картинка. Диапазон 0.02 .. 0.5. По-умолчанию 0.25
     */
    private float antiflickering_new_pixel_weight;
    /**
     * Кратность разрешения генерируемой текстуры.
     * При значении > 2 и не кратном 2, может возникнуть мерцание из-за специфики OpenGL
     * При значении < 0.5 изображение начинает перемещаться грубо
     */
    private float quality;
    private int iterationsNum;
    private float[] pos = NULL_POS;
    private float angle = 0;
    private float[] prev_pos = NULL_POS;
    private float prev_angle = 0;

    private ArrayList<Integer> fpsShowTimesList;

    public TouchExplorer(ScreenMatrix mat){
        screenMatrix = mat;
        fractal = Fractal.MANDELBROT;

        SharedPreferences prefs = WallpaperPreferencesActivity.WallpaperPreferences.getSharedPreferences();

        String str = prefs.getString("EXPLORE_RESOLUTION", "1");
        quality = Float.parseFloat(str);
        iterationsNum = prefs.getInt("ITERATIONS_NUM", fractal.DEFAULT_ITERATIONS_NUM);
        showFPS = prefs.getBoolean("SHOW_FPS",false);

        antiflickering_new_pixel_weight = 0;
        antiflickering_mode = 0;

        drawTimer = new DrawTimer();

        if(showFPS)
            fpsShowTimesList = new ArrayList((int)Math.ceil(FPS_SHOW_LATENCY / LiveWallpaper.MINIMAL_DELTA_TIME));

        pos = fractal.DEFAULT_POSITION;
        screenMatrix.setQuality(quality);
        screenMatrix.setPosition(pos[0], pos[1], pos[2] * 2, angle);
    }

    public Fractal getFractal(){
        return fractal;
    }

    @Override
    public float getQuality() {
        return quality;
    }

    @Override
    public void setQuality(float q) {
        quality = q;
    }

    @Override
    public int getIterationsNum() {
        return iterationsNum;
    }

    public byte benchmark(){
        return 0;
    }

    public boolean showingFPS() {
        return showFPS;
    }

    public void calculatePosition(){
        if(drawTimer.stopped())
            drawTimer.start();
        drawTimer.pressTimer();

        //Запись и расчет значений FPS
        if(showFPS) {
            fpsCycleTime += drawTimer.getDeltaTime();
            fpsShowTimesList.add(drawTimer.getDeltaTime());
            if (fpsCycleTime >= FPS_SHOW_LATENCY) {
                fpsCycleTime %= FPS_SHOW_LATENCY;
                fps = 1000 / MathFunctions.arithmeticalMean(fpsShowTimesList);
                if (fps == 0)
                    fps = LiveWallpaper.FPS_CAP;
                fpsShowTimesList.clear();
            }
        }

        prev_pos = pos;
        prev_angle = angle;

        pos = screenMatrix.getPosition();
        angle = screenMatrix.getAngle();
    }

    public void stopTimer(){
        drawTimer.stop();
    }

    @Override
    public float posX() {
        return pos[0];
    }

    @Override
    public float posY() {
        return pos[1];
    }

    @Override
    public float scale() {
        return pos[2];
    }

    @Override
    public float angle() {
        return angle;
    }

    @Override
    public float prevAngle() {
        return prev_angle;
    }

    @Override
    public boolean isStatic() {
        return pos[0] == prev_pos[0] && pos[1] == prev_pos[1] && pos[2] == prev_pos[2] && angle == prev_angle;
    }

    @Override
    public int antiflickeringEnabled() {
        return 0;
    }

    @Override
    public float getAntiflickeringPixelWeight() {
        return antiflickering_new_pixel_weight;
    }

    @Override
    public float[] posValues() {
        return pos;
    }

    @Override
    public float[] prevPosValues() {
        return prev_pos;
    }

    @Override
    public float getFPS() {
        return fps;
    }

    @Override
    public float getAverageFPS() {
        return fps;
    }
}
