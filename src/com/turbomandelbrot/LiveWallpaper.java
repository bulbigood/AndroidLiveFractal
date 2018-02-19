package com.turbomandelbrot;

import android.content.Context;
import android.service.wallpaper.WallpaperService;
import android.view.Display;
import android.view.SurfaceHolder;

import android.view.WindowManager;
import com.turbomandelbrot.draw.WallpaperGLSurface;
import com.turbomandelbrot.logic.CheckpointManager;
import com.turbomandelbrot.logic.Explorer;
import com.turbomandelbrot.logic.WallpaperAutoExplorer;

public class LiveWallpaper extends WallpaperService {

	public static float FPS_CAP = 60;
	public static float MINIMAL_DELTA_TIME = (float) 1000 / FPS_CAP;

	public static Engine myEngine;
	private WallpaperGLSurface wallpaperSurface;

	private CheckpointManager checkpointManager;
	private WallpaperAutoExplorer wallpaperAutoExplorer;


	@Override
	public void onCreate()
	{
		super.onCreate();
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		checkpointManager = new CheckpointManager(this);
		wallpaperAutoExplorer = new WallpaperAutoExplorer(checkpointManager);
		FPS_CAP = display.getRefreshRate();
		MINIMAL_DELTA_TIME = (float) 1000 / FPS_CAP;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	public Explorer getExplorer(){
		return wallpaperAutoExplorer;
	}

	@Override
	public Engine onCreateEngine()
	{
		myEngine = new FractalWallpaperEngine();
		return myEngine;
	}

	class FractalWallpaperEngine extends Engine
	{
		@Override
		public void onCreate(SurfaceHolder surfaceHolder)
		{
			super.onCreate(surfaceHolder);
			wallpaperSurface = new WallpaperGLSurface(LiveWallpaper.this, wallpaperAutoExplorer);
		}

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			//wallpaperSurface.onDestroy();
		}

		@Override
		public void onVisibilityChanged(boolean visible)
		{
			super.onVisibilityChanged(visible);

			if (visible)
				wallpaperSurface.onResume();
			else
				wallpaperSurface.onPause();
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
									 int width, int height)
		{
			super.onSurfaceChanged(holder, format, width, height);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder)
		{
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder)
		{
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
									 float yStep, int xPixels, int yPixels)
		{ }
	}
}
