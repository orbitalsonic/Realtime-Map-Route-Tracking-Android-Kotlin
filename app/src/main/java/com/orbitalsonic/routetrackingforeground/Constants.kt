package com.orbitalsonic.routetrackingforeground

import com.google.android.gms.maps.model.LatLng

object Constants {

    const val LOCATION_PERMISSIONS_REQUEST_CODE = 34

    const val ACTION_LOCATION_FOREGROUND_BROADCAST =
        "action.FOREGROUND_LOCATION_BROADCAST"

    const val EXTRA_LOCATION = "extra.LOCATION"

    // location updates interval - 10sec
    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 3000


    // location last updated time
    var mLastUpdateTime: String? = null

    var totalDistance: Float = 0.0F

    var locList: ArrayList<LatLng> = ArrayList()

}