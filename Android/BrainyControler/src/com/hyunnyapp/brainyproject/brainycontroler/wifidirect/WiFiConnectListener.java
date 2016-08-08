package com.hyunnyapp.brainyproject.brainycontroler.wifidirect;

public interface WiFiConnectListener 
{
	void connected(WiFiConnectionManager connectionManager);
	void disConnected();
}
