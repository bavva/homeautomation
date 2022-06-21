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
 *  Left It Open
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 *
 *  Author: Bharadwaj Avva
 *  Date: 2022-02-11
 */
definition(
    name: "Left It Open",
    namespace: "avvab",
    author: "Bharadwaj Avva",
    description: "Notifies when a door or window is left open longer that a specified amount of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {
	page(name: "mainPage", title: "Notify when a door is left open", install: true, uninstall: true)
}

def mainPage() {
  dynamicPage(name: "mainPage") {
      section("Select front door to monitor") {
        input "frontDoor", "capability.contactSensor"
      }
      
      section("Select front door virtual switch") {
        input "frontDoorSwitch", "capability.switch"
      }
      
      section("Select front door lock to monitor") {
        input "frontDoorLock", "capability.contactSensor"
      }
      
      section("Select front door lock virtual switch") {
        input "frontDoorLockSwitch", "capability.switch"
      }
      
      section("Select back door to monitor") {
        input "backDoor", "capability.contactSensor"
      }
      
      section("Select back door virtual switch") {
        input "backDoorSwitch", "capability.switch"
      }
      
      section("Select garage door to monitor") {
        input "garageDoor", "capability.contactSensor"
      }
      
      section("Select garage door virtual switch") {
        input "garageDoorSwitch", "capability.switch"
      }
      
      section("Select garage shutter to monitor") {
        input "garageShutter", "capability.contactSensor"
      }
      
      section("Select garage shutter virtual switch") {
        input "garageShutterSwitch", "capability.switch"
      }
      
      section("Select RGB light to set status") {
        input "statusLight", "capability.light"
      }

      section("And notify me if it's open for more than this many minutes (default 10)") {
        input "openThreshold", "number", description: "Number of minutes", required: false
      }

      section("Delay between notifications (default 10 minutes") {
        input "frequency", "number", title: "Number of minutes", description: "", required: false
      }
  }
}

def howManyOpen() {
  def howManyOpen = 0
  def contactState = null
  
  contactState = frontDoor.currentState("contact")
  if (contactState.value == "open") {
    howManyOpen++
  }
  
  contactState = frontDoorLock.currentState("contact")
  if (contactState.value == "open") {
    howManyOpen++
  }
  
  contactState = backDoor.currentState("contact")
  if (contactState.value == "open") {
    howManyOpen++
  }
  
  contactState = garageDoor.currentState("contact")
  if (contactState.value == "open") {
    howManyOpen++
  }
  
  contactState = garageShutter.currentState("contact")
  if (contactState.value == "open") {
    howManyOpen++
  }
  
  state.howManyOpen = howManyOpen
  
  return howManyOpen
}

def installed() {
  log.trace "installed()"
  subscribe()
  howManyOpen()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  subscribe()
  howManyOpen()
}

def subscribe() {
  subscribe(frontDoor, "contact.open", doorOpen)
  subscribe(frontDoor, "contact.closed", doorClosed)
  subscribe(frontDoorLock, "contact.open", doorOpen)
  subscribe(frontDoorLock, "contact.closed", doorClosed)
  subscribe(backDoor, "contact.open", doorOpen)
  subscribe(backDoor, "contact.closed", doorClosed)
  subscribe(garageDoor, "contact.open", doorOpen)
  subscribe(garageDoor, "contact.closed", doorClosed)
  subscribe(garageShutter, "contact.open", doorOpen)
  subscribe(garageShutter, "contact.closed", doorClosed)
}

def unsubscribe() {
  unsubscribe(frontDoor)
  unsubscribe(frontDoorLock)
  unsubscribe(backDoor)
  unsubscribe(garageDoor)
  unsubscribe(garageShutter)
}

