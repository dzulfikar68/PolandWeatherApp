package io.github.dzulfikar68.polandweatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import io.github.dzulfikar68.polandweatherapp.Utils.capitalizeWords
import io.github.dzulfikar68.polandweatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherAdapter: WeatherAdapter
    private var selectedCity: City? = null
    private var listCity: ArrayList<City> = arrayListOf()
    private var locationManager : LocationManager? = null
    private var isMyLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar?.setBackgroundDrawable(resources.getDrawable(R.color.purple_600))

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        checkingGPS {}

        weatherAdapter = WeatherAdapter()
        with(binding.rvForecast) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = weatherAdapter
        }
        emptyFillAndData()

        initSpinner()
        binding.msCity.setOnItemSelectedListener { view, position, id, item ->
            binding.tvForecast.visibility = View.VISIBLE
            binding.llContent.visibility = View.VISIBLE
            val selected = binding.msCity.getItems<City>().get(position)
            if (selected.lat != 0.0 && selected.long != 0.0) {
                selectedCity = selected
                getListWeather()
            } else if (selected.id == 9L) {
                isMyLocation = false
                gettingGPS()
            } else {
                emptyFillAndData()
            }
        }

        binding.btnAdd.setOnClickListener {
            val dialog = AddDialogFragment.newInstance(object : AddDialogFragment.AddCallback {
                override fun onClick(cityName: String, lat: String, lon: String) {
                    val city = City(
                            id = 0,
                            name = cityName,
                            country = "-",
                            lat = lat.toDouble(),
                            long = lon.toDouble()
                    )
                    listCity.add(city)
                    binding.msCity.setItems(listCity)
                    CityPreference.setCities(this@MainActivity, city)
                    Snackbar.make(binding.root, "Add Success", Snackbar.LENGTH_LONG)
                            .show()
                }
            })
            dialog.show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
        }

        binding.btnRefresh.setOnClickListener {
            CityPreference.delCities(this@MainActivity)
            Snackbar.make(binding.root, "Refreshing", Snackbar.LENGTH_LONG)
                    .show()
            object : CountDownTimer(1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                }
                override fun onFinish() {
                    finish()
                    startActivity(intent)
                }
            }.start()
        }
    }

    private fun initSpinner() {
        val firstList = arrayListOf(
                City(0, "--Pilih Kota--", "-", 0.0, 0.0),
                City(1, "Gdańsk", "PL", 54.352051, 18.64637),
                City(2, "Warszawa", "PL", 52.23547, 21.04191),
                City(3, "Kraków", "PL", 50.083328, 19.91667),
                City(4, "Wrocław", "PL", 51.099998, 17.033331),
                City(5, "Łódź", "PL", 51.75, 19.466669)
        )
        listCity.addAll(firstList)
        listCity.add(City(9, "Current GPS position", "ID", 0.0, 0.0))
        val savedCities = CityPreference.getCities(this@MainActivity)
        listCity.addAll(savedCities)
        binding.msCity.setItems(listCity)
    }

    private fun gettingGPS() {
        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
        } catch (ex: SecurityException) {
            checkingGPS {
                gettingGPS()
            }
        }
    }

    private fun checkingGPS(callback: () -> Unit) {
        try {
            if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        101
                )
            } else {
                callback()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val myCity = listCity.firstOrNull { it.id == 9L }
            myCity?.lat = location.latitude
            myCity?.long = location.longitude
            selectedCity = myCity
            if (!isMyLocation) {
                getListWeather()
                isMyLocation = true
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun emptyFillAndData() {
        weatherAdapter.setList(emptyList())
        binding.tvWeather.text = "-"
        binding.tvDate.text = "-"
        binding.tvLocation.text = "-"
        binding.tvHumidity.text = "-"
        binding.tvPressure.text = "-"
        binding.tvPercent.text = "-"
        binding.tvForecast.visibility = View.GONE
        binding.llContent.visibility = View.GONE
    }

    private fun getListWeather() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Now Loading")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(OpenWeatherService::class.java)
        val lat = selectedCity?.lat?.toString() ?: "0.0"
        val lon = selectedCity?.long?.toString() ?: "0.0"
        service.listWeather(lat = lat, lon = lon).enqueue(object : Callback<OpenWeatherResponse> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                    call: Call<OpenWeatherResponse>,
                    response: Response<OpenWeatherResponse>
            ) {
                val data = response.body()
                val listWeather = data?.daily ?: emptyList()
                weatherAdapter.setList(listWeather)
                binding.tvWeather.text =
                        data?.current?.weather?.get(0)?.description?.capitalizeWords() ?: "-"
                binding.tvDate.text = Utils.timestampToDate(data?.current?.dt ?: 0L)
                binding.tvLocation.text = data?.timezone?.replace("/", ", ") ?: "-"

                val pressure = data?.current?.pressure?.toString()
                binding.tvPressure.text = "${pressure} hPa"

                val humidity = data?.current?.wind_deg ?: 0
                binding.tvHumidity.text = "${humidity}%"
//                        .div(100)

                val celcius = data?.current?.temp ?: 0.0
                val result = celcius.minus(273.15).roundToInt()
                binding.tvPercent.text = "$result C"

                Glide.with(this@MainActivity)
                        .load(Utils.weatherToImage(data?.current?.weather?.get(0)?.main))
                        .into(binding.ivPicture)

                progressDialog.dismiss()
            }

            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                emptyFillAndData()
                progressDialog.dismiss()
                Snackbar.make(binding.root, "Error: " + t.localizedMessage, Snackbar.LENGTH_LONG)
                        .show()
            }
        })
    }
}
