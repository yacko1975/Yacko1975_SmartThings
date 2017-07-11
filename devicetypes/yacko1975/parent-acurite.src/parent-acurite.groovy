/**
 *  Acurite Arduino Reader
 *
 *  Copyright 2017 David Carter
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
 */
metadata {
	definition (name: "parent-acurite", namespace: "yacko1975", author: "David Carter") {
        capability "Configuration"
        capability "Refresh"
	}

    simulator {
		// TODO: define status and reply messages here
	}
    
	preferences {
      input "ip", "text", title: "Decoding device IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
      input "port", "text", title: "Arduino Port", description: "port in form of 8090", required: true, displayDuringSetup: true
      input "mac", "text", title: "Arduino MAC Addr", description: "MAC Address in form of 02A1B2C3D4E5", required: true, displayDuringSetup: true
    }
    


	tiles (scale:1){
    	standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action: "refresh.refresh", icon: "st.secondary.refresh-icon"
		}
		// TODO: define your main and details tiles here
       standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        childDeviceTiles("all")

        
	}
    
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'humidity' attribute
	// TODO: handle 'temperature' attribute
    def msg = parseLanMessage(description)
    def headerString = msg.header
    
    if(!headerString)
    {
      // no header was found
    }
    
    def bodyString = msg.body
    
    if (bodyString){
    log.debug "Parsing: $bodyString"
    def pairMap = bodyString
                  .split('&')
                  .collectEntries {entry -> 
                                  def pair = entry.split('=')
                                  [(pair.first()): pair.last()]
                                  }
    
    def childDevice = null
    childDevices.each{ item ->
      try 
        {
           if (item.deviceNetworkId == "${device.deviceNetworkId}-${pairMap.id}${pairMap.st}")
             {
               childDevice = item
             }
        
        }
      catch (e)
        {
          log.debug e
        }
    }
        
    if (childDevice == null)
    {
    childDevice = createChildDevice(pairMap.id, pairMap.st)
    }
        
  if (childDevice != null) {
    
  
  
  } else {
    
  }
    
      
 
    
    
    
    }

}

def CreateChildDevice(String deviceID, int deviceType)
{
  def deviceHandlerName = ""
  def childDevice = null
  switch(deviceType)
  {
   case 1: //Lightning Sensor
     deviceHandlerName = "LightningSensorType"
     break
   case 2: //Temp and Humidity Only
     deviceHandlerName = "THOnlySensorType"
     break
   case 3:
     deviceHandlerName = "WTHSensorType"
     break
   case 4:
     deviceHandlerName = "WDRSensorType"
     break
   default:
     log.error "Child device creation failed with device type ${deviceType}"
  }
  if (deviceHandlerName != "") {
   childDevice = addChildDevice(deviceHandlerName, "${device.deviceNetworkId}-${deviceID}${deviceType}", null,
		      		[completedSetup: true, label: "${device.displayName} (${deviceID}${deviceType})", 
                	isComponent: false, componentName: "${deviceID}${deviceType}", componentLabel: "${deviceID} ${deviceType}"])
  }
  
  return childDevice
 

}


def updateDeviceNetworkID() {
	log.debug "Executing 'updateDeviceNetworkID'"
    if(device.deviceNetworkId!=mac) {
    	log.debug "setting deviceNetworkID = ${mac}"
        device.setDeviceNetworkId("${mac}")
	}
    //Need deviceNetworkID updated BEFORE we can create Child Devices
	//Have the Arduino send an updated value for every device attached.  This will auto-created child devices!
	refresh()
}

def refresh() {
	log.debug "Executing 'refresh()'"
	sendEthernet("refresh")
	//sendEvent(name: "numberOfButtons", value: numButtons)
}

def initialize() {
	log.debug "Executing 'initialize()'"
    //sendEvent(name: "numberOfButtons", value: numButtons)
}

def updated() {
	if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 5000) {
		state.updatedLastRanAt = now()
		log.debug "Executing 'updated()'"
    	runIn(3, "updateDeviceNetworkID")
		//sendEvent(name: "numberOfButtons", value: numButtons)
	}
	else {
//		log.trace "updated(): Ran within last 5 seconds so aborting."
	}
}

def sendEthernet(message) {
	log.debug "Executing 'sendEthernet' ${message}"
	if (settings.ip != null && settings.port != null) {
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "POST",
            path: "/${message}?",
            headers: [ HOST: "${getHostAddress()}" ]
        ))
    }
    else {
        state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        runIn(2, "sendAlert")   
    }
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port
    
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

def configure() {
	log.debug "Executing 'configure()'"
    updateDeviceNetworkID()
	sendEvent(name: "numberOfButtons", value: numButtons)
}
