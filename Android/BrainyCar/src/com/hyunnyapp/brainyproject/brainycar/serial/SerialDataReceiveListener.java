package com.hyunnyapp.brainyproject.brainycar.serial;

public interface SerialDataReceiveListener
{
	void dataReceived(byte[] data, int dataLength);
}
