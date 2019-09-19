package com.marozzi.btle.heartrate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.marozzi.btle.scanner.DefaultBluetoothFactory
import com.marozzi.btle.scanner.DefaultScanService
import com.marozzi.btle.scanner.interfaces.ScanService
import com.marozzi.btle.scanner.interfaces.ScanServiceCallback
import com.marozzi.btle.scanner.interfaces.iBeacon
import com.marozzi.btle.scanner.model.ScanError
import com.marozzi.btle.scanner.model.ScanState
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates

/**
 * Created by andrea on 21/01/19.
 */
object HeartRateDevicesManager {

    private const val TAG = "HeartRateDevicesManager"

    /**
     * Event state for the HeartRateDevicesManager
     */
    enum class State {
        /**
         * Where the manager has stopped all the connections
         */
        Disconnected,
        /**
         * When the manager start scanning for Heart rate device
         */
        Scanning,
        /**
         * When the manager start connecting to a Heart rate device (if the manager has one or more connection this state can be delivered multiple times)
         */
        Connecting,
        /**
         * When the manager has established a connection with a Heart rate device (if the manager has one or more connection this state can be delivered multiple times)
         */
        Connected,
    }

    private var scanService: ScanService? = null
    private val scanServiceCallback = object : ScanServiceCallback {
        override fun onBeaconFound(beacon: iBeacon) {
            Log.d(TAG, "onBeaconFound ${beacon.device.address}")
            //check if we already have a connections with the device
            if (!connections.containsKey(beacon.device.address)) {
                handlerMainThread.post {
                    for (listener in listeners) {
                        listener.onHeartRateDeviceFound(beacon.device)
                    }
                }
            }
        }

        override fun onError(scanError: ScanError) {
            Log.d(TAG, "onError " + scanError.name)
            handlerMainThread.post {
                for (listener in listeners) {
                    listener.onUnableToStart(scanError)
                }
            }
        }
    }

    private val handlerMainThread = Handler(Looper.getMainLooper())
    private var runAsync = true
    private var context: WeakReference<Context>? = null
    private val listeners: ArrayList<HeartRateDevicesManagerListener> = ArrayList()
    private var state: State by Delegates.observable(
        State.Disconnected
    ) { _, _, new ->
        handlerMainThread.post {
            for (listener in listeners) {
                listener.onHeartRateDeviceStateChange(new)
            }
        }
    }

    private val connections: ConcurrentHashMap<String, HeartRateDevice> = ConcurrentHashMap()
    private val connectionListener = object : HeartRateDevice.HearRateDeviceListener {
        override fun onConnected(connection: HeartRateDevice) {
            Log.i(TAG, "Connected to ${connection.getDevice().address}")
            state = State.Connected
            handlerMainThread.post {
                for (listener in listeners) {
                    listener.onHeartRateDeviceConnected(connection.getDevice())
                }
            }
        }

        override fun onDisconnect(connection: HeartRateDevice) {
            Log.e(TAG, "Disconnect from " + connection.getDevice().toString())
            connections.remove(connection.getDevice().address)
            if (!isConnectedOrConnecting()) state = State.Disconnected
            handlerMainThread.post {
                for (listener in listeners) {
                    listener.onHeartRateDeviceDisconnected(connection.getDevice())
                }
            }
        }

        override fun onHeartRateValueChange(connection: HeartRateDevice, value: Int) {
            Log.i(
                TAG, "Received a new Heart Rate value $value from ${connection.getDevice().address}"
            )
            handlerMainThread.post {
                for (listener in listeners) {
                    listener.onHeartRateDeviceValueChange(connection.getDevice(), value)
                }
            }
        }
    }

    fun getManagerState(): State = state

    fun init(context: Context, async: Boolean = true): HeartRateDevicesManager {
        Log.d(TAG, "init")
        HeartRateDevicesManager.context = WeakReference(context.applicationContext)
        runAsync = async

        if (scanService == null) {
            scanService = DefaultScanService(
                HeartRateDevicesManager.context!!.get()!!,
                DefaultBluetoothFactory(),
                HeartRateDevicesProvider()
            )
            scanService!!.setCallback(scanServiceCallback)
        }

        return this
    }

    fun addListener(listener: HeartRateDevicesManagerListener): HeartRateDevicesManager {
        if (!listeners.contains(listener)) listeners.add(listener)
        return this
    }

