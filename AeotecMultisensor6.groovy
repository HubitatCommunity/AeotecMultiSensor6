/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *         v1.4  Added selectiveReport, enabled humidChangeAmount, luxChangeAmount
 *         v1.3  Restored logDebug as default logging is too much. cSteele
 *         v1.2  Merged Chuckles updates
 *         v1.1d Added remote version checking ** Cobra (CobraVmax) for his original version check code
 * csteele v1.1c converted to Hubitat.
 *
 * Chuckles V1.2 of multisensor 6
 * IMPORTANT NOTE: Assumes device has firmware version 1.10 or later. A warning will be logged if this is not the case.
 * Changes:
   1.  Implement standard "Power Source" capability to track whether USB power ("dc") or battery is in use
          - Remove custom "powerSupply" and "batteryStatus" attributes it replaces.
   2.  Use hub's timezone rather than setting a timezone for each device individually
   3.  Use hub's temperature scale setting rather than setting each device individually
   4.  Add event handler for ManufacturerSpecificReport (V1)
   5.  Corrected configuration parameter number (4) for PIR sensitivity
   6.  Corrected getTimeOptionValueMap
   7.  Disabled selective reporting on threshold
   8.  Device only supports V1 of COMMAND_CLASS_SENSOR_BINARY (0x30) - not V2
   9.  Device only supports V1 of COMMAND_CLASS_CONFIGURATION (0x70) - not V2
   10. Motion detection in NotificationReportV3 is event 8, not event 7
   11. Numerous minor bug fixes (e.g. motionDelayTime and reportInterval should always hold enumerated values, not number of seconds)
   12. Numerous small changes for internationalisation (e.g. region agnostic date formats)

 * LGK V1 of multisensor 6 with customizeable settings , changed some timeouts, also changed tamper to vibration so we can
 * use that as well (based on stock device type and also some changes copied from Robert Vandervoort device type.
 * Changes
   1. changes colors of temp slightly, add colors to humidity, add color to battery level
   2. remove tamper and instead use feature as contact and acceleration ie vibration sensor
   3. slightly change reporting interval times. (also fix issue where 18 hours was wrong time)
   4. add last update time.. sets when configured and whenever temp is reported. 
      This is used so at a glance you see the last update time tile to know the device is still reporting easily without looking
      at the logs.
   5. add a temp and humidity offset -12 to 12 to enable tweaking the settings if they appear off.
   6. added power status tile to display, currently was here but not displayed.
   7. added configure and refresh tiles.
   8. also added refresh capability so refresh can be forced from other smartapps like pollster. (refresh not currently working all the time for some reason)
   9. changed the sensitivity to have more values than min max etc, now is integer 0 - 127. 
   10. fix uv setting which in one release was value of 2 now it is 16.
   11. added icons for temp and humidity
   12. also change the default wakeup time to be the same as the report interval, 
        otherwise when on battery it disregarded the report interval. (only if less than the default 5 minutes).
   13. added a config option for the min change needed to report temp changes and set it in parameter 41.
   14. incresed range and colors for lux values, as when mine is in direct sun outside it goes as high as 1900
   15. support for celsius added. set in input options.
*/

metadata {
    definition (name: "AeotecMultiSensor6", namespace: "cSteele", author: "cSteele") {
        capability "Motion Sensor"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Illuminance Measurement"
        capability "Ultraviolet Index"
        capability "Configuration"
        capability "Sensor"
        capability "Battery"
        capability "Power Source"
        capability "Acceleration Sensor"
        capability "Contact Sensor"

        command    "refresh"

        attribute  "firmware", "decimal"
        attribute  "DriverVersion", "string"
        attribute  "DriverAuthor", "string"
        attribute  "DriverStatus", "string"
        attribute  "DriverUpdate", "string"

        fingerprint deviceId: "0x2101", inClusters: "0x5E,0x86,0x72,0x59,0x85,0x73,0x71,0x84,0x80,0x30,0x31,0x70,0x7A", outClusters: "0x5A"
    }

    preferences {

        // Note: Hubitat doesn't appear to honour sections in device handler preferences just now, but hopefully one day...
        section("Motion sensor settings") {
            input "motionDelayTime", "enum", title: "Motion Sensor Delay Time?",
                    options: ["20 seconds", "30 seconds", "1 minute", "2 minutes", "3 minutes", "4 minutes"], defaultValue: "1 minute", displayDuringSetup: true
            input "motionSensitivity", "number", title: "Motion Sensor Sensitivity? 0(min)..5(max)", range: "0..5", defaultValue: 5, displayDuringSetup: true
        }

        section("Automatic report settings") {
            input "reportInterval", "enum", title: "Sensors Report Interval?",
                    options: ["20 seconds", "30 seconds", "1 minute", "2 minutes", "3 minutes", "4 minutes", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "6 hours", "12 hours", "18 hours", "24 hours"], defaultValue: "5 minutes", displayDuringSetup: true
            input "tempChangeAmount", "number", title: "Temperature Change Amount (Tenths of a degree)?", range: "1..70", description: "The tenths of degrees the temperature must change to induce an automatic report.", defaultValue: 2, required: false
            input "humidChangeAmount", "number", title: "Humidity Change Amount (%)?", range: "1..100", description: "The percentage the humidity must change to induce an automatic report.", defaultValue: 10, required: false
            input "luxChangeAmount", "number", title: "Luminance Change Amount (LUX)?", range: "-1000..1000", description: "The amount of LUX the luminance must change to induce an automatic report.", defaultValue: 100, required: false
        }

        section("Calibration settings") {
            input "tempOffset", "number", title: "Temperature Offset -128 to +127 (Tenths of a degree)?", range: "-127..128", description: "If your temperature is inaccurate this will offset/adjust it by this many tenths of a degree.", defaultValue: 0, required: false, displayDuringSetup: true
            input "humidOffset", "number", title: "Humidity Offset/Adjustment -50 to +50 in percent?", range: "-10..10", description: "If your humidity is inaccurate this will offset/adjust it by this percent.", defaultValue: 0, required: false, displayDuringSetup: true
            input "luxOffset", "number", title: "Luminance Offset/Adjustment -10 to +10 in LUX?", range: "-10..10", description: "If your luminance is inaccurate this will offset/adjust it by this percent.", defaultValue: 0, required: false, displayDuringSetup: true
        }
        input name: "selectiveReporting", type: "bool", title: "Enable Selective Reporting?", defaultValue: false
        input name: "debugOutput", type: "bool", title: "Enable debug logging?", defaultValue: true
    }
}

// App Version   *********************************************************************************
def setVersion(){
    state.Version = "1.4"
    state.InternalName = "AeotecMultiSensor6"
    
    sendEvent(name: "DriverAuthor", value: "cSteele", isStateChange: true)
    sendEvent(name: "DriverVersion", value: state.version, isStateChange: true)
    sendEvent(name: "DriverStatus", value: state.Status, isStateChange: true)
}

def updated() {
    logDebug "In Updated with settings: ${settings}"
    logDebug "${device.displayName} is now on ${device.latestValue("powerSource")} power"
    if (debugOutput) runIn(1800,logsOff)
    version()

    // Check for any null settings and change them to default values
    if (motionDelayTime == null)
        motionDelayTime = "1 minute"
    if (motionSensitivity == null)
        motionSensitivity = 3
    if (reportInterval == null)
        reportInterval = "5 minutes"
    if (tempChangeAmount == null)
        tempChangeAmount = 2
    if (humidChangeAmount == null)
        humidChangeAmount = 10
    if (luxChangeAmount == null)
        luxChangeAmount = 100
    if (tempOffset == null)
        tempOffset = 0
    if (humidOffset == null)
        humidOffset = 0
    if (luxOffset == null)
        luxOffset = 0
    if (selectiveReporting) {
        selectiveReport = 1 
       }else { 
        selectiveReport = 0  }  

    if (motionSensitivity < 0)
    {
        logDebug "Illegal motion sensitivity ... resetting to 0!"
        motionSensitivity = 0
    }

    if (motionSensitivity > 5)
    {
        logDebug "Illegal motion sensitivity ... resetting to 5!"
        motionSensitivity = 5
    }

    // fix temp offset
    if (tempOffset < -128)
    {
        tempOffset = -128
        logDebug "Temperature Offset too low... resetting to -128 (-12.8 degrees)0"
    }

    if (tempOffset > 127)
    {
        tempOffset = 127
        logDebug "Temperature Offset too high ... resetting to 127 (+12.7 degrees)"
    }

    // fix humidity offset
    if (humidOffset < -50)
    {
        humidOffset = -50
        logDebug "Humidity Offset too low... resetting to -50%"
    }

    if (humidOffset > 50)
    {
        humidOffset = 50
        logDebug "Humidity Adjusment too high ... resetting to +50%"
    }

    if (luxOffset < -1000)
    {
        luxOffset = -1000
        logDebug "Luminance Offset too low ... resetting to -1000LUX"
    }

    if (luxOffset > 1000)
    {
        luxOffset = 1000
        logDebug "Luminance Offset too high ... resetting to +1000LUX"
    }


    if (device.latestValue("powerSource") == "dc") {  //case1: USB powered
        response(configure())
    } else if (device.latestValue("powerSource") == "battery") {  //case2: battery powered
        // setConfigured("false") is used by WakeUpNotification
        setConfigured("false") //wait until the next time device wakeup to send configure command after user change preference
        selectiveReport = 0 // battery, selective reporting is not supported
    } else { //case3: power source is not identified, ask user to properly pair the sensor again
        log.warn "power source is not identified, check it sensor is powered by USB, if so > configure()"
        def request = []
        request << zwave.configurationV1.configurationGet(parameterNumber: 101)
        response(commands(request))
    }
    return(configure())
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def parse(String description) {
    // log.debug "In parse() for description: $description"
    def result = null
    if (description.startsWith("Err 106")) {
        log.debug "parse() >> Err 106"
        result = createEvent( name: "secureInclusion", value: "failed", isStateChange: true,
                descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via Hubitat, you must remove it from your network and add it again.")
    } else if (description != "updated") {
        // log.debug "About to zwave.parse($description)"
        def cmd = zwave.parse(description, [0x31: 5, 0x30: 1, 0x70: 1, 0x72: 1, 0x84: 1])
        if (cmd) {
            // log.debug "About to call handler for ${cmd.toString()}"
            result = zwaveEvent(cmd)
        }
    }
    //log.debug "After zwaveEvent(cmd) >> Parsed '${description}' to ${result.inspect()}"
    return result
}

//this notification will be sent only when device is battery powered
def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
    def cmds = []
    if (!isConfigured()) {
        logDebug("late configure")
        result << response(configure())
    } else {
        //logDebug("Device has been configured sending >> wakeUpNoMoreInformation()")
        cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
        result << response(cmds)
    }
    result
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 5, 0x30: 1, 0x70: 1, 0x72: 1, 0x84: 1])
    state.sec = 1
    logDebug "encapsulated: ${encapsulatedCommand}"
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract encapsulated cmd from $cmd"
        createEvent(descriptionText: cmd.toString())
    }
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
    log.info "Executing zwaveEvent 98 (SecurityV1): 03 (SecurityCommandsSupportedReport) with cmd: $cmd"
    state.sec = 1
}

