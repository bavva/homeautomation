/**
 *  Copyright 2022 SmartThings
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
 *  Turn on lights if motion is detected.
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 *
 *  Author: Bharadwaj Avva
 *  Date: 2022-03-26
 */
definition(
    name: "Garage Lights Automation",
    namespace: "avvab",
    author: "Bharadwaj Avva",
    description: "Automate Garage Lights",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
    page(name: "mainPage", title: "Automate garage lights", install: true, uninstall: true)
}

def mainPage() {
  dynamicPage(name: "mainPage") {
    section("Motion sensors to monitor") {
        input "motionSensors", "capability.motionSensor", multiple: true, required: true
    }

    section("Contact sensors to monitor") {
        input "contactSensors", "capability.contactSensor", multiple: true, required: true
    }

    section("Switch to turn on") {
        input "lightSwitch", "capability.switch", required: true
    }
    
    section("Garage speaker to turn on") {
        input "garageSwitch", "capability.switch", required: true
    }

    section("Select zooz controller button") {
      input "zoozButton", "capability.Button"
    }

    section("How long to keep the lights on") {
      input "autoOffTime", "number", title: "Number of minutes", description: "", required: true
    }
    
    section("How long to keep the garage speaker on after lights are off") {
      input "speakerOffTime", "number", title: "Number of minutes", description: "", required: true
    }
  }
}

def howManyDetectingMotion() {
  def howManyDetectingMotion = 0

  for (motionSensor in motionSensors) {
    if (motionSensor.currentMotion == "active") {
      howManyDetectingMotion++
    }
  }
  atomicState.howManyDetectingMotion = howManyDetectingMotion

  return howManyDetectingMotion
}

def howManyOpen() {
  def howManyOpen = 0

  for (contactSensor in contactSensors) {
    if (contactSensor.currentState("contact").value == "open") {
      howManyOpen++
    }
  }
  atomicState.howManyOpen = howManyOpen

  return howManyOpen
}

def installed() {
  log.trace "installed()"
  subscribe()
  howManyDetectingMotion()
  howManyOpen()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  disableAlwaysOnMode()
  subscribe()
}

def initialize() {
  atomicState.alwaysOnMode = false
  atomicState.switched_on_how = null

  zoozButton.setLedColor("white")
  zoozButton.setLedMode("alwaysOn")
  zoozButton.setLedBrightness("bright")
}

def subscribe() {
  subscribe(motionSensors, "motion", motionHandle)
  subscribe(contactSensors, "contact.open", contactOpenHandle)
  subscribe(contactSensors, "contact.closed", contactCloseHandle)
  subscribe(lightSwitch, "switch.on", switchOn)
  subscribe(lightSwitch, "switch.off", switchOff)
  subscribe(zoozButton, "button", buttonPush)

  initialize()
}

def enableAlwaysOnMode() {
    atomicState.alwaysOnMode = true
    set_switched_on_how("manual")
    unschedule("lightsOff")
    lightSwitch.on()
}

def disableAlwaysOnMode() {
    atomicState.alwaysOnMode = false
    set_switched_on_how("automatic")
    lightsOnOff(true)
}

def lightsOff() {
  lightSwitch.off()
}

def lightsOnOff(immediateOff = false) {
  def delay = (autoOffTime != null && autoOffTime != "") ? autoOffTime * 60 : 2 * 60

  if (atomicState.alwaysOnMode == true) {
    return
  }

  if ((atomicState.howManyDetectingMotion == 0) && (atomicState.howManyOpen == 0)) {
    if (immediateOff) {
      lightsOff()
    } else {
      runIn(delay, lightsOff, [overwrite: true])
    }
  } else {
    set_switched_on_how("automatic")
    unschedule("lightsOff")
    lightSwitch.on()
  }
}

def buttonPush(evt) {
  log.trace "buttonPush($evt.name: $evt.value, $evt.device)"

  if (atomicState.alwaysOnMode == true) {
    disableAlwaysOnMode()
  } else {
    enableAlwaysOnMode()
  }
}

def motionHandle(evt) {
  log.trace "motionHandle($evt.name: $evt.value, $evt.device)"

  if (evt.value == "active") {
    atomicState.howManyDetectingMotion = atomicState.howManyDetectingMotion + 1
  } else {
    howManyDetectingMotion()
  }

  log.trace "Tripped motion sensors $atomicState.howManyDetectingMotion"
  lightsOnOff()
}

def contactOpenHandle(evt) {
  log.trace "contactOpenHandle($evt.name: $evt.value, $evt.device)"

  atomicState.howManyOpen = atomicState.howManyOpen + 1

  log.trace "Tripped contact sensors $atomicState.howManyOpen"
  lightsOnOff()
}

def contactCloseHandle(evt) {
  log.trace "contactCloseHandle($evt.name: $evt.value, $evt.device)"

  howManyOpen()

  log.trace "Tripped contact sensors $atomicState.howManyOpen"
  lightsOnOff()
}

def set_switched_on_how(how) {
  atomicState.switched_on_how = how

  if (how == "manual") {
    zoozButton.setLedColor("red")
  } else {
    zoozButton.setLedColor("white")
  }
}

def garageSwitchOff() {
  garageSwitch.off()
}

def switchOn(evt) {
  log.trace "switchOn($evt.name: $evt.value, $evt.device)"

  unschedule("lightsOff")
  unschedule("garageSwitchOff")
  
  if (atomicState.switched_on_how == null) {
    set_switched_on_how("manual")
    atomicState.alwaysOnMode = true
  }

  zoozButton.setLedBrightness("low")
  garageSwitch.on()
}

def switchOff(evt) {
  def delay = (speakerOffTime != null && speakerOffTime != "") ? speakerOffTime * 60 : 2 * 60
  
  log.trace "switchOff($evt.name: $evt.value, $evt.device)"

  atomicState.alwaysOnMode = false
  zoozButton.setLedBrightness("bright")
  set_switched_on_how(null)
  
  runIn(delay, garageSwitchOff, [overwrite: true])
}
