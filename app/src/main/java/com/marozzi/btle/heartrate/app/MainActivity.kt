package com.marozzi.btle.heartrate.app

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.marozzi.btle.heartrate.HeartRateDevicesManager

class MainActivity : AppCompatActivity(),
    ScanErrorFragment.ScanErrorListener,
    HRScannerFragment.HRScannerListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        HeartRateDevicesManager.init(this)
        if (HeartRateDevicesManager.canScan().isNotEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    ScanErrorFragment.getInstance()
                ).commit()
        } else {
            showScanner()
        }
    }

    override fun onNoError() {
        showScanner()
    }

    private fun showScanner() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                HRScannerFragment.getInstance()
            ).commit()
    }

    override fun onHRSelected(bluetoothDevice: BluetoothDevice) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                HRDeviceFragment.getInstance(
                    bluetoothDevice
                )
            ).commit()
    }
}