def zwaveEvent(hubitat.zwave.commands.securityv1.NetworkKeyVerify cmd) {
    state.sec = 1
    log.info "Executing zwaveEvent 98 (SecurityV1): 07 (NetworkKeyVerify) with cmd: $cmd (node is securely included)"
    def result = [createEvent(name:"secureInclusion", value:"success", descriptionText:"Secure inclusion was successful", isStateChange: true)]
    result
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
    logDebug "ManufacturerSpecificReport cmd = $cmd"

    logDebug "manufacturerId:   ${cmd.manufacturerId}"
    logDebug "manufacturerName: ${cmd.manufacturerName}"
    logDebug "productId:        ${cmd.productId}"
    def model = ""   // We'll decode the specific model for the log, but we don't currently use this info
    switch(cmd.productTypeId >> 8) {
        case 0: model = "EU"
                break
        case 1: model = "US"
                break
        case 2: model = "AU"
                break
        case 10: model = "JP"
                break
        case 29: model = "CN"
                break
        default: model = "unknown"
    }
    logDebug "model:            ${model}"
    logDebug "productTypeId:    ${cmd.productTypeId}"
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    updateDataValue("MSR", msr)
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    logDebug "In BatteryReport"
    def result = []
    def map = [ name: "battery", unit: "%" ]
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} battery is low"
        map.isStateChange = true
    } else {
        map.value = cmd.batteryLevel
    }

    createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd){
    logDebug "In multi level report cmd = $cmd"

    def map = [:]
    switch (cmd.sensorType) {
        case 1:
            logDebug "raw temp = $cmd.scaledSensorValue"

            def now = new Date()
            def tf = new java.text.SimpleDateFormat("dd-MMM-yyyy h:mm a")
            tf.setTimeZone(location.getTimeZone())
            def newtime = "${tf.format(now)}" as String
            sendEvent(name: "lastUpdate", value: newtime, descriptionText: "Last Update: $newtime ${tf.getTimeZone()}")

            logDebug "scaled sensor value = $cmd.scaledSensorValue  scale = $cmd.scale  precision = $cmd.precision"

            // Convert temperature (if needed) to the system's configured temperature scale
            def finalval = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)

            logDebug "finalval = $finalval"

            map.value = finalval
            map.unit = getTemperatureScale()
            map.name = "temperature"
            break
        case 3:
            logDebug "raw illuminance = $cmd.scaledSensorValue"
            map.name = "illuminance"
            map.value = cmd.scaledSensorValue.toInteger()
            map.unit = "lux"
            break
        case 5:
            logDebug "raw humidity = $cmd.scaledSensorValue"
            map.value = cmd.scaledSensorValue.toInteger()
            map.unit = "%"
            map.name = "humidity"
            break
        case 27:
            logDebug "raw uv index = $cmd.scaledSensorValue"
            map.name = "ultravioletIndex"
            map.value = cmd.scaledSensorValue.toInteger()
            break
        default:
            map.descriptionText = cmd.toString()
    }
    createEvent(map)
}

