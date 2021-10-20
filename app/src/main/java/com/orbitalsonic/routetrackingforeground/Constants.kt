package com.orbitalsonic.routetrackingforeground

import com.google.android.gms.maps.model.LatLng

object Constants {

    const val LOCATION_PERMISSIONS_REQUEST_CODE = 34

    const val ACTION_LOCATION_FOREGROUND_BROADCAST =
        "action.FOREGROUND_LOCATION_BROADCAST"

    const val EXTRA_LOCATION = "extra.LOCATION"


    // location last updated time
    var mLastUpdateTime: String? = null

    var totalDistance: Float = 0.0F

    var locList: ArrayList<LatLng> = ArrayList()

}