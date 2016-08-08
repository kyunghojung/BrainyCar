package com.hyunnyapp.brainyproject.brainycar.wifidirect;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.hyunnyapp.brainyproject.brainycar.Constants;

public class ClientSocketHandler extends Thread
{
	private static final String TAG = "ClientSocketHandler";
	private WiFiConnectionManager mConnectionManager;
	private InetAddress mAddress;
	private WiFiConnectListener mWiFiConnectListener;

    public void setConnectListener(WiFiConnectListener listener)
    {
    	mWiFiConnectListener = listener;
    }
    
	public ClientSocketHandler(InetAddress groupOwnerAddress)
	{
		this.mAddress = groupOwnerAddress;
	}
	
	@Override
	public void run()
	{
		Socket socket = new Socket();
		try
		{
			socket.bind(null);
			socket.connect(new InetSocketAddress(mAddress.getHostAddress(), Constants.WIFI_SERVER_PORT), 5000);
			if(Constants.WIFIDEBUG) Log.d(TAG, "Launching the I/O handler");
			mConnectionManager = new WiFiConnectionManager(socket);
			new Thread(mConnectionManager).start();
			mWiFiConnectListener.connected(mConnectionManager);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
			try
			{
				socket.close();
			} 
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			return;
		}
	}
}
