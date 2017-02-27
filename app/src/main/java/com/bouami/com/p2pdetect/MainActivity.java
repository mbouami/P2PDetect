package com.bouami.com.p2pdetect;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.bouami.com.p2pdetect.WiFiDirectServicesList.WiFiDevicesAdapter;
import com.google.android.gms.common.api.GoogleApiClient;


import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSION_STATE = 1;
    public static final String TAG = "p2pdetect";
    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager mManager;
    static final int SERVER_PORT = 4545;
    private IntentFilter mIntentFilter;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    //    private Handler handler = new Handler(this);
    private WiFiChatFragment chatFragment;
    private WiFiDirectServicesList servicesList = null;
    private TextView statusTxtView;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        ChetcherVoisin();
//        startRegistration();
        startRegistrationAndDiscovery();
        servicesList = new WiFiDirectServicesList(mReceiver.getPeersService());
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.add(R.id.container_root,new WiFiDirectServicesList(),"services").commit();
        getSupportFragmentManager().beginTransaction().add(R.id.container_root, servicesList, "services").commit();
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

//    @Override
//    public void onResume() {
//        super.onResume();
//        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
//        registerReceiver(mReceiver, mIntentFilter);
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        unregisterReceiver(mReceiver);
//    }

    public void ChetcherVoisin() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
//                Log.d(TAG, "discoverPeers success");
                appendStatus("discoverPeers success");
                new mTaost(getBaseContext(), "discoverPeers success");
            }

            @Override
            public void onFailure(int reason) {
//                Log.d(TAG, "discoverPeers onFailure");
                new mTaost(getBaseContext(), "discoverPeers onFailure");
                appendStatus("discoverPeers onFailure");
            }
        });
    }

    private void startRegistration() {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        // Add the local service, sending the service info, network mChannel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Added Local Service ");
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                switch (arg0) {
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


//    public Handler getHandler() {
//        return handler;
//    }
//    public void setHandler(Handler handler) {
//        this.handler = handler;
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
//                            Manifest.permission.ACCESS_WIFI_STATE,
//                            Manifest.permission.CHANGE_WIFI_STATE,
//                            Manifest.permission.INTERNET,
//                            Manifest.permission.ACCESS_NETWORK_STATE,
//                            Manifest.permission.ACCESS_FINE_LOCATION},
//                    REQUEST_PERMISSION_STATE);
//        }
//        setContentView(R.layout.main);
//        statusTxtView = (TextView) findViewById(R.id.status_text);
//        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//        mIntentFilter
//                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//        mIntentFilter
//                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
//        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        mChannel = mManager.initialize(this, getMainLooper(), null);
//        startRegistrationAndDiscovery();
//        servicesList = new WiFiDirectServicesList();
//        getFragmentManager().beginTransaction()
//                .add(R.id.container_root, servicesList, "services").commit();
//    }
//

    //
//    @Override
//    protected void onRestart() {
//        Fragment frag = getFragmentManager().findFragmentByTag("services");
//        if (frag != null) {
//            getFragmentManager().beginTransaction().remove(frag).commit();
//        }
//        super.onRestart();
//    }
//    @Override
//    protected void onStop() {
//        if (mManager != null && mChannel != null) {
//            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onFailure(int reasonCode) {
//                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
//                }
//                @Override
//                public void onSuccess() {
//                }
//            });
//        }
//        super.onStop();
//    }
//    /**
//     * Registers a local service and then initiates a service discovery
//     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        mManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
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
            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
//                buddies.put(device.deviceAddress, record.get("buddyname"));
                appendStatus("DnsSdTxtRecord available -" + record.toString());
            }
        };
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice resourceType) {

                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                        appendStatus("instanceName :"+instanceName);
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            // update the UI and add the item the discovered
                            // device.
                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getSupportFragmentManager()
                                    .findFragmentByTag("services");
                            appendStatus(""+(fragment!=null));
                            if (fragment != null) {
                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = resourceType;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.add(service);
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "onBonjourServiceAvailable "
                                        + instanceName);
                            }
                        }
            }
        };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
