package com.marozzi.btle.heartrate

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.marozzi.btle.heartrate.HeartRateUtils.Companion.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID

/**
 * Created by andrea on 21/01/19.
 */
class HeartRateDevice(
    private var listener: HearRateDeviceListener,
    private val device: BluetoothDevice
) : BluetoothGattCallback() {

    companion object {
        const val TAG = "HeartRateDevice"
    }

    private var mGatt: BluetoothGatt? = null
    private var mainHandler: Handler = Handler(Looper.getMainLooper())

    fun getDevice(): BluetoothDevice {
        return device
    }

    fun connect(context: Context?) {
        device.connectGatt(context, false, this)
    }

    fun disconnect() {
        mGatt?.disconnect()
    }

    private fun disconnected() {
        Log.i(TAG, "disconnect")

        //not os OP but if the TGLoginConnections lost the parent how we know if the TgConnctions run in other thread?
        //So check if the looper is different from the main looper and kill it if true
        if (Looper.myLooper() != null && Looper.myLooper() != Looper.getMainLooper()) {
            Looper.myLooper()!!.quit()
        }

        mainHandler.post {
            listener.onDisconnect(this@HeartRateDevice)
            mainHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            // From NRF Connect App:
            // After 1.6s the services are already discovered so the following gatt.discoverServices() finishes almost immediately.
            // NOTE: This also works with shorted waiting time. The gatt.discoverServices() must be called after the indication is received which is
            // about 600ms after establishing connection. Values 600 - 1600ms should be OK.
            try {
                Thread.sleep(1600)
            } catch (e: Exception) {
                e.printStackTrace()

            }
            mGatt = gatt
            if (mGatt?.discoverServices() == true)
                Log.d(TAG, "Discover services Started")
            else
                Log.d(TAG, "Discover services Failed")

            listener.onConnected(this)
        } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Correct disconnection!")
            mGatt?.apply {
                disconnect()
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                close()
            }
            mGatt = null
            disconnected()
        } else if (status != BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Error: $status state: $newState")
            if (mGatt == null) {
                Log.d(TAG, "Controller Gatt is null -> use Callback's gatt")
                mGatt = gatt
            } else {
                disconnected()
                return
            }

            refreshGatt(mGatt)
            Log.d(TAG, "Gatt disconnect")
            mGatt?.disconnect()
            try {
                Log.d(TAG, "Sleep 1 sec")
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            Log.d(TAG, "Gatt close")
            mGatt?.close()
            try {
                Log.d(TAG, "Sleep 800 milliSec")
                Thread.sleep(800)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            Log.d(TAG, "Gatt connect")
            mGatt?.connect()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt.getHeartRateCharacteristic()?.let {
                Log.d(TAG, "Found heart Rate measurement characteristic register onchangevalue")
                mGatt?.setCharacteristicNotification(it, true)
                val descriptor =
                    it.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                mGatt?.writeDescriptor(descriptor)
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        Log.d(TAG, "onCharacteristicChanged")
        // this will get called anytime you perform a read or write characteristic operation
        if (characteristic.isHeartRateCharacteristic()) {
            val format = if (characteristic.properties and 0x01 != 0)
                BluetoothGattCharacteristic.FORMAT_UINT16
            else
                BluetoothGattCharacteristic.FORMAT_UINT8
            listener.onHeartRateValueChange(this, characteristic.getIntValue(format, 1))
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "New characteristic discovered from GATT server.")
        }
    }

    private fun refreshGatt(gatt: BluetoothGatt?): Boolean {
        try {
            Log.d(TAG, "Refresh Gatt")
            gatt?.javaClass?.getMethod("refresh", *arrayOfNulls(0))?.let {
                return (it.invoke(gatt, *arrayOfNulls(0)) as Boolean)
            }
        } catch (localException: Exception) {
            Log.e(TAG, "Refresh Gatt Exception", localException)
        }

        return false
    }

    interface HearRateDeviceListener {
        /**
         * When the connection to the equipment is successful
         *
         * @param connection       this
         */
        fun onConnected(connection: HeartRateDevice)

        /**
         * When the connection is interrupt or ended
         *
         * @param connection this
         * @param reason     disconnectReason of the disconnections
         */
        fun onDisconnect(connection: HeartRateDevice)

        /**
         * When the status of the equipment is changed, NOTE this method is called only if the equipment support the fitness machine service
         *
         * @param connection this
         * @param value  the new hear rate value
         */
        fun onHeartRateValueChange(connection: HeartRateDevice, value: Int)
    }
}