def motionEvent(value) {
    def map = [name: "motion"]
    if (value) {
        logDebug "motion active"
        map.value = "active"
        map.descriptionText = "$device.displayName detected motion"
    } else {
        logDebug "motion inactive"
        map.value = "inactive"
        map.descriptionText = "$device.displayName motion has stopped"
    }
    createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
    motionEvent(cmd.sensorValue)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    // Sensor sends value 0xFF on motion, 0x00 on no motion (after expiry interval)
    motionEvent(cmd.value)
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
    def result = []
    if (cmd.notificationType == 7) {
        switch (cmd.event) {
            case 0:
                sendEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed", displayed: true)
                result << motionEvent(0)
                //result << createEvent(name: "tamper", value: "clear", displayed: false)
                result << createEvent(name: "acceleration", value: "inactive", descriptionText: "$device.displayName is inactive", displayed: true)
                break
            case 3:
                sendEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open", displayed: true)
                //result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName was tampered")
                result << createEvent(name: "acceleration", value: "active", descriptionText: "$device.displayName is active", displayed: true)
                break
            case 8:
                result << motionEvent(1)
                break
        }
    } else {
        log.warn "Need to handle this cmd.notificationType: ${cmd.notificationType}"
        result << createEvent(descriptionText: cmd.toString(), isStateChange: false)
    }
    result
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    logDebug "---CONFIGURATION REPORT V1--- ${device.displayName} parameter ${cmd.parameterNumber} with a byte size of ${cmd.size} is set to ${cmd.configurationValue}"

    def result = []
    def value
    if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 0) {
        value = "dc"
        if (!isConfigured()) {
            logDebug("ConfigurationReport: configuring device")
            result << response(configure())
        }
        result << createEvent(name: "powerSource", value: value, displayed: false)
    }
    else if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 1) {
        value = "battery"
        result << createEvent(name: "powerSource", value: value, displayed: false)
    } 
    else if (cmd.parameterNumber == 101){
        result << response(configure())
    }
    result
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    logDebug "General zwaveEvent cmd: ${cmd}"
    createEvent(descriptionText: cmd.toString(), isStateChange: false)
}

