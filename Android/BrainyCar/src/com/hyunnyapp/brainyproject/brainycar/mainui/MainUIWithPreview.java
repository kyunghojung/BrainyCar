package com.hyunnyapp.brainyproject.brainycar.mainui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Random;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.hyunnyapp.brainyproject.brainycar.Constants;
import com.hyunnyapp.brainyproject.brainycar.R;
import com.hyunnyapp.brainyproject.brainycar.controler.cam.CameraPreview;
import com.hyunnyapp.brainyproject.brainycar.serial.SerialConnectionService;
import com.hyunnyapp.brainyproject.brainycar.bluetooth.BluetoothConnection;
import com.hyunnyapp.brainyproject.brainycar.bluetooth.BluetoothDeviceSearch;
import com.hyunnyapp.brainyproject.brainycar.bluetooth.BluetoothDataReceiveListener;

public class MainUIWithPreview extends Activity implements SensorEventListener
{
	private final String TAG = "ControlerUICam";

	//UI
    private CameraPreview mPreview;
    
    // Bluetooth
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothConnection mBluetoothConnection = null;
    
	private ImageView mSettingsMenu;

	private boolean mAutoFocus = true;

	private boolean mFlashBoolean = false;

	private SensorManager mSensorManager;
	private Sensor mAccel;
	private boolean mInitialized = false;
	private float mLastX = 0;
	private float mLastY = 0;
	private float mLastZ = 0;

	private int mScreenHeight;
	private int mScreenWidth;
	
	SendPreviewThread mSendPreview;
	private boolean mSendPreviewStarted = false;
	
	private boolean isSendAvailable = true;

	public static final int mPreviewCaptureWidth = 128;
	public static final int mPreviewCaptureHeight = 96;
	public static final int mPreviewCaptureQuality = 50;
	
