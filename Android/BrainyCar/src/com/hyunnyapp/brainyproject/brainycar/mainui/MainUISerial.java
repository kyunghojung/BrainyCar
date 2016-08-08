package com.hyunnyapp.brainyproject.brainycar.mainui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hyunnyapp.brainyproject.brainycar.Constants;
import com.hyunnyapp.brainyproject.brainycar.R;
import com.hyunnyapp.brainyproject.brainycar.arduino.ArduinoInfo;
import com.hyunnyapp.brainyproject.brainycar.controler.joystick.DualJoystickView;
import com.hyunnyapp.brainyproject.brainycar.controler.joystick.JoystickMovedListener;
import com.hyunnyapp.brainyproject.brainycar.controler.joystick.JoystickDoubleClickedListener;
import com.hyunnyapp.brainyproject.brainycar.controler.joystick.TripleJoystickView;
import com.hyunnyapp.brainyproject.brainycar.mainui.MainUIWithPreview.IncomingSerialHandler;
import com.hyunnyapp.brainyproject.brainycar.serial.SerialConnectionService;

public class MainUISerial extends Activity 
{
	private final String TAG = "MainUISerial";

	public static final String SerialIntentTAG = "SerialData";
	
    private TripleJoystickView mTripleJoystickView;

	private ScrollView  mDebugScreen= null;
	private TextView mDebugView = null;

