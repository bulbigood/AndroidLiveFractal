package com.turbomandelbrot.logic;

import android.graphics.Matrix;

public class ScreenMatrix {

    private Fractal fractal;
    private float quality = 1;
    private Matrix matrix = new Matrix();
    private float[] values = new float[9];

    private float[] pos = new float[3];
    private float angle = 0;

    public ScreenMatrix(){
        fractal = Fractal.MANDELBROT;
        matrix.reset();
        refreshArray();
    }

    public void setQuality(float q){
        quality = q;
    }

    public float[] getPosition(){
        return new float[] {pos[0], pos[1], pos[2]};
    }

    public float getAngle(){
        return angle;
    }

    public void translate(float x, float y) {
        float dx = x * quality / pos[2];
        float dy = y * quality / pos[2];
        //Проверка на ограничения координат
        float newposx = Math.max(fractal.BORDERS[0], Math.min(pos[0] + dx,fractal.BORDERS[2]));
        float newposy = Math.max(fractal.BORDERS[1], Math.min(pos[1] + dy,fractal.BORDERS[3]));
        matrix.postTranslate(newposx - pos[0], newposy - pos[1]);
        refreshArray();
    }

    public void scale(float xscale, float yscale, float x, float y){
        float dx = x * quality / pos[2];
        float dy = y * quality / pos[2];
        //Проверка на ограничения координат
        float newposscale = Math.max(fractal.BORDERS[4] * quality, Math.min(pos[2]*xscale, fractal.BORDERS[5] * quality));
        matrix.postScale(newposscale / pos[2], newposscale / pos[2], pos[0] + dx, pos[1] + dy);
        refreshArray();
    }

    public void rotate(float degrees, float x, float y){
        matrix.postRotate(degrees, x, y);
        refreshArray();
    }

    public void setPosition(float x, float y, float scale, float degrees) {
        matrix.postScale(scale, scale);
        matrix.postTranslate(x, y);
        matrix.postRotate(degrees);
        refreshArray();
    }

    private void refreshArray(){
        matrix.getValues(values);
        pos[0] = values[2];
        pos[1] = values[5];
        pos[2] = values[0];
        angle = 0;
    }
}
