package com.marozzi.btle.heartrate.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.marozzi.btle.heartrate.HeartRateDevicesManager
import kotlinx.android.synthetic.main.fragment_hr_device.view.*

class HRDeviceFragment : Fragment() {

    companion object {

        fun getInstance(device: BluetoothDevice): HRDeviceFragment {
            val instance = HRDeviceFragment()
            instance.arguments = Bundle().apply {
                putParcelable("device", device)
            }
            return instance
        }
    }

    private var scaleDown: ValueAnimator? = null
    private var device: BluetoothDevice? = null
    private val listener =
        object : HeartRateDevicesManager.SimpleHeartRateDevicesManagerListener() {

            override fun onHeartRateDeviceConnected(device: BluetoothDevice) {
                view?.icon?.let {
                    scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                        it,
                        PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.2f)
                    )
                    scaleDown!!.duration = 350
                    scaleDown!!.repeatCount = ObjectAnimator.INFINITE
                    scaleDown!!.repeatMode = ObjectAnimator.REVERSE
                    scaleDown!!.start()
                }
                view?.value?.text = getString(R.string.connected)
            }

            override fun onHeartRateDeviceDisconnected(device: BluetoothDevice) {
                scaleDown?.end()
                view?.value?.text = getString(R.string.disconnected)
            }

            override fun onHeartRateDeviceValueChange(device: BluetoothDevice, value: Int) {
                view?.value?.text = value.toString()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hr_device, container, false)
    }

    override fun onResume() {
        super.onResume()
        HeartRateDevicesManager.addListener(listener)
        arguments?.getParcelable<BluetoothDevice>("device")?.let {
            device = it
            HeartRateDevicesManager.connectTo(it)
        }
    }

    override fun onPause() {
        super.onPause()
        HeartRateDevicesManager.removeListener(listener)
        device?.let {
            HeartRateDevicesManager.disconnect(it)
        }
    }
}