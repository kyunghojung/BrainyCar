package com.hyunnyapp.brainyproject.brainycar.serial;

import android.hardware.usb.UsbDevice;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

public class SerialDeviceEntry 
{
    public UsbDevice device;
    public UsbSerialDriver driver;

    public SerialDeviceEntry(UsbDevice device, UsbSerialDriver driver) 
    {
        this.device = device;
        this.driver = driver;
    }
}
