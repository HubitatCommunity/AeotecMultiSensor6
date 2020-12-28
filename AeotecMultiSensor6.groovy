/*
 * IMPORT URL: https://raw.githubusercontent.com/HubitatCommunity/AeotecMultiSensor6/master/AeotecMultisensor6.groovy
 *
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
 *         v2.0.1   Added in optional Param Set / Get commands.
 *                    Added Fingerprint for this device type. 
 *                    Added Limit Check on Temp, Illuminance, Humitity and UV values for wildly out of range cases. 
 *         v2.0.0   Converted to Hubitat Security method [zwaveSecureEncap()] for S2+S0 This device still only offers unsecure or S0.
 *
 *         v1.7.1   removed Preference hiding (settingEnable)
 *                    LED Options reflect firmware v1.3 specs. (On or Off only)
 *                    Mostion Sensitivity changed to Enum
 *         v1.7     refactored using inputValidationCheck
 *                    added de-duplication code for Motion Events (motion Events can occur in <1ms)
 *                    |  Motion reports (unsolicited) are duplicated: a NotificationReport (7) plus either: 
 *                    |    (1) for basicSet; (2) for sensorBinary set via Param 5. 
 *                    |  Motion Detection via NotificationReport can be considered a duplicate. 
 *                    |  This version removes motion event processing from NotificationReport relying
 *                    |    on basicSet or SensorBinary.
 *                    |  Added detail to Motion is... to identify the path.
 *                    removed duplicate call to config()
 *                    lots of cosmetic formatting cleanup, cosmetic reordering of modules 
 *
 *         v1.6.13  version report NPE (thanks Christi999)
 *		    	    tempOffset to "as Int"
 *         v1.6.12  corrected NPE from malformed Packet. (thanks Mike Maxwell)
 *         v1.6.11  corrected ledOption scaling. (thanks LJP-hubitat)
 *    BAB  v1.6.10a Changed to HALF_ROUND_UP, standardized DescriptionText
 *         v1.6.10  Swapped to latest updateCheck() code.
 *         v1.6.9   Added Initialize to preset ledOptions to prevent an NPE when sending the option value to the device.
 *                  revised updateCheck to use version2.json's format.
 *         v1.6.8   revised Contact Sensor per @Wounded suggestion, restoring tamperAlert
 *         v1.6.7   replaced updateCheck with asynchttp version -- removed setVersion, etc.
 *                  added descTextEnable as option to reduce log.info 
 *         v1.6.6   corrected limitation on Humidity Offset
 *         v1.6.5   alternate description for settingEnabled input
 *         v1.6.4   corrected temp offset -128 to 127 in configure()
 *         v1.6.3   added Preference hiding (settingEnable)
 *         v1.6.2   removed mapping to ledOption(s)
 *         v1.6.1   added degree symbol to temp scale
 *         v1.6     deleted isStateChange throughout
 *         v1.5     Added LED Options
 *         v1.4     Added selectiveReport, enabled humidChangeAmount, luxChangeAmount
 *         v1.3     Restored if (debugOutput) log.debug as default logging is too much. cSteele
 *         v1.2     Merged Chuckles updates
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

 *         v1.1d    Added remote version checking ** Cobra (CobraVmax) for his original version check code
 * csteele v1.1c converted to Hubitat.
 *
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

 public static String version()      {  return "v2.0.1"  }
import groovy.transform.Field

metadata {
    definition (name: "AeotecMultiSensor6", namespace: "cSteele", author: "cSteele", importUrl: "https://raw.githubusercontent.com/HubitatCommunity/AeotecMultiSensor6/master/AeotecMultisensor6.groovy") {
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
        capability "TamperAlert"

        command    "refresh"
	  // command    "updateCheck"			// **---** delete these for Release
        command "getParameterReport", [[name:"parameterNumber",type:"NUMBER", description:"Parameter Number (omit for a complete listing of parameters that have been set)", constraints:["NUMBER"]]]
        command "setParameter",[[name:"parameterNumber",type:"NUMBER", description:"Parameter Number", constraints:["NUMBER"]],[name:"size",type:"NUMBER", description:"Parameter Size", constraints:["NUMBER"]],[name:"value",type:"NUMBER", description:"Parameter Value", constraints:["NUMBER"]]]

        attribute  "firmware", "decimal"

        fingerprint deviceId: "0x2101", inClusters: "0x5E,0x86,0x72,0x59,0x85,0x73,0x71,0x84,0x80,0x30,0x31,0x70,0x7A", outClusters: "0x5A"
        fingerprint  mfr:"0086", prod:"0102", deviceId:"0064", inClusters:"0x5E,0x86,0x72,0x59,0x85,0x73,0x71,0x84,0x80,0x30,0x31,0x70,0x7A,0x5A" 
    }
    

    preferences {
        // Note: Hubitat doesn't appear to honour 'sections' in device handler preferences just now, but hopefully one day...
        section("Motion sensor settings") {
            input "motionDelayTime", "enum", title: "<b>Motion Sensor Delay Time?</b>",
                                      options: ["20 seconds", "30 seconds", "1 minute", "2 minutes", "3 minutes", "4 minutes"], defaultValue: "1 minute", displayDuringSetup: true
            input "motionSensitivity", "enum", title: "<b>Motion Sensor Sensitivity?</b>", options: [5:"Very High", 4:"High", 3:"Medium High", 2:"Medium", 1:"Low"], defaultValue: 5, displayDuringSetup: true
        }

        section("Automatic report settings") {
            input "reportInterval", "enum", title: "<b>Sensors Report Interval?</b>",
                                      options: ["20 seconds", "30 seconds", "1 minute", "2 minutes", "3 minutes", "4 minutes", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "6 hours", "12 hours", "18 hours", "24 hours"], defaultValue: "5 minutes", displayDuringSetup: true
            input "tempChangeAmount", "number", title: "<b>Temperature Change Amount (Tenths of a degree)?</b>", range: "1..70", description: "<br><i>The tenths of degrees the temperature must change to induce an automatic report.</i><br>", defaultValue: 2, required: false
            input "humidChangeAmount", "number", title: "<b>Humidity Change Amount (%)?</b>", range: "1..100", description: "<br><i>The percentage the humidity must change to induce an automatic report.</i><br>", defaultValue: 10, required: false
            input "luxChangeAmount", "number", title: "<b>Luminance Change Amount (LUX)?</b>", range: "-1000..1000", description: "<br><i>The amount of LUX the luminance must change to induce an automatic report.</i><br>", defaultValue: 100, required: false
        }

        section("Calibration settings") {
            input "tempOffset", "number", title: "<b>Temperature Offset?</b>", range: "-127..128", description: "<br><i> -128 to +127 (Tenths of a degree)<br>If your temperature is inaccurate this will offset/adjust it by this many tenths of a degree.</i><br>", defaultValue: 0, required: false, displayDuringSetup: true
            input "humidOffset", "number", title: "<b>Humidity Offset/Adjustment -50 to +50 in percent?</b>", range: "-50..50", description: "<br><i>If your humidity is inaccurate this will offset/adjust it by this percent.</i><br>", defaultValue: 0, required: false, displayDuringSetup: true
            input "luxOffset", "number", title: "<b>Luminance Offset/Adjustment -10 to +10 in LUX?</b>", range: "-10..10", description: "<br><i>If your luminance is inaccurate this will offset/adjust it by this percent.</i><br>", defaultValue: 0, required: false, displayDuringSetup: true
        }

        input "ledOptions", "enum", title: "<b>LED Options</b>",
                                      options: [0:"Fully Enabled", 1:"Fully Disabled", 2:"Disable When Motion (Aeon v1.10 only)"], defaultValue: "0", displayDuringSetup: true
        input name: "selectiveReporting", type: "bool", title: "<b>Enable Selective Reporting?</b>", defaultValue: false

        input name: "debugOutput",   type: "bool", title: "<b>Enable debug logging?</b>",   description: "<br>", defaultValue: true
        input name: "descTextEnable", type: "bool", title: "<b>Enable descriptionText logging?</b>", defaultValue: true
    }
}


/*
	updated
    
	When "Save Preferences" gets clicked...
*/
def updated() {
	if (debugOutput) log.debug "In Updated with settings: ${settings}"
	if (debugOutput) log.debug "${device.displayName} is now on ${device.latestValue("powerSource")} power"
	unschedule()
	initialize()
	dbCleanUp()		// remove antique db entries created in older versions and no longer used.
	schedule("0 0 8 ? * FRI *", updateCheck)
	if (debugOutput) runIn(1800,logsOff)
	runIn(20, updateCheck) 
	
	// Check for any null settings and change them to default values
	inputValidationCheck()
	
	if (device.latestValue("powerSource") == "dc") {  //case1: USB powered
	  //  response(configure(2))
		} else if (device.latestValue("powerSource") == "battery") {  //case2: battery powered
		    // setConfigured("false") is used by WakeUpNotification
		    setConfigured("false") //wait until the next time device wakeup to send configure command after user change preference
		    selectiveReport = 0 // battery, selective reporting is not supported
		} else { //case3: power source is not identified, ask user to properly pair the sensor again
		    log.warn "power source is not identified, check that sensor is powered by USB, if so > configure()"
		    def request = []
		    request << zwave.configurationV1.configurationGet(parameterNumber: 9)  // Retrieve current power mode
		    response(commands(request))
	}
	return(configure(1))
}


