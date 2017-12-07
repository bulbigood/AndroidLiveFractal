package com.turbomandelbrot.logic;

import android.content.SharedPreferences;
import android.content.res.Resources;
import com.turbomandelbrot.LiveWallpaper;
import com.turbomandelbrot.ui.WallpaperPreferencesActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class WallpaperAutoExplorer implements Explorer {
    DrawTimer drawTimer;
    Fractal fractal;
    byte benchmark;
    boolean showFPS;

    boolean circular;

    private float fps = LiveWallpaper.FPS_CAP;

    private int drawCycleTime;
    private int checkpointCycleTime = 0;
    private int fpsCycleTime = 0;
    private int lastCircularCheckpointDeltaTime = 30000;

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

    private ArrayList<Checkpoint> checkpoints;
    private ArrayList<Float> checkpointsScalingDelta;

    private ArrayList<Integer> fpsShowTimesList;
    private LinkedList<Integer> benchmarkTimesList;

    public WallpaperAutoExplorer(){
        fractal = Fractal.MANDELBROT;
        SharedPreferences prefs = WallpaperPreferencesActivity.WallpaperPreferences.getSharedPreferences();

        String str = prefs.getString("WALLPAPER_RESOLUTION", "1");
        quality = Float.parseFloat(str);
        iterationsNum = prefs.getInt("ITERATIONS_NUM", fractal.DEFAULT_ITERATIONS_NUM);
        str = prefs.getString("ANTIFLICKERING", "");
        antiflickering_new_pixel_weight = Float.parseFloat(str);
        if(antiflickering_new_pixel_weight > 0 && antiflickering_new_pixel_weight < 1)
            antiflickering_mode = 1;

        checkpoints = new ArrayList<>();
        checkpointsScalingDelta = new ArrayList<>();
        checkpoints.add(new Checkpoint(-0.84002f, -0.224302f,
                120000, 0, 0));
        checkpoints.add(new Checkpoint(-0.84002f, -0.224302f,
                20000000, 0, 30000));
        showFPS = false;
        benchmark = 0;
        circular = true;

        doCycle();
        //Расчет коэффициента изменения зума для каждой пары ближайших чекпоинтов
        for(int i = 0; i < checkpoints.size(); i++){
            Checkpoint a = checkpoints.get(i);
            Checkpoint b = i == checkpoints.size() - 1 ? checkpoints.get(0) : checkpoints.get(i + 1);

            float scaleDelta = (float) Math.pow(b.scale / a.scale, LiveWallpaper.MINIMAL_DELTA_TIME / (b.time - a.time)) - 1;
            checkpointsScalingDelta.add(scaleDelta);
        }

        drawCycleTime = checkpoints.get(checkpoints.size() - 1).time;
        drawTimer = new DrawTimer();

        if(showFPS)
            fpsShowTimesList = new ArrayList((int)Math.ceil(FPS_SHOW_LATENCY / LiveWallpaper.MINIMAL_DELTA_TIME));
        if(benchmark > 0)
            benchmarkTimesList = new LinkedList<>();

        pos = new float[] {checkpoints.get(0).x, checkpoints.get(0).y, checkpoints.get(0).scale * quality};
        angle = checkpoints.get(0).angle;
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

    public byte benchmark(){
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

        if (benchmark != 0 && !drawTimer.stopped())
            benchmarkTimesList.add(drawTimer.getDeltaTime());

        checkpointCycleTime = (checkpointCycleTime + drawTimer.getDeltaTime()) % drawCycleTime;

        Checkpoint startPoint = null;
        Checkpoint endPoint = null;
        float scaleDelta = 0;
        for(int i = 0; i < checkpoints.size() - 1; i++){
            startPoint = checkpoints.get(i);
            endPoint = checkpoints.get(i + 1);
            if(checkpointCycleTime >= startPoint.time && checkpointCycleTime < endPoint.time) {
                scaleDelta = checkpointsScalingDelta.get(i);
                break;
            }
        }

        float progress = (float)(checkpointCycleTime - startPoint.time) / (endPoint.time - startPoint.time);
        Checkpoint diff = diffCheckpoint(startPoint, endPoint, scaleDelta, progress);

        //Подгонка координат для правильной работы оптимизации перемещения картинки
        if(MathFunctions.floatEqual(diff.scale, 0)) {
            diff.x = Math.round(diff.x * (startPoint.scale + diff.scale) * quality) /
                    ((startPoint.scale + diff.scale) * quality);
            diff.y = Math.round(diff.y * (startPoint.scale + diff.scale) * quality) /
                    ((startPoint.scale + diff.scale) * quality);
        }

        prev_pos = pos;
        prev_angle = angle;

        pos = new float[] {MathFunctions.getEnteringTheRange(startPoint.x + diff.x, startPoint.x, endPoint.x),
                MathFunctions.getEnteringTheRange(startPoint.y + diff.y, startPoint.y, endPoint.y),
                MathFunctions.getEnteringTheRange(prev_pos[2] + diff.scale, startPoint.scale, endPoint.scale)};
        angle = startPoint.angle + diff.angle;
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
        if(benchmark != 0 && benchmarkTimesList.size() > 0) {
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
                checkpoints.add(new Checkpoint(start.x, start.y, start.scale, start.angle, end.time + lastCircularCheckpointDeltaTime));
        } else {
            for (int i = checkpoints.size() - 2; i >= 0; i--){
                Checkpoint point = checkpoints.get(i);
                int newtime = end.time + checkpoints.get(i + 1).time - checkpoints.get(i).time;
                checkpoints.add(new Checkpoint(point.x, point.y, point.scale, point.angle, newtime));
            }
        }
    }

    private Checkpoint diffCheckpoint(Checkpoint a, Checkpoint b, float scaleDelta, float timeCoef){
        return new Checkpoint(timeCoef * (b.x - a.x), timeCoef * (b.y - a.y), prev_pos[2] * scaleDelta * drawTimer.getDeltaTime() / LiveWallpaper.MINIMAL_DELTA_TIME,
                timeCoef * (b.angle - a.angle), (int)(timeCoef * (b.time - a.time)));
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