def doorOpen(evt) {
  log.trace "doorOpen($evt.name: $evt.value, $evt.device)"
  def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
  
  if (evt.device.displayName == frontDoor.displayName) {
    runIn(delay, frontDoorOpenTooLong, [overwrite: true])
  } else if (evt.device.displayName == frontDoorLock.displayName) {
    runIn(delay, frontDoorLockOpenTooLong, [overwrite: true])
  } else if (evt.device.displayName == backDoor.displayName) {
    runIn(delay, backDoorOpenTooLong, [overwrite: true])
  } else if (evt.device.displayName == garageDoor.displayName) {
    runIn(delay, garageDoorOpenTooLong, [overwrite: true])
  } else if (evt.device.displayName == garageShutter.displayName) {
    runIn(delay, garageShutterOpenTooLong, [overwrite: true])
  }
  
  state.howManyOpen = state.howManyOpen + 1
  statusLight.setLedColor("red")
}

def doorClosed(evt) {
  log.trace "doorClosed($evt.name: $evt.value, $evt.device)"
  
  if (evt.device.displayName == frontDoor.displayName) {
    unschedule(frontDoorOpenTooLong)
  } else if (evt.device.displayName == frontDoorLock.displayName) {
    unschedule(frontDoorLockOpenTooLong)
  } else if (evt.device.displayName == backDoor.displayName) {
    unschedule(backDoorOpenTooLong)
  } else if (evt.device.displayName == garageDoor.displayName) {
    unschedule(garageDoorOpenTooLong)
  } else if (evt.device.displayName == garageShutter.displayName) {
    unschedule(garageShutterOpenTooLong)
  }
  
  //state.howManyOpen = state.howManyOpen - 1
  howManyOpen()
  if (state.howManyOpen == 0) {
    statusLight.setLedColor("green")
  }
}

def frontDoorOpenTooLong() {
  def contactState = frontDoor.currentState("contact")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "open") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage(frontDoor)
      runIn(freq, frontDoorOpenTooLong, [overwrite: false])
    } else {
      log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
  }
}

def frontDoorLockOpenTooLong() {
  def contactState = frontDoorLock.currentState("contact")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "open") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage(frontDoorLock)
      runIn(freq, frontDoorLockOpenTooLong, [overwrite: false])
    } else {
      log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
  }
}

def backDoorOpenTooLong() {
  def contactState = backDoor.currentState("contact")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "open") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage(backDoor)
      runIn(freq, backDoorOpenTooLong, [overwrite: false])
    } else {
      log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
  }
}

def garageDoorOpenTooLong() {
  def contactState = garageDoor.currentState("contact")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "open") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage(garageDoor)
      runIn(freq, garageDoorOpenTooLong, [overwrite: false])
    } else {
      log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
  }
}

def garageShutterOpenTooLong() {
  def contactState = garageShutter.currentState("contact")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (contactState.value == "open") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage(garageShutter)
      runIn(freq, garageShutterOpenTooLong, [overwrite: false])
    } else {
      log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
  }
}

void autoOffFrontDoorSwitch() {
  frontDoorSwitch.off()
}

void autoOffFrontDoorLockSwitch() {
  frontDoorLockSwitch.off()
}

void autoOffBackDoorSwitch() {
  backDoorSwitch.off()
}

void autoOffGarageDoorSwitch() {
  garageDoorSwitch.off()
}

void autoOffGarageShutterSwitch() {
  garageShutterSwitch.off()
}

void sendMessage(device) {
  if (device.displayName == frontDoor.displayName) {
    frontDoorSwitch.on()
    runIn(10, autoOffFrontDoorSwitch, [overwrite: true])
  } else if (device.displayName == frontDoorLock.displayName) {
    frontDoorLockSwitch.on()
    runIn(10, autoOffFrontDoorLockSwitch, [overwrite: true])
  } else if (device.displayName == backDoor.displayName) {
    backDoorSwitch.on()
    runIn(10, autoOffBackDoorSwitch, [overwrite: true])
  } else if (device.displayName == garageDoor.displayName) {
    garageDoorSwitch.on()
    runIn(10, autoOffGarageDoorSwitch, [overwrite: true])
  } else if (device.displayName == garageShutter.displayName) {
    garageShutterSwitch.on()
    runIn(10, autoOffGarageShutterSwitch, [overwrite: true])
  }
}