/*
	parse
    
	Respond to received zwave commands.
*/
def parse(String description) {
	// log.debug "In parse() for description: $description"
	def result = null
	if (description.startsWith("Err 106")) {
	    log.warn "parse() >> Err 106"
	    result = createEvent( name: "secureInclusion", value: "failed",
	                         descriptionText: "This sensor (${device.displayName}) failed to complete the network security key exchange. If you are unable to control it via Hubitat, you must remove it from your network and add it again.")
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


/*
	installed
    
	Doesn't do much other than call initialize().
*/
void installed()
{
	initialize()
}


/*
	initialize
    
	Doesn't do much.
*/
def initialize() {
	if (settings.ledOptions == null) settings.ledOptions = 0 // default to Full
	state.firmware = state.firmware ?: 0.0d
}


/*
	Beginning of Z-Wave Commands
*/

//this notification will be sent only when device is battery powered
def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	def result = [createEvent(descriptionText: "${device.displayName} woke up")]
	def cmds = []
	if (!isConfigured()) {
	    if (debugOutput) log.debug("late configure")
	    result << response(configure(3))
	} else {
	    //if (debugOutput) log.debug("Device has been configured sending >> wakeUpNoMoreInformation()")
	    cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	    result << response(cmds)
	}
	result
}


def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 5, 0x30: 1, 0x70: 1, 0x72: 1, 0x84: 1])
	state.sec = 1
	if (debugOutput) log.debug "encapsulated: ${encapsulatedCommand}"
	if (encapsulatedCommand) {
	    zwaveEvent(encapsulatedCommand)
	} else {
	    log.warn "Unable to extract encapsulated cmd from $cmd"
	    createEvent(descriptionText: cmd.toString())
	}
}


