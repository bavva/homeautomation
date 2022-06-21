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
 *  Stairs Lights
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 *
 *  Author: Bharadwaj Avva
 *  Date: 2022-03-26
 */
definition(
    name: "Kitchen Lights Control",
    namespace: "avvab",
    author: "Bharadwaj Avva",
    description: "Control a light using Zooz scene controller button",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet-luminance@2x.png"
)

preferences {
	page(name: "mainPage", title: "Control a light using Zooz scene controller button", install: true, uninstall: true)
}

def mainPage() {
  dynamicPage(name: "mainPage") {
      section("Select zooz controller button") {
        input "zoozButton", "capability.Button"
      }

      section("Select light switch to control") {
        input "lightSwitch", "capability.Switch"
      }
  }
}

def initializeState() {
  zoozButton.setLedColor("white")
  zoozButton.setLedMode("alwaysOn")
  zoozButton.setLedBrightness("bright")
  lightSwitch.off()
}

def cleanupState() {
  zoozButton.setLedColor("white")
  zoozButton.setLedMode("alwaysOff")
}

def installed() {
  log.trace "installed()"
  subscribe()
  initializeState()
}

def uninstalled() {
  log.trace "uninstalled()"
  unsubscribe()
  cleanupState()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  initializeState()
  subscribe()
}

def subscribe() {
  subscribe(lightSwitch, "switch.on", switchOn)
  subscribe(lightSwitch, "switch.off", switchOff)
  subscribe(zoozButton, "button", buttonPush)
}

def unsubscribe() {
  unsubscribe(lightSwitch)
}

def switchOn(evt) {
  log.trace "switchOn($evt.name: $evt.value, $evt.device)"
  zoozButton.setLedBrightness("low")
}

def switchOff(evt) {
  log.trace "switchOff($evt.name: $evt.value, $evt.device)"
  zoozButton.setLedBrightness("bright")
}

def buttonPush(evt) {
  log.trace "buttonPush($evt.name: $evt.value, $evt.device)"
  if (lightSwitch.currentValue('switch').contains('on')) {
    lightSwitch.off()
  } else {
    lightSwitch.on()
  }
}
