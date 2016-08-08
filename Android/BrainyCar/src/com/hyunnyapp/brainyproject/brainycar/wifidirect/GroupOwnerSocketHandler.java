package com.hyunnyapp.brainyproject.brainycar.wifidirect;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.hyunnyapp.brainyproject.brainycar.Constants;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 */
public class GroupOwnerSocketHandler extends Thread
{

	ServerSocket socket = null;
	private final int THREAD_COUNT = 10;
	private static final String TAG = "ServerSocketHandler";
	private WiFiConnectionManager mConnectionManager;
	private WiFiConnectListener mWiFiConnectListener;

    public void setConnectListener(WiFiConnectListener listener)
    {
    	mWiFiConnectListener = listener;
    }
    
	public GroupOwnerSocketHandler() throws IOException
	{
		try
		{
			socket = new ServerSocket(Constants.WIFI_SERVER_PORT);
			Log.d(TAG, "Socket Started");
		} catch (IOException e)
		{
			e.printStackTrace();
			pool.shutdownNow();
			throw e;
		}

	}

	/**
	 * A ThreadPool for client sockets.
	 */
	private final ThreadPoolExecutor pool = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				// A blocking operation. Initiate a ChatManager instance when
				// there is a new connection
				mConnectionManager = new WiFiConnectionManager(socket.accept());
				pool.execute(mConnectionManager);
				mWiFiConnectListener.connected(mConnectionManager);
				if(Constants.WIFIDEBUG) Log.d(TAG, "Launching the I/O handler");

			} catch (IOException e)
			{
				try
				{
					if (socket != null && !socket.isClosed())
						socket.close();
				} catch (IOException ioe)
				{

				}
				e.printStackTrace();
				pool.shutdownNow();
				break;
			}
		}
	}
	
	public WiFiConnectionManager getConnectionManager()
	{
		return mConnectionManager;
	}

}
