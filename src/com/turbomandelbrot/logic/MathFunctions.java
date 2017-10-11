package com.turbomandelbrot.logic;

import java.util.ArrayList;

public class MathFunctions {

    public static float[] getLBCornerPos(float[] centerPos, float[] screenSize) {
        float[] vrArray = new float[3];
        vrArray[0] = centerPos[0] - screenSize[0] / (2 * centerPos[2]);
        vrArray[1] = centerPos[1] - screenSize[1] / (2 * centerPos[2]);
        vrArray[2] = centerPos[2];
        return vrArray;
    }

    public static float arithmeticalMean(ArrayList<Integer> array){
        if(array.size() == 0)
            return 0;

        float sum = 0;
        for(int elem : array)
            sum += elem;
        return sum / array.size();
    }

    public static float getEnteringTheRange(float val, float a, float b){
        return Math.max(Math.min(a, b), Math.min(val, Math.max(a, b)));
    }

    public static float[] rotationMatrix(float angle_rad) {
        float sin = (float)Math.sin(angle_rad);
        float cos = (float)Math.cos(angle_rad);
        return new float[] {cos, sin, -sin, cos};
    }

    public static float[][] multiplyByMatrix(float[][] m1, float[][] m2) {
        int m1ColLength = m1[0].length; // m1 columns length
        int m2RowLength = m2.length;    // m2 rows length
        if(m1ColLength != m2RowLength) return null; // matrix multiplication is not possible
        int mRRowLength = m1.length;    // m result rows length
        int mRColLength = m2[0].length; // m result columns length
        float[][] mResult = new float[mRRowLength][mRColLength];
        for(int i = 0; i < mRRowLength; i++) {         // rows from m1
            for(int j = 0; j < mRColLength; j++) {     // columns from m2
                for(int k = 0; k < m1ColLength; k++) { // columns from m1
                    mResult[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return mResult;
    }

    public static int roundToNextHighestPowerOf2(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        return ++v;
    }

    public static boolean floatEqual(float a, float b){
        return Math.abs(a - b) < 0.0001f;
    }
}
