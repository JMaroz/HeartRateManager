package com.marozzi.btle.heartrate.app

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.marozzi.btle.heartrate.HeartRateDevicesManager
import com.marozzi.btle.heartrate.app.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.synthetic.main.device_item.view.*
import kotlinx.android.synthetic.main.fragment_hr_scanner.view.*

class HRScannerFragment : Fragment() {

    companion object {

        fun getInstance(): HRScannerFragment =
            HRScannerFragment()
    }

    private val hrsFound = mutableSetOf<BluetoothDevice>()
    private val listener =
        object : HeartRateDevicesManager.SimpleHeartRateDevicesManagerListener() {

            override fun onHeartRateDeviceFound(device: BluetoothDevice) {
                hrsFound.add(device)
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 500)
            }
        }

    private val adapter = FastItemAdapter<DeviceItem>()
    private val handler = Handler()
    private val runnable = Runnable {
        FastAdapterDiffUtil[adapter.itemAdapter] =
            DeviceItem.getItems(hrsFound.toList())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_hr_scanner, container, false)
        view.recycler_view.layoutManager = LinearLayoutManager(requireContext())
        view.recycler_view.adapter = adapter
        view.recycler_view.itemAnimator?.apply {
            addDuration = 500
            moveDuration = 500
            removeDuration = 500
        }
        adapter.onClickListener = { _, _, item, _ ->
            val activity = requireActivity()
            if (activity is HRScannerListener) {
                activity.onHRSelected(item.device)
            }
            false
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        HeartRateDevicesManager.addListener(listener).startScanning()
    }

    override fun onPause() {
        super.onPause()
        HeartRateDevicesManager.removeListener(listener).stop(false)
    }

    interface HRScannerListener {

        fun onHRSelected(bluetoothDevice: BluetoothDevice)
    }

    open class DeviceItem(val device: BluetoothDevice) : AbstractItem<DeviceItem.ViewHolder>() {

        companion object {

            fun getItems(devices: List<BluetoothDevice>): List<DeviceItem> = devices.mapTo(
                mutableListOf()
            ) {
                DeviceItem(it).apply {
                    identifier = it.address.hashCode().toLong()
                }
            }
        }

        /** defines the type defining this item. must be unique. preferably an id */
        override val type: Int
            get() = R.id.fastadapter_device_item_id

        /** defines the layout which will be used for this item in the list  */
        override val layoutRes: Int
            get() = R.layout.device_item

        override fun getViewHolder(v: View): ViewHolder {
            return ViewHolder(v)
        }

        class ViewHolder(view: View) : FastAdapter.ViewHolder<DeviceItem>(view) {

            var name: TextView = view.name
            var address: TextView = view.address

            override fun bindView(item: DeviceItem, payloads: MutableList<Any>) {
                name.text = item.device.name
                address.text = item.device.address
            }

            override fun unbindView(item: DeviceItem) {
                name.text = null
                address.text = null
            }
        }
    }
}