def configure() {
    // This sensor joins as a secure device if you double-click the button to include it
    log.debug "${device.displayName} is configuring its settings"

    if (motionDelayTime == null)
        motionDelayTime = "1 minute"
    if (reportInterval == null)
        reportInterval = "5 minutes"
    if (motionSensitivity == null)
        motionSensitivity = 3
    if (tempOffset == null)
        tempOffset = 0
    if (humidOffset == null)
        humidOffset = 0
    if (tempChangeAmount == null)
        tempChangeAmount = 2
    if (humidChangeAmount == null)
        humidChangeAmount = 10
    if (luxChangeAmount == null)
        luxChangeAmount = 100


    if (motionSensitivity < 0) {
        logDebug "Motion sensitivity too low ... resetting to 0"
        motionSensitivity = 0
    } else if (motionSensitivity > 5) {
        logDebug "Motion sensitivity too high ... resetting to 5"
        motionSensitivity = 5
    }

    // fix temp offset
    if (tempOffset < -10) {
        tempOffset = -10
        logDebug "Temperature calibration too low... resetting to -10"
    } else if (tempOffset > 10) {
        tempOffset = 10
        logDebug "Temperature calibration too high ... resetting to 10"
    }

    // fix humidity offset
    if (humidOffset < -50) {
        humidOffset = -50
        logDebug "Humidity calibration too low... resetting to -50"
    } else if (humidOffset > 50) {
        humidOffset = 50
        logDebug "Humidity calibration too high ... resetting to 50"
    }

    if (luxOffset < -1000) {
        luxOffset = -1000
        logDebug "Luminance calibration too low ... resetting to -1000"
    } else if (luxOffset > 1000) {
        luxOffset = 1000
        logDebug "Luminance calibration too high ... resetting to 1000"
    }

    logDebug "In configure: Report Interval = $settings.reportInterval"
    logDebug "Motion Delay Time = $settings.motionDelayTime"
    logDebug "Motion Sensitivity = $settings.motionSensitivity"
    logDebug "Temperature adjust = $settings.TempOffset"
    logDebug "Humidity adjust = $settings.HumidOffset"
    logDebug "Temp Scale = $settings.tempScale"
    logDebug "Min Temp change for reporting = $settings.TempChangeAmount"

    def now = new Date()
    def tf = new java.text.SimpleDateFormat("dd-MMM-yyyy h:mm a")
    tf.setTimeZone(location.getTimeZone())
    def newtime = "${tf.format(now)}" as String
    sendEvent(name: "lastUpdate", value: newtime, descriptionText: "Configured: $newtime")

    setConfigured("true")
    def waketime

    if (timeOptionValueMap[settings.reportInterval] < 300)
        waketime = timeOptionValueMap[settings.reportInterval]
    else waketime = 300

    logDebug "wake time reset to $waketime"

    logDebug "Current firmware: $device.currentFirmware"

    // Retrieve local temperature scale: "C" = Celsius, "F" = Fahrenheit
    // Convert to a value of 1 or 2 as used by the device to select the scale
    logDebug "Location temperature scale: ${location.getTemperatureScale()}"
    byte tempScaleByte = (location.getTemperatureScale() == "C" ? 1 : 2)

    def request = [
            // set wakeup interval to report time otherwise it doesnt report in time
            zwave.wakeUpV1.wakeUpIntervalSet(seconds:waketime, nodeid:zwaveHubNodeId),

            zwave.versionV1.versionGet(),
            zwave.manufacturerSpecificV1.manufacturerSpecificGet(),

            // Hubitat have not yet implemented the firmwareUpdateMdV2 class
            //zwave.firmwareUpdateMdV2.firmwareMdGet(),

            //1. set association groups for hub
            zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId),
            zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId),

            //2. automatic report flags
            // params 101-103 [4 bytes] 128: light sensor, 64 humidity, 32 temperature sensor, 16 ultraviolet sensor, 1 battery sensor -> send command 241 to get all reports
            zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 241),   //association group 1 - all reports
            zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 1),     //association group 2 - battery report

            //3. no-motion report x seconds after motion stops (default 60 secs)
            zwave.configurationV1.configurationSet(parameterNumber: 3, size: 2, scaledConfigurationValue: timeOptionValueMap[motionDelayTime] ?: 60),

            //4. motion sensitivity: 0 (least sensitive) - 5 (most sensitive)
            zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, scaledConfigurationValue: motionSensitivity),

            //5. report every x minutes (threshold reports don't work on battery power, default 8 mins)
            zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: timeOptionValueMap[reportInterval]), //association group 1

            // battery report time.. too long at  every 6 hours change to 2 hours.
            zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 2*60*60),  //association group 2

            //6. enable/disable selective reporting only on thresholds
            zwave.configurationV1.configurationSet(parameterNumber: 40, size: 1, scaledConfigurationValue: selectiveReport),

            // Set the temperature scale for automatic reports
            // US units default to reporting in Fahrenheit, whilst all others default to reporting in Celsius, but we can configure the preferred scale with this setting
            zwave.configurationV1.configurationSet(parameterNumber: 64, size: 1, configurationValue: [tempScaleByte]),

            // Automatically generate a report when temp changes by specified amount
            zwave.configurationV1.configurationSet(parameterNumber: 41, size: 4, configurationValue: [0, tempChangeAmount, tempScaleByte, 0]),

            // Automatically generate a report when humidity changes by specified amount
            zwave.configurationV1.configurationSet(parameterNumber: 42, size: 1, scaledConfigurationValue: humidChangeAmount),

            // Automatically generate a report when lux changes by specified amount
            zwave.configurationV1.configurationSet(parameterNumber: 43, size: 2, scaledConfigurationValue: luxChangeAmount),

            // send binary sensor report for motion
            zwave.configurationV1.configurationSet(parameterNumber: 0x05, size: 1, scaledConfigurationValue: 2),

            // Set temperature calibration offset
            zwave.configurationV1.configurationSet(parameterNumber: 201, size: 2, configurationValue: [tempOffset, tempScaleByte]),

            // Set humidity calibration offset
            zwave.configurationV1.configurationSet(parameterNumber: 202, size: 1, scaledConfigurationValue: humidOffset),

            // Set luminance calibration offset
            zwave.configurationV1.configurationSet(parameterNumber: 203, size: 2, scaledConfigurationValue: luxOffset),

            //7. query sensor data
            zwave.batteryV1.batteryGet(),
            zwave.sensorBinaryV1.sensorBinaryGet(),
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1), //temperature
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 3), //illuminance
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5), //humidity
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 27) //ultravioletIndex
    ]
    return commands(request) + ["delay 20000", zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
}

