package com.bouami.com.p2pdetect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Mohammed on 14/02/2017.
 */

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Activity activity;
    private WifiP2pManager.PeerListListener myPeerListListener;
    private List<WifiP2pDevice> peers;
    private List<WiFiP2pService> mPeersService;
    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p mChannel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(final WifiP2pManager manager, final WifiP2pManager.Channel channel,
                                       final Activity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.activity = activity;
        myPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                Collection<WifiP2pDevice> refreshedPeers = (Collection<WifiP2pDevice>) peerList.getDeviceList();
                if (!refreshedPeers.equals(peers)) {
                    peers.clear();
                    peers.addAll(refreshedPeers);
                    mPeersService.clear();
                    int i=0;
                    for (WifiP2pDevice  device : peers) {
                        Log.d(MainActivity.TAG, "WifiP2pDevice : "+device.deviceName+"---"+device.deviceAddress);
                        WiFiP2pService service = new WiFiP2pService();
                        service.device = device;
                        service.instanceName = device.deviceName;
                        service.serviceRegistrationType = device.primaryDeviceType;
                        mPeersService.set(i++,service);
                    }
                    if (peers.size() > 0) {
                        connect(peers.get(0));
                    }
                    // If an AdapterView is backed by this data, notify it
                    // of the change.  For instance, if you have a ListView of
                    // available peers, trigger an update.
//                    ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

                    // Perform any other updates needed based on the new list of
                    // peers connected to the Wi-Fi P2P network.
                }

                if (peers.size() == 0) {
                    Log.d(MainActivity.TAG, "No devices found");
                    return;
                }
            }


        };
    }

    public List<WiFiP2pService> getPeersService(){
        return mPeersService;
    }

    public void connect(WifiP2pDevice device) {
        // Picking the first device found on the network.

        WifiP2pDevice mDevice = device;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = mDevice.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Log.d(MainActivity.TAG, "Connect failed. Retry.");
            }
        });
    }
    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(MainActivity.TAG, action);
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Log.d(MainActivity.TAG, "Wifi P2P is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                Log.d(MainActivity.TAG, "Wifi P2P is not enabled");
            }
        }else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(MainActivity.TAG,"Connected to p2p network. Requesting network details");
//                mManager.requestConnectionInfo(mChannel,(WifiP2pManager.ConnectionInfoListener) activity);
            } else {
                // It's a disconnect
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {
            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(MainActivity.TAG, "Device status -" + device.status);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // Request available peers from the wifi p2p mManager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, myPeerListListener);
            }
            Log.d(MainActivity.TAG, "P2P peers changed");
        }
    }
}
