package com.cooperthecoder.implant.data

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceProperties {
    fun deviceId(context: Context): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return deviceId
    }

    fun model(): String {
        return "${Build.MANUFACTURER}/${Build.MODEL}"
    }

    fun identifier(context: Context): String {
        return "${model()}/${deviceId(context)}".replace("\\s+".toRegex(), "")
    }

    fun commandChannelName(context: Context): String {
        return "commands/${identifier(context)}"
    }

    fun replyChannelName(context: Context): String {
        return "replies/${identifier(context)}"
    }
}