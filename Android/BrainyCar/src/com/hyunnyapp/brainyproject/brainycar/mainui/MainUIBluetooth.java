package com.hyunnyapp.brainyproject.brainycar.mainui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import com.hyunnyapp.brainyproject.brainycar.bluetooth.BluetoothConnection;
import com.hyunnyapp.brainyproject.brainycar.bluetooth.BluetoothDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycar.bluetooth.BluetoothDeviceSearch;
import com.hyunnyapp.brainyproject.brainycar.serial.SerialConnection;
import com.hyunnyapp.brainyproject.brainycar.serial.SerialDataReceiveListener;

public class MainUIBluetooth extends Activity
{
	private static final String TAG = "MainUIBluetooth";

	// Bluetooth
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothConnection mBluetoothConnection = null;
	private BluetoothDataReceiveListener mBluetoothDataReceiveListener;

	private SerialConnection mSerialConnection = null;
	private SerialDataReceiveListener mSerialDataReceiveListener;
	
	private int mCameraId = 0;
	private Camera mCamera;
	private CamPreview mCamPreview;
	private CamCallback mCamCallback;
	public int mPreviewHeight = 0;
	public int mPreviewWidth = 0;


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

		mCamPreview = new CamPreview(MainUIBluetooth.this, mCamera);
		mCamPreview.setSurfaceTextureListener((SurfaceTextureListener) mCamPreview);

		FrameLayout preview = (FrameLayout) findViewById(R.id.previewLayout);
		preview.addView(mCamPreview);

		// Attach a callback for preview
		mCamCallback = new CamCallback();
		mCamera.setPreviewCallback(mCamCallback);

		
		// Start Serial Service 
		//startService(new Intent(ControlerUIWithoutPreview.this, SerialConnectionService.class));
		//doBindSerialService();

		// Initialize Bluetooth
		initializeBluetooth();
		
		initSerialConnection(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		releaseCamera();
		
		if (mBluetoothConnection != null)
		{
			mBluetoothConnection.stop();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_ui_without_preview_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (Constants.BTDEBUG)
			Log.d(TAG, "onOptionsItemSelected() ID: " + item.getItemId());

		Intent serverIntent = null;
		switch (item.getItemId())
		{
			case R.id.serial_device_search:
				if (Constants.BTDEBUG)
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
			case R.id.secure_connect_scan:
				if (Constants.BTDEBUG)
					Log.i(TAG, "Bluetooth device search start!!!");
				if (mBluetoothAdapter == null)
				{
					mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
					if (mBluetoothAdapter == null)
					{
						finish();
						return true;
					}

					if (!mBluetoothAdapter.isEnabled())
					{
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
					}
				}
				serverIntent = new Intent(MainUIBluetooth.this, BluetoothDeviceSearch.class);
				startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE);
				return true;
			case R.id.set_discoverable:
				if(Constants.BTDEBUG)
					Log.i(TAG, "Set Bluetooth discoverable!!!");

				if (mBluetoothAdapter == null)
				{
					mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
					if (mBluetoothAdapter == null)
					{
						finish();
						return true;
					}

					if (!mBluetoothAdapter.isEnabled())
					{
						Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
					}
				}
				if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
				{
					Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
					startActivity(discoverableIntent);
				}
				return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(Constants.BTDEBUG)
			Log.i(TAG, "onActivityResult()");
		switch (requestCode)
		{
			case Constants.REQUEST_CONNECT_DEVICE:
				if(Constants.BTDEBUG)
					Log.i(TAG, "REQUEST_CONNECT_DEVICE_SECURE");

				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{
					// Get the device MAC address
					String address = data.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS);
					connectDevice(address);
				}
				break;
			case Constants.REQUEST_ENABLE_BT:
				if(Constants.BTDEBUG)
					Log.i(TAG, "REQUEST_ENABLE_BT");

				// When the request to enable Bluetooth returns
				if (resultCode != Activity.RESULT_OK)
				{
					// User did not enable Bluetooth or an error occurred
					if (Constants.BTDEBUG) Log.d(TAG, "BT not enabled");
					Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
					finish();
				}
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
				if(Constants.BTDEBUG) Log.d(TAG, "Camera found");
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
			if(Constants.BTDEBUG) Log.e(TAG, "failed to open Camera");
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

	public void setFlash(boolean flash)
	{
		Parameters mParameters = mCamera.getParameters();
		if (flash)
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
		private static final String TAG = "ControlerUIWithoutPreview";

		public void onPreviewFrame(byte[] data, Camera camera)
		{
			// Process the camera data here
			Parameters params = camera.getParameters();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			// Set the captureing Size
			Rect rect = new Rect(0, 0, params.getPreviewSize().width, params.getPreviewSize().height);
			
			if(Constants.BTDEBUG) Log.d(TAG, "params.getPreviewSize().width: " + params.getPreviewSize().width 
									+ ", params.getPreviewSize().height: " + params.getPreviewSize().height);
			if(Constants.BTDEBUG) Log.d(TAG, "capture size width: " + rect.width() +", height: " + rect.height());
			
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
			sendDataTodevice(out.toByteArray());

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

				if(Constants.BTDEBUG) Log.i(TAG, "SavePhoto() fileName: " + picName);

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
			int previewWidth = 128;
			int previewHeight = 96;
			parameters = camera.getParameters();

			// Now that the size is known, set up the camera parameters and begin the preview.
			try
			{
				for (Integer i : parameters.getSupportedPreviewFormats()) 
				{
					if(Constants.BTDEBUG) Log.i(TAG, "supported preview format: " + i);
				} 

				List<Size> sizes = parameters.getSupportedPreviewSizes();
				for (Size size : sizes) 
				{
					if(Constants.BTDEBUG) Log.i(TAG, "supported preview size: " + size.width + "x" + size.height);
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

			if(Constants.BTDEBUG) Log.i(TAG, "Preview: checking it was set: " + previewSize.width + "x" + previewSize.height); // DEBUG
			
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
				if(Constants.BTDEBUG) Log.d(TAG, "Serial Data Received!!! data: "+ "dataLength: " + dataLength);
				
				// To do anything with receive data

			}

		};

		mSerialConnection.setDataReceiveListener(mSerialDataReceiveListener);
	}
	
	private void sendMessageToSerialService(String direction)
	{
		if (Constants.BTDEBUG) Log.d(TAG, "sendMessageToSerialService() direction: " + direction);

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
				if (Constants.BTDEBUG) Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED");
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				if (Constants.BTDEBUG) Log.i(TAG, "ACTION_USB_DEVICE_DETACHED");
			}
		}

	};

	// Bluetooth

	void initializeBluetooth()
	{
		if (Constants.BTDEBUG) Log.d(TAG, "initializeBluetooth()");

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null)
		{
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			return;
		}

		if (!mBluetoothAdapter.isEnabled())
		{
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
		}

		if (mBluetoothConnection == null)
		{
			setupBT();
		}

		if (mBluetoothConnection != null)
		{
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mBluetoothConnection.getState() == Constants.BLUETOOTH_STATE_NONE)
			{
				// Start the Bluetooth chat services
				mBluetoothConnection.start();
			}
		}

