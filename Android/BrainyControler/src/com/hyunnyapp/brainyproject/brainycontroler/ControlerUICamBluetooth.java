package com.hyunnyapp.brainyproject.brainycontroler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothConnection;
import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothDeviceSearch;
import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycontroler.arduino.ArduinoInfo;
import com.hyunnyapp.brainyproject.brainycontroler.cam.ControlerView;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.DualJoystickView;
import com.hyunnyapp.brainyproject.brainycontroler.joystick.JoystickMovedListener;

public class ControlerUICamBluetooth extends Activity
{
	private final String TAG = "ControlerUICamBluetooth";

	//UI
    private ControlerView mControlerView;
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
		
		setContentView(R.layout.controlerui_cam);

		mControlerView = (ControlerView)findViewById(R.id.controlerView);
		
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
	        if(Constants.BTDEBUG) Log.d(TAG,"Car Move: " + ArduinoInfo.getDirectionString(direction));
	        
	        sendDataTodevice(Integer.toString(direction));
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
			if(Constants.BTDEBUG) Log.d(TAG, " Servo Move: " + ArduinoInfo.getServoDirectionString(direction));

			sendDataTodevice(Integer.toString(direction));
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

        if(Constants.BTDEBUG) Log.e(TAG, "--- ON DESTROY ---");	
        
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
        if(Constants.BTDEBUG) Log.d(TAG, "onOptionsItemSelected() ID: "+item.getItemId());
        
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
        Log.d(TAG, "initializeBluetooth()");
        
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

			    if(Constants.BTDEBUG) Log.d(TAG, "Data Received!!! dataType: "+dataType+", dataLength: "+dataLength);
			    
			    if(dataType == Constants.DATA_TYPE_DIRECTION)
			    {
				    if(Constants.BTDEBUG) Log.d(TAG, "Constants.DATA_TYPE_DIRECTION");
			    }
			    else if(dataType == Constants.DATA_TYPE_PREVIEW_IMAGE)
			    {
				    if(Constants.BTDEBUG) Log.d(TAG, "Constants.DATA_TYPE_PREVIEW_IMAGE");
				    //savePhoto(data);
				    mControlerView.setUpdateFrameBuffer(data);
			    }
				
			}
	    	
	    };
	    
	    mBluetoothConnection.setDataReceiveListener(dataReceiveListener);
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
	
    private void setupBT() 
    {
        Log.d(TAG, "setupBT()");
        // Initialize the BluetoothChatService to perform bluetooth connections
        mBluetoothConnection = new BluetoothConnection(this);
    }
    
    private void connectDevice(String address) 
    {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        
        if(Constants.BTDEBUG) Log.i(TAG, "connectDevice address: " + address);        
        
        mBluetoothConnection.connect(device);
    }
    
    private void sendDataTodevice(String data) 
    {
        if(Constants.BTDEBUG) Log.e(TAG, "sendBluetoothData: " + data);	
        if (mBluetoothConnection.getState() != Constants.BLUETOOTH_STATE_CONNECTED) 
        {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (data.length() > 0) 
        {
            byte[] send = data.getBytes(Charset.forName("UTF-8"));
            mBluetoothConnection.write(send, (byte)Constants.DATA_TYPE_DIRECTION);
        }
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
//            mMjpegView.setDisplayMode(MjpegView.SIZE_STANDARD);
            mMjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mMjpegView.showFps(true);
        }
    }
    */
}
