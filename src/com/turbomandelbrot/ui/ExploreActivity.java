package com.turbomandelbrot.ui;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import com.turbomandelbrot.R;
import com.turbomandelbrot.draw.ExploreGLSurface;
import com.turbomandelbrot.logic.ScreenMatrix;
import com.turbomandelbrot.logic.TouchExplorer;
import com.turbomandelbrot.logic.touch_listeners.TouchExplorerListener;

/**
 * Created by Никита on 25.08.2017.
 */
public class ExploreActivity extends Activity {

    // Our OpenGL Surfaceview
    private GLSurfaceView glSurfaceView;

    private TouchExplorerListener touchListener;
    private TouchExplorer touchExplorer;
    private ScreenMatrix screenMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Super
        super.onCreate(savedInstanceState);

        // Fullscreen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        screenMatrix = new ScreenMatrix();
        touchExplorer = new TouchExplorer(screenMatrix);
        touchListener = new TouchExplorerListener(screenMatrix);
        glSurfaceView = new ExploreGLSurface(ExploreActivity.this, touchExplorer);

        // Set our view.
        setContentView(R.layout.explore_layout);

        // Retrieve our Relative layout from our main layout we just set to our view.
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.exploreLayout);

        // Attach our surfaceview to our relative layout from our main layout.
        RelativeLayout.LayoutParams glParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        glSurfaceView.setOnTouchListener(touchListener);
        layout.addView(glSurfaceView, glParams);
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }
}