def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
	if (debugOutput) log.debug "ManufacturerSpecificReport cmd = $cmd"
	if (debugOutput) log.debug "manufacturerId:   ${cmd.manufacturerId}"
	if (debugOutput) log.debug "manufacturerName: ${cmd.manufacturerName}"
	if (debugOutput) log.debug "productId:        ${cmd.productId}"
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
	if (debugOutput) log.debug "model:            ${model}"
	if (debugOutput) log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
}


def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	if (debugOutput) log.debug "In BatteryReport $cmd"
	def result = []
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
	    map.value = 1
	    map.descriptionText = "${device.displayName} battery is low"
	} else {
	    map.value = cmd.batteryLevel
	    map.descriptionText = "${device.displayName} battery is ${map.value}%"
	}
	createEvent(map)
}

@Field static Map<String, Map> LIMIT_VALUES = [ tempC: [upper: 100, lower: -40], tempF: [upper: 212, lower: -40], lux: [upper: 30000, lower: 0], rh: [upper: 100, lower: 0], uv: [upper: 11, lower: 1]]

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd){
	if (debugOutput) log.debug "In multi level report cmd = $cmd"

	if (cmd.scaledSensorValue == null) return
	def map = [:]
	switch (cmd.sensorType) {
	    case 1:
	        if (debugOutput) log.debug "raw temp = $cmd.scaledSensorValue"
	        if ((cmd.scale == 1 && (cmd.scaledSensorValue >= LIMIT_VALUES.tempF.upper || cmd.scaledSensorValue < LIMIT_VALUES.tempF.lower)) || (cmd.scale == 0 && (cmd.scaledSensorValue >= LIMIT_VALUES.tempC.upper || cmd.scaledSensorValue < LIMIT_VALUES.tempC.lower))) return
	        // Convert temperature (if needed) to the system's configured temperature scale
	        map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
	        if (debugOutput) log.debug "finalval = $map.value"

	        map.unit = "\u00b0" + getTemperatureScale()
	        map.name = "temperature"
	        map.descriptionText = "${device.displayName} temperature is ${map.value}${map.unit}"
	        if (descTextEnable) log.info "Temperature is ${map.value}${map.unit}"
	        break
	    case 3:
	        if (debugOutput) log.debug "raw illuminance = $cmd.scaledSensorValue"
	        if (cmd.scaledSensorValue >= LIMIT_VALUES.lux.upper || cmd.scaledSensorValue < LIMIT_VALUES.lux.lower) return
	        map.name = "illuminance"
	        map.value = cmd.scaledSensorValue.toInteger() // roundIt((cmd.scaledSensorValue / 2.0),0) as Integer // .toInteger()
	        map.unit = "lux"
	        map.descriptionText = "${device.displayName} illuminance is ${map.value} Lux"
	        if (descTextEnable) log.info "Illuminance is ${map.value} Lux"
	        break
	    case 5:
	        if (debugOutput) log.debug "raw humidity = $cmd.scaledSensorValue"
	        if (cmd.scaledSensorValue >= LIMIT_VALUES.rh.upper || cmd.scaledSensorValue < LIMIT_VALUES.rh.lower) return
	        map.value = roundIt(cmd.scaledSensorValue, 0) as Integer     // .toInteger()
	        map.unit = "%"
	        map.name = "humidity"
	        map.descriptionText = "${device.displayName} humidity is ${map.value}%"
	        if (descTextEnable) log.info "Humidity is ${map.value}%"
	        break
	    case 27:
	        if (debugOutput) log.debug "raw uv index = $cmd.scaledSensorValue"
	        if (cmd.scaledSensorValue >= LIMIT_VALUES.uv.upper || cmd.scaledSensorValue < LIMIT_VALUES.uv.lower) return
	        map.name = "ultravioletIndex"
	        map.value = roundIt(cmd.scaledSensorValue, 0) as Integer    // .toInteger()
	        map.descriptionText = "${device.displayName} ultraviolet index is ${map.value}"
	        if (descTextEnable) log.info "Ultraviolet index is ${map.value}"
	        break
	    default:
	        map.descriptionText = cmd.toString()
	}
	createEvent(map)
}


def zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	motionEvent(cmd.sensorValue, "SensorBinaryReport")
}


def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	// Sensor sends value 0xFF on motion, 0x00 on no motion (after expiry interval)
	motionEvent(cmd.value, "BasicSet")
}


def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
	if (debugOutput) log.debug "NotificationReport: ${cmd}"
	def result = []
	if (cmd.notificationType == 7) {
	//  spec says type 7 is 'Home Security'
	    switch (cmd.event) {
	        case 0:
	        //  spec says this is 'clear previous alert'
	            //sendEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed", displayed: true)
	     //       result << motionEvent(0, "NotificationReport")  // see Change Note for v1.7
	            result << createEvent(name: "tamper", value: "clear", descriptionText: "${device.displayName} tamper cleared", displayed: true)
	            if (descTextEnable) log.info "Tamper cleared by NotificationReport"
	            result << createEvent(name: "acceleration", value: "inactive", descriptionText: "${device.displayName} acceleration is inactive", displayed: true)
	            if (descTextEnable) log.info "Acceleration is inactive by NotificationReport"
	            break
	        case 3:
	        //  spec says this is 'tamper'
	            //sendEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open", displayed: true)
	            result << createEvent(name: "tamper", value: "detected", descriptionText: "${device.displayName} tamper detected", displayed: true)
	            if (descTextEnable) log.info "Tamper detected"
	            result << createEvent(name: "acceleration", value: "active", descriptionText: "${device.displayName} acceleration is active", displayed: true)
	            if (descTextEnable) log.info "Acceleration is active"
	            break
	        case 8:
	        //  spec says this is 'unknown motion detection'
	      //      result << motionEvent(1, "NotificationReport")  // see Change Note for v1.7
	            break
	    }
	} else {
	    log.warn "Need to handle this cmd.notificationType: ${cmd.notificationType}"
	    result << createEvent(descriptionText: cmd.toString())
	}
	result
}


