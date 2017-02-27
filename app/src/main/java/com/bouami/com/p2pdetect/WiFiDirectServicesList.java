package com.bouami.com.p2pdetect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mohammed on 14/02/2017.
 */

public class WiFiDirectServicesList extends ListFragment {

    public static final String TAG = "WiFiDirectServicesList";
    WiFiDevicesAdapter listAdapter = null;

    interface DeviceClickListener {
        public void connectP2p(WiFiP2pService wifiP2pService);

        void onConnectionInfoAvailable(WifiP2pInfo p2pInfo);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.devices_list, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listAdapter = new WiFiDevicesAdapter(this.getActivity(),
                android.R.layout.simple_list_item_2, android.R.id.text1,
                new ArrayList<WiFiP2pService>());
        setListAdapter(listAdapter);
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        ((DeviceClickListener) getActivity()).connectP2p((WiFiP2pService) l.getItemAtPosition(position));
        ((TextView) v.findViewById(android.R.id.text2)).setText("Connecté ...");
    }

    public class WiFiDevicesAdapter extends ArrayAdapter<WiFiP2pService> {
        private List<WiFiP2pService> items;
        public WiFiDevicesAdapter(Context context, int resource,
                                  int textViewResourceId, List<WiFiP2pService> items) {
            super(context, resource, textViewResourceId, items);
            this.items = items;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_2, null);
            }
            WiFiP2pService service = items.get(position);
            if (service != null) {
                TextView nameText = (TextView) v.findViewById(android.R.id.text1);
                if (nameText != null) {
                    nameText.setText(service.device.deviceName + " - " + service.instanceName);
                }
                TextView statusText = (TextView) v.findViewById(android.R.id.text2);
                statusText.setText(getDeviceStatus(service.device.status));
            }
            return v;
        }
    }


    public static String getDeviceStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connecté";
            case WifiP2pDevice.INVITED:
                return "Invité";
            case WifiP2pDevice.FAILED:
                return "Erreur";
            case WifiP2pDevice.AVAILABLE:
                return "Disponible";
            case WifiP2pDevice.UNAVAILABLE:
                return "Indisponible";
            default:
                return "Unconnu";
        }
    }
}
