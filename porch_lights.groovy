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
    name: "Light it up on motion",
    namespace: "avvab",
    author: "Bharadwaj Avva",
    description: "Turns light on when motion is detected",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
    page(name: "mainPage", title: "Turns light on when motion is detected", install: true, uninstall: true)
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

    section("Select color bulbs") {
        input "colorBulbs", "capability.colorControl", multiple: true, required: true
    }

    section ("Sunset offset (optional)...") {
        input "sunsetOffsetValue", "text", title: "HH:MM", required: true
        input "sunsetOffsetDir", "enum", title: "Before or After", required: true, options: ["Before","After"]
    }

    section("How long to keep the AlwaysOnMode in minutes (default 180)") {
        input "eveningOnTime", "number", description: "Number of minutes", required: false
    }

    section("Zip code") {
        input "zipCode", "text", required: true
    }

    section("Select zooz controller button") {
      input "zoozButton", "capability.Button"
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

def setBulbsColor() {
  def currentColor = atomicState.allColors[atomicState.currentColorIndex]
  log.debug "setBulbsColor: $currentColor"

  for (colorBulb in colorBulbs) {
    colorBulb.setColor(hue: currentColor[0], saturation: currentColor[1])
  }
  for (colorBulb in colorBulbs) {
    colorBulb.setColor(hue: currentColor[0], saturation: currentColor[1])
  }

  atomicState.changeBulbsColor = false
  atomicState.currentColorIndex = (atomicState.currentColorIndex + 1) % atomicState.allColors.size()
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
  disableEveningOnMode()
  subscribe()
}

def locationPositionChange(evt) {
  log.trace "locationChange()"
  updated()
}

def initialize() {
  atomicState.alwaysOnMode = false
  atomicState.setTime = null
  atomicState.switched_on_how = null
  atomicState.eveningOnMode = false
  atomicState.changeBulbsColor = true
  atomicState.currentColorIndex = 3
  atomicState.allColors = [[50, 50], [130, 40], [43, 67], [18, 76], [10, 87], [98, 83], [85, 65], [75, 65], [62, 87], [15, 7]]

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
  scheduleNextSunset()
}

private getSunsetOffset() {
    if (sunsetOffsetDir == "Before") {
        return sunsetOffsetValue ? "-$sunsetOffsetValue" : null
    }

    return sunsetOffsetValue
}

def scheduleNextSunset(date = null) {
    def s = getSunriseAndSunset(zipCode: zipCode, sunsetOffset: getSunsetOffset(), date: date)
    def now = new Date()
    def setTime = s.sunset
    log.debug "setTime: $setTime"

    // use state to keep track of sunset times between executions
    // if sunset time has changed, unschedule and reschedule handler with updated time
    if(atomicState.setTime != setTime.time) {
        unschedule("sunsetHandler")

        if(setTime.before(now)) {
        	s = getSunriseAndSunset(zipCode: zipCode, sunsetOffset: getSunsetOffset(), date: now + 1)
            setTime = s.sunset
        }

        atomicState.setTime = setTime.time

        log.info "scheduling sunset handler for $setTime"
        schedule(setTime, sunsetHandler)
    }
}

def enableAlwaysOnMode() {
    atomicState.alwaysOnMode = true
    set_switched_on_how("manual")
    lightSwitch.on()
}

def disableAlwaysOnMode() {
    atomicState.alwaysOnMode = false
    set_switched_on_how("automatic")
    lightsOnOff()

    if (atomicState.eveningOnMode == true) {
      lightSwitch.on()
    }
}

def enableEveningOnMode() {
    atomicState.eveningOnMode = true

    atomicState.changeBulbsColor = true
    runIn(180, setBulbsColor, [overwrite: true])

    if (atomicState.alwaysOnMode == true) {
      return
    }

    if (atomicState.switched_on_how == null) {
      set_switched_on_how("automatic")
    }
    lightSwitch.on()
}

def disableEveningOnMode() {
    atomicState.eveningOnMode = false
    lightsOnOff()
}

def sunsetHandler() {
    def delay = (eveningOnTime != null && eveningOnTime != "") ? eveningOnTime * 60 : 180 * 60

    log.info "Executing sunset handler"

    enableEveningOnMode()
    runIn(delay, disableEveningOnMode, [overwrite: true])

    // schedule for tomorrow
    scheduleNextSunset(new Date() + 1)
}

def lightsOnOff() {
  if (atomicState.alwaysOnMode == true) {
    return
  }

  if (atomicState.eveningOnMode == true) {
    return
  }

  if ((atomicState.howManyDetectingMotion == 0) && (atomicState.howManyOpen == 0)) {
    lightSwitch.off()
  } else {
    set_switched_on_how("automatic")
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

def switchOn(evt) {
  log.trace "switchOn($evt.name: $evt.value, $evt.device)"

  if (atomicState.switched_on_how == null) {
    set_switched_on_how("manual")
    atomicState.alwaysOnMode = true
  }

  zoozButton.setLedBrightness("low")
}

def switchOff(evt) {
  log.trace "switchOff($evt.name: $evt.value, $evt.device)"

  atomicState.alwaysOnMode = false
  zoozButton.setLedBrightness("bright")
  set_switched_on_how(null)
}
