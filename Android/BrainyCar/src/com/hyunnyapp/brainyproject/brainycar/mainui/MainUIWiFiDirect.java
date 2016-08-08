package com.hyunnyapp.brainyproject.brainycar.mainui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera.CameraInfo;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.hyunnyapp.brainyproject.brainycar.Constants;
import com.hyunnyapp.brainyproject.brainycar.R;
import com.hyunnyapp.brainyproject.brainycar.serial.SerialConnection;
import com.hyunnyapp.brainyproject.brainycar.serial.SerialDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycar.wifidirect.WiFiConnectionManager;
import com.hyunnyapp.brainyproject.brainycar.wifidirect.WiFiDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycar.wifidirect.WiFiDeviceSearch;
import com.hyunnyapp.brainyproject.brainycar.wifidirect.WiFiDirectService;
import com.hyunnyapp.brainyproject.brainycar.wifidirect.WiFiConnection;

public class MainUIWiFiDirect extends Activity
{
	private static final String TAG = "MainUIWiFiDirect";

	private SerialConnection mSerialConnection = null;
	private SerialDataReceiveListener mSerialDataReceiveListener;
	
	private int mCameraId = 0;
	private Camera mCamera;
	private CamPreview mCamPreview;
	private CamCallback mCamCallback;
	
	private int mPreviewHeight = 0;
	private int mPreviewWidth = 0;
	
	private boolean mSendDataAvailable = false;

	private WiFiConnectionManager mWiFiConnectionManager;
	
