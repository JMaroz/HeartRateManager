# HeartRateManager
Library for scan and connect to the HeartRate devices

## Contents

- [Installation](#installation)
- [How to use](#how-to-use)
- [Bugs and feedback](#bugs-and-feedback)
- [Credits](#credits)
- [License](#license)

## Installation

[ ![Download](https://api.bintray.com/packages/maro/maven/HeartRateManager/images/download.svg) ](https://bintray.com/maro/maven/HeartRateManager/_latestVersion)

    implementation 'com.marozzi:heartratemanager:$latest_version'

## How to use

### Init the manager

The manager must be inizializate before scan or connect to a Device

    HeartRateDevicesManager.init(this)

### Scan devices

Before scan ensure that the manager can scan for devices by calling

    HeartRateDevicesManager.canScan()

after that call to start the scanning

    HeartRateDevicesManager.startScanning()
    
every heart rate devices found will be notify by 
    
    object : HeartRateDevicesManager.SimpleHeartRateDevicesManagerListener() {
    
        override fun onHeartRateDeviceFound(device: BluetoothDevice) {
        
        }
    }

### Connect

To connect simple call the method connect with a mac address or a BluetoothDevice

    HeartRateDevicesManager.addListener(listener)

the result of the connect and the heart rate value will be dispatched to the listener

    object : HeartRateDevicesManager.SimpleHeartRateDevicesManagerListener() {

        override fun onHeartRateDeviceConnected(device: BluetoothDevice) {
            // connected to the heart rate device
        }

        override fun onHeartRateDeviceDisconnected(device: BluetoothDevice) {
            // disconnected to the heart rate device
        }

        override fun onHeartRateDeviceValueChange(device: BluetoothDevice, value: Int) {
            // new heart rate value from the device
        }
    }

## Bugs and Feedback

For bugs, feature requests, and discussion please use [GitHub Issues](https://github.com/JMaroz/HeartRateManager/issues)

## Credits

This library was inspired by some other repos and some bluetooth articles found online

## License

    MIT License

    Copyright (c) 2017 JMaroz

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.