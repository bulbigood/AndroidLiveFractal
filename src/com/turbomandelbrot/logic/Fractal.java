package com.turbomandelbrot.logic;

public enum Fractal {

    MANDELBROT(100, false, true, new float[]{0,0}, new float[]{-1.5f, 0.0f, 200.0f}, new float[]{-2, -2, 2, 2, 100, 20000000});

    public final int DEFAULT_ITERATIONS_NUM;
    public final boolean X_SYMMETRY;
    public final boolean Y_SYMMETRY;
    public final float[] POINT_OF_SYMMETRY;
    public final float[] DEFAULT_POSITION;
    public final float[] BORDERS;

    Fractal(int iter, boolean x, boolean y, float[] o, float[] p, float[] borders){
        DEFAULT_ITERATIONS_NUM = iter;
        X_SYMMETRY = x;
        Y_SYMMETRY = y;
        POINT_OF_SYMMETRY = o;
        DEFAULT_POSITION = p;
        BORDERS = borders;
    }
}
