package com.hyunnyapp.brainyproject.brainycontroler.wifidirect;

public interface WiFiDataReceiveListener
{
	void dataReceived(int dataType, byte[] data, int dataLength);
}

