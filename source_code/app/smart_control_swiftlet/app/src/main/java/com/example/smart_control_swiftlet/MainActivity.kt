package com.example.smart_control_swiftlet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import org.json.JSONObject


class MainActivity : AppCompatActivity() {


    var lightState = 0
    var fanState = 0
    var mistState = 0
    var speakerState = 0
    var modeState = 0
    var humi = 0.0f
    var temp = 0.0f
    var light = 0.0f
    var time = ""

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AndroidMqttClient.getInstance().connect(this)
        
        supportActionBar!!.hide()


        val mode_switch = findViewById<Switch>(R.id.modeSwitch)
        val fan_switch = findViewById<Switch>(R.id.fanSwitch)
        val mist_switch = findViewById<Switch>(R.id.mistSwitch)
        val speaker_switch = findViewById<Switch>(R.id.speakerSwitch)
        val light_switch = findViewById<Switch>(R.id.lightSwitch)

        val humi_textview = findViewById<TextView>(R.id.humTextview)
        val temp_texview = findViewById<TextView>(R.id.tempTextview)
        val light_texview = findViewById<TextView>(R.id.lightTextview)

        val light_device_textview = findViewById<TextView>(R.id.lightDeviceTextview)
        val mode_device_textview = findViewById<TextView>(R.id.modeDeviceTextView)
        val fan_device_textview = findViewById<TextView>(R.id.fanDeviceTextView)
        val mist_device_textview =  findViewById<TextView>(R.id.mistDeviceTextview)
        val speaker_device_textview =  findViewById<TextView>(R.id.speakerDeviceTextview)

        val mqttState = findViewById<TextView>(R.id.connectStateTextview)

        val history = findViewById<Button>(R.id.historybtn)

        AndroidMqttClient.getInstance().onConnected = {
            if (it) {
                mqttState.text = "MQTT: Đã kết nối"
            } else {
                mqttState.text = "MQTT: Chưa kết nối"
            }
        }

        AndroidMqttClient.getInstance().listener = { topic, data ->
            if (topic == AndroidMqttClient.getInstance().sensor_topic) {
                try {
                    val json = JSONObject(data.toString())
                    humi = json.get("hum").toString().toFloat()
                    temp = json.get("temp").toString().toFloat()
                    light = json.get("lightss").toString().toFloat()
                    time = json.get("time").toString()

                    modeState = json.get("mode").toString().toInt()
                    fanState = json.get("fan").toString().toInt()
                    mistState = json.get("mist").toString().toInt()
                    speakerState = json.get("speaker").toString().toInt()
                    lightState = json.get("light").toString().toInt()

                    Log.d("fanState", fanState.toString())
                    Log.d("modeState", modeState.toString())


                    light_device_textview.text = if (lightState == 1) "Bật" else "Tắt"
                    fan_device_textview.text = if (fanState == 1) "Bật" else "Tắt"
                    mist_device_textview.text = if (mistState == 1) "Bật" else "Tắt"
                    speaker_device_textview.text = if (speakerState == 1) "Bật" else "Tắt"
                    mode_device_textview.text = if (modeState == 1) "Bật" else "Tắt"
//
//                    mode_switch.isChecked = if (modeState == 1) true else false
//                    fan_switch.isChecked = if (fanState == 1) true else false
//                    mist_switch.isChecked = if (mistState == 1) true else false
//                    speaker_switch.isChecked = if (speakerState == 1) true else false

                    humi_textview.text =  "Độ ẩm: " + humi.toString() + " %"
                    temp_texview.text = "Nhiệt độ: " + temp.toString() + " °C"
                    light_texview.text = "Độ sáng: " +  light.toString() + " lux"

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

        history.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        mode_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            modeState = if (isChecked == true) 1 else 0
            send_mqtt()
        }

        fan_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            fanState = if (isChecked == true) 1 else 0
            send_mqtt()
        }

        mist_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            mistState = if (isChecked == true) 1 else 0
            send_mqtt()
        }

        speaker_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            speakerState = if (isChecked == true) 1 else 0
            send_mqtt()
        }

        light_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            lightState = if (isChecked == true) 1 else 0
            send_mqtt()
        }
    }

    fun send_mqtt() {
        val json = JsonObject()
        json.addProperty("mist", mistState)
        json.addProperty("mode", modeState)
        json.addProperty("fan", fanState)
        json.addProperty("speaker", speakerState)
        json.addProperty("hum", humi)
        json.addProperty("temp", temp)
        json.addProperty("light", lightState)
        json.addProperty("lightss", light)
        json.addProperty("time", time)
        val str = json.toString()

        AndroidMqttClient.getInstance().publish(AndroidMqttClient.getInstance().control_topic, str)
        AndroidMqttClient.getInstance().publish(AndroidMqttClient.getInstance().sensor_topic, str)
    }
}