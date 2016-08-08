package com.hyunnyapp.brainyproject.brainycar.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.*;
import com.hyunnyapp.brainyproject.brainycar.Constants;

public class SerialConnectionService extends Service 
{
    private static final String TAG = "SerialConnectionService";

	public static final String SerialIntentTAG = "SerialData";

    public static final int ARDUINO_USB_VENDOR_ID = 0x2341;
    public static final int SPLDUINO_USB_VENDOR_ID = 0x10C4;
    public static final int ARDUINO_UNO_R3_USB_PRODUCT_ID = 0x43;
    public static final int SPLDUINO_USB_PRODUCT_ID = 0xEA60;
    
    private UsbManager mUsbManager;
    private List<SerialDeviceEntry> mEntries = new ArrayList<SerialDeviceEntry>();
    
    private SerialDeviceEntry mTargetDevice;

    private static boolean isConnectedToTarget = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    final Messenger mMessenger = new Messenger(new IncomingHandler()); 

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;

    public static final int MSG_DISCONNECT = 3;
    public static final int MSG_CONNECT = 4;
    public static final int MSG_SEND_TO_DEVICE = 5;
    public static final int MSG_SEND_TO_UI = 6;
    public static final int MSG_STOP_SEND_THREAD = 7;

    private static boolean isRunning = false;
    
    private SenderThread mSenderThread;
    
    @Override
    public IBinder onBind(Intent intent) 
    {
        return mMessenger.getBinder();
    }
    