def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	if (debugOutput) log.debug "---CONFIGURATION REPORT V1--- ${device.displayName} parameter ${cmd.parameterNumber} with a byte size of ${cmd.size} is set to ${cmd.configurationValue}"
	def result = []
	def value
	if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 0) {
	    value = "dc"
	    if (!isConfigured()) {
	        if (debugOutput) log.debug("Configuration Report: configuring device")
	        result << response(configure(4))
	    }
	    result << createEvent(name: "powerSource", value: value, descriptionText: "${device.displayName} power source is dc (mains)", displayed: false)
	    if (descTextEnable) log.info "Power source is DC (mains)"
	}
	else if (cmd.parameterNumber == 9 && cmd.configurationValue[0] == 1) {
	    value = "battery"
	    result << createEvent(name: "powerSource", value: value, descriptionText: "${device.displayName} power source is battery", displayed: false)
	    if (descTextEnable) log.info "Power source is battery"
	} 
	else if (cmd.parameterNumber == 101){
	    result << response(configure(5))
	}
	result
}


def zwaveEvent(hubitat.zwave.Command cmd) {
	if (debugOutput) log.debug "General zwaveEvent cmd: ${cmd}"
	createEvent(descriptionText: cmd.toString(), isStateChange: false)
}


def zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
	if (debugOutput) log.debug "in version command class report"
	if (debugOutput) log.debug "---VERSION COMMAND CLASS REPORT V1--- ${device.displayName} has version: ${cmd.commandClassVersion} for command class ${cmd.requestedCommandClass} - payload: ${cmd.payload}"
}


def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
	if (debugOutput) log.debug "in version report"
	// SubVersion is in 1/100ths so that 1.01 < 1.08 < 1.10, etc.//    state.firmware = 0.0d
	if (cmd.firmware0Version) {
	    BigDecimal fw = cmd.firmware0Version + (cmd.firmware0SubVersion/100)
	    state.firmware = fw
	}
	if (debugOutput) log.debug "---VERSION REPORT V1--- ${device.displayName} is running firmware version: ${String.format("%1.2f",state.firmware)}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
	if(state.firmware < 1.10)
	    log.warn "--- WARNING: Device handler expects devices to have firmware 1.10 or later"
}


def zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
	// NOTE: This command class is not yet implemented by Hubitat...
	if (debugOutput) log.debug "---FIRMWARE METADATA REPORT V2 ${device.displayName}   manufacturerId: ${cmd.manufacturerId}   firmwareId: ${cmd.firmwareId}"
}


private command(hubitat.zwave.Command cmd) {
	    if (debugOutput) log.debug "Sending Z-wave command: ${cmd.toString()}"
	    return zwaveSecureEncap(cmd.format())
}


private commands(commands, delay=1000) {
	//if (descTextEnable) log.info "sending commands: ${commands}"
	return delayBetween(commands.collect{ command(it) }, delay)
}

String secureCmd(cmd) {
	if (getDataValue("zwaveSecurePairingComplete") == "true" && getDataValue("S2") == null) {
		return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		return secure(cmd)
	}	
}

String secure(String cmd) { return zwaveSecureEncap(cmd) }
String secure(hubitat.zwave.Command cmd) { return zwaveSecureEncap(cmd) }

