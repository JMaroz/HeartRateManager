package com.marozzi.btle.heartrate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.marozzi.btle.scanner.model.ScanError
import kotlinx.android.synthetic.main.fragment_scan_error.view.*
import kotlinx.android.synthetic.main.view_scan_error.view.*

class ScanErrorFragment : Fragment() {

    companion object {

        fun getInstance(): ScanErrorFragment = ScanErrorFragment()

        private val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
        private const val REQUEST_CODE_PERMISSION_LOCATION = 99
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan_error, container, false)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        view?.error_list?.let { group ->
            group.removeAllViews()
            val error = HeartRateDevicesManager.canScan()
            if (error.isEmpty()) {
                val activity = requireActivity()
                if (activity is ScanErrorListener) {
                    activity.onNoError()
                }
            } else {
                error.forEach {
                    val view = getErrorView(it, group)
                    group.addView(view)
                }
            }
        }
    }

    private fun getErrorView(error: ScanError, container: ViewGroup): View {
        val view: View = layoutInflater.inflate(R.layout.view_scan_error, container, false)
        val stringRes = when (error) {
            ScanError.NO_BLUETOOTH_PERMISSION -> R.string.no_bt_permission
            ScanError.NO_BLUETOOTH_LE -> R.string.no_btle
            ScanError.BLUETOOTH_OFF -> R.string.bt_off
            ScanError.LOCATION_OFF -> R.string.gps_off
            ScanError.NO_LOCATION_PERMISSION -> R.string.no_gps_permission
            ScanError.UNKNOWN -> R.string.unknow_bt_error
            ScanError.PROVIDER_BEACON_ERROR -> R.string.bt_provider_error
            else -> R.string.no_error
        }
        view.scan_error_title.setText(stringRes)
        view.setOnClickListener {
            when (error) {
                ScanError.NO_BLUETOOTH_PERMISSION -> Toast.makeText(
                    context, R.string.change_your_manifest, Toast.LENGTH_SHORT
                ).show()
                ScanError.NO_BLUETOOTH_LE -> Toast.makeText(
                    context, R.string.nothing_todo, Toast.LENGTH_SHORT
                ).show()
                ScanError.BLUETOOTH_OFF -> {
                    BluetoothAdapter.getDefaultAdapter().enable()
                    refresh()
                }
                ScanError.LOCATION_OFF -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                ScanError.NO_LOCATION_PERMISSION -> requestPermissions(
                    permissions, REQUEST_CODE_PERMISSION_LOCATION
                )
                else -> Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    interface ScanErrorListener {

        fun onNoError()
    }
}