def refresh() {
    logDebug "in refresh"

    return commands([
            zwave.versionV1.versionGet(),                                // Retrieve version info (includes firmware version)
//            zwave.firmwareUpdateMdV2.firmwareMdGet(),                  // Command class not implemented by Hubitat yet
            zwave.configurationV1.configurationGet(parameterNumber: 9),  // Retrieve current power mode
            zwave.batteryV1.batteryGet(),
            zwave.sensorBinaryV1.sensorBinaryGet(),                      //motion
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1), //temperature
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 3), //illuminance
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5), //humidity
            zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 27) //ultravioletIndex
    ])
}

private def getTimeOptionValueMap() { [
        "20 seconds" : 20,
        "30 seconds" : 30,
        "1 minute"   : 60,
        "2 minutes"  : 2*60,
        "3 minutes"  : 3*60,
        "4 minutes"  : 4*60,
        "5 minutes"  : 5*60,
        "10 minutes" : 10*60,
        "15 minutes" : 15*60,
        "30 minutes" : 30*60,
        "1 hour"     : 1*60*60,
        "6 hours"    : 6*60*60,
        "12 hours"   : 12*60*60,
        "18 hours"   : 18*60*60,
        "24 hours"   : 24*60*60,
]}

private setConfigured(configure) {
    updateDataValue("configured", configure)
}

