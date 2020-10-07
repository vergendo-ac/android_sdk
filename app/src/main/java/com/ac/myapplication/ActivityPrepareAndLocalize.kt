package com.ac.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ac.api.apis.LocalizerApi
import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Sun
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response



open class ActivityPrepareAndLocalize : AppCompatActivity() {
    val apiClient = ApiClient(SERVER_URL)
    val webService = apiClient.createService(LocalizerApi::class.java)
    lateinit var context: Context
    lateinit var arFragment: ArFragment

    var prepareLocalizationDone: Boolean = false
    var localizeDone: Boolean = false
    var inLocalizeProgressFlag: Boolean = false
    var currentLocation: Location = Location("")

    private lateinit var settingsClient: SettingsClient
    private lateinit var locationManager: LocationManager

    private var lastLocalizeTime: Long = 0

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        arFragment.planeDiscoveryController.apply {
            hide()
            setInstructionView(null)
        }
        settingsClient = LocationServices.getSettingsClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        requestPermissions()
        arFragment.arSceneView.scene.addOnUpdateListener { onUpdateSceneFrame(it) }
    }

    open fun clearSceneObjects() {
        val children: List<Node> = ArrayList(arFragment.arSceneView.scene.children)
        for (node in children) {
            if (node is AnchorNode) {
                node.anchor?.detach()
            }
            if (node !is com.google.ar.sceneform.Camera && node !is Sun) {
                node.setParent(null)
            }
        }
    }



    @ExperimentalCoroutinesApi
    open fun onUpdateSceneFrame(frameTime: FrameTime) {
        arFragment.onUpdate(frameTime)
        val frame: Frame = arFragment.arSceneView.arFrame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            return
        }
        val time = System.currentTimeMillis()
        val delta = time - lastLocalizeTime
        if (frame.camera.trackingState === TrackingState.TRACKING)
        {
            if ((delta >= LOCALIZE_INTERVAL) and (prepareLocalizationDone) and (!inLocalizeProgressFlag)) {
                inLocalizeProgressFlag = true
                var imageData: ByteArray? = null
                try {
                    val image: Image = frame.acquireCameraImage()
                    imageData = image.toByteArray()
                    image.close()
                } catch (e: NotYetAvailableException) {
                }
                if (imageData != null) {
                    localization(imageData)
                } else {
                    inLocalizeProgressFlag = false
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun makeNetworkLocalizeCall(image: ByteArray, location: Location) =
        Dispatchers.Default {
            //var result: LocalizationResult? = null
            val gps = ImageDescriptionGps(location.latitude, location.longitude, location.altitude)
            val imageDesc = ImageDescription(gps, null, null, false, 90)
            val mp = createMultipartBody(image)
            val hint = LocalizationHint(emptyList(), null)

            val callResult: Call<LocalizationResult> = webService.localize(imageDesc, mp, hint)
            try {
                val response: Response<LocalizationResult> = callResult.execute()
                return@Default response
            } catch (ex: Exception) {
                return@Default "Localize error: ${ex.message}"
            }
        }


    @ExperimentalCoroutinesApi
    private fun localization(imageData: ByteArray) {
        CoroutineScope(Dispatchers.Main).launch {
            toast("Localization start", false)
            val location = Location(currentLocation)
            val response = makeNetworkLocalizeCall(imageData, location)
            if (response is String) {
                toast(response)
            } else if (response is Response<*>) {
                if (response.isSuccessful) {
                    val result = response.body() as LocalizationResult
                    if (result.status.code == RESPONSE_STATUS_CODE_OK) {
                        localizeDone = true
                        toast("Localization done")
                    } else {
                        toast(response.toString())
                    }
                    inLocalizeProgressFlag = false
                } else {
                    toast(response.toString())
                }
            }
            lastLocalizeTime = System.currentTimeMillis()
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun makeNetworkPrepareLocalizeCall(lat: Double, lon: Double) =
        Dispatchers.Default {
            val callResult: Call<PrepareResult> = webService.prepare(lat, lon)
            try {
                return@Default callResult.execute()
            } catch (ex: Exception) {
                return@Default "Prepare error: ${ex.message}"
            }
        }


    fun toast(txt: String, isLong: Boolean = true){
        CoroutineScope(Dispatchers.Main.immediate).launch{
            Toast.makeText(
                context,
                txt,
                if(isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    @ExperimentalCoroutinesApi
    private fun prepareLocalization(location: Location) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = makeNetworkPrepareLocalizeCall(location.latitude, location.longitude)
            if (result is String) {
                toast(result)
            }
            if (result is Response<*>) {
                if (result.isSuccessful) {
                    toast(result.body().toString())
                    prepareLocalizationDone = true
                } else {
                    toast( result.toString())
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    protected val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation.set(location)
            if(!prepareLocalizationDone){
                prepareLocalization(location)
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        }
        override fun onProviderEnabled(provider: String) {
        }
        override fun onProviderDisabled(provider: String) {
        }
    }

    @SuppressLint("MissingPermission")
    @ExperimentalCoroutinesApi
    private fun startLocationUpdates() {
        turnGPSOn()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(this)
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            DEFAULT_LOCATION_UPDATE_INTERVAL,
            0f,
            locationListener
        )
    }

    @ExperimentalCoroutinesApi
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    @ExperimentalCoroutinesApi
    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
        prepareLocalizationDone = false
        localizeDone = false

    }


    private fun isAllPermissionsGranted() =
        PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }


    protected fun requestPermissions() {
        if (!isAllPermissionsGranted()) {
            requestPermissions(this)
        }
    }

    private fun shouldShowRationale(activityCompat: Activity) =
        PERMISSIONS.all {
            ActivityCompat.shouldShowRequestPermissionRationale(activityCompat, it)
        }

    private fun requestPermissions(activityCompat: Activity) {
        if (shouldShowRationale(activityCompat)) {
            AlertDialog.Builder(activityCompat)
                .setMessage("This app needs camera permission.")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(
                        activityCompat,
                        PERMISSIONS,
                        REQUEST_PERMISSIONS
                    )
                }
                .setNegativeButton(
                    android.R.string.cancel
                ) { _, _ -> activityCompat.finish() }
                .create().show()
        } else {
            ActivityCompat.requestPermissions(
                activityCompat,
                PERMISSIONS,
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest
            .create()
            .apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = DEFAULT_LOCATION_UPDATE_INTERVAL
                fastestInterval = DEFAULT_LOCATION_UPDATE_INTERVAL / 3
                maxWaitTime = DEFAULT_LOCATION_UPDATE_INTERVAL * 2
            }

    private fun turnGPSOn() {
        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        ) {
            val locationRequest: LocationRequest =
                createLocationRequest()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            builder.setAlwaysShow(true)
            val settings = builder.build()

            settingsClient.checkLocationSettings(settings)
                .addOnSuccessListener(this) {}
                .addOnFailureListener(this) { exception ->
                    when ((exception as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val rae: ResolvableApiException = exception as ResolvableApiException
                            rae.startResolutionForResult(this, GPS_REQUEST)
                        } catch (sie: IntentSender.SendIntentException) {
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage =
                                "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings."
                            Toast.makeText(
                                this,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
