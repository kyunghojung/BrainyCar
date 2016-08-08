package com.hyunnyapp.brainyproject.brainycar.wifidirect;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Arrays;

import com.hyunnyapp.brainyproject.brainycar.Constants;
import com.hyunnyapp.brainyproject.brainycar.Utils;
/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class WiFiConnectionManager implements Runnable, Serializable
{
	private static final String TAG = "ConnectionManager";
	private Socket socket = null;

    private WiFiDataReceiveListener dataReceiveListener;

    private int mDataType = 0;
    private int mTotalDataSize = 0;
    private boolean mReceiveBody = false;
    private ByteArrayOutputStream mDataOutputStream = null;
    
    public void setDataReceiveListener(WiFiDataReceiveListener listener)
    {
    	dataReceiveListener = listener;
    }
    
	public WiFiConnectionManager(Socket socket)
	{
		this.socket = socket;
	}

	private InputStream iStream;
	private OutputStream oStream;

    private int receivedHeader(byte[] buffer, int readBytes, int headerIndex)
    {
        int remainingSize = 0;
        byte[] receiveBuffer;
        
        if(mDataOutputStream != null)
        {
        	mDataOutputStream.reset();
        }
        
    	mDataType = buffer[headerIndex + Constants.STREAM_MARKER_LENGTH];

    	if(mDataType != Constants.DATA_TYPE_PREVIEW_IMAGE 
    		&& mDataType != Constants.DATA_TYPE_DIRECTION
    		&& mDataType != Constants.DATA_TYPE_SET_FLASH)
    	{
            if(Constants.WIFIDEBUG) Log.d(TAG, "Header Received. But mDataType is wrong. mDataType: "+mDataType);
    		return -1;
    	}
    	
        byte[] dataSizeBuffer = Arrays.copyOfRange(buffer, 
        		headerIndex + Constants.STREAM_MARKER_LENGTH + Constants.DATA_TYPE_MARKER_LENGTH, 
        		headerIndex + Constants.HEADER_LENGTH);
        
        mTotalDataSize = Utils.byteArrayToInt(dataSizeBuffer);
        
        if(mTotalDataSize <= Constants.HEADER_LENGTH)
        {
        	return -1;
        }
        
        if(Constants.WIFIDEBUG) Log.d(TAG, "Header Received. headerIndex: "+headerIndex
        		+", dataType: "+mDataType
        		+", totalSize: "+mTotalDataSize
        		+", readBytes: "+readBytes);

        if(mTotalDataSize > readBytes - headerIndex)    // must receive more body.
        {
    		if(Constants.WIFIDEBUG) Log.d(TAG,"must receive more body. ");
        	remainingSize = mTotalDataSize - (readBytes - headerIndex);
        	mReceiveBody = true;
        	
        	if(mDataOutputStream != null)
        	{
        		mDataOutputStream.write(buffer, headerIndex + Constants.HEADER_LENGTH, readBytes - headerIndex - Constants.HEADER_LENGTH);
        	}
        	else
        	{
        		if(Constants.WIFIDEBUG) Log.d(TAG,"dataOutputStream is null!!! ");
        	}
        }
        else    //Receive complete
        {
    		if(Constants.WIFIDEBUG) Log.d(TAG,"Receive complete! ");
    		
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
        		if(Constants.WIFIDEBUG) Log.d(TAG,"dataOutputStream is null!!! ");
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
        		if(Constants.WIFIDEBUG) Log.d(TAG,"dataOutputStream is null!!! ");
        	}

        }                    	

    	return tempRemainingSize;
    }

	@Override
	public void run()
	{
		byte[] buffer = new byte[1024];
        int readBytes = -1;
        int headerIndex = -1;
        int remainingSize = -1;
        
		try
		{
			iStream = socket.getInputStream();
			oStream = socket.getOutputStream();
			
            mDataOutputStream = new ByteArrayOutputStream();

			while (true)
			{
				try
				{
					// Read from the InputStream
                    readBytes = iStream.read(buffer);
                    
                    headerIndex = Utils.BytesIndexOf(buffer, Constants.STREAM_START_MARKER, 0);

                    //if(Constants.WIFIDEBUG) Log.d(TAG, "readBytes: "+readBytes+", headerIndex: "+headerIndex+", mReceiveBody: "+mReceiveBody);
                    
                    // receive header part
                    if (headerIndex >= 0 && mReceiveBody == false) 
                    {
                    	remainingSize = receivedHeader(buffer, readBytes, headerIndex);
                    }
                    else if(mReceiveBody == false)
                    {
                    	if(Constants.WIFIDEBUG) Log.e(TAG, "Did not receive correct header.");
                        mReceiveBody = false;
                        remainingSize = -1;
                	}
                    else    // receive body part
                    {
                        //if(Constants.WIFIDEBUG) Log.v(TAG, "Body Received. totalSize: "+mTotalDataSize +", remainingSize: "+remainingSize);
                        remainingSize = receivedBody(buffer, remainingSize, readBytes, headerIndex);
                        
                        if(headerIndex >= 0)
                        {
                        	remainingSize = receivedHeader(buffer, readBytes, headerIndex);
                        }
                    }
					
				} catch (IOException e)
				{
					Log.e(TAG, "disconnected", e);
				}
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				socket.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void write(byte[] buffer, byte dataType)
	{
        byte[] sendData = new byte[buffer.length+Constants.HEADER_LENGTH];
        byte[] sendDataSize = Utils.intToByteArray(buffer.length+Constants.HEADER_LENGTH);

        if(Constants.WIFIDEBUG) Log.d(TAG, " Preview data length: "+buffer.length);
        
        // make header
        System.arraycopy(Constants.STREAM_START_MARKER, 0, sendData, 0, Constants.STREAM_MARKER_LENGTH);
        sendData[Constants.STREAM_MARKER_LENGTH] = (byte) dataType;
        System.arraycopy(sendDataSize, 0, sendData, Constants.STREAM_MARKER_LENGTH + Constants.DATA_TYPE_MARKER_LENGTH, sendDataSize.length);

        //copy body
        System.arraycopy(buffer, 0, sendData, Constants.HEADER_LENGTH, buffer.length);

        if(Constants.WIFIDEBUG) Log.d(TAG, "Send Preview data length: "+sendData.length);

		try
		{
			oStream.write(sendData);
		} catch (IOException e)
		{
			if(Constants.WIFIDEBUG) Log.e(TAG, "Exception during write", e);
		}
	}

}
