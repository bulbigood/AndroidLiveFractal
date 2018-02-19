package com.turbomandelbrot.logic;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Никита on 07.02.2018.
 */
public class CheckpointManager {
    public static final String PATH_FILE_NAME = "path";
    //public static final float[][] DEFAULT_POINTS = {{-0.84002f, -0.224302f, 120, 0}, {-0.84002f, -0.224302f, 20000, 0}, {-0.73f, -0.24f, 20000, 0}};
    public static final float[][] DEFAULT_POINTS = {{-1.5f, -0.01f, 120, 0}, {-1.5f, -0.01f, 10000, 0}};
    //public static final float[][] DEFAULT_POINTS = {{-0.84f, -0.24f, 20000, 0}, {-0.73f, -0.24f, 20000, 0}};
    public static final float[][] BENCHMARK_POINTS = {{-0.84002f, -0.224302f, 120, 0}, {-0.84002f, -0.224302f, 20000, 0}};

    private Context context;

    private ArrayList<float[]> benchPath = new ArrayList<>();
    private ArrayList<float[]> path = new ArrayList<>();

    public CheckpointManager(Context context){
        this.context = context;
        loadPath();

        //Инициализация пути бенчмарка
        for (float[] point : BENCHMARK_POINTS) {
            benchPath.add(point);
        }
    }

    public Iterator<float[]> pathBenchIterator(){
        return benchPath.iterator();
    }

    public Iterator<float[]> pathIterator(){
        if(path.size() > 0)
            return path.iterator();
        else
            return null;
    }

    public boolean loadPath(){
        File file = new File(context.getFilesDir(), PATH_FILE_NAME);
        if(file.exists()){
            try {
                FileInputStream in = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(in);

                float[] doubles = (float[]) ois.readObject();

                for (float d : doubles) {
                    float[] point = new float[4];
                    for (int i = 0; i < 4; i++) {
                        point[i] = d;
                    }
                    path.add(point);
                }
            } catch(IOException | ClassNotFoundException ioe){
                ioe.printStackTrace();
                return false;
            }
        } else {
            for(float[] point : DEFAULT_POINTS){
                path.add(point);
            }
        }
        return true;
    }

    public boolean savePath(){
        try {
            File file = new File(context.getFilesDir(), PATH_FILE_NAME);
            FileOutputStream in = new FileOutputStream(file);
            ObjectOutputStream ois = new ObjectOutputStream(in);

            boolean r1 = file.delete();
            boolean r2 = file.createNewFile();
            if(!r1 || !r2)
                return false;

            for(float[] point : path) {
                for(float val : point) {
                    ois.writeFloat(val);
                }
            }
        } catch(IOException ioe){
            ioe.printStackTrace();
            return false;
        }
        return true;
    }
}
