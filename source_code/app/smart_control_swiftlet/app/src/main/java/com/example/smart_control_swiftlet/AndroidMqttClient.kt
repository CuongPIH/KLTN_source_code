package com.example.smart_control_swiftlet

//import org.eclipse.paho.android.service.MqttAndroidClient
import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class AndroidMqttClient private constructor()  {

    private lateinit var mqttClient: MqttAndroidClient

    var listener: ((topic: String, message : MqttMessage?) -> Unit)? = null
    var onConnected: ((isconnected: Boolean) -> Unit)? = null

    val sensor_topic = "data/sensor"
    val control_topic = "data/control"
    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"

        private var instance: AndroidMqttClient? = null

        fun getInstance(): AndroidMqttClient {
            if (instance == null) {
                instance = AndroidMqttClient()
            }
            return instance!!
        }
    }


    fun connect(context: Context) {
        val serverURI = "tcp://broker.hivemq.com:1883"
//        val serverURI = "ssl://av40k46vr2pm9-ats.iot.ap-southeast-2.amazonaws.com:8883"
        val name = "myapp_client_ID_" + (10000..999999).random().toString()
        mqttClient = MqttAndroidClient(context, serverURI, name )
        Log.d(TAG, name)
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                listener!!.invoke(topic!!, message)
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
                onConnected!!.invoke(false)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })

        val options = MqttConnectOptions()

        try {
            mqttClient.connect(options, context, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    subscribe(sensor_topic)
                    subscribe(control_topic)
                    onConnected!!.invoke(true)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                    onConnected!!.invoke(false)
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Unsubscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to unsubscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


}