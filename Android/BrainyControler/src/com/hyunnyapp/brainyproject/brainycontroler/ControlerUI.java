package com.hyunnyapp.brainyproject.brainycontroler;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothConnection;
import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothDeviceSearch;
import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycontroler.arduino.ArduinoInfo;

import com.hyunnyapp.brainyproject.brainycontroler.joystick.DualJoystickView;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.JoystickMovedListener;

public class ControlerUI extends Activity
{
	
	private final String TAG = "ControlerUI";
	
	private ScrollView  mDebugScreen= null;
	private TextView mDebugView = null;
	
	//UI
	private DualJoystickView mDualJoystickView;
	
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothConnection mBluetoothConnection = null;
	
	private BluetoothDataReceiveListener dataReceiveListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	
		setContentView(R.layout.controlerui_joystick);
	
	    mDebugScreen = (ScrollView) findViewById(R.id.debugscreen_controler);
	    
		mDebugView = (TextView) findViewById(R.id.debugview_controler);
	
		mDebugView.addTextChangedListener(new TextWatcher() 
		{
	        @Override
	        public void afterTextChanged(Editable arg0) 
	        {
	        	mDebugScreen.fullScroll(ScrollView.FOCUS_DOWN);
	        }
	        @Override
	        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	        @Override
	        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	    });
		
	    mDualJoystickView = (DualJoystickView)findViewById(R.id.dualjoystickView_controler);
	    mDualJoystickView.setOnJostickMovedListener(listenerLeft, listenerRight);
	    
	    //Initialize Bluetooth
	    initializeBluetooth();
	
		//new DoRead().execute( hostname, videoPortNum);
	}
	
	private JoystickMovedListener listenerLeft = new JoystickMovedListener() 
	{
		@Override
		public void OnMoved(int pan, int tilt) 
		{
			Move(pan, tilt);
		}
	
		private void Move(int direction) 
		{

	        if(Constants.UIDEBUG) 
	        {
	        	Log.d(TAG,"Car Move: " + ArduinoInfo.getDirectionString(direction));
	        	Log(TAG,"Car Move: " + ArduinoInfo.getDirectionString(direction));
	        }
	        
	        sendDataTodevice(Integer.toString(direction), (byte)Constants.DATA_TYPE_DIRECTION);
		}
		
		public void Move(int pan, int tilt)
		{
			Move(ArduinoInfo.get4Direction(pan, tilt));
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
	
	private JoystickMovedListener listenerRight = new JoystickMovedListener() 
	{
		@Override
		public void OnMoved(int pan, int tilt) 
		{
			Move(pan, tilt);
		}
	
		private void Move(int direction) 
		{
			if(Constants.UIDEBUG) 
			{
				Log.d(TAG, " Servo Move: " + ArduinoInfo.getServoDirectionString(direction));
				Log(TAG, " Servo Move: " + ArduinoInfo.getServoDirectionString(direction));
			}
	
			sendDataTodevice(Integer.toString(direction), (byte)Constants.DATA_TYPE_DIRECTION);
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
			//Move(MOVE_STOP);
		}
		
		@Override
		public void OnReturnedToCenter() 
		{
			Move(ArduinoInfo.SERVO_CENTER);
		};
	}; 
	
	@Override
	public void onDestroy() 
	{
	    super.onDestroy();
	
	    if(Constants.UIDEBUG) Log.e(TAG, "--- ON DESTROY ---");	
	    
	    if (mBluetoothConnection != null) 
	    {
	    	mBluetoothConnection.stop();
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.controlerui_bluetooth_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    if(Constants.UIDEBUG) 
	    {
	    	Log.d(TAG, "onOptionsItemSelected() ID: "+item.getItemId());
	    	Log(TAG, "onOptionsItemSelected() ID: "+item.getItemId());
	    }
	    
	    Intent serverIntent = null;
	    switch (item.getItemId()) 
	    {
	    case R.id.secure_connect_scan:
	        // Launch the DeviceListActivity to see devices and do scan
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
	        
	        serverIntent = new Intent(this, BluetoothDeviceSearch.class);
	        startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE);
	        return true;
	    case R.id.discoverable:
	        // Ensure this device is discoverable by others
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
	    switch (requestCode) 
	    {
	    case Constants.REQUEST_CONNECT_DEVICE:
	        // When DeviceListActivity returns with a device to connect
	        if (resultCode == Activity.RESULT_OK) 
	        {
	            // Get the device MAC address
	            String address = data.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS);
	            connectDevice(address);
	        }
	        break;
	    case Constants.REQUEST_ENABLE_BT:
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
	
	void initializeBluetooth()
	{
		if(Constants.UIDEBUG) 
		{
		    Log.d(TAG, "initializeBluetooth()");
		    Log(TAG, "initializeBluetooth()");
		}
	    
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
				
			}
	    	
	    };
	    mBluetoothConnection.setDataReceiveListener(dataReceiveListener);
	}
	
	private void setupBT() 
	{
	    if(Constants.UIDEBUG) 
	    {
	    	Log.d(TAG, "setupBT()");
	    	Log(TAG, "setupBT()");
	    }
	    // Initialize the BluetoothChatService to perform bluetooth connections
	    mBluetoothConnection = new BluetoothConnection(this);
	}
	
	private void connectDevice(String address) 
	{
	    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	    
	    if(Constants.UIDEBUG) 
	    {
	    	Log.i(TAG, "connectDevice address: " + address);
	    	Log(TAG, "connectDevice address: " + address);
	    }

	    mBluetoothConnection.connect(device);
	}
	
	private void sendDataTodevice(String data, byte dataType) 
	{
	    if(Constants.UIDEBUG)
	    {
	    	Log.e(TAG, "sendDataTodevice: " + data);	
	    	Log(TAG, "sendDataTodevice: " + data);
	    }
	    
	    if (mBluetoothConnection.getState() != Constants.BLUETOOTH_STATE_CONNECTED) 
	    {
	        Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
	        return;
	    }
	
	    if (data.length() > 0) 
	    {
	        byte[] send = data.getBytes();
	        mBluetoothConnection.write(send, dataType);
	    }
	}

	public void receiveData(int type, byte[] data, int length)
	{
		if(Constants.UIDEBUG) Log.d(TAG, "receiveData length: "+length);
		
	}
	
	/*
	public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
		protected MjpegInputStream doInBackground( String... params){
			Socket socket = null;
			try {
				socket = new Socket( params[0], Integer.valueOf( params[1]));
	    		return (new MjpegInputStream(socket.getInputStream()));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
	    protected void onPostExecute(MjpegInputStream result) {
	        mMjpegView.setSource(result);
	        if(result!=null){
	        	result.setSkip(1);
	        }
	//        mMjpegView.setDisplayMode(MjpegView.SIZE_STANDARD);
	        mMjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
	        mMjpegView.showFps(true);
	    }
	}
	*/
	void Log(String Tag, String message)
	{
		mDebugView.append("["+Tag+"]"+ message +" \n");
	}

}