		mBluetoothDataReceiveListener = new BluetoothDataReceiveListener()
		{

			@Override
			public void dataReceived(int dataType, byte[] data, int dataLength)
			{
				if (Constants.BTDEBUG) Log.d(TAG, "Data Received!!! dataType: " + dataType + ", dataLength: " + dataLength);

				if (dataType == Constants.DATA_TYPE_DIRECTION)
				{
					try
					{
						String direction = new String(data, "UTF-8");
						if (Constants.BTDEBUG) Log.d(TAG, "Constants.DATA_TYPE_DIRECTION direction: " + direction);

						sendMessageToSerialService(direction);
					} 
					catch (UnsupportedEncodingException e)
					{

					}
				} 
				else if (dataType == Constants.DATA_TYPE_PREVIEW_IMAGE)
				{
					if (Constants.BTDEBUG) Log.d(TAG, "Constants.DATA_TYPE_PREVIEW_IMAGE");

				}

			}

		};

		mBluetoothConnection.setDataReceiveListener(mBluetoothDataReceiveListener);
	}

	private void setupBT()
	{
		if (Constants.BTDEBUG) Log.d(TAG, "setupBT()");

		// Initialize the BluetoothChatService to perform bluetooth connections
		mBluetoothConnection = new BluetoothConnection(this);
	}

	private void connectDevice(String address)
	{
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		if (Constants.BTDEBUG) Log.i(TAG, "connectDevice address: " + address);

		mBluetoothConnection.connect(device);
	}

	private void sendDataTodevice(String data)
	{
		if (Constants.BTDEBUG) Log.e(TAG, "sendDataTodevice: " + data);

		if (mBluetoothConnection.getState() != Constants.BLUETOOTH_STATE_CONNECTED)
		{
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		if (data.length() > 0)
		{
			byte[] send = data.getBytes();
			mBluetoothConnection.write(send, (byte) Constants.DATA_TYPE_DIRECTION);
		}
	}

	private void sendDataTodevice(byte[] data)
	{
		if (Constants.BTDEBUG) Log.d(TAG, "[Bluetooth] sendMessageToBluetoothService() byte[].lgenth: " + data.length);

		mBluetoothConnection.write(data, (byte) Constants.DATA_TYPE_PREVIEW_IMAGE);
	}

	public void receiveData(int type, byte[] data, int length)
	{
		if (Constants.BTDEBUG) Log.d(TAG, "receiveData length: " + length);

	}

}
