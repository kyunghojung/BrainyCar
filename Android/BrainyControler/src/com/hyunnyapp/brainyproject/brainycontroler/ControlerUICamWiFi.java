package com.hyunnyapp.brainyproject.brainycontroler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.hyunnyapp.brainyproject.brainycontroler.Constants;
import com.hyunnyapp.brainyproject.brainycontroler.wifidirect.WiFiConnection;
import com.hyunnyapp.brainyproject.brainycontroler.wifidirect.WiFiConnectionManager;
import com.hyunnyapp.brainyproject.brainycontroler.wifidirect.WiFiDeviceSearch;
import com.hyunnyapp.brainyproject.brainycontroler.wifidirect.WiFiDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycontroler.arduino.ArduinoInfo;
import com.hyunnyapp.brainyproject.brainycontroler.cam.ControlerView;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.DualJoystickView;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.JoystickDoubleClickedListener;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.JoystickMovedListener;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.TripleJoystickView;

public class ControlerUICamWiFi extends Activity
{
	private final String TAG = "ControlerUICamWiFi";

	//UI
    private ControlerView mControlerView;
    private TripleJoystickView mTripleJoystickView;

	private boolean mSendDataAvailable = false;
	private WiFiConnectionManager mWiFiConnectionManager;
	private WiFiDataReceiveListener mWiFiDataReceiveListener;
	
	private ImageView mSettingsMenu;
	private ImageView mFlashOnOff;
	
