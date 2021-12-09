package com.example.weather_ghh

import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.weather_ghh.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.RoundingMode
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
private lateinit var binding : ActivityMainBinding
private lateinit var saCitiesArray : ArrayList<String>
private  var selectedCity = ""
    private var requestString = ""
    private var convertDegree = 0.0
    private var convertLowDegree = 0.0
    private var convertHighDegree = 0.0
    private var isCelsius = true


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale", "SimpleDateFormat", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        saCitiesArray = arrayListOf ("mecca","jeddah","riyadh","dammam","medina","taif","abha")
        setSpinner()
        //by name sa cities
        //https://api.openweathermap.org/data/2.5/weather?q=mecca&APPID=010ffc3aca82936b9447bfd9f0b28fea
        //by zip code us cities
        //https://api.openweathermap.org/data/2.5/weather?zip=93221&APPID=010ffc3aca82936b9447bfd9f0b28fea

        binding.tvLocation.setOnClickListener {
           binding.lyForm.isVisible = true
            binding.lyFirst.isVisible = false
            binding.lySecond.isVisible = false
            binding.lyThird.isVisible = false

        }
        binding.btnCancel.setOnClickListener {
            binding.lyForm.isVisible = false
            binding.lyFirst.isVisible = true
            binding.lySecond.isVisible = true
            binding.lyThird.isVisible = true
            binding.tvAddZipCode.text.clear()

        }
        binding.btnZipCode.setOnClickListener {
            val zipCode = binding.tvAddZipCode.text.toString()
            binding.lyForm.isVisible = false
            binding.lyFirst.isVisible = true
            binding.lySecond.isVisible = true
            binding.lyThird.isVisible = true
            requestString ="zip=$zipCode"
            request(requestString)
            binding.tvAddZipCode.text.clear()
        }
       binding.btnCites.setOnClickListener {
           binding.lyForm.isVisible = false
           binding.lyFirst.isVisible = true
           binding.lySecond.isVisible = true
           binding.lyThird.isVisible = true
           requestString = "q=$selectedCity"
           request(requestString)
           binding.tvAddZipCode.text.clear()
       }
        binding.lyRefresh.setOnClickListener {
            request(requestString)
            isCelsius = true
        }
        binding.tvDegree.setOnClickListener {
            if (isCelsius){
                //to f
                binding.tvDegree.text = "${fromCelsiusToFahrenheit(convertDegree)}° F"
                binding.tvLow.text = "${fromCelsiusToFahrenheit(convertLowDegree)}° F"
                binding.tvHigh.text = "${fromCelsiusToFahrenheit(convertHighDegree)}° F"
                isCelsius = false
            }else{
                //to c
                binding.tvDegree.text = "$convertDegree° C"
                binding.tvLow.text = "$convertLowDegree° C"
                binding.tvHigh.text = "$convertHighDegree° C"
                isCelsius = true
            }

        }

    }
    //function to request the url link from the fetch data function
    // it'll check if data exist ->set result not exist-> display alert
    private fun request(selected:String){
        CoroutineScope(IO).launch{
            try {
                val data = async { fetchData(selected) }.await()
                if (data.isNotEmpty()) {
                    // set data
                    setResultToUI(data)
                } else {
                    Log.d("MAIN", "something wrong with fetching data")
                    println("the zip code does not exist or wrong")
                    withContext(Main) {
                            mainAlert()
                    }
                }
            }catch (e: Exception){
                Log.d("MAIN","the zip code does not exist or wrong")

            }
        }

    }

    //function to request the url and get the respones
    private fun fetchData(Selected:String):String{
        var response = ""
        try {
            response = URL("https://api.openweathermap.org/data/2.5/weather?$Selected&APPID=010ffc3aca82936b9447bfd9f0b28fea").readText()
        }catch(e : Exception){
            Log.d("MAIN","something wrong ${e.localizedMessage}")

        }
        return response
    }
    // function to set all response data came from request function and set it in App UI elements
    @SuppressLint("SetTextI18n")
    private suspend fun setResultToUI(data:String){
        withContext(Main){
            val jsonObj = JSONObject(data)
            //formatTime(Instant.ofEpochSecond(sunrise))
            println(jsonObj)

            binding.tvHumidity.text = "Humidity \n ${jsonObj.getJSONObject("main").getString("humidity")}"
            binding.tvPressure.text = "Pressure \n ${jsonObj.getJSONObject("main").getString("pressure")}"
            binding.tvWind.text = "Wind \n ${jsonObj.getJSONObject("wind").getString("speed")}"
            val sunset = jsonObj.getJSONObject("sys").getString("sunset").toLong()
            binding.tvSunset.text = "Sunset \n ${getDateString(sunset)}"
            val sunrise = jsonObj.getJSONObject("sys").getString("sunrise").toLong()
            binding.tvSunrise.text = "Sunrise \n ${getDateString(sunrise)}"
            binding.tvLocation.text = "${jsonObj.getString("name")} , ${jsonObj.getJSONObject("sys").getString("country")}"
            binding.tvDate.text = timeZone(jsonObj.getString("timezone").toString())
            val jsonObjSec =  jsonObj.getJSONArray("weather")
            binding.tvWeather.text = jsonObjSec.getJSONObject(0).getString("description").toString()
             convertDegree = fromKelvinToCelsius(jsonObj.getJSONObject("main").getString("temp"))
            binding.tvDegree.text =  "$convertDegree° C"
             convertLowDegree = fromKelvinToCelsius(jsonObj.getJSONObject("main").getString("temp_min"))
            binding.tvLow.text =  "High $convertLowDegree°"
             convertHighDegree = fromKelvinToCelsius(jsonObj.getJSONObject("main").getString("temp_max"))
            binding.tvHigh.text =  "Low $convertHighDegree°"
        }
    }
    //function convert numeric time zone to readably string one
    @SuppressLint("SimpleDateFormat")
    fun timeZone(timezone:String) : String{
        val c = Calendar.getInstance()
        c.timeZone = TimeZone.getTimeZone(timezone)
        val date = c.time
        val df = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val strDate = df.format(date)
        println( c.time.toString())
        return strDate
    }
    // function convert long milliseconds to readably hour and minutes string one
    @SuppressLint("SimpleDateFormat")
    private fun getDateString(time: Long): String {
        val df = SimpleDateFormat("hh:mm")
        return df.format(time * 1000L)
    }

    // function convert kelvin degree to Celsius degree
    private fun fromKelvinToCelsius(kelvinForm:String):Double{
        var degree = kelvinForm.toDouble() - 273.15
        degree = degree.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
        return degree
    }
    private fun fromCelsiusToFahrenheit(CelsiusForm:Double):Double{
        return (1.8* CelsiusForm + 32).toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }
    //alert function
    private fun mainAlert(){
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("the zip code does not exist or wrong")

            .setNegativeButton("Close") { dialog, _ ->
                dialog.cancel()
            }
        val alert = dialogBuilder.create()
        alert.setTitle("")
        alert.show()
    }
    //function to set the spinner data and on click
    private fun setSpinner(){
       val mySpinner = binding.spinnerCites
        val adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item, saCitiesArray
        )
        mySpinner.adapter = adapter

        mySpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                selectedCity = saCitiesArray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action

            }
        }

    }
}