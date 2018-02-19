package com.turbomandelbrot.logic;

import android.content.SharedPreferences;
import com.turbomandelbrot.LiveWallpaper;
import com.turbomandelbrot.ui.WallpaperPreferencesActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class WallpaperAutoExplorer implements Explorer {

    public static final float SPEED_COEF = 25000;

    DrawTimer drawTimer;
    Fractal fractal;
    boolean benchmark;
    boolean showFPS;

    private float speed = 1;
    boolean circular;

    private float fps = LiveWallpaper.FPS_CAP;

    private int drawCycleTime;
    private int checkpointCycleTime = 0;
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
    private float[] prev_pos = NULL_POS;

    private ArrayList<Checkpoint> checkpoints;
    private ArrayList<Integer> fpsShowTimesList;
    private LinkedList<Integer> benchmarkTimesList;

    private CheckpointManager manager;

    public WallpaperAutoExplorer(CheckpointManager cman){
        manager = cman;

        fractal = Fractal.MANDELBROT;
        SharedPreferences prefs = WallpaperPreferencesActivity.WallpaperPreferences.getSharedPreferences();

        String str = prefs.getString("WALLPAPER_RESOLUTION", "1");
        quality = Float.parseFloat(str);
        iterationsNum = prefs.getInt("ITERATIONS_NUM", fractal.DEFAULT_ITERATIONS_NUM);
        str = prefs.getString("ANTIFLICKERING", "");
        speed = prefs.getInt("WALLPAPER_SPEED", 1);
        antiflickering_new_pixel_weight = Float.parseFloat(str);
        if(antiflickering_new_pixel_weight > 0 && antiflickering_new_pixel_weight < 1)
            antiflickering_mode = 1;
        showFPS = false;
        benchmark = false;
        circular = prefs.getBoolean("Circular", true);

        initializeCheckpoints();
        doCycle();

        drawCycleTime = checkpoints.get(checkpoints.size() - 1).time;
        drawTimer = new DrawTimer();

        if(showFPS)
            fpsShowTimesList = new ArrayList((int)Math.ceil(FPS_SHOW_LATENCY / LiveWallpaper.MINIMAL_DELTA_TIME));
        if(benchmark)
            benchmarkTimesList = new LinkedList<>();

        pos = new float[] {checkpoints.get(0).x, checkpoints.get(0).y, checkpoints.get(0).scale * quality, checkpoints.get(0).angle};
    }

    public void initializeCheckpoints(){
        checkpoints = new ArrayList<>();
        Iterator iter = null;
        if(benchmark)
            iter = manager.pathBenchIterator();
        else
            iter = manager.pathIterator();

        while (iter.hasNext()) {
            float[] point = (float[]) iter.next();
            checkpoints.add(new Checkpoint(point[0], point[1], point[2], point[3], 0));
        }

        for (int i = 0; i < checkpoints.size() - 1; i++) {
            Checkpoint a = checkpoints.get(i);
            Checkpoint b = checkpoints.get(i+1);
            b.time = a.time + (int)(distance(a, b) / speed * SPEED_COEF);
        }
    }

    public int antiflickeringEnabled(){
        return antiflickering_mode;
    }

    @Override
    public float getAntiflickeringPixelWeight() {
        return antiflickering_new_pixel_weight;
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

    public boolean benchmark(){
        return benchmark;
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

        if (benchmark && !drawTimer.stopped())
            benchmarkTimesList.add(drawTimer.getDeltaTime());

        checkpointCycleTime = (checkpointCycleTime + drawTimer.getDeltaTime()) % drawCycleTime;

        Checkpoint startPoint = null;
        Checkpoint endPoint = null;
        for(int i = 0; i < checkpoints.size() - 1; i++){
            startPoint = checkpoints.get(i);
            endPoint = checkpoints.get(i + 1);
            if(checkpointCycleTime >= startPoint.time && checkpointCycleTime < endPoint.time)
                break;
        }

        float progress = (float)(checkpointCycleTime - startPoint.time) / (endPoint.time - startPoint.time);
        Checkpoint diff = diffCheckpoint(startPoint, endPoint, progress);

        //Подгонка координат для правильной работы оптимизации перемещения картинки
        //TODO: сделать плавное перемещение картинки при низком разрешении (рендер за границы экрана на 1 пиксель)

        if(MathFunctions.floatEqual(diff.scale, 0, 0.001f)) {
            float scale = startPoint.scale + diff.scale;
            diff.x = Math.round(diff.x * scale * quality) / (scale * quality);
            diff.y = Math.round(diff.y * scale * quality) / (scale * quality);
        }

        prev_pos = pos;

        pos = new float[] {startPoint.x + diff.x, startPoint.y + diff.y, startPoint.scale + diff.scale, startPoint.angle + diff.angle};
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
        return pos[3];
    }

    @Override
    public boolean isStatic() {
        return pos[0] == prev_pos[0] && pos[1] == prev_pos[1] && pos[2] == prev_pos[2] && pos[3] == prev_pos[3];
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
        if(benchmark && benchmarkTimesList.size() > 0) {
            float sum = 0;
            Iterator<Integer> itr = benchmarkTimesList.iterator();
            while (itr.hasNext())
                sum += itr.next();
            return 1000.0f / Math.max((sum / benchmarkTimesList.size()), LiveWallpaper.MINIMAL_DELTA_TIME);
        }
        return getFPS();
    }

    private void doCycle(){
        Checkpoint start = checkpoints.get(0);
        Checkpoint end = checkpoints.get(checkpoints.size() - 1);

        if(circular)
        {
            if (!(start.x == end.x && start.y == end.y && start.scale == end.scale))
                checkpoints.add(new Checkpoint(start.x, start.y, start.scale, start.angle, end.time + (int)(distance(end, start) / speed * SPEED_COEF)));
        } else {
            for (int i = checkpoints.size() - 2; i >= 0; i--){
                Checkpoint point = checkpoints.get(i);
                int newtime = end.time + checkpoints.get(i + 1).time - checkpoints.get(i).time;
                checkpoints.add(new Checkpoint(point.x, point.y, point.scale, point.angle, newtime));
            }
        }
    }

    private Checkpoint diffCheckpoint(Checkpoint a, Checkpoint b, float timeCoef){
        float scale = 0;
        if(!MathFunctions.floatEqual(a.scale, b.scale, 0.0001f))
            scale = MathFunctions.geomProgression(a.scale, b.scale, MathFunctions.tanhSmoothing(1, timeCoef)) - a.scale;

        return new Checkpoint(MathFunctions.tanhSmoothing(b.x - a.x, timeCoef),
                MathFunctions.tanhSmoothing(b.y - a.y, timeCoef),
                scale,
                MathFunctions.tanhSmoothing(b.angle - a.angle, timeCoef),
                (int)(timeCoef * (b.time - a.time)));
    }

    private float distance(Checkpoint a, Checkpoint b){
        return (float) Math.pow(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2) +
                Math.pow(Math.log(Math.max(a.scale, b.scale)) / Math.log(Math.min(a.scale, b.scale)), 2), 0.5);
    }

    public void stopTimer(){
        drawTimer.stop();
    }

    private class Checkpoint {
        private float x;
        private float y;
        private float scale;
        private float angle;
        private int time;

        private Checkpoint(float x, float y, float scale, float angle, int time){
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.angle = angle;
            this.time = time;
        }
    }
}