private isConfigured() {
    getDataValue("configured") == "true"
}

private command(hubitat.zwave.Command cmd) {
    if (state.sec) {
        logDebug "Sending secure Z-wave command: ${cmd.toString()}"
        return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        logDebug "Sending Z-wave command: ${cmd.toString()}"
        return cmd.format()
    }
}

private commands(commands, delay=1000) {
    //log.info "sending commands: ${commands}"
    return delayBetween(commands.collect{ command(it) }, delay)
}


def zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    logDebug "in version command class report"
    //if (state.debug)
    logDebug "---VERSION COMMAND CLASS REPORT V1--- ${device.displayName} has version: ${cmd.commandClassVersion} for command class ${cmd.requestedCommandClass} - payload: ${cmd.payload}"
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
    logDebug "in version report"
    // SubVersion is in 1/100ths so that 1.01 < 1.08 < 1.10, etc.
    BigDecimal fw = cmd.applicationVersion + (cmd.applicationSubVersion / 100)
    state.firmware = fw
    logDebug "---VERSION REPORT V1--- ${device.displayName} is running firmware version: ${String.format("%.2f",fw)}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
    if(fw < 1.10)
        log.warn "--- WARNING: Device handler expects devices to have firmware 1.10 or later"
}

def zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
    // NOTE: This command class is not yet implemented by Hubitat...
    logDebug "---FIRMWARE METADATA REPORT V2 ${device.displayName}   manufacturerId: ${cmd.manufacturerId}   firmwareId: ${cmd.firmwareId}"
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) {
		log.debug "$msg"
	}
}