    fun removeListener(listener: HeartRateDevicesManagerListener): HeartRateDevicesManager {
        listeners.remove(listener)
        return this
    }

    fun startScanning() {
        Log.d(TAG, "start")
        if (context == null || context?.get() == null || scanService == null) throw IllegalAccessError("Call init first")

        scanService!!.start()

        if (!isConnectedOrConnecting()) state = State.Scanning
    }

    fun stop(withConnectionToo: Boolean) {
        Log.d(TAG, "stop")
        stopScanService()
        if (withConnectionToo && connections.size > 0) {
            Log.d(TAG, "stop connections too")
            disconnectAll()
        }
        scanService = null
    }

    /**
     * Stop scan, all connections alive and release resources
     */
    fun destroy() {
        stop(true)
        connections.clear()
        listeners.clear()
        context = null
    }

    fun canScan(): List<ScanError> = scanService!!.canScan()

    fun isScanning(): Boolean = ScanState.STATE_SCANNING == scanService?.state

    fun isConnectedOrConnecting(): Boolean = connections.size > 0

    fun getHearRateDevices(): List<HeartRateDevice> = connections.values.toList()

    private fun stopScanService() {
        Log.d(TAG, "stopScanService")
        scanService?.stop()
    }

    fun connectTo(address: String): Boolean {
        Log.d(TAG, "connectTo $address")
        if (!connections.contains(address)) {
            BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)?.let {
                connectTo(it)
                return true
            }
        }
        return false
    }

    fun connectTo(device: BluetoothDevice) {
        Log.d(TAG, "connectTo $device")
        //prevent creations more than one connections to the device
        if (!connections.contains(device.address)) {
            stopScanService()
            if (runAsync) {
                object : Thread("HeartRateDevice") {
                    override fun run() {
                        super.run()
                        Looper.prepare()
                        createHearRateDevice(
                            device
                        )
                        Looper.loop()
                    }
                }.start()
            } else {
                createHearRateDevice(device)
            }
            state = State.Connecting
        }
    }

    private fun createHearRateDevice(device: BluetoothDevice) {
        var connection: HeartRateDevice? = connections[device.address]
        if (connection == null) { // prevent from create multiple connections
            connection = HeartRateDevice(
                connectionListener, device
            )
            connections[device.address] = connection
            connection.connect(context?.get())
        }
    }

    fun disconnectAll() {
        connections.values.forEach {
            it.disconnect()
        }
    }

    fun disconnect(device: BluetoothDevice) {
        disconnect(device.address)
    }

    fun disconnect(address: String) {
        connections[address]?.disconnect()
    }

    interface HeartRateDevicesManagerListener {

        /**
         * Unable to start the scan
         *
         * @param scanError error
         */
        fun onUnableToStart(scanError: ScanError)

        /**
         * The equipment is found and available to connect
         *
         * @param device           the device with the connection is established
         */
        fun onHeartRateDeviceFound(device: BluetoothDevice)

        /**
         * The equipment is connected
         *
         * @param device           the device with the connection is established
         */
        fun onHeartRateDeviceConnected(device: BluetoothDevice)

        /**
         * The equipment is disconnected
         *
         * @param device           the device with the disconnected as occur
         */
        fun onHeartRateDeviceDisconnected(device: BluetoothDevice)

        /**
         * Called when the heart rate device send a new value
         *
         * @param device     device
         * @param value  new status
         */
        fun onHeartRateDeviceValueChange(device: BluetoothDevice, value: Int)

        /**
         * When the manager change state, this event will be called only when
         */
        fun onHeartRateDeviceStateChange(state: State)
    }

    open class SimpleHeartRateDevicesManagerListener : HeartRateDevicesManagerListener {
        override fun onUnableToStart(scanError: ScanError) {
        }

        override fun onHeartRateDeviceFound(device: BluetoothDevice) {
        }

        override fun onHeartRateDeviceConnected(device: BluetoothDevice) {
        }

        override fun onHeartRateDeviceDisconnected(device: BluetoothDevice) {
        }

        override fun onHeartRateDeviceValueChange(device: BluetoothDevice, value: Int) {
        }

        override fun onHeartRateDeviceStateChange(sate: State) {
        }
    }
}