package com.hyunnyapp.brainyproject.brainycontroler.wifidirect;

public class WiFiConnection 
{
	private static WiFiConnection instance = null;
	
	private WiFiConnectionManager mConnectionManager = null;
	
	protected WiFiConnection()
	{
		
	}
	
	public WiFiConnectionManager getConnectionManager()
	{
		return mConnectionManager;
	}
	
	public void setConnectionManager(WiFiConnectionManager connectionManager)
	{
		mConnectionManager = connectionManager;
	}
	
	public static WiFiConnection getInstance()
	{
		if(instance == null)
		{
			instance = new WiFiConnection();
		}
		return instance;
	}
}
