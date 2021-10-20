package com.orbitalsonic.routetrackingforeground

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.orbitalsonic.routetrackingforeground.Constants.ACTION_LOCATION_FOREGROUND_BROADCAST
import com.orbitalsonic.routetrackingforeground.Constants.EXTRA_LOCATION
import com.orbitalsonic.routetrackingforeground.Constants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
import com.orbitalsonic.routetrackingforeground.Constants.UPDATE_INTERVAL_IN_MILLISECONDS
import com.orbitalsonic.routetrackingforeground.Constants.locList
import java.util.*

class LocationForegroundService : Service() {
    private val TAG = "LocationTag"

    private var isServiceRunningInForeground = false
    private val localBinder = LocalBinder()
    private lateinit var notificationUtils: NotificationUtils

    private var  mIntent:Intent =Intent(ACTION_LOCATION_FOREGROUND_BROADCAST)


    // bunch of location related apis
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null

    private var mRequestingLocationUpdates: Boolean? = null

    override fun onCreate() {
        super.onCreate()

        notificationUtils = NotificationUtils(this)

        initValues()


        if (isServiceRunningInForeground) {
            notificationUtils.launchNotification()
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val cancelLocationTrackingFromNotification =
            intent.getBooleanExtra(
                notificationUtils.EXTRA_CANCEL_LOCATION_FROM_NOTIFICATION,
                false
            )

        if (cancelLocationTrackingFromNotification) {
            stopLocationButtonClick()
            stopSelf()
        }
        // Tells the system not to recreate the service after it's been killed.
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder {
        stopForeground(true)
        isServiceRunningInForeground = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {

        stopForeground(true)
        isServiceRunningInForeground = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {

        startForeground(notificationUtils.NOTIFICATION_ID, notificationUtils.getNotification())
        isServiceRunningInForeground = true

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }



    fun destroyService(){
        stopLocationButtonClick()
        stopSelf()
    }



    private fun initValues() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                // location is received
                mCurrentLocation = locationResult.lastLocation
                updateLocationUI()
            }
        }
        mRequestingLocationUpdates = false
        mLocationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            isWaitForAccurateLocation = true
        }

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    fun getCurrentLocation() {

        mSettingsClient
            ?.checkLocationSettings(mLocationSettingsRequest!!)
            ?.addOnSuccessListener{

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                }
//                mFusedLocationClient!!.requestLocationUpdates(
//                    mLocationRequest!!,
//                    mLocationCallback!!, Looper.myLooper()!!
//                )

                mFusedLocationClient!!.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    null
                )
                mFusedLocationClient!!.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mIntent.putExtra(EXTRA_LOCATION, "${location.latitude},${location.longitude }")
                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(mIntent)
                        }
                    }

            }
            ?.addOnFailureListener { e ->
                val statusCode = (e as ApiException).statusCode

            }
    }

    private fun updateLocationUI() {
        if (mCurrentLocation != null) {


            locList.add(LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude))

            mIntent.putExtra(EXTRA_LOCATION, "${mCurrentLocation!!.latitude},${mCurrentLocation!!.longitude }")
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(mIntent)
        }

    }

    fun startLocationButtonClick() {

        startService(Intent(applicationContext, LocationForegroundService::class.java))
        mRequestingLocationUpdates = true
        startLocationUpdates()

    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private fun startLocationUpdates() {
        mSettingsClient
            ?.checkLocationSettings(mLocationSettingsRequest!!)
            ?.addOnSuccessListener {
                Log.i(TAG, "All location settings are satisfied.")

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                }
                mFusedLocationClient!!.requestLocationUpdates(
                    mLocationRequest!!,
                    mLocationCallback!!, Looper.myLooper()!!
                )
                updateLocationUI()
            }
            ?.addOnFailureListener { e ->
                val statusCode = (e as ApiException).statusCode

            }
    }



    fun stopLocationButtonClick() {
        mRequestingLocationUpdates = false
        stopSelf()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
            ?.removeLocationUpdates(mLocationCallback!!)
    }


    inner class LocalBinder : Binder() {
        internal val service: LocationForegroundService
            get() = this@LocationForegroundService
    }

}