	private WiFiDataReceiveListener mWiFiDataReceiveListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_ui_bluetooth);

		// Initalize camera and preview
		CameraInfo cameraInfo = new CameraInfo();
		
		mCameraId = findCamera(cameraInfo.CAMERA_FACING_BACK);  // selectable....
		
		if (mCameraId < 0)
		{
			Toast.makeText(this, "Cannot find camera.", Toast.LENGTH_LONG).show();
		} 
		else
		{
			safeCameraOpen(mCameraId);
		}

		mCamPreview = new CamPreview(MainUIWiFiDirect.this, mCamera);
		mCamPreview.setSurfaceTextureListener((SurfaceTextureListener) mCamPreview);

		FrameLayout preview = (FrameLayout) findViewById(R.id.previewLayout);
		preview.addView(mCamPreview);

		// Attach a callback for preview
		mCamCallback = new CamCallback();
		mCamera.setPreviewCallback(mCamCallback);
		
		initSerialConnection(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		releaseCamera();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_ui_wifip2p_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (Constants.WIFIDEBUG)
			Log.d(TAG, "onOptionsItemSelected() ID: " + item.getItemId());

		switch (item.getItemId())
		{
			case R.id.serial_device_search:
				if (Constants.WIFIDEBUG)
					Log.i(TAG, "serial device search start!!!");
				if(mSerialConnection.connectToArduino())
				{
					Toast.makeText(this, "Secceed to Connect to Arduino", Toast.LENGTH_SHORT).show();
				}
				else
				{
					Toast.makeText(this, "Failed to Connect to Arduino", Toast.LENGTH_SHORT).show();
				}
				
				return true;
			case R.id.wifi_device_search:
	        	Intent intent = new Intent(MainUIWiFiDirect.this, WiFiDeviceSearch.class);
	            startActivityForResult(intent, Constants.REQUEST_SEARCH_WIFI);
				return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (Constants.WIFIDEBUG) Log.i(TAG, "onActivityResult()");
		switch (requestCode)
		{
			case Constants.REQUEST_SEARCH_WIFI:				
				if (Constants.WIFIDEBUG) Log.i(TAG, "REQUEST_SEARCH_WIFI");

				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{		
					if (Constants.WIFIDEBUG) Log.i(TAG, "RESULT_OK");
					
					// Get the WiFiService
					mWiFiConnectionManager = WiFiConnection.getInstance().getConnectionManager();
					
					initWifiConnection();
					mSendDataAvailable = true;
				}
				break;
			case Constants.DEVICE_SEARCH_FAIL:

		}
	}

	private int findCamera(int cameraType)
	{
		int cameraId = -1;

		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++)
		{
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == cameraType)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG, "Camera found");
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}

	// I think Android Documentation recommends doing this in a separate
	// task to avoid blocking main UI
	private boolean safeCameraOpen(int id)
	{
		boolean qOpened = false;
		try
		{
			releaseCamera();
			mCamera = Camera.open(id);
			qOpened = (mCamera != null);
		} catch (Exception e)
		{
			if(Constants.WIFIDEBUG) Log.e(TAG, "failed to open Camera");
			e.printStackTrace();
		}
		return qOpened;
	}

	private void releaseCamera()
	{
		if (mCamera != null)
		{
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	public void setFlash(boolean onOff)
	{
		Parameters mParameters = mCamera.getParameters();
		if (onOff)
		{
			mParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
			mCamera.setParameters(mParameters);
		}
		else
		{
			mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(mParameters);
		}
	}
	
	class CamCallback implements Camera.PreviewCallback
	{
		private static final String TAG = "CamCallback";

		public void onPreviewFrame(byte[] data, Camera camera)
		{
			if(mSendDataAvailable == false)
			{
				return;
			}
			// Process the camera data here
			Parameters params = camera.getParameters();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			// Set the captureing Size
			Rect rect = new Rect(0, 0, params.getPreviewSize().width, params.getPreviewSize().height);
			
			/*
			Log.d(TAG, "params.getPreviewSize().width: " + params.getPreviewSize().width 
					+ ", params.getPreviewSize().height: " + params.getPreviewSize().height);
			Log.d(TAG, "capture size width: " + rect.width() +", height: " + rect.height());
			 */
			
			// Capture
			YuvImage yuvImage = new YuvImage(data, 
					params.getPreviewFormat(), 
					params.getPreviewSize().width, 
					params.getPreviewSize().height, null);
			yuvImage.compressToJpeg(rect, 100, out);

			/*
			Bitmap bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size()); // decode JPG
			if (bitmap != null) 
			{
				int h = bitmap.getHeight();
				int w = bitmap.getWidth();
				Log.i(TAG, "savePhoto(): Bitmap WxH is " + w + "x" + h);
			} 
			else 
			{
				Log.i(TAG, "savePhoto(): Bitmap is null..");
			}
			*/
			
			//savePhoto(out.toByteArray());
			sendDataTodevice(out.toByteArray(), (byte) Constants.DATA_TYPE_PREVIEW_IMAGE);

			out.reset();

			yuvImage = null;
			out = null;
			//System.gc();
		}

		public boolean savePhoto(byte[] data)
		{
	        // For Debugging to save picture
	        File dir = new File(Environment.getExternalStorageDirectory() + "/brainy/pictures");
	        dir.mkdirs();
	        FileOutputStream outStream;
			try
			{
				String picName = dir.toString() + "/" + System.currentTimeMillis() + ".jpg";

				if(Constants.WIFIDEBUG) Log.i(TAG, "SavePhoto() fileName: " + picName);

				outStream = new FileOutputStream(picName);
				outStream.write(data);
				outStream.flush();
				outStream.close();
			} 
			catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

			return true;
		}

	}
	
	class CamPreview extends TextureView implements SurfaceTextureListener
	{

		private Camera camera;
		Parameters parameters;

		public CamPreview(Context context, Camera c)
		{
			super(context);
			camera = c;
		}

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
		{
			int orientation = 90;
			int previewWidth = 640;
			int previewHeight = 480;
			parameters = camera.getParameters();

			// Now that the size is known, set up the camera parameters and begin the preview.
			try
			{
				for (Integer i : parameters.getSupportedPreviewFormats()) 
				{
					if(Constants.WIFIDEBUG) Log.i(TAG, "supported preview format: " + i);
				} 

				List<Size> sizes = parameters.getSupportedPreviewSizes();
				for (Size size : sizes) 
				{
					if(Constants.WIFIDEBUG) Log.i(TAG, "supported preview size: " + size.width + "x" + size.height);
				}
			    
			    parameters.setPreviewSize(previewWidth,previewHeight);
			    
			    camera.setParameters(parameters); // apply the changes
			} 
			catch (Exception e) 
			{
				// older phone - doesn't support these calls
			}

			// Change orientation to LANDSCAPE
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(mCameraId, info);
		    
		    orientation = (orientation + 45) / 90 * 90;
		    
		    int rotation = 0;
		    if (info.facing == CameraInfo.CAMERA_FACING_FRONT) 
		    {
		    	rotation = (info.orientation - orientation + 360) % 360;
		    } 
		    else // back-facing camera
		    {  
		    	rotation = (info.orientation + orientation) % 360;
		    }
		    
		    parameters.setRotation(rotation);

		    
			Size previewSize = camera.getParameters().getPreviewSize();
			setLayoutParams(new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER));

			try
			{
				camera.setPreviewTexture(surface);
			} 
			catch (IOException t)
			{
			}

			if(Constants.WIFIDEBUG) Log.i(TAG, "Preview: checking it was set: " + previewSize.width + "x" + previewSize.height); // DEBUG
			
			camera.startPreview();
			this.setVisibility(INVISIBLE); // Make the surface invisible as soon as it is created
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
		{
			// Put code here to handle texture size change if you want to
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
		{
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface)
		{
			// Update your view here!
		}
	}
	
	// Serial Connection
	private void initSerialConnection(Context context)
	{
		mSerialConnection = new SerialConnection(context);

		mSerialDataReceiveListener = new SerialDataReceiveListener()
		{

			@Override
			public void dataReceived(byte[] data, int dataLength)
			{
				if (Constants.WIFIDEBUG) Log.d(TAG, "Serial Data Received!!! data: "+ "dataLength: " + dataLength);
				
				// To do anything with receive data

			}

		};

		mSerialConnection.setDataReceiveListener(mSerialDataReceiveListener);
	}
	
	private void sendMessageToSerialService(String direction)
	{
		if (Constants.WIFIDEBUG) Log.d(TAG, "sendMessageToSerialService() direction: " + direction);

		if (mSerialConnection != null)
		{
			mSerialConnection.sendDataToArduino(direction);
		}
	}

	BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
			{
				if (Constants.WIFIDEBUG) Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED");
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				if (Constants.WIFIDEBUG) Log.i(TAG, "ACTION_USB_DEVICE_DETACHED");
			}
		}

	};

	// WiFi P2P
	void initWifiConnection()
	{
		if (Constants.WIFIDEBUG) Log.d(TAG, "initializeWifi()");
		
		mWiFiDataReceiveListener = new WiFiDataReceiveListener()
		{

			@Override
			public void dataReceived(int dataType, byte[] data, int dataLength) 
			{
				if (Constants.WIFIDEBUG) Log.d(TAG, "WiFi Data Received!!!");

				if (dataType == Constants.DATA_TYPE_DIRECTION)
				{
					try
					{
						String direction = new String(data, "UTF-8");
						if (Constants.WIFIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_DIRECTION direction: " + direction);

						sendMessageToSerialService(direction);
					} 
					catch (UnsupportedEncodingException e)
					{

					}
				} 
				else if (dataType == Constants.DATA_TYPE_PREVIEW_IMAGE)
				{
					if (Constants.WIFIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_PREVIEW_IMAGE");

				}
				else if (dataType == Constants.DATA_TYPE_SET_FLASH)
				{
					try
					{
						String setFlash = new String(data, "UTF-8");
	
						if (Constants.WIFIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_SET_FLASH setFlash: " + setFlash);
						
						if(setFlash.equals(Constants.SET_FLASH_ON))
						{
							setFlash(true);
						}
						else if(setFlash.equals(Constants.SET_FLASH_OFF))
						{
							setFlash(false);
						}
					} 
					catch (UnsupportedEncodingException e)
					{

					}
				}
			}
		};
		
		mWiFiConnectionManager.setDataReceiveListener(mWiFiDataReceiveListener);
		
	}

	private void connectDevice(WiFiDirectService service)
	{
		if (Constants.WIFIDEBUG) Log.d(TAG, "connectDevice()");
	}

	private void sendDataTodevice(String data)
	{
		if (Constants.WIFIDEBUG) Log.e(TAG, "sendDataTodevice: " + data);
	}

	private void sendDataTodevice(byte[] data, byte dataType)
	{
		if(mSendDataAvailable == true)
		{
			if (Constants.WIFIDEBUG) Log.d(TAG, "sendDataTodevice() byte[].lgenth: " + data.length);
			mWiFiConnectionManager.write(data,dataType);
		}

	}
}
