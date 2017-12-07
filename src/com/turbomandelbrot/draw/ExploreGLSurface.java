package com.turbomandelbrot.draw;

import android.content.Context;
import android.opengl.GLSurfaceView;
import com.turbomandelbrot.logic.Explorer;

/**
 * Created by Никита on 25.08.2017.
 */
public class ExploreGLSurface extends GLSurfaceView {

    private final GLRenderer mRenderer;

    public ExploreGLSurface(Context context, Explorer explorer) {
        super(context);
        getHolder().addCallback(this);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new GLRenderer(context, explorer);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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
