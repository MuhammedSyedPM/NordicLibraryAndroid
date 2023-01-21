/* 
  Copyright 2016- Nordic ID 
  NORDIC ID DEMO SOFTWARE DISCLAIMER

  You are about to use Nordic ID Demo Software ("Software"). 
  It is explicitly stated that Nordic ID does not give any kind of warranties, 
  expressed or implied, for this Software. Software is provided "as is" and with 
  all faults. Under no circumstances is Nordic ID liable for any direct, special, 
  incidental or indirect damages or for any economic consequential damages to you 
  or to any third party.

  The use of this software indicates your complete and unconditional understanding 
  of the terms of this disclaimer. 
  
  IF YOU DO NOT AGREE OF THE TERMS OF THIS DISCLAIMER, DO NOT USE THE SOFTWARE.  
*/

package com.technowave.techno_rfid.NurApi;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.technowave.techno_rfid.NordicId.NurApi;
import com.technowave.techno_rfid.R;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nordic ID on 18.7.2016.
 */

public class NurDeviceListActivity extends Activity implements NurDeviceScanner.NurDeviceScannerListener {
    public static final String TAG = "NurDeviceListActivity";
    public NurDeviceScanner mDeviceScanner;
    public static final String REQUESTED_DEVICE_TYPES = "TYPE_LIST";
    public static final int REQUEST_SELECT_DEVICE = 32778;
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = 1;
    public static final int REQ_BLE_DEVICES = 1;
    public static final int REQ_USB_DEVICES = 2;
    public static final int REQ_ETH_DEVICES = 4;
    public static final int LAST_DEVICE = 4;
    public static final int ALL_DEVICES = 7;
    public static final String STR_SCANTIMEOUT = "SCAN_TIMEOUT";
    public static final String STR_CHECK_NID = "NID_FILTER_CHECK";
    public static final String SPECSTR = "SPECSTR";
    private int mRequestedDevices = 0;
    private boolean mCheckNordicID = false;
    List<NurDeviceSpec> mDeviceList;
    private NurDeviceListActivity.DeviceAdapter deviceAdapter;
    private static final long DEF_SCAN_PERIOD = 5000L;
    private long mScanPeriod = 5000L;
    private boolean mScanning = false;
    private ProgressBar mScanProgress;
    private Button mCancelButton;
    private static NurApi mApi;
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            NurDeviceListActivity.this.mDeviceScanner.stopScan();
            NurDeviceSpec deviceSpec = (NurDeviceSpec)NurDeviceListActivity.this.mDeviceList.get(position);
            Bundle b = new Bundle();
            b.putString("SPECSTR", deviceSpec.getSpec());
            Intent result = new Intent();
            result.putExtras(b);
            NurDeviceListActivity.this.setResult(1, result);
            NurDeviceListActivity.this.finish();
        }
    };

    public NurDeviceListActivity() {
    }

    public void onScanStarted() {
        Log.d("NurDeviceListActivity", "Scan for devices started");
        this.mScanProgress.setVisibility(View.VISIBLE);
        this.mCancelButton.setText(R.string.cancel);
        this.mScanning = true;
    }

    public void onDeviceFound(NurDeviceSpec device) {
        this.mDeviceList.add(device);
        this.deviceAdapter.notifyDataSetChanged();
    }

    public void onScanFinished() {
        Log.d("NurDeviceListActivity", "Scan for devices finished");
        this.mCancelButton.setText(R.string.scan);
        this.mScanning = false;
        this.mScanProgress.setVisibility(View.GONE);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setResult(0);
        Log.d("NurDeviceListActivity", "onCreate");
        this.setContentView(R.layout.device_list);
        WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity = 48;
        layoutParams.y = 200;
        this.mCancelButton = (Button)this.findViewById(R.id.btn_cancel);
        this.mScanProgress = (ProgressBar)this.findViewById(R.id.scan_progress);
        this.mScanProgress.setVisibility(View.VISIBLE);
        this.mScanProgress.setScaleY(0.5F);
        this.mScanProgress.setScaleX(0.5F);
        this.mRequestedDevices = this.getIntent().getIntExtra("TYPE_LIST", 7);
        this.mScanPeriod = this.getIntent().getLongExtra("SCAN_TIMEOUT", 5000L);
        this.mCheckNordicID = this.getIntent().getBooleanExtra("NID_FILTER_CHECK", true);
        this.mDeviceScanner = new NurDeviceScanner(this, this.mRequestedDevices, this, mApi);
        if ((this.mRequestedDevices & 1) != 0) {
            this.mCancelButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!NurDeviceListActivity.this.mScanning) {
                        NurDeviceListActivity.this.mDeviceScanner.scanDevices();
                    } else {
                        NurDeviceListActivity.this.mDeviceScanner.stopScan();
                        if (!NurDeviceListActivity.this.mDeviceScanner.isEthQueryRunning()) {
                            NurDeviceListActivity.this.finish();
                        } else {
                            NurDeviceListActivity.this.showMessage("Ethernet query not ready...");
                        }
                    }

                }
            });
        } else {
            this.mCancelButton.setEnabled(false);
        }

        this.populateList();
    }

    private void populateList() {
        Log.d("NurDeviceListActivity", "populateList");
        this.mDeviceList = new ArrayList();
        this.deviceAdapter = new NurDeviceListActivity.DeviceAdapter(this, this.mDeviceList);
        ListView newDevicesListView = (ListView)this.findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(this.deviceAdapter);
        newDevicesListView.setOnItemClickListener(this.mDeviceClickListener);
        this.mDeviceScanner.scanDevices();
    }

    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.FOUND");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
    }

    public void onStop() {
        super.onStop();
        this.mDeviceScanner.stopScan();
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mDeviceScanner.stopScan();
    }

    protected void onPause() {
        super.onPause();
        this.mDeviceScanner.stopScan();
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public static void startDeviceRequest(Activity activity, NurApi api) throws InvalidParameterException {
        startDeviceRequest(activity, 7, 0L, false, api);
    }

    public static void startDeviceRequest(Activity activity, int devMask, NurApi api) throws InvalidParameterException {
        startDeviceRequest(activity, devMask, 0L, false, api);
    }

    public static void startDeviceRequest(Activity activity, int devMask, long scanTimeout, boolean filterNID, NurApi api) throws InvalidParameterException {
        if (devMask != 0 && (devMask & 7) != 0) {
            mApi = api;
            Intent newIntent = new Intent(activity.getApplicationContext(), NurDeviceListActivity.class);
            newIntent.putExtra("TYPE_LIST", devMask & 7);
            newIntent.putExtra("SCAN_TIMEOUT", scanTimeout);
            newIntent.putExtra("NID_FILTER_CHECK", filterNID);
            activity.startActivityForResult(newIntent, 32778);
        } else {
            throw new InvalidParameterException("startDeviceRequest(): no devices specified or context is invalid");
        }
    }

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<NurDeviceSpec> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<NurDeviceSpec> devices) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        public int getCount() {
            return this.devices.size();
        }

        public Object getItem(int position) {
            return this.devices.get(position);
        }

        public long getItemId(int position) {
            return (long)position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;
            if (convertView != null) {
                vg = (ViewGroup)convertView;
            } else {
                vg = (ViewGroup)this.inflater.inflate(R.layout.device_element, (ViewGroup)null);
            }

            NurDeviceSpec deviceSpec = (NurDeviceSpec)this.devices.get(position);
            TextView tvadd = (TextView)vg.findViewById(R.id.address);
            TextView tvname = (TextView)vg.findViewById(R.id.name);
            TextView tvpaired = (TextView)vg.findViewById(R.id.paired);
            TextView tvrssi = (TextView)vg.findViewById(R.id.rssi);
            if (deviceSpec.getType().equals("BLE")) {
                int rssiVal = deviceSpec.getRSSI();
                if (rssiVal < 0) {
                    tvrssi.setText("RSSI: " + rssiVal);
                } else {
                    tvrssi.setText("RSSI: N/A");
                }

                tvrssi.setVisibility(View.VISIBLE);
                if (deviceSpec.getBondState()) {
                    tvpaired.setVisibility(View.VISIBLE);
                } else {
                    tvpaired.setVisibility(View.GONE);
                }
            } else {
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.GONE);
            }

            tvname.setText(deviceSpec.getName());
            if (deviceSpec.getType().equals("TCP")) {
                tvadd.setText(deviceSpec.getAddress() + " (" + deviceSpec.getPart("transport", "LAN") + ")");
            } else {
                tvadd.setText(deviceSpec.getAddress());
            }

            return vg;
        }
    }
}
