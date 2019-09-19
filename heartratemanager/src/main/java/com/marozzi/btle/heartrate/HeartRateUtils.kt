package com.marozzi.btle.heartrate

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.*

/**
 * Created by andrea on 21/01/19.
 */
class HeartRateUtils private constructor() {

    companion object {
        val SERVICE_GARMIN_UUID: UUID = UUID.fromString("6a4e3e10-667b-11e3-949a-0800200c9a66")
        val SERVICE_HEART_RATE_GARMIN_UUID: UUID =
            UUID.fromString("6a4e2500-667b-11e3-949a-0800200c9a66")

        val SERVICE_HEART_RATE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")

        val CHARACTERISTIC_HEART_RATE_MEASUREMENT_GARMIN_UUID: UUID =
            UUID.fromString("6a4e2501-667b-11e3-949a-0800200c9a66")
        val CHARACTERISTIC_HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

        val CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

/**
 * Search and find the Service for the HeartRate
 */
fun BluetoothGatt.getHeartRateService(): BluetoothGattService? =
    services.find { it.isHeartRateService() }

/**
 * Indicate if the service has the standard Heart Rate identifier
 */
fun BluetoothGattService.isHeartRateService(): Boolean =
    uuid == HeartRateUtils.SERVICE_HEART_RATE_UUID || uuid == HeartRateUtils.SERVICE_HEART_RATE_GARMIN_UUID

/**
 * Indicate if the service has the at least one Characteristic for subscribe to receive the heart rate measurement
 */
fun BluetoothGattService.hasHeartRateCharacteristic(): Boolean =
    getHeartRateCharacteristic() != null

/**
 * Search and find the first Characteristic where subscribe for receive the heart rate measurement
 */
fun BluetoothGattService.getHeartRateCharacteristic(): BluetoothGattCharacteristic? =
    characteristics.find {
        it.isHeartRateCharacteristic()
    }

/**
 * Indicate if the characteristic  has the standard Heart Rate identifier for subscribe to receive the heart rate measurement
 */
fun BluetoothGattCharacteristic.isHeartRateCharacteristic(): Boolean =
    uuid == HeartRateUtils.CHARACTERISTIC_HEART_RATE_MEASUREMENT_UUID ||
            uuid == HeartRateUtils.CHARACTERISTIC_HEART_RATE_MEASUREMENT_GARMIN_UUID

/**
 * Find the first service in the gatt that has the HearRate uuid and
 */
fun BluetoothGatt.getHeartRateCharacteristic(): BluetoothGattCharacteristic? =
    services.firstOrNull { it.isHeartRateService() && it.hasHeartRateCharacteristic() }?.getHeartRateCharacteristic()