// Check Version   ***** with great thanks and acknowlegment to Cobra (CobraVmax) for his original code **************
def version(){
    updatecheck()
    schedule("0 0 8 ? * FRI *", updatecheck)
}

def updatecheck(){
    setVersion()
     def paramsUD = [uri: "https://hubitatcommunity.github.io/AeotecMultiSensor6/versions.json"]
      try {
            httpGet(paramsUD) { respUD ->
                  //  log.warn " Version Checking - Response Data: ${respUD.data}"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver
                  def copyrightRead = (respUD.data.copyright)
                  state.Copyright = copyrightRead
                  def newVerRaw = (respUD.data.versions.Driver.(state.InternalName))
                  def newVer = (respUD.data.versions.Driver.(state.InternalName).replace(".", ""))
                  def currentVer = state.Version.replace(".", "")
                  state.UpdateInfo = (respUD.data.versions.UpdateInfo.Driver.(state.InternalName))
                  state.author = (respUD.data.author)
                  if(newVer == "NLS"){
                       state.Status = "<b>** This driver is no longer supported by $state.author  **</b>"       
                       log.warn "** This driver is no longer supported by $state.author **"      
                  }           
                  else if(currentVer < newVer){
                       state.Status = "<b>New Version Available (Version: $newVerRaw)</b>"
                       log.warn "** There is a newer version of this driver available  (Version: $newVerRaw) **"
                       log.warn "** $state.UpdateInfo **"
                 } 
                 else{ 
                     state.Status = "Current"
                     log.info "You are using the current version of this driver"
                 }
            } // httpGet
      } // try

      catch (e) {
           log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI -  $e"
      }

      if(state.Status == "Current"){
           state.UpdateInfo = "N/A"
           sendEvent(name: "DriverUpdate", value: state.UpdateInfo, isStateChange: true)
           sendEvent(name: "DriverStatus", value: state.Status, isStateChange: true)
      }
      else {
           sendEvent(name: "DriverUpdate", value: state.UpdateInfo, isStateChange: true)
           sendEvent(name: "DriverStatus", value: state.Status, isStateChange: true)
      }

      sendEvent(name: "DriverAuthor", value: state.author, isStateChange: true)
      sendEvent(name: "DriverVersion", value: state.Version, isStateChange: true)
}
