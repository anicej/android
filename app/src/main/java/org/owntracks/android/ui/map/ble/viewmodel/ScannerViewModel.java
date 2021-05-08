/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.owntracks.android.ui.map.ble.viewmodel;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.ui.map.ble.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScannerViewModel extends AndroidViewModel {
    private static final String PREFS_FILTER_UUID_REQUIRED = "filter_uuid";
    private static final String PREFS_FILTER_NEARBY_ONLY = "filter_nearby";
    LiveData<List<ScanResult>> liveResults;
    /**
     * MutableLiveData containing the list of devices.
     */
    private final DevicesLiveData devicesLiveData;
    /**
     * MutableLiveData containing the scanner state.
     */
    public final ScannerStateLiveData scannerStateLiveData;

    private final SharedPreferences preferences;

    public DevicesLiveData getDevices() {
        return devicesLiveData;
    }

    public ScannerStateLiveData getScannerState() {
        Log.e("TAG", "getScannerState: ___________________" );
        return scannerStateLiveData;
    }

    public ScannerViewModel(final Application application) {
        super(application);
        preferences = PreferenceManager.getDefaultSharedPreferences(application);

        final boolean filterUuidRequired = isUuidFilterEnabled();
        final boolean filerNearbyOnly = isNearbyFilterEnabled();

        scannerStateLiveData = new ScannerStateLiveData(Utils.isBleEnabled(),
                Utils.isLocationEnabled(application));
        devicesLiveData = new DevicesLiveData(filterUuidRequired, filerNearbyOnly);
        registerBroadcastReceivers(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        getApplication().unregisterReceiver(bluetoothStateBroadcastReceiver);

        if (Utils.isMarshmallowOrAbove()) {
            getApplication().unregisterReceiver(locationProviderChangedReceiver);
        }
    }

    public boolean isUuidFilterEnabled() {
        return preferences.getBoolean(PREFS_FILTER_UUID_REQUIRED, true);
    }

    public boolean isNearbyFilterEnabled() {
        return preferences.getBoolean(PREFS_FILTER_NEARBY_ONLY, false);
    }


    public void refresh() {
        scannerStateLiveData.refresh();
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param uuidRequired if true, the list will display only devices with Led-Button Service UUID
     *                     in the advertising packet.
     */
    public void filterByUuid(final boolean uuidRequired) {
        preferences.edit().putBoolean(PREFS_FILTER_UUID_REQUIRED, uuidRequired).apply();
        if (devicesLiveData.filterByUuid(uuidRequired))
            scannerStateLiveData.recordFound();
        else
            scannerStateLiveData.clearRecords();
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param nearbyOnly if true, the list will show only devices with high RSSI.
     */
    public void filterByDistance(final boolean nearbyOnly) {
        preferences.edit().putBoolean(PREFS_FILTER_NEARBY_ONLY, nearbyOnly).apply();
        if (devicesLiveData.filterByDistance(nearbyOnly))
            scannerStateLiveData.recordFound();
        else
            scannerStateLiveData.clearRecords();
    }

    /**
     * Start scanning for Bluetooth devices.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startScan() {
        if (scannerStateLiveData.isScanning()) {
            Log.e("TAG", "startScan: ----------" );
            return;
        }

        // Scanning settings
        Log.e("TAG", "startScan: ++++++++++++" );
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(100)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        final ScanFilter filter = new ScanFilter.Builder()
//				.setServiceUuid(ParcelUuid.fromString("33604c19-3a88-3cdf-a815-8c315ee24629"))
//				.setServiceData(fromString("33604c19-3a88-3cdf-a815-8c315ee24629"))
                .setDeviceName("softlogistics")
                .build();

        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(filter);
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(filters, settings, scanCallback);
        scannerStateLiveData.scanningStarted();
    }

    /**
     * Stop scanning for bluetooth devices.
     */
    public void stopScan() {

        if (scannerStateLiveData.isScanning() && scannerStateLiveData.isBluetoothEnabled()) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
            scannerStateLiveData.scanningStopped();
            Log.e("TAG", "stopScan: -----------" );
        }
        Log.e("TAG", "stopScan: +++++++++++" );

    }


    private final BluetoothGattCallback scanGattCallback = new BluetoothGattCallback() {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
			Log.e("TAG", "onServicesDiscovered: ");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
			Log.e("TAG", "onCharacteristicRead: " );
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
			Log.e("TAG", "onCharacteristicWrite: " );
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
			Log.e("TAG", "onConnectionStateChange: "+gatt.getConnectedDevices().get(0).getUuids()+gatt.getDevice().getName());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
			Log.e("TAG", "onCharacteristicChanged: ");
        }
    };


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
            // This callback will be called only if the scan report delay is not set or is set to 0.
            Log.e("TAG", "onScanResult:  ___________________________" + result.getDevice().getAddress());

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
                Utils.markLocationNotRequired(getApplication());
            Log.e("TAG", "onScanResult:  ___________________________" + result.getDevice().getAddress());
            if (devicesLiveData.deviceDiscovered(result)) {
                devicesLiveData.applyFilter();
                scannerStateLiveData.recordFound();
            }

            result.getDevice().connectGatt(getApplication().getBaseContext(),true,scanGattCallback);
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
            // This callback will be called only if the report delay set above is greater then 0.

            if (liveResults == null) {
                liveResults = new MutableLiveData<>();
            }

            double distance = 0;
            for (int i = 0; i < results.size(); i++) {

                //				results.get(i).getDevice().connectGatt(getApplication().getBaseContext(),true,scanGattCallback);
                double ratio = results.get(i).getRssi() * 1.0 / results.get(i).getTxPower();
//                Log.e("TAG", "onBatchScanResults: " + ratio);
                if (ratio < 1.0) {
                    distance = Math.pow(ratio, 10);
                } else {
                    distance = (0.42093) * Math.pow(ratio, 6.9476) + 0.54992;

                }
                distance = (0.42093) * Math.pow(ratio, 6.9476) + 0.54992;
                Log.e("TAG", "onBatchScanResults()" + results.get(i).getDevice().getAddress() + "___" + results.get(i).getRssi() + "___" +
                        results.get(i).getDevice().describeContents() + "___" + "___" + results.get(i).getDevice().getName()
                        + "___" + results.get(i).getScanRecord().getServiceData()+ "____" + distance + ")))))"
                +"_____");

                Map<ParcelUuid, byte[]> map= results.get(i).getScanRecord().getServiceData();
//                Log.e("TAG", "onBatchScanResults: "+map.keySet().toString());

//					ParseRecord(results.get(i).getScanRecord().getBytes());
            }
            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
                Utils.markLocationNotRequired(getApplication());

            boolean atLeastOneMatchedFilter = false;
            for (final ScanResult result : results)
                atLeastOneMatchedFilter = devicesLiveData.deviceDiscovered(result) || atLeastOneMatchedFilter;
            if (atLeastOneMatchedFilter) {
                devicesLiveData.applyFilter();
                scannerStateLiveData.recordFound();
            }



		}

        @Override
        public void onScanFailed(final int errorCode) {
            // TODO This should be handled
            scannerStateLiveData.scanningStopped();
        }
    };

    /**
     * Register for required broadcast receivers.
     */
    private void registerBroadcastReceivers(@NonNull final Application application) {
        application.registerReceiver(bluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (Utils.isMarshmallowOrAbove()) {
            application.registerReceiver(locationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider.
     */
    private final BroadcastReceiver locationProviderChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean enabled = Utils.isLocationEnabled(context);
            scannerStateLiveData.setLocationEnabled(enabled);
        }
    };

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter.
     */
    private final BroadcastReceiver bluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    scannerStateLiveData.bluetoothEnabled();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                        stopScan();
                        scannerStateLiveData.bluetoothDisabled();
                    }
                    break;
            }
        }
    };


    static public Map<Integer, String> ParseRecord(byte[] scanRecord) {
        Map<Integer, String> ret = new HashMap<Integer, String>();
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Zero value indicates that we are done with the record now
            if (length == 0) break;

            int type = scanRecord[index];
            //if the type is zero, then we are pass the significant section of the data,
            // and we are thud done
            if (type == 0) break;

            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            if (data != null && data.length > 0) {
                StringBuilder hex = new StringBuilder(data.length * 2);
                // the data appears to be there backwards
                for (int bb = data.length - 1; bb >= 0; bb--) {
                    hex.append(String.format("%02X", data[bb]));
                    Log.e("TAG", "ParseRecord: " + data[bb]);
                }
                ret.put(type, hex.toString());
            }
            index += length;
        }
        Log.e("TAG", "ParseRecord: " + ret);
        return ret;
    }

//	public void ParseRecord (byte[] scanRecord) {
//
//		try {
//			String decodedRecord = new String(scanRecord,"UTF-8");
//			Log.d("DEBUG","decoded String : " + ByteArrayToString(scanRecord));
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//
//		// Parse data bytes into individual records
//		List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);
//
//
//		// Print individual records
//		if (records.size() == 0) {
//			Log.i("DEBUG", "Scan Record Empty");
//		} else {
//			Log.i("DEBUG", "Scan Record: " + TextUtils.join(",", records));
//		}
//
//	}


    public String getUUID(ScanResult result) {
        String UUIDx = UUID.nameUUIDFromBytes(result.getScanRecord().getBytes()).toString();
        Log.e("UUID", " as String ->>" + UUIDx + result.getScanRecord().getDeviceName());
        return UUIDx;
    }
}
