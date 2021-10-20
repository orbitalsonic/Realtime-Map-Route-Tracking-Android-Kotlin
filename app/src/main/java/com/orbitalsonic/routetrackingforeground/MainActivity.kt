package com.orbitalsonic.routetrackingforeground

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.orbitalsonic.routetrackingforeground.Constants.LOCATION_PERMISSIONS_REQUEST_CODE
import com.orbitalsonic.routetrackingforeground.Constants.locList
import com.orbitalsonic.routetrackingforeground.Constants.totalDistance
import com.orbitalsonic.routetrackingforeground.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var marker: MarkerOptions? = null

    private lateinit var binding: ActivityMainBinding


    private var locationForegroundService: LocationForegroundService? = null
    private lateinit var foregroundBroadcastReceiver: ForegroundBroadcastReceiver
    private lateinit var  serviceIntent:Intent

    private var foregroundServiceBound = false

    private val foregroundServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationForegroundService.LocalBinder
            locationForegroundService = binder.service
            foregroundServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationForegroundService = null
            foregroundServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        foregroundBroadcastReceiver = ForegroundBroadcastReceiver()
        serviceIntent = Intent(this, LocationForegroundService::class.java)

        initViews()
        onClickMethod()
        if (!locationPermissionApproved()) {
            requestLocationPermissions()
            enableButtons(startBtn = false,stopBtn = false)
        }else{
            enableButtons(startBtn = true,stopBtn = false)

            Handler(Looper.getMainLooper()).postDelayed({
                locationForegroundService?.getCurrentLocation()
            }, 1000)
        }

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

    }

    private fun routeDraw(loc: LatLng) {

        mMap.clear()

        if (locList.size >= 2) {
            totalDistance += getDistance(
                locList[locList.size - 2].latitude,
                locList[locList.size - 2].longitude,
                locList[locList.size - 1].latitude,
                locList[locList.size - 1].longitude
            )
        }

        binding.txtLocDistance.text = String.format("Distance %.2f km", convertMeterToKm(totalDistance))

        val route: Polyline = mMap.addPolyline(PolylineOptions().width(12F).color(Color.BLUE))
        route.points = locList
        marker?.position(loc)
        mMap.addMarker(marker!!)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 17F))


    }


    /***
     * Returns the approximate distance in meters
     * between this location and the given location.
     * Distance is defined using the WGS84 ellipsoid.
     */
    private fun getDistance(
        startLat: Double,
        startLang: Double,
        endLat: Double,
        endLang: Double
    ): Float {
        val locStart = Location("")
        locStart.latitude = startLat
        locStart.longitude = startLang
        val locEnd = Location("")
        locEnd.latitude = endLat
        locEnd.longitude = endLang
        return locStart.distanceTo(locEnd)
    }

    private fun convertMeterToKm(meter: Float): Float {
        return meter / 1000
    }


    private fun initViews() {
        marker = MarkerOptions()
    }


    private fun onClickMethod() {
        binding.btnStartLocationUpdates.setOnClickListener {
            if (locationPermissionApproved()) {
                locationForegroundService?.startLocationButtonClick()

                enableButtons(startBtn = false, stopBtn = true)
            } else {
                requestLocationPermissions()
            }
        }
        binding.btnStopLocationUpdates.setOnClickListener {
            locationForegroundService?.stopLocationButtonClick()
            enableButtons(startBtn = true, stopBtn = false)
        }

    }

    private fun  settingViews(mLocationString:String){

        Log.i("ServiceTesting","SettingViews")

        val splitLocSW = mLocationString.split(",").toTypedArray()
        binding.txtLocationResult.text = mLocationString

        binding.txtLocationResult.alpha = 0f
        binding.txtLocationResult.animate().alpha(1f).duration = 300

        // location last updated time
        binding.txtUpdatedOn.text = "Last updated on: ${DateFormat.getTimeInstance().format(Date())}"

        val lat:Double = splitLocSW[0].toDouble()
        val lng:Double = splitLocSW[1].toDouble()

        routeDraw(LatLng(lat, lng))

    }

    override fun onStart() {
        super.onStart()
        Log.i("ServiceTesting","onStart")
        bindService(serviceIntent, foregroundServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        Log.i("ServiceTesting","onStop")
        if (foregroundServiceBound) {
            unbindService(foregroundServiceConnection)
            foregroundServiceBound = false
        }

        super.onStop()
    }


    override fun onPause() {
        Log.i("ServiceTesting","onPause")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundBroadcastReceiver
        )
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Log.i("ServiceTesting","onResume")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundBroadcastReceiver,
            IntentFilter(
                Constants.ACTION_LOCATION_FOREGROUND_BROADCAST
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
//        setRunningTimer(this,false)
        locationForegroundService?.destroyService()
    }


    private inner class ForegroundBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val mLocation = intent.getStringExtra(Constants.EXTRA_LOCATION)

            if (mLocation != null) {
                settingViews(mLocation)
            }
        }
    }

    private fun enableButtons(startBtn:Boolean,stopBtn:Boolean) {
        binding.btnStartLocationUpdates.isEnabled = startBtn
        binding.btnStopLocationUpdates.isEnabled = stopBtn
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun locationPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestLocationPermissions() {
        val provideRationale = locationPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(binding.activityMap,
                "Location permission needed for tracking the path",
                Snackbar.LENGTH_LONG
            ).setAction("OK") {
                // Request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSIONS_REQUEST_CODE
                )
            }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSIONS_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d("PermissionTag", "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->{
                    // Permission was granted.
                    Log.d("PermissionTag", "Permission was granted")
                    enableButtons(startBtn = true,stopBtn = false)
                    locationForegroundService?.getCurrentLocation()
                }

                else -> {
                    // Permission denied.

                    Snackbar.make(binding.activityMap,
                        "Permission was denied, but is needed for tracking path",
                        Snackbar.LENGTH_LONG
                    ).setAction("Settings") {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }.show()
                }
            }
        }
    }

}