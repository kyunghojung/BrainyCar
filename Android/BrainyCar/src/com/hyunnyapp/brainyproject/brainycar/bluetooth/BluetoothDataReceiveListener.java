package com.hyunnyapp.brainyproject.brainycar.bluetooth;

public interface BluetoothDataReceiveListener 
{
	void dataReceived(int dataType, byte[] data, int dataLength);
}
