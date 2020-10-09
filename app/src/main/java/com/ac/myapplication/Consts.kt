package com.ac.myapplication

import android.Manifest


const val REQUEST_PERMISSIONS = 1000
const val DEFAULT_LOCATION_UPDATE_INTERVAL = 5000L
const val RESPONSE_STATUS_CODE_OK = 0
const val GPS_REQUEST = 1001


val PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.CAMERA
)

