package com.hyunnyapp.brainyproject.brainycontroler.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.hyunnyapp.brainyproject.brainycontroler.bluetooth.BluetoothDataReceiveListener;
import com.hyunnyapp.brainyproject.brainycontroler.Constants;
import com.hyunnyapp.brainyproject.brainycontroler.Utils;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothConnection 
{
    // Debugging
    private static final String TAG = "BluetoothConnection";

    // Name for the SDP record when creating server socket
    private static final String NAME_BLUETOOTH = "BluetoothConnection";

    // Unique UUID for this application
    private static final UUID BRAINY_UUID =
        UUID.fromString("dd2b8a90-7c6b-11e3-baa7-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private BluetoothDataReceiveListener dataReceiveListener;

    public void setDataReceiveListener(BluetoothDataReceiveListener listener)
    {
    	dataReceiveListener = listener;
    }
    
    public BluetoothConnection(Context context) 
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = Constants.BLUETOOTH_STATE_NONE;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) 
    {
    	if(Constants.BTDEBUG) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() 
    {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() 
    {
    	if(Constants.BTDEBUG) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) 
        {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) 
        {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        setState(Constants.BLUETOOTH_STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) 
        {
            if (Constants.BTDEBUG) Log.d(TAG, "mAcceptThread == null");
            
            mAcceptThread = new AcceptThread();
            if (Constants.BTDEBUG) Log.d(TAG, "mAcceptThread.start();");
            mAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) 
    {
        if (Constants.BTDEBUG) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == Constants.BLUETOOTH_STATE_CONNECTING) 
        {
            if (mConnectThread != null) 
            {
            	mConnectThread.cancel(); 
            	mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) 
        {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(Constants.BLUETOOTH_STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) 
    {
        if (Constants.BTDEBUG) Log.d(TAG, "connected()");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) 
        {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) 
        {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) 
        {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(Constants.BLUETOOTH_STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() 
    {
        if (Constants.BTDEBUG) Log.d(TAG, "stop");

        if (mConnectThread != null) 
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) 
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) 
        {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(Constants.BLUETOOTH_STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out, byte dataType) 
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) 
        {
            if (mState != Constants.BLUETOOTH_STATE_CONNECTED) 
            {
            	return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out, dataType);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() 
    {
    	if (Constants.BTDEBUG) Log.d(TAG, "connectionFailed() ");

        // Start the service over to restart listening mode
        BluetoothConnection.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() 
    {
    	if (Constants.BTDEBUG) Log.d(TAG, "connectionLost() ");


        // Start the service over to restart listening mode
        BluetoothConnection.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread 
    {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() 
        {
        	if (Constants.BTDEBUG) Log.d(TAG, "AcceptThread()");
        	
            BluetoothServerSocket tmp = null;

        	
            // Create a new listening server socket
            try 
            {
            	if (Constants.BTDEBUG) Log.d(TAG, "listenUsingRfcommWithServiceRecord() NAME_SECURE: " + NAME_BLUETOOTH);
            	if (Constants.BTDEBUG) Log.d(TAG, "MY_UUID_SECURE: " + BRAINY_UUID);

            	tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_BLUETOOTH, Constants.SERIAL_PORT_PROFILE);
            } 
            catch (IOException e) 
            {
            	if (Constants.BTDEBUG) Log.e(TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() 
        {
            if (Constants.BTDEBUG) Log.d(TAG, "BEGIN mAcceptThread" + this);
            
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != Constants.BLUETOOTH_STATE_CONNECTED) 
            {
                try 
                {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } 
                catch (IOException e) 
                {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) 
                {
                    synchronized (BluetoothConnection.this) 
                    {
                        switch (mState) 
                        {
                        case Constants.BLUETOOTH_STATE_LISTEN:
                        case Constants.BLUETOOTH_STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case Constants.BLUETOOTH_STATE_NONE:
                        case Constants.BLUETOOTH_STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try 
                            {
                                socket.close();
                            } 
                            catch (IOException e)
                            {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            
            if (Constants.BTDEBUG) Log.i(TAG, "END mAcceptThread" );

        }

        public void cancel() 
        {
            if (Constants.BTDEBUG) Log.d(TAG, "cancel " + this);
            try 
            {
                mmServerSocket.close();
            } 
            catch (IOException e) 
            {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread 
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) 
        {
            Log.d(TAG, "create ConnectThread: ");
            
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try 
            {
            	//, MY_UUID_SECURE
                tmp = device.createRfcommSocketToServiceRecord(Constants.SERIAL_PORT_PROFILE);
            }
            catch (IOException e) 
            {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() 
        {

            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try 
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } 
            catch (IOException e) 
            {
                // Close the socket
                try 
                {
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnection.this) 
            {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() 
        {
            try 
            {
                mmSocket.close();
            } 
            catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private int mDataType = 0;
        private int mTotalDataSize = 0;
        private boolean mReceiveBody = false;
        private ByteArrayOutputStream mDataOutputStream = null;
        
        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "create ConnectedThread: ");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try 
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } 
            catch (IOException e)
            {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
            mDataOutputStream = new ByteArrayOutputStream();
        }
        
        private int receivedHeader(byte[] buffer, int readBytes, int headerIndex)
        {
            int remainingSize = 0;
            byte[] receiveBuffer;
            
            if(headerIndex + Constants.HEADER_LENGTH > readBytes)
            {
            	return -1;
            }
            
            if(mDataOutputStream != null)
            {
            	mDataOutputStream.reset();
            }
            
        	mDataType = buffer[headerIndex + Constants.STREAM_MARKER_LENGTH];

        	if(mDataType != Constants.DATA_TYPE_PREVIEW_IMAGE && mDataType != Constants.DATA_TYPE_DIRECTION)
        	{
                if(Constants.BTDEBUG) Log.d(TAG, "Header Received. But mDataType is wrong. mDataType: "+mDataType);
        		return -1;
        	}
        	
            byte[] dataSizeBuffer = Arrays.copyOfRange(buffer, 
            		headerIndex + Constants.STREAM_MARKER_LENGTH + Constants.DATA_TYPE_MARKER_LENGTH, 
            		headerIndex + Constants.HEADER_LENGTH);
            
            mTotalDataSize = Utils.byteArrayToInt(dataSizeBuffer);
            
            if(mTotalDataSize < 0)
            {
            	return -1;
            }
            
            if(Constants.BTDEBUG) Log.d(TAG, "Header Received. headerIndex: "+headerIndex
            		+", dataType: "+mDataType
            		+", totalSize: "+mTotalDataSize
            		+", readBytes: "+readBytes);

            if(mTotalDataSize > readBytes - headerIndex)    // must receive more body.
            {
        		if(Constants.BTDEBUG) Log.d(TAG,"must receive more body. ");
            	remainingSize = mTotalDataSize - (readBytes - headerIndex);
            	mReceiveBody = true;
            	
            	if(mDataOutputStream != null)
            	{
            		mDataOutputStream.write(buffer, headerIndex + Constants.HEADER_LENGTH, readBytes - headerIndex - Constants.HEADER_LENGTH);
            	}
            	else
            	{
            		if(Constants.BTDEBUG) Log.d(TAG,"dataOutputStream is null!!! ");
            	}
            }
            else    //Receive complete
            {
        		if(Constants.BTDEBUG) Log.d(TAG,"Receive complete! ");
        		
            	remainingSize = 0;
            	mReceiveBody = false;
                receiveBuffer = new byte[mTotalDataSize-Constants.HEADER_LENGTH];
            	System.arraycopy(buffer, headerIndex + Constants.HEADER_LENGTH, receiveBuffer, 0, mTotalDataSize-Constants.HEADER_LENGTH);
            	dataReceiveListener.dataReceived(mDataType, receiveBuffer, receiveBuffer.length);
            	//receiveBuffer = null;
            }

        	return remainingSize;
        }

        private int receivedBody(byte[] buffer, int remainingSize, int readBytes, int headerIndex)
        {
            byte[] receiveBuffer;
            int tempRemainingSize = -1;
            
            tempRemainingSize = remainingSize - readBytes;
            
            if(tempRemainingSize > 0)  // Have not yet been received to complete.
            {
            	if(mDataOutputStream != null)
            	{
            		mDataOutputStream.write(buffer, 0, readBytes);
            	}
            	else
            	{
            		if(Constants.BTDEBUG) Log.d(TAG,"dataOutputStream is null!!! ");
            	}
            }
            else  //Receive complete
            {
            	if(mDataOutputStream != null)
            	{
            		mDataOutputStream.write(buffer, 0, remainingSize);
                	
                	receiveBuffer = mDataOutputStream.toByteArray();
                	
                	dataReceiveListener.dataReceived(mDataType, receiveBuffer, receiveBuffer.length);

                	mReceiveBody = false;
            	}
            	else
            	{
            		if(Constants.BTDEBUG) Log.d(TAG,"dataOutputStream is null!!! ");
            	}

            }                    	

        	return tempRemainingSize;
        }

        public void run() 
        {
            byte[] buffer = new byte[1024];
            int readBytes = -1;
            int headerIndex = -1;
            int remainingSize = -1;
        	
            // Keep listening to the InputStream while connected
            while (true) 
            {
                try 
                {
                    // Read from the InputStream
                    readBytes = mmInStream.read(buffer);
                    
                    headerIndex = Utils.BytesIndexOf(buffer, Constants.STREAM_START_MARKER, 0);

                    Log.d(TAG, "readBytes: "+readBytes+", headerIndex: "+headerIndex+", mReceiveBody: "+mReceiveBody);
                    
                    // receive header part
                    if (headerIndex >= 0 && mReceiveBody == false) 
                    {
                    	remainingSize = receivedHeader(buffer, readBytes, headerIndex);
                    }
                    else if(mReceiveBody == false)
                    {
                        Log.e(TAG, "Did not receive correct header.");
                        mReceiveBody = false;
                        remainingSize = -1;
                	}
                    else    // receive body part
                    {
                        if(Constants.BTDEBUG) Log.v(TAG, "Body Received. totalSize: "+mTotalDataSize +", remainingSize: "+remainingSize);
                        remainingSize = receivedBody(buffer, remainingSize, readBytes, headerIndex);
                        
                        if(headerIndex >= 0)
                        {
                        	remainingSize = receivedHeader(buffer, readBytes, headerIndex);
                        }
                    }
                } 
                catch (IOException e) 
                {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothConnection.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer, byte dataType) 
        {
            byte[] sendData = new byte[buffer.length+Constants.HEADER_LENGTH];
            byte[] sendDataSize = Utils.intToByteArray(buffer.length+Constants.HEADER_LENGTH);

            if(Constants.BTDEBUG) Log.d(TAG, " Preview data length: "+buffer.length);
            
            // make header
            System.arraycopy(Constants.STREAM_START_MARKER, 0, sendData, 0, Constants.STREAM_MARKER_LENGTH);
            sendData[Constants.STREAM_MARKER_LENGTH] = (byte) dataType;
            System.arraycopy(sendDataSize, 0, sendData, Constants.STREAM_MARKER_LENGTH + Constants.DATA_TYPE_MARKER_LENGTH, sendDataSize.length);

            //copy body
            System.arraycopy(buffer, 0, sendData, Constants.HEADER_LENGTH, buffer.length);

            if(Constants.BTDEBUG) Log.d(TAG, "Send Preview data length: "+sendData.length);

            try 
            {
                mmOutStream.write(sendData);
            } 
            catch (IOException e)
            {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try 
            {
                mmSocket.close();
            } 
            catch (IOException e) 
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
