package com.marozzi.btle.heartrate

import com.marozzi.btle.heartrate.HeartRateUtils.Companion.SERVICE_GARMIN_UUID
import com.marozzi.btle.heartrate.HeartRateUtils.Companion.SERVICE_HEART_RATE_GARMIN_UUID
import com.marozzi.btle.heartrate.HeartRateUtils.Companion.SERVICE_HEART_RATE_UUID
import com.marozzi.btle.scanner.interfaces.Provider
import com.marozzi.btle.scanner.interfaces.iBeacon


/**
 * Created by andrea on 21/01/19.
 */
open class HeartRateDevicesProvider : Provider<iBeacon> {

    private var callback: Provider.ProviderCallback<iBeacon>? = null

    override fun start() {

    }

    override fun stop() {

    }

    override fun setProviderCallback(callback: Provider.ProviderCallback<iBeacon>) {
        this.callback = callback
    }

    override fun elaborateBeacon(beacon: iBeacon) {
        val services = beacon.services ?: return
        val heartRateService = services.filter {
            (it.uuid == SERVICE_HEART_RATE_UUID ||
                    it.uuid == SERVICE_HEART_RATE_GARMIN_UUID ||
                    it.uuid == SERVICE_GARMIN_UUID)
        }
        if (heartRateService.isNotEmpty()) callback?.onProviderCompleted(beacon)
    }
}