	private boolean mFlashOnoff = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.controlerui_cam);

		mControlerView = (ControlerView)findViewById(R.id.controlerView);
		
        mTripleJoystickView = (TripleJoystickView)findViewById(R.id.triplejoystickView_controler);
        mTripleJoystickView.setOnJostickMovedListener(movedListenerLeft, movedListenerRight, movedListenerCenter);
        mTripleJoystickView.setOnJostickDoubleClickedListener(doubleClickedListenerLeft, doubleClickedListenerRight, doubleClickedListenerCenter);
        
		mSettingsMenu = (ImageView) findViewById(R.id.wifi_settings);
		mSettingsMenu.setOnClickListener(settingsListener);
		
		mFlashOnOff = (ImageView) findViewById(R.id.flash_onoff);
		mFlashOnOff.setOnClickListener(setFlashListener);
	}

	private OnClickListener settingsListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{

	        PopupMenu popupMenu = new PopupMenu(ControlerUICamWiFi.this, v);
	        popupMenu.getMenuInflater().inflate(R.menu.controlerui_wifi_menu, popupMenu.getMenu());
	        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() 
	        {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) 
					{
					case R.id.wifi_device_search:
			            if(Constants.WIFIDEBUG) Log.i(TAG, "serial device search start!!!");
			        	Intent intent = new Intent(ControlerUICamWiFi.this, WiFiDeviceSearch.class);
			            startActivityForResult(intent, Constants.REQUEST_SEARCH_WIFI);
			            return true;
					}
					return false;
				}
	        });        
	        popupMenu.show();  
		}
	};
	
	private OnClickListener setFlashListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if(mFlashOnoff == false)
			{
				Toast.makeText(ControlerUICamWiFi.this, "Flash is turned on!", Toast.LENGTH_SHORT).show();
				mFlashOnOff.setImageResource(R.drawable.ic_action_flash_off); 
				mFlashOnoff = true;
				setFlash(true);
			}
			else
			{
				Toast.makeText(ControlerUICamWiFi.this, "Flash is turned off!", Toast.LENGTH_SHORT).show();
				mFlashOnOff.setImageResource(R.drawable.ic_action_flash_on); 
				mFlashOnoff = false;
				setFlash(false);
			}
		}
	};

	void setFlash(boolean onOff)
	{
		sendDataTodevice(onOff == true?Constants.SET_FLASH_ON:Constants.SET_FLASH_OFF, (byte) Constants.DATA_TYPE_SET_FLASH);
	}
	
    private JoystickMovedListener movedListenerLeft = new JoystickMovedListener() 
    {
		@Override
		public void OnMoved(int pan, int tilt) 
		{
			Move(pan, tilt);
		}

		private void Move(int direction) 
		{
	        if(Constants.WIFIDEBUG) Log.d(TAG,"Car Move: " + ArduinoInfo.getDirectionString(direction));
	        
	        sendDataTodevice(Integer.toString(direction), (byte) Constants.DATA_TYPE_DIRECTION);
		}
		
		public void Move(int pan, int tilt)
		{
			Move(ArduinoInfo.get4Direction(pan, tilt));
		}

		@Override
		public void OnReleased() 
		{
			Move(ArduinoInfo.STOP);
			Move(ArduinoInfo.STOP);
			Move(ArduinoInfo.STOP);
		}
		
		@Override
		public void OnReturnedToCenter() 
		{
			Move(ArduinoInfo.STOP);
			Move(ArduinoInfo.STOP);
			Move(ArduinoInfo.STOP);
		};
	}; 

    private JoystickMovedListener movedListenerRight = new JoystickMovedListener() 
    {
    	@Override
		public void OnMoved(int pan, int tilt) 
		{
			Move(pan, tilt);
		}

		private void Move(int direction) 
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, " Servo Move: " + ArduinoInfo.getServoDirectionString(direction));

			sendDataTodevice(Integer.toString(direction), (byte) Constants.DATA_TYPE_DIRECTION);
		}
	    		
		public void Move(int pan, int tilt)
		{
			// limit to {0..10}
			int radiusL = (byte) ( Math.min( Math.sqrt((pan*pan) + (tilt*tilt)), 10.0 ) );
			
			// scale to {0..35}
			int angleL = (byte) ( Math.atan2(-pan, -tilt) * 18.0 / Math.PI + 36.0 + 0.5 );

			if(radiusL < 3 )
			{
				return;
			}
			
			if( angleL >= 36 )	
			{
				angleL = (byte)(angleL-36);
			}
			
			Move(ArduinoInfo.getServoDirection(angleL));

		}

		@Override
		public void OnReleased() 
		{
		}
		
		@Override
		public void OnReturnedToCenter() 
		{
			//Move(ArduinoInfo.SERVO_CENTER);
		};
	}; 

    private JoystickMovedListener movedListenerCenter = new JoystickMovedListener() 
    {
    	@Override
		public void OnMoved(int pan, int tilt) 
		{
			Move(pan, tilt);
		}

		private void Move(int direction) 
		{
			String sendData = null;
			
			if(Constants.WIFIDEBUG) Log.d(TAG, " Hand Move: " + ArduinoInfo.getHandActionString(direction));

			switch(direction)
			{
				case ArduinoInfo.WRIST_UP:
					sendData = "a";
					break;
				case ArduinoInfo.WRIST_DOWN:
					sendData = "b";
					break;
				case ArduinoInfo.HAND_OPEN:
					sendData = "c";
					break;
				case ArduinoInfo.HAND_CLOSE:
					sendData = "d";
					break;
				case ArduinoInfo.STOP:
				default:
					sendData = "0";
					break;
			}
			sendDataTodevice(sendData, (byte) Constants.DATA_TYPE_DIRECTION);
		}
	    		
		public void Move(int pan, int tilt)
		{
			// limit to {0..10}
			int radiusL = (byte) ( Math.min( Math.sqrt((pan*pan) + (tilt*tilt)), 10.0 ) );
			
			// scale to {0..35}
			int angleL = (byte) ( Math.atan2(-pan, -tilt) * 18.0 / Math.PI + 36.0 + 0.5 );

			if(radiusL < 3 )
			{
				return;
			}
			
			if( angleL >= 36 )	
			{
				angleL = (byte)(angleL-36);
			}
			
			Move(ArduinoInfo.getHandAction(angleL));
		}

		@Override
		public void OnReleased() 
		{
			Move(ArduinoInfo.STOP);
		}
		
		@Override
		public void OnReturnedToCenter() 
		{
			Move(ArduinoInfo.STOP);

		};
	}; 

    private JoystickDoubleClickedListener doubleClickedListenerLeft = new JoystickDoubleClickedListener() 
    {
		@Override
		public void OnDoubleClicked() 
		{
			Stop();
		}

		private void Stop() 
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, "Double clicked! Car Stop");
			sendDataTodevice("0", (byte) Constants.DATA_TYPE_DIRECTION);
		}
    };
    
    private JoystickDoubleClickedListener doubleClickedListenerRight = new JoystickDoubleClickedListener() 
    {
		@Override
		public void OnDoubleClicked() 
		{
			Center();
		}

		private void Center() 
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, "Double clicked! Servo Center");

			sendDataTodevice("5", (byte) Constants.DATA_TYPE_DIRECTION);
		}
	    		
    };
    
    private JoystickDoubleClickedListener doubleClickedListenerCenter = new JoystickDoubleClickedListener() 
    {
		@Override
		public void OnDoubleClicked() 
		{
			Stop();
		}

		private void Stop() 
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, "Double clicked! Hand Stop");
			sendDataTodevice("0", (byte) Constants.DATA_TYPE_DIRECTION);
		}
    };
    
    @Override
    public void onDestroy() 
    {
        super.onDestroy();

        if(Constants.WIFIDEBUG) Log.e(TAG, "--- ON DESTROY ---");	
        
    }
	
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
		if (Constants.WIFIDEBUG) Log.d(TAG, "onActivityResult() : "+requestCode);

		switch (requestCode)
		{
			case Constants.REQUEST_SEARCH_WIFI:				
				if (Constants.WIFIDEBUG)
					Log.i(TAG, "REQUEST_SEARCH_WIFI");
	
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{		
					if (Constants.WIFIDEBUG)
						Log.i(TAG, "RESULT_OK");
	
					// Get the WiFiService
					mWiFiConnectionManager = WiFiConnection.getInstance().getConnectionManager();
					
					initWifiConnection();
					mSendDataAvailable = true;
				}
				break;
			case Constants.DEVICE_SEARCH_FAIL:
				break;
		}
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

			Log.i(TAG, "SavePhoto() fileName: " + picName);

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
	
    
    private void sendDataTodevice(String data, byte dataType) 
    {
        if(Constants.WIFIDEBUG) Log.e(TAG, "sendDataTodevice: " + data);	
        
        if (mSendDataAvailable != true) 
        {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

		if(mSendDataAvailable == true)
		{
            byte[] send = data.getBytes(Charset.forName("UTF-8"));
			mWiFiConnectionManager.write(send, dataType);
		}
    }

	private void sendDataTodevice(byte[] data, byte dataType)
	{
		if (Constants.WIFIDEBUG) Log.d(TAG, "sendDataTodevice() byte[].lgenth: " + data.length);
		if(mSendDataAvailable == true)
		{
			mWiFiConnectionManager.write(data, dataType);
		}

	}
	
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


			    if(Constants.WIFIDEBUG) Log.d(TAG, "Data Received!!! dataType: "+dataType+", dataLength: "+dataLength);
			    
			    if(dataType == Constants.DATA_TYPE_DIRECTION)
			    {
				    if(Constants.WIFIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_DIRECTION");
			    }
			    else if(dataType == Constants.DATA_TYPE_PREVIEW_IMAGE)
			    {
				    if(Constants.WIFIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_PREVIEW_IMAGE");
				    //savePhoto(data);
				    mControlerView.setUpdateFrameBuffer(data);
			    }
				else if (dataType == Constants.DATA_TYPE_SET_FLASH)
				{
				    if(Constants.WIFIDEBUG) Log.d(TAG, "Constants.DATA_TYPE_SET_FLASH");
				}

			}
		};
		
		mWiFiConnectionManager.setDataReceiveListener(mWiFiDataReceiveListener);
	}
}