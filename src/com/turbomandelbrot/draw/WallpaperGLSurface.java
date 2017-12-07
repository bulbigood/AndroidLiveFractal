package com.turbomandelbrot.draw;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;

import com.turbomandelbrot.LiveWallpaper;
import com.turbomandelbrot.logic.Explorer;
import android.service.wallpaper.WallpaperService.Engine;

public class WallpaperGLSurface extends GLSurfaceView {

    private final GLRenderer mRenderer;

    public WallpaperGLSurface(Context context, Explorer explorer) {
        super(context);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new GLRenderer(context, explorer);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public SurfaceHolder getHolder() {
        return LiveWallpaper.myEngine.getSurfaceHolder();
    }

    public void onDestroy() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRenderer.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public GLRenderer getRenderer(){
        return mRenderer;
    }
}