/*
	End of Z-Wave Commands

	Beginning of Driver Commands

*/
def configure(ccc) {
	if (debugOutput) log.debug "ccc: $ccc"
	// This sensor joins as a secure device if you double-click the button to include it
	if (descTextEnable) log.info "${device.displayName} is configuring its settings"

	if (device.currentValue('tamper') == null) {
	    sendEvent(name: 'tamper', value: 'clear', descriptionText: '${device.displayName} tamper cleared')
	    sendEvent(name: 'acceleration', value: 'inactive', descriptionText: "$device.displayName} acceleration is inactive")
	}

	// inputValidationCheck()

	if (debugOutput) {
			log.debug "Report Interval = $reportInterval"
			log.debug "Motion Delay Time = $motionDelayTime"
			log.debug "Motion Sensitivity = $motionSensitivity"
			log.debug "Temperature adjust = $tempOffset (${tempOffset/10}°)"
			log.debug "Humidity adjust = $humidOffset"
			log.debug "Min Temp change for reporting = $tempChangeAmount"
			log.debug "Min Humidity change for reporting = $humidChangeAmount"
			log.debug "Min Lux change for reporting = $luxChangeAmount"
			log.debug "LED Option = $ledOptions"
	}

	def now = new Date()
	def tf = new java.text.SimpleDateFormat("dd-MMM-yyyy h:mm a")
	tf.setTimeZone(location.getTimeZone())
	def newtime = "${tf.format(now)}" as String
	sendEvent(name: "lastUpdate", value: newtime, descriptionText: "${device.displayName} configured at ${newtime}")

	setConfigured("true")
	def waketime

	if (timeOptionValueMap[settings.reportInterval] < 300)
	    waketime = timeOptionValueMap[settings.reportInterval]
	else waketime = 300

	if (debugOutput) log.debug "wake time reset to $waketime"
	if (debugOutput) log.debug "Current firmware: ${sprintf ("%1.2f", state.firmware)}"

	// Retrieve local temperature scale: "C" = Celsius, "F" = Fahrenheit
	// Convert to a value of 1 or 2 as used by the device to select the scale
	if (debugOutput) log.debug "Location temperature scale: ${location.getTemperatureScale()}"
	byte tempScaleByte = (location.getTemperatureScale() == "C" ? 1 : 2)
	selectiveReport = selectiveReporting ? 1 : 0

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
	        zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, scaledConfigurationValue: motionSensitivity as int),
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
	        // send (1) BasicSet or (2) SensorBinary report for motion
	        zwave.configurationV1.configurationSet(parameterNumber: 0x05, size: 1, scaledConfigurationValue: 2),
	        // Set temperature calibration offset
	        zwave.configurationV1.configurationSet(parameterNumber: 201, size: 2, configurationValue: [tempOffset as int, tempScaleByte]),
	        // Set humidity calibration offset
	        zwave.configurationV1.configurationSet(parameterNumber: 202, size: 1, scaledConfigurationValue: humidOffset),
	        // Set luminance calibration offset
	        zwave.configurationV1.configurationSet(parameterNumber: 203, size: 2, scaledConfigurationValue: luxOffset),
	        // Set LED Option value
	        zwave.configurationV1.configurationSet(parameterNumber: 81, size: 1, configurationValue: [ledOptions as int]),
	        //7. query sensor data
//	        zwave.configurationV1.configurationGet(parameterNumber: 9),  // Retrieve current power mode
	        zwave.batteryV1.batteryGet(),
	        zwave.sensorBinaryV1.sensorBinaryGet(),                      //motion
	        zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1), //temperature
	        zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 3), //illuminance
	        zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5), //humidity
	        zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 27) //ultravioletIndex
	]
	return commands(request) + ["delay 20000", zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
}


def refresh() {
	if (debugOutput) log.debug "in refresh"

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

/*
	Begin support methods
*/

def motionEvent(value, by) {
	def map = [name: "motion"]
	if (value) {
	    if (descTextEnable) log.info "Motion is active by $by"
	    map.value = "active"
	    map.descriptionText = "${device.displayName} motion is active by $by"
	} else {
	    if (descTextEnable) log.info "Motion is inactive by $by"
	    map.value = "inactive"
	    map.descriptionText = "${device.displayName} motion is inactive by $by"
	}
	createEvent(map)
}


def getTimeOptionValueMap() { [
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


// Check for any null settings and change them to default values
void inputValidationCheck () {

	motionSensitivity	= motionSensitivity	?: 0
	motionDelayTime	= motionDelayTime		?: "1 minute"
	reportInterval	= reportInterval		?: "5 minute"
	tempChangeAmount	= tempChangeAmount	?: 2
	humidChangeAmount	= humidChangeAmount	?: 10
	luxChangeAmount	= luxChangeAmount		?: 100
	tempOffset		= tempOffset		?: 0
	ledOptions		= ledOptions		?: 0

	// Validate Input Ranges
	def motionRange     = 0..5
	def tempRange       = 0..128
	def humidRange      = 0..50
	def luxRange        = 0..1000

	if ( !motionRange.contains(motionSensitivity as int) )  { motionSensitivity = 3 ; log.warn "Selection out of Range: Motion Sensitivity set to 3"; }
	if ( !tempRange.contains( tempOffset.abs() as int ) )   { tempOffset = 0 ; log.warn "Selection out of Range: Temperature Offset set to 0"; }
	if ( !humidRange.contains( humidOffset.abs() as int ) ) { humidOffset = 0 ; log.warn "Selection out of Range: Humidity Offset set to 0"; }
	if ( !luxRange.contains( luxOffset.abs() as int ) )     { luxOffset = 0 ; log.warn "Selection out of Range: Luminance Offset set to 0"; }
}


private setConfigured(configure) {
	updateDataValue("configured", configure)
}


private isConfigured() {
	getDataValue("configured") == "true"
}


def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}


def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}


def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("debugOutput",[value:"false",type:"bool"])
}


