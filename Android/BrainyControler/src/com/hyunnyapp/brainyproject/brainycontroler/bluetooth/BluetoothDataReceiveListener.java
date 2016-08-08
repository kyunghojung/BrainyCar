package com.hyunnyapp.brainyproject.brainycontroler.bluetooth;

public interface BluetoothDataReceiveListener 
{
	void dataReceived(int dataType, byte[] data, int dataLength);
}
