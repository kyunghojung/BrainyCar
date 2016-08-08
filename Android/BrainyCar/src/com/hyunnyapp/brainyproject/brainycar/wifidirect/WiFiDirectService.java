
package com.hyunnyapp.brainyproject.brainycar.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * A structure to hold service information.
 */
public class WiFiDirectService
{
	WifiP2pDevice device;
	String instanceName = null;
	String serviceRegistrationType = null;
	
	public String toString()
	{
		return device.deviceName;
	}
}
