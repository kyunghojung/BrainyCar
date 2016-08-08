package com.hyunnyapp.brainyproject.brainycar;

import android.os.Bundle;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.hyunnyapp.brainyproject.brainycar.mainui.MainUIBluetooth;
import com.hyunnyapp.brainyproject.brainycar.mainui.MainUISerial;
import com.hyunnyapp.brainyproject.brainycar.mainui.MainUIWiFiDirect;
import com.hyunnyapp.brainyproject.brainycar.mainui.MainUIWithPreview;

public class BrainyCarMain extends Activity {

	Button launchDirectControl;
	Button launchWiFicontrol;
	Button launchBluetoothControl;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.brainy_car_main);

		launchBluetoothControl = (Button) findViewById(R.id.launchBluetoothControl);
		launchBluetoothControl.setOnClickListener(new View.OnClickListener() 
		{
            public void onClick(View v) 
            {
            	try
            	{
	            	Intent intent = new Intent(BrainyCarMain.this, MainUIBluetooth.class);
	            	startActivity(intent);
            	}
            	catch(ActivityNotFoundException e)
            	{
            		//
            	}
            }
        });

		launchWiFicontrol = (Button) findViewById(R.id.launchWiFicontrol);
		launchWiFicontrol.setOnClickListener(new View.OnClickListener() 
		{
            public void onClick(View v) 
            {
            	try
            	{
	            	Intent intent = new Intent(BrainyCarMain.this, MainUIWiFiDirect.class);
	            	startActivity(intent);
            	}
            	catch(ActivityNotFoundException e)
            	{
            		//
            	}
            }
        });

		launchDirectControl = (Button) findViewById(R.id.launchDirectControl);
		launchDirectControl.setOnClickListener(new View.OnClickListener() 
		{
            public void onClick(View v) 
            {
            	try
            	{
	            	Intent intent = new Intent(BrainyCarMain.this, MainUISerial.class);
	            	startActivity(intent);
            	}
            	catch(ActivityNotFoundException e)
            	{
            		//
            	}
            }
        });
    	
	}

    @Override
    public void onDestroy() 
    {
        super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{

		return true;
	}

}