private dbCleanUp() {
	// clean up state variables that are obsolete
//	state.remove("tempOffset")
//	state.remove("version")
//	state.remove("Version")
//	state.remove("sensorTemp")
//	state.remove("author")
//	state.remove("Copyright")
	state.remove("verUpdate")
	state.remove("verStatus")
	state.remove("Type")
}

List<String> setParameter(parameterNumber = null, size = null, value = null){
	if (parameterNumber == null || size == null || value == null) {
		log.warn "incomplete parameter list supplied..."
		log.info "syntax: setParameter(parameterNumber,size,value)"
	} else {
		return delayBetween([
	    	secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: parameterNumber, size: size)),
	    	secureCmd(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber))
		],500)
	}
}

List<String> getParameterReport(param = null){
	if (!debugOutput) {
		log.warn "debug logging auto-enabled..."
		device.updateSetting("debugOutput",[value:"true",type:"bool"])
		runIn(1800,logsOff)
	}
	
	List<String> cmds = []
	if (param != null) {
		cmds = [secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param))]
	} else {
		0.upto(255, {
	   	cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: it)))	
		})
	}
	log.trace "configurationGet command(s) ($param) sent..."
	return delayBetween(cmds,500)
}	



// Check Version   ***** with great thanks and acknowlegment to Cobra (CobraVmax) for his original code ****
def updateCheck()
{
	def paramsUD = [uri: "https://hubitatcommunity.github.io/AeotecMultiSensor6/version2.json"]
	asynchttpGet("updateCheckHandler", paramsUD) 
}


def updateCheckHandler(resp, data) {

	state.InternalName = "AeotecMultiSensor6_2"

	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		respUD = parseJson(resp.data)
		// log.warn " Version Checking - Response Data: $respUD"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver 
		state.Copyright = "${thisCopyright} -- ${version()}"
		// uses reformattted 'version2.json' 
		def newVer = padVer(respUD.driver.(state.InternalName).ver)
		def currentVer = padVer(version())               
		state.UpdateInfo = (respUD.driver.(state.InternalName).updated)
            // log.debug "updateCheck: ${respUD.driver.(state.InternalName).ver}, $state.UpdateInfo, ${respUD.author}"

		switch(newVer) {
			case { it == "NLS"}:
			      state.Status = "<b>** This Driver is no longer supported by ${respUD.author}  **</b>"       
			      if (descTextEnable) log.warn "** This Driver is no longer supported by ${respUD.author} **"      
				break
			case { it > currentVer}:
			      state.Status = "<b>New Version Available (Version: ${respUD.driver.(state.InternalName).ver})</b>"
			      if (descTextEnable) log.warn "** There is a newer version of this Driver available  (Version: ${respUD.driver.(state.InternalName).ver}) **"
			      if (descTextEnable) log.warn "** $state.UpdateInfo **"
				break
			case { it < currentVer}:
			      state.Status = "<b>You are using a Test version of this Driver (Expecting: ${respUD.driver.(state.InternalName).ver})</b>"
			      if (descTextEnable) log.warn "You are using a Test version of this Driver (Expecting: ${respUD.driver.(state.InternalName).ver})"
				break
			default:
				state.Status = "Current"
				if (descTextEnable) log.info "You are using the current version of this driver"
				break
		}
 	sendEvent(name: "chkUpdate", value: state.UpdateInfo)
	sendEvent(name: "chkStatus", value: state.Status)
    }
    else
    {
        log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI"
    }
}

/*
	padVer

	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each duple is padded first.

*/ 
String padVer(ver) {
	def pad = ""
	ver.replaceAll( "[vV]", "" ).split( /\./ ).each { pad += it.padLeft( 2, '0' ) }
	return pad
}

String getThisCopyright(){"&copy; 2019 C Steele "}