    public static int currentDirection = -1;
    public static int currentSpeed = -1;
    
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingSerialHandler());


    // Serial Service 
    boolean mIsBoundSerialService;
    Messenger mSerialService = null;
    final Messenger mSerialMessenger = new Messenger(new IncomingSerialHandler());

    class IncomingSerialHandler extends Handler 
    {
        @Override
        public void handleMessage(Message msg) 
        {
            if(Constants.SERIALDEBUG) Log.d(TAG, "IncomingSerialHandler() handleMessage: "+msg.what);
            switch (msg.what) 
            {
            case SerialConnectionService.MSG_SEND_TO_UI:
                if(Constants.SERIALDEBUG) Log.d(TAG, "MSG_SEND_TO_UI");
                
                String message = msg.getData().getString("message");
                if(Constants.SERIALDEBUG) Log.i(TAG, message);
    			mDebugView.append("["+TAG+"]"+ "message: " + message +" \n");

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
            if(Constants.SERIALDEBUG) Log.d(TAG, "onServiceConnected() className: "+className.toString());
            
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
            if(Constants.SERIALDEBUG) Log.d(TAG, "onServiceDisconnected()  className: "+className.toString());
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mSerialService = null;
        }
    };
    
    private void sendMessageToSerialService(String direction) 
    {
        if(Constants.SERIALDEBUG) Log.d(TAG, "sendMessageToSerialService() direction: "+direction);
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
        if(Constants.SERIALDEBUG) Log.d(TAG, "sendMessageToSerialService() what: "+what);
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
        if(Constants.SERIALDEBUG) Log.d(TAG, "doBindService()");
        bindService(new Intent(this, SerialConnectionService.class), mSerialConnection, Context.BIND_AUTO_CREATE);
        mIsBoundSerialService = true;
    }
    
    void doUnbindSerialService() 
    {
        if(Constants.SERIALDEBUG) Log.d(TAG, "doUnbindService()");
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
				if(Constants.SERIALDEBUG) Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED");
			}
			else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) 
			{                
				if(Constants.SERIALDEBUG) Log.i(TAG, "ACTION_USB_DEVICE_DETACHED");
			}
	    }

	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_ui_serial);

        mTripleJoystickView = (TripleJoystickView)findViewById(R.id.triplejoystickView_serial);
        mTripleJoystickView.setOnJostickMovedListener(movedListenerLeft, movedListenerRight, movedListenerCenter);
        mTripleJoystickView.setOnJostickDoubleClickedListener(doubleClickedListenerLeft, doubleClickedListenerRight, doubleClickedListenerCenter);
        
        mDebugScreen = (ScrollView) findViewById(R.id.debugscreen_serial);
        
		mDebugView = (TextView) findViewById(R.id.debugview_serial);

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

        startService(new Intent(MainUISerial.this, SerialConnectionService.class));	
        doBindSerialService();
        
	}

    @Override
    public void onDestroy() 
    {
        super.onDestroy();

        doUnbindSerialService();
        stopService(new Intent(MainUISerial.this, SerialConnectionService.class));
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
			if(currentDirection != -1 && currentDirection == direction)
			{
				return;
			}
			
			currentDirection = direction;
			
			mDebugView.append("["+TAG+"]"+ "Move: " + ArduinoInfo.getDirectionString(direction) +" \n");

	        if(Constants.SERIALDEBUG) Log.d(TAG,"Move: " + ArduinoInfo.getDirectionString(direction));
	        
	    	sendMessageToSerialService("D:"+Integer.toString(direction));
		}
		private void SetSpeed(int speed)
		{
			if(speed <= 3)
			{
				currentSpeed = 0;
		    	sendMessageToSerialService("S:000");
				
			}
			else if(speed > 3 && speed < 8 )
			{
				if(currentSpeed != 100)
				{
			        if(Constants.SERIALDEBUG) Log.d(TAG,"SetSpeed: 100");
					mDebugView.append("["+TAG+"]"+ "Move: " + "SetSpeed: 100 \n");
			        
					currentSpeed = 100;
			    	sendMessageToSerialService("S:100");
				}
			}
			else
			{
				if(currentSpeed != 255)
				{
			        if(Constants.SERIALDEBUG) Log.d(TAG,"SetSpeed: 255");
					mDebugView.append("["+TAG+"]"+ "Move: " + "SetSpeed: 255 \n");
			        
					currentSpeed = 255;
			    	sendMessageToSerialService("S:255");
				}
			}
		}
		public void Move(int pan, int tilt)
		{
			// limit to {0..10}
			int radiusL = (byte) ( Math.min( Math.sqrt((pan*pan) + (tilt*tilt)), 10.0 ) );
			
			// scale to {0..35}
			int angleL = (byte) ( Math.atan2(-pan, -tilt) * 18.0 / Math.PI + 36.0 + 0.5 );

			SetSpeed(radiusL);
			
			if( angleL >= 36 )	
			{
				angleL = (byte)(angleL-36);
			}
			
			Move(ArduinoInfo.get4Direction(angleL));

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

    private JoystickMovedListener movedListenerRight = new JoystickMovedListener() 
    {
    	@Override
		public void OnMoved(int pan, int tilt) 
		{
			Move(pan, tilt);
		}

		private void Move(int direction) 
		{
			mDebugView.append("["+TAG+"]"+ "Move: " + ArduinoInfo.getServoDirectionString(direction) +" \n");

			sendMessageToSerialService("C:"+Integer.toString(direction));
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

			mDebugView.append("["+TAG+"]"+ "Hand Move: " + ArduinoInfo.getHandActionString(direction) +" \n");

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
			}
	    	sendMessageToSerialService("H:"+sendData);
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
	    	sendMessageToSerialService("D:"+"0");
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

	    	sendMessageToSerialService("C:"+"5");
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
	    	sendMessageToSerialService("H:"+"0");
		}
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
		getMenuInflater().inflate(R.menu.main_ui_with_preview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        if(Constants.SERIALDEBUG) Log.e(TAG, "onOptionsItemSelected() ID: "+item.getItemId());
        
        switch (item.getItemId()) 
        {
        case R.id.serial_device_search:
            if(Constants.SERIALDEBUG) Log.e(TAG, "device search start!!!");
        	if(Constants.SERIALDEBUG) mDebugView.append("device search start!!! \n");

        	sendMessageToSerialService(SerialConnectionService.MSG_CONNECT);
            return true;
        }
        return false;
    }

	public void addDebugMessage(String message)
	{
		try
		{
			if(message.length() <= 280 )
			{
				mDebugView.append(message+"\n");
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "occure exception: " + e);
		}
	}
}