    class IncomingHandler extends Handler // Handler of incoming messages from clients.
    { 
        @Override
        public void handleMessage(Message msg) 
        {
            if(Constants.SERIALDEBUG)  Log.d(TAG, "IncomingHandler() handleMessage(): "+msg.what);
            switch (msg.what) 
            {
            case MSG_REGISTER_CLIENT:
	            if(Constants.SERIALDEBUG)  Log.d(TAG, "MSG_REGISTER_CLIENT");
                mClients.add(msg.replyTo);
                break;
                
            case MSG_UNREGISTER_CLIENT:
	            if(Constants.SERIALDEBUG)  Log.d(TAG, "MSG_UNREGISTER_CLIENT");
                mClients.remove(msg.replyTo);
                break;
                
            case MSG_DISCONNECT:
	            if(Constants.SERIALDEBUG)  Log.d(TAG, "MSG_DISCONNECT");
	            mSenderThread.mHandler.sendEmptyMessage(MSG_STOP_SEND_THREAD);
	            isConnectedToTarget = false;
                break;
                
            case MSG_CONNECT:
	            if(Constants.SERIALDEBUG)  Log.d(TAG, "MSG_CONNECT");
	            
	            connectToArduino();

                break;
            case MSG_SEND_TO_DEVICE:
	            if(Constants.SERIALDEBUG)  Log.d(TAG, "MSG_SEND_TO_DEVICE");
            	String message = msg.getData().getString("direction");

	            if(Constants.SERIALDEBUG)  Log.d(TAG, "send message: " + message);
	            
            	if(isConnectedToTarget == false)
	    		{
		            if(Constants.SERIALDEBUG)  Log.d(TAG, "Device isn't connected!");
	    			return;
	    		}

                mSenderThread.mHandler.obtainMessage(MSG_SEND_TO_DEVICE, message).sendToTarget();

                break;
            case MSG_SEND_TO_UI:

                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    private void sendMessageToUI(String message) 
    {
        for (int i = mClients.size()-1; i >= 0; i--) 
        {
            try 
            {
                //Send data as a String
                Bundle b = new Bundle();
                b.putString("message", message);
                Message msg = Message.obtain(null, MSG_SEND_TO_UI);
                msg.setData(b);
                mClients.get(i).send(msg);

            } 
            catch (RemoteException e) 
            {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    
    public static boolean isRunning()
    {
        return isRunning;
    }
    
    @Override
    public void onCreate() 
    {
        super.onCreate();
        if(Constants.SERIALDEBUG)  Log.d(TAG, "onCreate()");
        
        isRunning = true;
        
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
        if(Constants.SERIALDEBUG)  Log.d(TAG, "onStartCommand() flags: "+flags+", startId: "+startId);

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();

        isConnectedToTarget = false;
        isRunning = false;
    }

    private boolean getSerialDeviceList() 
    {
        Log.d(TAG, "Getting device list ...");

        mEntries.clear();

        final List<SerialDeviceEntry> result = new ArrayList<SerialDeviceEntry>();
        
        for (final UsbDevice device : mUsbManager.getDeviceList().values()) 
        {
            final List<UsbSerialDriver> drivers =
                    UsbSerialProber.probeSingleDevice(mUsbManager, device);
            
            Log.d(TAG, "Found usb device: " + device);

            if (drivers.isEmpty()) 
            {
                Log.d(TAG, "  - No UsbSerialDriver available.");
                result.add(new SerialDeviceEntry(device, null));
            } 
            else 
            {
                for (UsbSerialDriver driver : drivers) 
                {
                    Log.d(TAG, "  + " + driver);
                    result.add(new SerialDeviceEntry(device, driver));
                }
            }
        }

        if(result.isEmpty())
        {
        	return false;
        }
        mEntries.addAll(result);
        return true;
    }
    
    private void setTargetDevice()
    {
    	Log.d(TAG, "setTargetDevice() "+mEntries.size() + " devices is found! " );
    	
        for(SerialDeviceEntry entry : mEntries)
        {
        	if (Constants.SERIALDEBUG) 
            {
	            Log.d(TAG, "VendorId: " + entry.device.getVendorId());
	            Log.d(TAG, "ProductId: " + entry.device.getProductId());
	            Log.d(TAG, "DeviceName: " + entry.device.getDeviceName());
	            Log.d(TAG, "DeviceId: " + entry.device.getDeviceId());
	            Log.d(TAG, "DeviceClass: " + entry.device.getDeviceClass());
	            Log.d(TAG, "DeviceSubclass: " + entry.device.getDeviceSubclass());
	            Log.d(TAG, "InterfaceCount: " + entry.device.getInterfaceCount());
	            Log.d(TAG, "DeviceProtocol: " + entry.device.getDeviceProtocol());
            }

            if (entry.device.getVendorId() == ARDUINO_USB_VENDOR_ID 
            		|| entry.device.getVendorId() == SPLDUINO_USB_VENDOR_ID) 
            {
                mTargetDevice = entry;

                if (Constants.SERIALDEBUG) 
                {
                	Log.i(TAG, "Arduino device found!");

	                switch (entry.device.getProductId()) 
	                {
	                case ARDUINO_UNO_R3_USB_PRODUCT_ID:
	                	sendMessageToUI("Arduino Uno R3 found!!!!");
	    	    		break;
	                case SPLDUINO_USB_PRODUCT_ID:
	                	sendMessageToUI("SPL Duino found!!!!");
	    	    		break;
	                }
                }
            }
        }
    }

    private boolean connectToTarget()
    {
        if(Constants.SERIALDEBUG)  Log.d(TAG, "connectToTarget, targetDevice.driver=" + mTargetDevice.driver);

        if (mTargetDevice.driver == null) 
        {
            if(Constants.SERIALDEBUG)  Log.d(TAG, "No serial device.");
            return false;
        } 
        else 
        {
            try 
            {
            	mTargetDevice.driver.open();
            	mTargetDevice.driver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            } 
            catch (IOException e) 
            {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);

                try 
                {
                	mTargetDevice.driver.close();
                } 
                catch (IOException e2) 
                {
                    // Ignore.
                }
                mTargetDevice.driver = null;
                return false;
            }
            Log.d(TAG, "Serial device: " + mTargetDevice.driver.getClass().getSimpleName());
    	}

        return true;
    }
    
    private boolean connectToArduino()
    {
        isConnectedToTarget = false;
        
        if(getSerialDeviceList() == false)
        {
            if(Constants.SERIALDEBUG)  Log.d(TAG, "getSerialDeviceList() fail!");
            return false;
        }
        
        setTargetDevice();
        
        if(connectToTarget() == false)
        {
            if(Constants.SERIALDEBUG)  Log.d(TAG, "connectToTarget() fail!");
            return false;
        }
        
        isConnectedToTarget = true;
        
        startReceiverThread();
        startSenderThread();
        
        if(Constants.SERIALDEBUG)  Log.d(TAG, "Connect success!");
        displayDebugMessage("Connect success!");

        Toast.makeText(this, "Arduino is connected successfully!!!", Toast.LENGTH_LONG).show();
        
        return true;
    }

    private boolean disconnectToArduino()
    {
        if(Constants.SERIALDEBUG)  Log.d(TAG, "disconnectToTarget, targetDevice.driver=" + mTargetDevice.driver);
        displayDebugMessage("disconnectToTarget, targetDevice.driver=" + mTargetDevice.driver);

        if (mTargetDevice.driver != null) 
        {
            try 
            {
            	mTargetDevice.driver.close();
            } 
            catch (IOException e) {
                // Ignore.
            }
            mTargetDevice.driver = null;
            mTargetDevice.device = null;
        }
        return true;
    }
    
    void updateReceivedData(byte[] data) 
    {
        final String message = "Read " + data.length + " bytes: \n";
        String receivedData = new String(data);

    	Log.i(TAG,"updateReceivedData: " + message + " : " + receivedData + "\n");
	}
    
    public void sendMessageToDevice(byte[] message)
    {
    	if(mTargetDevice.driver != null)
    	{
    		String temp = new String(message);

    		//mSerialIoManager.writeAsync(message);
        	Log.i(TAG,"sendMessage: " + temp);
        	
            try
            {
            	mTargetDevice.driver.writeOneByte(message, 1000);
            }
            catch(IOException e)
            {
            	Log.i(TAG,"sendMessage: occur IOException");
                Toast.makeText(getBaseContext(), "sendMessage: occur IOException", Toast.LENGTH_SHORT).show();
                disconnectToArduino();
            }
    	}
    	else
    	{
    		Log.i(TAG,"sendMessage() fail!!! targetDevice.driver == null \n");
    	}
    }

    public void sendMessageToDevice(String message)
    {
    	if(mTargetDevice.driver != null && message != null)
    	{ 
    		Log.i(TAG,"sendMessageToDevice: " + message );
            
    		byte[] bytes = message.getBytes();
    		
        	//mSerialIoManager.writeAsync(bytes);
            try
            {
            	mTargetDevice.driver.writeOneByte(bytes, 100);
            }
            catch(IOException e)
            {
            	Log.i(TAG,"sendMessage: occur IOException \n");
                displayDebugMessage("sendMessage: occur IOException");
                disconnectToArduino();
            }
    	}
    	else
    	{
    		Log.i(TAG,"sendMessage() fail!!! targetDevice.driver == null \n");
    	}
    }
    
    private void displayDebugMessage(String message)
    {
    	sendMessageToUI(message);
    }
    
    private void startSenderThread() 
    {
		Log.i(TAG,"startSenderThread() ");
    	if(mSenderThread != null)
    	{
    		mSenderThread.mHandler.sendEmptyMessage(MSG_STOP_SEND_THREAD);
    		mSenderThread = null;
    	}

        mSenderThread = new SenderThread("arduino_sender");
        mSenderThread.start();
    }

    private class SenderThread extends Thread 
    {
        public Handler mHandler;

        public SenderThread(String string) 
        {
            super(string);
        }

        public void run() 
        {

            Looper.prepare();

            mHandler = new Handler() 
            {
                public void handleMessage(Message msg) 
                {
                    if (msg.what == MSG_SEND_TO_DEVICE) 
                    {
                        String message = (String)msg.obj;

                        sendMessageToDevice(message);
                    } 
                    else if (msg.what == MSG_STOP_SEND_THREAD) 
                    {
                    	if (Constants.SERIALDEBUG) Log.d(TAG, "MSG_STOP_SEND_THREAD ");
                        Looper.myLooper().quit();
                    }
                }
            };

            Looper.loop();
            if (Constants.SERIALDEBUG)  Log.i(TAG, "sender thread stopped");
        }
    }

    private void startReceiverThread() 
    {
        new Thread("arduino_receiver") 
        {
            public void run() 
            {
                byte[] inBuffer = new byte[4096];
                int len = 0;
                while(mTargetDevice.device != null ) 
                {
                    // Handle incoming data.
					try 
					{
						len = mTargetDevice.driver.read(inBuffer, 200);

	                    if (len > 0) 
	                    {
	                    	if (Constants.SERIALDEBUG) Log.d(TAG, "ReceiverThread() Read data len=" + len);
	                        byte[] buffer = new byte[len];
	                        System.arraycopy(inBuffer, 0, buffer, 0, len);
	                        updateReceivedData(buffer);
	                    }
					} 
					catch (IOException e) 
					{
						if (Constants.SERIALDEBUG) Log.d(TAG, "ReceiverThread() Occur IOException");
					}
                    
                }

                if (Constants.SERIALDEBUG) Log.d(TAG, "receiver thread stopped.");
            }
        }.start();
    }

}