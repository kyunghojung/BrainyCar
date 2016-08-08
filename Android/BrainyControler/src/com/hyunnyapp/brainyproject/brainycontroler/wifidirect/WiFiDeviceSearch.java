package com.hyunnyapp.brainyproject.brainycontroler.wifidirect;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.hyunnyapp.brainyproject.brainycontroler.Constants;
import com.hyunnyapp.brainyproject.brainycontroler.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class WiFiDeviceSearch extends Activity implements ConnectionInfoListener
{
    private static final String TAG = "WiFiDeviceSearch";
    
    private ArrayAdapter<WiFiDirectService> mNewDevicesArrayAdapter;

	// TXT RECORD properties
	public static final String TXTRECORD_PROP_AVAILABLE = "available";
	public static final String SERVICE_INSTANCE = "_brainyproject";
	public static final String SERVICE_REG_TYPE = "_presence._tcp";

	private WifiP2pManager manager;

	private final IntentFilter intentFilter = new IntentFilter();
	private Channel channel;
	private BroadcastReceiver receiver = null;
	private WifiP2pDnsSdServiceRequest serviceRequest;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);
		
        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() 
        {
            public void onClick(View v) 
            {
            	startRegistrationAndDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mNewDevicesArrayAdapter = new ArrayAdapter<WiFiDirectService>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
    }

	@Override
	public void onResume()
	{
		super.onResume();
		receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
		registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		unregisterReceiver(receiver);
	}

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
    }

	/**
	 * Registers a local service and then initiates a service discovery
	 */
	private void startRegistrationAndDiscovery()
	{
		Map<String, String> record = new HashMap<String, String>();
		record.put(TXTRECORD_PROP_AVAILABLE, "visible");

		WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
		manager.addLocalService(channel, service, new ActionListener()
		{

			@Override
			public void onSuccess()
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Added Local Service");
			}

			@Override
			public void onFailure(int error)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Failed to add a service");
			}
		});
		discoverService();
	}

	private void discoverService()
	{
		/*
		 * Register listeners for DNS-SD services. These are callbacks invoked
		 * by the system when a service is actually discovered.
		 */
		manager.setDnsSdResponseListeners(channel, new DnsSdServiceResponseListener()
		{

			@Override
			public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice)
			{
				// A service has been discovered. Is this our app?

				if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE))
				{
					WiFiDirectService service = new WiFiDirectService();
					service.device = srcDevice;
					service.instanceName = instanceName;
					service.serviceRegistrationType = registrationType;
					
					mNewDevicesArrayAdapter.add(service);
					if(Constants.WIFIDEBUG) Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
				}

			}
		}, 
		new DnsSdTxtRecordListener()
		{
			/**
			 * A new TXT record is available. Pick up the advertised buddy name.
			 */
			@Override
			public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> record, WifiP2pDevice device)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG, device.deviceName + " is " + record.get(TXTRECORD_PROP_AVAILABLE));
			}
		});

		// After attaching listeners, create a service request and initiate
		// discovery.
		serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
		manager.addServiceRequest(channel, serviceRequest, new ActionListener()
		{

			@Override
			public void onSuccess()
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Added service discovery request");
			}

			@Override
			public void onFailure(int arg0)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Failed adding service discovery request");
			}
		});
		manager.discoverServices(channel, new ActionListener()
		{

			@Override
			public void onSuccess()
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Service discovery initiated");
			}

			@Override
			public void onFailure(int arg0)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Service discovery failed");

			}
		});
	}

	public void connectP2p(WiFiDirectService service)
	{
		if(Constants.WIFIDEBUG) Log.d(TAG,"connectP2p() deviceName: "+service.device.deviceName);
		
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = service.device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		
		if (serviceRequest != null)
		{
			manager.removeServiceRequest(channel, serviceRequest, new ActionListener()
			{

				@Override
				public void onSuccess()
				{
					if(Constants.WIFIDEBUG) Log.d(TAG,"connectP2p() removeServiceRequest() success");
				}

				@Override
				public void onFailure(int arg0)
				{
					if(Constants.WIFIDEBUG) Log.d(TAG,"connectP2p() removeServiceRequest() fail");
				}
			});
		}

		manager.connect(channel, config, new ActionListener()
		{

			@Override
			public void onSuccess()
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Connecting to service");
			}

			@Override
			public void onFailure(int errorCode)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG,"Failed connecting to service");
			}
		});
	}

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() 
    {
        public void onItemClick(AdapterView<?> av, View v, int position, long id)
        {
            if(Constants.WIFIDEBUG) Log.d(TAG, "onItemClick(): position: "+position);

            WiFiDirectService service = (WiFiDirectService)av.getItemAtPosition(position);
            
            connectP2p(service);
        }
    };

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo)
	{
		Thread handler = null;
		/*
		 * The group owner accepts connections using a server socket and then
		 * spawns a client socket for every client. This is handled by {@code
		 * GroupOwnerSocketHandler}
		 */

		if (p2pInfo.isGroupOwner)
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, "Connected as group owner");
			try
			{
				handler = new GroupOwnerSocketHandler();
				((GroupOwnerSocketHandler) handler).setConnectListener(mWiFiConnectListener);
				handler.start();
			} catch (IOException e)
			{
				if(Constants.WIFIDEBUG) Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
				return;
			}
		} 
		else
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, "Connected as peer");
			handler = new ClientSocketHandler(p2pInfo.groupOwnerAddress);
			((ClientSocketHandler) handler).setConnectListener(mWiFiConnectListener);
			handler.start();
		}

	}

	private WiFiConnectListener mWiFiConnectListener = new WiFiConnectListener()
	{
		@Override
		public void connected(WiFiConnectionManager connectionManager) 
		{
			if(Constants.WIFIDEBUG) Log.d(TAG, "mWiFiConnectListener connected()");

			WiFiConnection.getInstance().setConnectionManager(connectionManager);

	        setResult(Activity.RESULT_OK);
	        finish();
			
		}

		@Override
		public void disConnected() 
		{

		}
	
	};
}
