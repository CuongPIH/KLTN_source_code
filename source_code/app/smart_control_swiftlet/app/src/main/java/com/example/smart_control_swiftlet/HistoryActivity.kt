package com.example.smart_control_swiftlet

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class HistoryActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    var history: Button? = null
    var textview_date: TextView? = null
    var cal = Calendar.getInstance()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar!!.hide()

        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)

        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this)

        // ArrayList of class ItemsViewModel
        val data = ArrayList<ItemsViewModel>()
        val adapter = CustomAdapter(data)

        val numberdata = findViewById<TextView>(R.id.numberdata)

        textview_date = findViewById(R.id.datedisplay)
        history = findViewById(R.id.historybtn)

        textview_date!!.text = "12/02/2023"

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("/")

        // create an OnDateSetListener
        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int,
                                   dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        }

        textview_date!!.setOnClickListener {
            DatePickerDialog(this,
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // when you click on the button, show DatePickerDialog that is set with OnDateSetListener
        history!!.setOnClickListener {
            myRef.child("history").child(textview_date!!.text as String).get().addOnSuccessListener {
//                Log.i("firebase", "Got value ${it.value}")
                val gson = Gson()
                val _json = gson.toJson(it.value)
                data.clear()
                adapter.notifyDataSetChanged()
//                Log.d("key", _json)
                try {
                    val jsonObject = JSONObject(_json)
                    var n = 0
                    jsonObject.keys().forEach { key ->
                        n = n + 1
                        Log.d("key: ", "$key")
                        val _obj_data = jsonObject.get(key)
                        val i = gson.fromJson(_obj_data.toString(), ItemsViewModel::class.java)
                        data.add(ItemsViewModel(i.time, i.mode, i.speaker, i.mist, i.fan, i.light, i.temp, i.hum))

                    }
                    numberdata.text = "số lượng: $n"

                    adapter.notifyDataSetChanged()
                } catch (err: JSONException) {
                    Log.d("Error", err.toString())
                    numberdata.text = "Không có DL"
                }
                adapter.notifyDataSetChanged()
            }.addOnFailureListener{
                Log.e("firebase", "Error getting data", it)
                numberdata.text = "Không có DL"
            }
        }




        // This loop will create 20 Views containing
        // the image with the count of view
//        for (i in 1..20) {
//            data.add(ItemsViewModel("12:22", 1, 1, 1, 1, 100.2f, 33.3f, 88.0f))
//        }

        // This will pass the ArrayList to our Adapter


        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter

    }

    private fun updateDateInView() {
        val myFormat = "dd-MM-yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        textview_date!!.text = sdf.format(cal.getTime())
    }
}