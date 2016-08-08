package com.hyunnyapp.brainyproject.brainycontroler;

import java.util.UUID;

public class Constants 
{
    public static final boolean WIFIDEBUG = false;
    public static final boolean BTDEBUG = false;
    public static final boolean UIDEBUG = false;
    public static final boolean SERIALDEBUG = false;
    public static final boolean CAMDEBUG = false;

    // Intent request codes
    public static final int DEVICE_SEARCH_FAIL = -1;
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_SEARCH_WIFI = 3;

    // WiFi Direct
	public static final int WIFI_SERVER_PORT = 8969;
	
	// UUID
	//public static final UUID SERIAL_PORT_PROFILE = UUID.fromString("05844c50-7c6e-11e3-baa7-0800200c9a66");
	public static final UUID SERIAL_PORT_PROFILE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_WIFI_SERVICE = "wifi_service";

    // flash data
    public static String SET_FLASH_ON = "flash_on";
    public static String SET_FLASH_OFF = "flash_off";
    
    // Send, Receive Data type
    public static final int DATA_TYPE_PREVIEW_IMAGE = 1;
    public static final int DATA_TYPE_DIRECTION = 2;
    public static final int DATA_TYPE_SET_FLASH = 3;

    // Constants that indicate the current connection state
    public static final int BLUETOOTH_STATE_NONE = 0;       // we're doing nothing
    public static final int BLUETOOTH_STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int BLUETOOTH_STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int BLUETOOTH_STATE_CONNECTED = 3;  // now connected to a remote device

    // Data header marker and length
    public static final byte[] STREAM_START_MARKER = { (byte) 0xFF, (byte) 0xD5, (byte) 0xFF, (byte) 0xD6};
    public static final byte[] STREAM_END_MARKER = { (byte) 0xFF, (byte) 0xD6, (byte) 0xFF, (byte) 0xD5};
	public static final int STREAM_MARKER_LENGTH = 4;
	public static final int DATA_TYPE_MARKER_LENGTH = 1;
	public static final int DATA_LENGTH_MARKER_LENGTH = 4;
	public static final int HEADER_LENGTH = STREAM_MARKER_LENGTH + DATA_TYPE_MARKER_LENGTH + DATA_LENGTH_MARKER_LENGTH;

    public static final byte[] JPEG_START_MARKER = { (byte) 0xFF, (byte) 0xD5 };
    public static final byte[] JPEG_END_MARKER = { (byte) 0xFF, (byte) 0xD6 };
    
    // The kind of arduino board
    public static final int ARDUINO_USB_VENDOR_ID = 0x2341;
    public static final int SPLDUINO_USB_VENDOR_ID = 0x10C4;
    public static final int ARDUINO_UNO_R3_USB_PRODUCT_ID = 0x43;
    public static final int SPLDUINO_USB_PRODUCT_ID = 0xEA60;
    
    public static final int FALSE = 0;
    public static final int TRUE = 1;
}
