package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.datatype.Duration;

/**
 * Created by Yetong on 2017/10/8.
 */

public class RxTxActivity extends Activity {

    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mCharaDescriptor;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mDeviceAddressTextView;

    private String mServiceUUID, mDeviceAddress;
    private String mCharaUUID, mDeviceName;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private Button EditButton;
    private Button CharaSubscribeButton;
    private EditText EditText;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private String CHARA_DESC = "";
    private String properties = "";

    private Context context = this;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

            mBluetoothLeService.readCustomDescriptor(mCharaUUID, mServiceUUID);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    break;
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Data Received!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    break;
                case BluetoothLeService.ACTION_DESCRIPTOR_AVAILABLE:
                    Log.i("Receiving data", "Broadcast received");
                    displayDescriptor(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_transfer);

        final Intent intent = getIntent();

        Log.i("OnCreate", "Created");

        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mServiceUUID = intent.getStringExtra("Service UUID");
        mCharaUUID = intent.getStringExtra("Characteristic UUID");
        CHARA_DESC = intent.getStringExtra("Characteristic Descriptor");
        properties = intent.getStringExtra("Characteristic properties");

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address_rxtx)).setText("Characteristic UUID: " + mCharaUUID);
        ((TextView) findViewById(R.id.characteristic_Descriptor)).setText("Characteristic Descriptor: " + CHARA_DESC);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);

        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mConnectionState.setText("Connected");

        mDataField = (TextView) findViewById(R.id.data_value);
        EditText = (EditText) findViewById(R.id.characteristicEditText);
        EditButton = (Button) findViewById(R.id.characteristicButton);
        CharaSubscribeButton = (Button) findViewById(R.id.characteristic_Subscribe);
        mCharaDescriptor = (TextView) findViewById(R.id.characteristic_Descriptor);

        EditButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String str = EditText.getText().toString();
                mBluetoothLeService.writeCustomCharacteristic(str, mServiceUUID, mCharaUUID);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Message Sent!", Toast.LENGTH_SHORT).show();
                    }
                });
                mBluetoothLeService.readCustomDescriptor(mCharaUUID, mServiceUUID);
            }
        });

        CharaSubscribeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (properties.indexOf("Indicate") >= 0) {
                    mBluetoothLeService.subscribeCustomCharacteristic(mServiceUUID, mCharaUUID, 1);
                } else if (properties.indexOf("Notify") >= 0) {
                    mBluetoothLeService.subscribeCustomCharacteristic(mServiceUUID, mCharaUUID, 2);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Characteristic Subscribed!", Toast.LENGTH_SHORT).show();
                    }
                });
                mBluetoothLeService.readCustomDescriptor(mCharaUUID, mServiceUUID);
            }
        });

        checkProperties();
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void checkProperties() {
        if (properties.indexOf("Write") >= 0) {
        } else {
            EditButton.setEnabled(false);
        }
        if (properties.indexOf("Indicate") >= 0) {
        } else {
            CharaSubscribeButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private void displayDescriptor(final String data) {
        if( data != null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCharaDescriptor.setText(mCharaDescriptor.getText().toString() + "\n" + data);
                }
            });
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DESCRIPTOR_AVAILABLE);
        return intentFilter;
    }
}


