package com.gabriel.tcc;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceList extends ListActivity{

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    static String ConDeviceAddress = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                String DeviceName = device.getName();
                String DeviceAddress = device.getAddress();
                ArrayBluetooth.add(DeviceName + "\n     " + DeviceAddress);
            }
        }
        setListAdapter(ArrayBluetooth);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String deviceInfo = ((TextView) v).getText().toString();
        //Toast.makeText(getApplicationContext(),"Info: " + deviceInfo, Toast.LENGTH_LONG).show();

        String deviceAddress = deviceInfo.substring(deviceInfo.length()-17);
        //Toast.makeText(getApplicationContext(),"Address: " + deviceAddress, Toast.LENGTH_LONG).show();

        Intent returnAddress = new Intent();
        returnAddress.putExtra(ConDeviceAddress,deviceAddress);
        setResult(RESULT_OK, returnAddress);
        finish();
    }
}
