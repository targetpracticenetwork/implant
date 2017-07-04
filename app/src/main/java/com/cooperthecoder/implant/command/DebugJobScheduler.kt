package com.cooperthecoder.implant.command

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.cooperthecoder.implant.Config

class DebugJobScheduler : IntentService(TAG) {

    companion object {
        val TAG = DebugJobScheduler::class.java.name
        const val EXTRA_TASK_NAME = "task_name"
    }

    override fun onHandleIntent(intent: Intent) {
        Log.d(TAG, "Starting DebugJobScheduler.")
        if (!Config.DEBUG) {
            throw RuntimeException("DebugJobScheduler can only be run in debug mode.")
        }

        val name = intent.getStringExtra(EXTRA_TASK_NAME)
        runTask(name)
    }

    private fun runTask(tag: String) {
        when (tag) {
            else -> {
                Log.d(TAG, "Job not implemented yet: $tag")
            }
        }
    }

}