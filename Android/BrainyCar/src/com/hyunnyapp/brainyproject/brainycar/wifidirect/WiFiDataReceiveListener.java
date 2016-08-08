package com.hyunnyapp.brainyproject.brainycar.wifidirect;

public interface WiFiDataReceiveListener
{
	void dataReceived(int dataType, byte[] data, int dataLength);
}

