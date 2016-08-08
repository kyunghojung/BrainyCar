package com.hyunnyapp.brainyproject.brainycontroler;

import android.os.Bundle;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class BrainyControlerMain extends Activity
{
	Button launchControlerMode;
	Button launchBluetoothControler;
	Button launchWiFiControler;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.brainy_controler_main);

		launchBluetoothControler = (Button) findViewById(R.id.launchBluetoothControler);
		launchBluetoothControler.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					Intent intent = new Intent(BrainyControlerMain.this, ControlerUICamBluetooth.class);
					startActivity(intent);
				} 
				catch (ActivityNotFoundException e)
				{
					//
				}
			}
		});

		launchWiFiControler = (Button) findViewById(R.id.launchWiFiControler);
		launchWiFiControler.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					Intent intent = new Intent(BrainyControlerMain.this, ControlerUICamWiFi.class);
					startActivity(intent);
				} 
				catch (ActivityNotFoundException e)
				{
					//
				}
			}
		});

		launchControlerMode = (Button) findViewById(R.id.launchControlerMode);
		launchControlerMode.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					Intent intent = new Intent(BrainyControlerMain.this, ControlerUI.class);
					startActivity(intent);
				} 
				catch (ActivityNotFoundException e)
				{
					//
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.brainy_controler_main, menu);
		return true;
	}

}
