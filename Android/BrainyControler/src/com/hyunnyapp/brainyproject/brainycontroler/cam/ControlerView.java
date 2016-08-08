package com.hyunnyapp.brainyproject.brainycontroler.cam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Random;

import com.hyunnyapp.brainyproject.brainycontroler.Constants;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class ControlerView extends SurfaceView implements SurfaceHolder.Callback 
{	
	private final String TAG = "ControlerView";

	private SurfaceHolder mHolder;

	//private DrawingThread drawingThread = null;
	
	private final int MAX_FPS = 30;
	private final int MIN_FPS = 8;
	private final int MAX_HALF_FPS = (MAX_FPS / 2);
	private final int HIGH_FPS_MODE_LOWER_LIMIT = 14;
	private final int DEFAULT_FPS = MIN_FPS;
	
	private int mScreenWidth;
	private int mScreenHeight;
	
	//private boolean mUpdateAvailable = false;
	//private Bitmap mBitmap = null;
	//private byte[] mBuffer;
    Canvas canvas;
    
	private int fps = 0;
	
	public ControlerView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		init(context);
	}

	public ControlerView(Context context) 
	{
		super(context);
		init(context);
	}

	public void init(Context context) 
	{
		setWidthHeight(context);
		if(Constants.CAMDEBUG) Log.d(TAG,"mScreenWidth: "+mScreenWidth+", mScreenHeight: "+mScreenHeight);
		
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
	    setFocusable(true);
	    //drawingThread = new DrawingThread();
	    //drawingThread.setName(TAG);
	    fps = DEFAULT_FPS;
	}
	
	public void setUpdateFrameBuffer(byte[] buffer)
	{
		if(Constants.CAMDEBUG) Log.d(TAG,"setUpdateFrameBuffer");
		//mBuffer = buffer;
		//mUpdateAvailable = true;
		updateFrameBuffer(buffer);
	}
	
	private synchronized void updateFrameBuffer(byte[] buffer)
	{
		Bitmap bitmap = null;
		if(Constants.CAMDEBUG) Log.d(TAG, "updateFrameBuffer()");
		
		
		bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
		if(bitmap == null)
		{
			return;
		}
		if(Constants.CAMDEBUG) Log.d(TAG, "bitmap.getWidth(): "+bitmap.getWidth()+", bitmap.getHeight(): "+bitmap.getHeight());
		
        bitmap = Bitmap.createScaledBitmap(bitmap, mScreenWidth, mScreenHeight, false);

        try 
        {
        	canvas = mHolder.lockCanvas();
            if (canvas != null) 
            {
                synchronized (mHolder) 
                {
                	if(Constants.CAMDEBUG) Log.d(TAG, "Update!!!!!!!!!!!!!!!!!!!!");
                	canvas.drawBitmap(bitmap, 0, 0, null);
                }               
            }	                
            else 
            {
                if (canvas == null) Log.w(TAG, "run(): lockCanvas returned null canvas");
                else Log.w(TAG, "run(): null screen!");
            }
        } 
        catch (Exception e) 
        {
        	if(Constants.CAMDEBUG) Log.e(TAG, "run(): drawing canvas:" + Log.getStackTraceString(e) );
        }
        finally 
        {
            if (canvas != null) 
            	mHolder.unlockCanvasAndPost(canvas);
        }
	
	}
	
	private void setWidthHeight(Context context)
	{
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display d = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		d.getMetrics(metrics);
		// since SDK_INT = 1;
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
		// includes window decorations (statusbar bar/menu bar)
		if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
		{
			try 
			{
				mScreenWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
				mScreenHeight = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
			} 
			catch (Exception ignored) 
			{
			}
		}
		// includes window decorations (statusbar bar/menu bar)
		if (Build.VERSION.SDK_INT >= 17)
		{
			try 
			{
			    Point realSize = new Point();
			    Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
			    mScreenWidth = realSize.x;
				mScreenHeight = realSize.y;
			} 
			catch (Exception ignored) 
			{
			}
		}
	}
	
	public void surfaceCreated(SurfaceHolder holder) 
	{
		if(Constants.CAMDEBUG) Log.i(TAG, "surfaceCreated()");
		//startDrawingThread();
	}

	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		if(Constants.CAMDEBUG) Log.i(TAG, "surfaceDestroyed()");
		//drawingThread.setRunning(false);
		//stopDrawingThread();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) 
	{
		if(Constants.CAMDEBUG) Log.i(TAG, "surfaceChanged()");
	}
	
	/*
	public synchronized void startDrawingThread() 
	{
		Log.i(TAG, "startDrawingThread()");
        if (drawingThread == null) 
        	drawingThread = new DrawingThread();
        
        if (!drawingThread.isAlive())
        {
            drawingThread.setRunning(true);
            drawingThread.start();
        }
    }
	
	private synchronized void stopDrawingThread()
	{
	    if (drawingThread != null)
	    {
	    	drawingThread = null;
	    }
	}
	
	class DrawingThread extends Thread 
	{
	    private boolean running = false;

	    public synchronized void setRunning(boolean run) 
	    {
	        running = run;
	    }
	    

	    @Override
	    public void run() 
	    {

	        while (running)
	        {
            	//Log.d(TAG, "mUpdateAvailable: "+mUpdateAvailable);
	        	if(mUpdateAvailable == true && mBuffer != null)
	        	{

	        		mBitmap = BitmapFactory.decodeByteArray(mBuffer, 0, mBuffer.length);
	                mBitmap = Bitmap.createScaledBitmap(mBitmap, mBitmap.getWidth() * 2, mBitmap.getHeight() * 2, false);

	                try 
	                {
	                	canvas = mHolder.lockCanvas();
	                    if (canvas != null) 
	                    {
	                        synchronized (mHolder) 
	                        {
	                        	Log.d(TAG, "Update!!!!!!!!!!!!!!!!!!!!");
	                        	canvas.drawBitmap(mBitmap, 0, 0, null);
	                        }               
	                    }	                
	                    else 
	                    {
	                        if (canvas == null) Log.w(TAG, "run(): lockCanvas returned null canvas");
	                        else Log.w(TAG, "run(): null screen!");
	                    }
	                } 
	                catch (Exception e) 
	                {
	                    Log.e(TAG, "run(): drawing canvas:" + Log.getStackTraceString(e) );
	                }
	                finally 
	                {
	                    if (canvas != null) 
	                    	mHolder.unlockCanvasAndPost(canvas);
	                }
	        	
	        	}
    
	        }
	    }
	}
	*/

}