//        mManager.setDnsSdResponseListeners(mChannel,
//                new WifiP2pManager.DnsSdServiceResponseListener() {
//                    @Override
//                    public void onDnsSdServiceAvailable(String instanceName,
//                                                        String registrationType, WifiP2pDevice srcDevice) {
//                        // A service has been discovered. Is this our app?
//                        appendStatus("instanceName :"+instanceName);
//                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
//                            // update the UI and add the item the discovered
//                            // device.
//                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getSupportFragmentManager()
//                                    .findFragmentByTag("services");
//                            appendStatus(""+(fragment!=null));
//                            if (fragment != null) {
//                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
//                                        .getListAdapter());
//                                WiFiP2pService service = new WiFiP2pService();
//                                service.device = srcDevice;
//                                service.instanceName = instanceName;
//                                service.serviceRegistrationType = registrationType;
//                                adapter.add(service);
//                                adapter.notifyDataSetChanged();
//                                Log.d(TAG, "onBonjourServiceAvailable "
//                                        + instanceName);
//                            }
//                        }
//                    }
//                }, new WifiP2pManager.DnsSdTxtRecordListener() {
//                    /**
//                     * A new TXT record is available. Pick up the advertised
//                     * buddy name.
//                     */
//                    @Override
//                    public void onDnsSdTxtRecordAvailable(
//                            String fullDomainName, Map<String, String> record,
//                            WifiP2pDevice device) {
//                        Log.d(TAG,
//                                device.deviceName + " is "
//                                        + record.get(TXTRECORD_PROP_AVAILABLE));
//                    }
//                });
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
//    @Override
//    public void connectP2p(WiFiP2pService service) {
//        WifiP2pConfig config = new WifiP2pConfig();
//        config.deviceAddress = service.device.deviceAddress;
//        config.wps.setup = WpsInfo.PBC;
//        if (serviceRequest != null)
//            mManager.removeServiceRequest(mChannel, serviceRequest,
//                    new WifiP2pManager.ActionListener() {
//                        @Override
//                        public void onSuccess() {
//                        }
//                        @Override
//                        public void onFailure(int arg0) {
//                        }
//                    });
//        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                appendStatus("Connecting to service");
//            }
//            @Override
//            public void onFailure(int errorCode) {
//                appendStatus("Failed connecting to service");
//            }
//        });
//    }
//    @Override
//    public boolean handleMessage(Message msg) {
//        switch (msg.what) {
//            case MESSAGE_READ:
//                byte[] readBuf = (byte[]) msg.obj;
//                // construct a string from the valid bytes in the buffer
//                String readMessage = new String(readBuf, 0, msg.arg1);
//                Log.d(TAG, readMessage);
//                (chatFragment).pushMessage("Buddy: " + readMessage);
//                break;
//            case MY_HANDLE:
//                Object obj = msg.obj;
//                (chatFragment).setChatManager((ChatManager) obj);
//        }
//        return true;
//    }
    @Override
    public void onResume() {
        super.onResume();
//        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
//    @Override
//    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
//        Thread handler = null;
//        /*
//         * The group owner accepts connections using a server socket and then spawns a
//         * client socket for every client. This is handled by {@code
//         * GroupOwnerSocketHandler}
//         */
//        if (p2pInfo.isGroupOwner) {
//            Log.d(TAG, "Connected as group owner");
//            try {
//                handler = new GroupOwnerSocketHandler(
//                        ((MessageTarget) this).getHandler());
//                handler.start();
//            } catch (IOException e) {
//                Log.d(TAG,
//                        "Failed to create a server thread - " + e.getMessage());
//                return;
//            }
//        } else {
//            Log.d(TAG, "Connected as peer");
//            handler = new ClientSocketHandler(
//                    ((MessageTarget) this).getHandler(),
//                    p2pInfo.groupOwnerAddress);
//            handler.start();
//        }
//        chatFragment = new WiFiChatFragment();
//        getFragmentManager().beginTransaction()
//                .replace(R.id.container_root, chatFragment).commit();
//        statusTxtView.setVisibility(View.GONE);
//    }

    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    public Action getIndexApiAction() {
//        Thing object = new Thing.Builder()
//                .setName("Main Page") // TODO: Define a title for the content shown.
//                // TODO: Make sure this auto-generated URL is correct.
//                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
//                .build();
//        return new Action.Builder(Action.TYPE_VIEW)
//                .setObject(object)
//                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
//                .build();
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        AppIndex.AppIndexApi.start(client, getIndexApiAction());
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        AppIndex.AppIndexApi.end(client, getIndexApiAction());
//        client.disconnect();
//    }

    public class mTaost {
        public mTaost(Context baseContext, String s) {
            Toast.makeText(baseContext, s, Toast.LENGTH_LONG).show();
        }
    }
}