	private BluetoothDataReceiveListener dataReceiveListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.main_ui_with_preview);
        
		// the accelerometer is used for autofocus
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		// get the window width and height to display buttons
		// according to device screen size
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		mScreenHeight = displaymetrics.heightPixels;
		mScreenWidth = displaymetrics.widthPixels;

		mSettingsMenu = (ImageView) findViewById(R.id.settings);

		mPreview = (CameraPreview) findViewById(R.id.carmerapreview);

		mSettingsMenu.setOnClickListener(settingsListener);

        startService(new Intent(MainUIWithPreview.this, SerialConnectionService.class));	
        doBindSerialService();

        //Initialize Bluetooth
        initializeBluetooth();
	}

	// this is the autofocus call back
	private AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback()
	{
		public void onAutoFocus(boolean autoFocusSuccess, Camera arg1)
		{
			//Wait.oneSec();
			mAutoFocus = true;
		}
	};
	
	// with this I get the ratio between screen size and pixels
	// of the image so I can capture only the rectangular area of the
	// image and save it.
	public Double[] getRatio()
	{
		Size s = mPreview.getCameraParameters().getPreviewSize();
		double heightRatio = (double)s.height/(double)mScreenHeight;
		double widthRatio = (double)s.width/(double)mScreenWidth;
		Double[] ratio = {heightRatio,widthRatio};
		return ratio;
	}

	// I am not using this in this example, but its there if you want
	// to turn on and off the flash.
	private OnClickListener flashListener = new OnClickListener()
	{

		@Override
		public void onClick(View v) 
		{
			if (mFlashBoolean)
			{
				mPreview.setFlash(false);
			}
			else
			{
				mPreview.setFlash(true);
			}
			mFlashBoolean = !mFlashBoolean;
		}

	};

	private OnClickListener settingsListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{

	        PopupMenu popupMenu = new PopupMenu(MainUIWithPreview.this, v);
	        popupMenu.getMenuInflater().inflate(R.menu.main_ui_with_preview_menu, popupMenu.getMenu());
	        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() 
	        {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) 
					{
					case R.id.serial_device_search:
			            if(Constants.UIDEBUG) Log.i(TAG, "serial device search start!!!");
			        	sendMessageToSerialService(SerialConnectionService.MSG_CONNECT);
			            return true;
					case R.id.secure_connect_scan:
			            if(Constants.UIDEBUG) Log.i(TAG, "Bluetooth device search start!!!");
			            if(mBluetoothAdapter == null)
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
			        	Intent serverIntent = new Intent(MainUIWithPreview.this, BluetoothDeviceSearch.class);
			            startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE);
			            return true;
					case R.id.set_discoverable:
			            if(Constants.UIDEBUG) Log.i(TAG, "Set Bluetooth discoverable!!!");
			            
			            if(mBluetoothAdapter == null)
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
	        });        
	        popupMenu.show();  
		}
	};

	// just to close the app and release resources.
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			finish();
		}
		return super.onKeyDown(keyCode, event); 
	}

    private class SendPreviewThread extends Thread 
    {

        public SendPreviewThread(String string) 
        {
            super(string);
            mSendPreviewStarted = true;
        }

        public void run() 
        {
        	while(mSendPreviewStarted)
        	{
                if(isSendAvailable != false)
                {
                	Bitmap bitmap = mPreview.getPreview(0,0,mPreviewCaptureWidth, mPreviewCaptureHeight, mPreviewCaptureQuality);
                	savePhoto(bitmap);
                	SendPreview(bitmap);
                }
        	}
        }
    }

	private boolean SendPreview(Bitmap bitmap) 
	{
		int bytes = bitmap.getByteCount();
		ByteBuffer buffer = ByteBuffer.allocate(bytes);
		bitmap.copyPixelsToBuffer(buffer);
		byte[] byteArray = buffer.array();

		sendDataTodevice(byteArray);
		
		return true;
	}

	private boolean savePhoto(Bitmap bitmap) 
	{
		Random r = new Random();
		String file = String.valueOf(r.nextInt())+".jpg";
		
		Log.i(TAG, "SavePhoto() fileName: "+file);
		
		File mLocation = new File(Environment.getExternalStorageDirectory(),file);
		
		FileOutputStream image = null;
		try 
		{
			image = new FileOutputStream(mLocation);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		bitmap.compress(CompressFormat.JPEG, 100, image);
		
		if (bitmap != null) 
		{
			int h = bitmap.getHeight();
			int w = bitmap.getWidth();
			Log.i(TAG, "savePhoto(): Bitmap WxH is " + w + "x" + h);
		} 
		else 
		{
			Log.i(TAG, "savePhoto(): Bitmap is null..");
			return false;
		}
		return true;
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
	// mainly used for autofocus to happen when the user takes a picture
	// I also use it to redraw the canvas using the invalidate() method
	// when I need to redraw things.
	public void onSensorChanged(SensorEvent event) 
	{
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized)
		{
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			mInitialized = true;
		}
		float deltaX  = Math.abs(mLastX - x);
		float deltaY = Math.abs(mLastY - y);
		float deltaZ = Math.abs(mLastZ - z);

		if (deltaX > .5 && mAutoFocus)
		{ //AUTOFOCUS (while it is not autofocusing)
			mAutoFocus = false;
			mPreview.setCameraFocus(myAutoFocusCallback);
		}
		if (deltaY > .5 && mAutoFocus)
		{ //AUTOFOCUS (while it is not autofocusing)
			mAutoFocus = false;
			mPreview.setCameraFocus(myAutoFocusCallback);
		}
		if (deltaZ > .5 && mAutoFocus)
		{ //AUTOFOCUS (while it is not autofocusing) */
			mAutoFocus = false;
			mPreview.setCameraFocus(myAutoFocusCallback);
		}

		mLastX = x;
		mLastY = y;
		mLastZ = z;

	}

    @Override
    public synchronized void onResume() 
    {
        super.onResume();

		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_UI);
		
    }

    @Override
    public synchronized void onPause() 
    {
        super.onPause();
		mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();

        if (mBluetoothConnection != null) 
        {
        	mBluetoothConnection.stop();
        }
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        if(Constants.UIDEBUG) Log.i(TAG, "onActivityResult()");
        switch (requestCode) 
        {
        case Constants.REQUEST_CONNECT_DEVICE:
            if(Constants.UIDEBUG) Log.i(TAG, "REQUEST_CONNECT_DEVICE_SECURE");
            
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) 
            {
                // Get the device MAC address
                String address = data.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS);
                connectDevice(address);
            }
            break;
        case Constants.REQUEST_ENABLE_BT:
            if(Constants.UIDEBUG) Log.i(TAG, "REQUEST_ENABLE_BT");
            
            // When the request to enable Bluetooth returns
            if (resultCode != Activity.RESULT_OK) 
            {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    // Serial Service 
    boolean mIsBoundSerialService;
    Messenger mSerialService = null;
    final Messenger mSerialMessenger = new Messenger(new IncomingSerialHandler());

    class IncomingSerialHandler extends Handler 
    {
        @Override
        public void handleMessage(Message msg) 
        {
            if(Constants.UIDEBUG) Log.d(TAG, "IncomingSerialHandler() handleMessage: "+msg.what);
            switch (msg.what) 
            {
            case SerialConnectionService.MSG_SEND_TO_UI:
                if(Constants.UIDEBUG) Log.d(TAG, "MSG_SEND_TO_UI");
                
                String message = msg.getData().getString("message");
                if(Constants.UIDEBUG) Log.i(TAG, message);

                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    private ServiceConnection mSerialConnection = new ServiceConnection() 
    {
        public void onServiceConnected(ComponentName className, IBinder service) 
        {
            if(Constants.UIDEBUG) Log.d(TAG, "onServiceConnected() className: "+className.toString());
            
            mSerialService = new Messenger(service);

            try
            {
                Message msg = Message.obtain(null, SerialConnectionService.MSG_REGISTER_CLIENT);
                msg.replyTo = mSerialMessenger;
                mSerialService.send(msg);
            } 
            catch (RemoteException e) 
            {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) 
        {
            if(Constants.UIDEBUG) Log.d(TAG, "onServiceDisconnected()  className: "+className.toString());
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mSerialService = null;
        }
    };
    
    private void sendMessageToSerialService(String direction) 
    {
        if(Constants.UIDEBUG) Log.d(TAG, "sendMessageToSerialService() direction: "+direction);
        if (mIsBoundSerialService) 
        {
            if (mSerialService != null) 
            {
                try 
                {
                    Bundle bundle = new Bundle();
                    bundle.putString("direction", direction);
                    Message msg = Message.obtain(null, SerialConnectionService.MSG_SEND_TO_DEVICE);
                    msg.setData(bundle);

                    msg.replyTo = mSerialMessenger;
                    mSerialService.send(msg);
                } 
                catch (RemoteException e) 
                {
                }
            }
        }
    }

    private void sendMessageToSerialService(int what) 
    {
        if(Constants.UIDEBUG) Log.d(TAG, "sendMessageToSerialService() what: "+what);
        if (mIsBoundSerialService) 
        {
            if (mSerialService != null) 
            {
                try 
                {
                	Message msg = Message.obtain(null, what, 0, 0);
                    msg.replyTo = mSerialMessenger;
                    mSerialService.send(msg);
                } 
                catch (RemoteException e) 
                {
                }
            }
        }
    }
    
    void doBindSerialService() 
    {
        if(Constants.UIDEBUG) Log.d(TAG, "doBindService()");
        bindService(new Intent(this, SerialConnectionService.class), mSerialConnection, Context.BIND_AUTO_CREATE);
        mIsBoundSerialService = true;
    }
    
    void doUnbindSerialService() 
    {
        if(Constants.UIDEBUG) Log.d(TAG, "doUnbindService()");
        if (mIsBoundSerialService) 
        {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mSerialService != null) 
            {
                try 
                {
                    Message msg = Message.obtain(null, SerialConnectionService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mSerialMessenger;
                    mSerialService.send(msg);
                } 
                catch (RemoteException e) 
                {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mSerialConnection);
            mIsBoundSerialService = false;
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
				Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED");
			}
			else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) 
			{                
				Log.i(TAG, "ACTION_USB_DEVICE_DETACHED");
			}
	    }

	};
	
	// Bluetooth
	
	void initializeBluetooth()
	{
		if(Constants.UIDEBUG) Log.d(TAG, "initializeBluetooth()");

	    
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
	        // Only if the state is STATE_NONE, do we know that we haven't started already
	        if (mBluetoothConnection.getState() == Constants.BLUETOOTH_STATE_NONE) 
	        {
	          // Start the Bluetooth chat services
	        	mBluetoothConnection.start();
	        }
	    }
	    
	    dataReceiveListener = new BluetoothDataReceiveListener()
	    {

			@Override
			public void dataReceived(int dataType, byte[] data, int dataLength) 
			{
			    if(Constants.UIDEBUG) Log.d(TAG, "Data Received!!! dataType: "+dataType+", dataLength: "+dataLength);
			    
			    if(dataType == Constants.DATA_TYPE_DIRECTION)
			    {
			    	try
			    	{
			    		String direction = new String(data, "UTF-8");
					    if(Constants.UIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_DIRECTION direction: "+direction);
					    
					    sendMessageToSerialService(direction);
			    	}
			    	catch(UnsupportedEncodingException e)
			    	{
			    	
			    	}
			    }
			    else if(dataType == Constants.DATA_TYPE_PREVIEW_IMAGE)
			    {
				    if(Constants.UIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_PREVIEW_IMAGE");
			    	
			    }
				
			}
	    	
	    };
	    
	    mBluetoothConnection.setDataReceiveListener(dataReceiveListener);
	}
	
	private void setupBT() 
	{
	    if(Constants.UIDEBUG) Log.d(TAG, "setupBT()");

	    // Initialize the BluetoothChatService to perform bluetooth connections
	    mBluetoothConnection = new BluetoothConnection(this);
	}
	
	private void connectDevice(String address) 
	{
	    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	    
	    if(Constants.UIDEBUG) Log.i(TAG, "connectDevice address: " + address);


	    mBluetoothConnection.connect(device);
	}
	
	private void sendDataTodevice(String data) 
	{
	    if(Constants.UIDEBUG) Log.e(TAG, "sendDataTodevice: " + data);	

	    
	    if (mBluetoothConnection.getState() != Constants.BLUETOOTH_STATE_CONNECTED) 
	    {
	        Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
	        return;
	    }
	
	    if (data.length() > 0) 
	    {
	        byte[] send = data.getBytes();
	        mBluetoothConnection.write(send, (byte)Constants.DATA_TYPE_DIRECTION);
	    }
	}
    private void sendDataTodevice(byte[] data) 
    {
        if(isSendAvailable == false)
        {
        	return;
        }
        isSendAvailable = false;
        
        if(Constants.UIDEBUG) Log.d(TAG, "[Bluetooth] sendMessageToBluetoothService() byte[].lgenth: "+data.length);

        mBluetoothConnection.write(data,(byte)Constants.DATA_TYPE_PREVIEW_IMAGE);
    }

	public void receiveData(int type, byte[] data, int length)
	{
		if(Constants.UIDEBUG) Log.d(TAG, "receiveData length: "+length);

	}	

}
