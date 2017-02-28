package com.bouami.com.p2pdetect;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bouami.com.p2pdetect.WiFiDirectServicesList.WiFiDevicesAdapter;
import com.bouami.com.p2pdetect.WiFiDirectServicesList.DeviceClickListener;
import com.bouami.com.p2pdetect.WiFiChatFragment.MessageTarget;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DeviceClickListener, Handler.Callback, MessageTarget,
        ConnectionInfoListener {

    private final int REQUEST_PERMISSION_STATE = 1;
    public static final String TAG = "p2pdetect";
    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "ETAT";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager mManager;
    static final int SERVER_PORT = 4545;
    private IntentFilter mIntentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    //    private Handler handler = new Handler(this);
    private WiFiDirectBroadcastReceiver mReceiver = null;
    private WiFiChatFragment chatFragment;
    private WiFiDirectServicesList servicesList = null;
    private TextView statusTxtView;
    final HashMap<String, String> buddies = new HashMap<String, String>();
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_STATE);
        }
        setContentView(R.layout.main);
        statusTxtView = (TextView) findViewById(R.id.status_text);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        startRegistrationAndDiscovery();
        getSupportFragmentManager().beginTransaction().add(R.id.container_root, new WiFiDirectServicesList(), "services").commit();
        discoverService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_STATE: {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                return;
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected void onRestart() {
        Fragment frag = getFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        super.onRestart();
    }
    @Override
    protected void onStop() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }
                @Override
                public void onSuccess() {
                }
            });
        }
        super.onStop();
    }
    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put("PORT_UTILISE", String.valueOf(SERVER_PORT));
        record.put("ADMINISTRATEUR", "Mohammed BOUAMI" + (int) (Math.random() * 1000));
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        mManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Enregistrement du service local");
            }

            @Override
            public void onFailure(int error) {
                switch (error) {
                    case 0:
                        appendStatus("Erreur d'activation du service");
                        break;
                    case 1:
                        appendStatus("Wi-Fi P2P non Supporté");
                        break;
                    case 2:
                        appendStatus("Service Occupé");
                        break;

                }
            }
        });
    }

    private void discoverService() {
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                appendStatus("DnsSdTxtRecord disponible - " + record.toString());
            }
        };
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                appendStatus("Nom de session :"+instanceName);
                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.

                if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                    // update the UI and add the item the discovered
                    // device.
                    WiFiDirectServicesList fragment = (WiFiDirectServicesList) getSupportFragmentManager().findFragmentByTag("services");
                    appendStatus(""+(fragment!=null));
                    if (fragment != null) {
                        WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment.getListAdapter());
                        WiFiP2pService service = new WiFiP2pService();
                        service.device = resourceType;
                        service.instanceName = instanceName;
                        service.serviceRegistrationType = registrationType;
                        adapter.add(service);
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "onBonjourServiceAvailable "+ instanceName);
                    }
                }
            }
        };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");
            }
        });
    }

    @Override
    public void connectP2p(WiFiP2pService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        final WiFiP2pService mService = service;
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
            mManager.removeServiceRequest(mChannel, serviceRequest,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                        }
                        @Override
                        public void onFailure(int arg0) {
                        }
                    });
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Connecté au service : "+mService.device.deviceName);
            }
            @Override
            public void onFailure(int errorCode) {
                appendStatus("Erreur de Connection au service");
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                (chatFragment).pushMessage("Buddy: " + readMessage);
                break;
            case MY_HANDLE:
                Object obj = msg.obj;
                (chatFragment).setChatManager((ChatManager) obj);
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        Log.d(TAG, "onConnectionInfoAvailable");
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(((WiFiChatFragment.MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new ClientSocketHandler(((WiFiChatFragment.MessageTarget) this).getHandler(),p2pInfo.groupOwnerAddress);
            handler.start();
        }
        chatFragment = new WiFiChatFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container_root, chatFragment).commit();
        statusTxtView.setVisibility(View.GONE);

    }

    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }

    public class mTaost {
        public mTaost(Context baseContext, String s) {
            Toast.makeText(baseContext, s, Toast.LENGTH_LONG).show();
        }
    }
}
