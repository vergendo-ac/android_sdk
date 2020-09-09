package com.ac.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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
import com.doors.api.apis.LocalizerApi
import com.doors.api.infrastructure.ApiClient
import com.doors.api.models.ImageDescription
import com.doors.api.models.ImageDescriptionGps
import com.doors.api.models.LocalizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        const val GPS_REQUEST = 1001
        const val LOCALIZE_INTERVAL = 1500
        const val DEFAULT_LOCATION_UPDATE_INTERVAL = 500L
        const val REQUEST_PERMISSIONS = 1000
        const val SERVER_URL = "http://developer.augmented.city:15000/api/v2"
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
    }

    private lateinit var arFragment: ArFragment
    private lateinit var arSceneView: ArSceneView
    private var lastLocalizeTime: Long = 0

    private lateinit var settingsClient: SettingsClient
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private val apiClient = ApiClient(SERVER_URL)
    private lateinit var context: Context

    private fun isAllPermissionsGranted() =
        PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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

    private fun requestPermissions() {
        if (!isAllPermissionsGranted())
            requestPermissions(this)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this

        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        arFragment.planeDiscoveryController.apply {
            hide()
            setInstructionView(null)
        }

        arSceneView = arFragment.arSceneView
        settingsClient = LocationServices.getSettingsClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        requestPermissions()

        turnGPSOn()

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            DEFAULT_LOCATION_UPDATE_INTERVAL,
            0f,
            locationListener
        )

        arSceneView.scene.addOnUpdateListener { onUpdateFrame(it) }
    }


    private fun onUpdateFrame(frameTime: FrameTime) {
        val frame: Frame = arSceneView.arFrame ?: return
        if (System.currentTimeMillis() - lastLocalizeTime >= LOCALIZE_INTERVAL && lastLocation != null) {
            var imageData: ByteArray? = null
            try {
                val image: Image = frame.acquireCameraImage();
                imageData = image.toByteArray()
                image.close()
                lastLocalizeTime = System.currentTimeMillis()
            } catch (e: NotYetAvailableException) {
                Log.d("DBG", "onUpdateFrame NotYetAvailableException: $e")
            }
            if (imageData != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    val lat: Double =
                        (if (lastLocation != null) lastLocation?.latitude else 0.0) as Double
                    val lon: Double =
                        (if (lastLocation != null) lastLocation?.longitude else 0.0) as Double
                    val result = makeNetworkLocalizeCall(imageData, lat, lon)
                    if (result != null) {
                        Toast.makeText(
                            context,
                            result.toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            }
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
                        }
                    }
                }

        }
    }


    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
            Log.d("DBG", "onLocationChanged: $location")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.d("DBG", "onStatusChanged: $provider")

        }

        override fun onProviderEnabled(provider: String) {
            Log.d("DBG", "onProviderEnabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d("DBG", "onProviderDisabled: $provider")
        }
    }

    //    suspend fun getResult() = Dispatchers.Default {
//        val result: String
//        // make network call
//        return@Default result
//    }
//
    private suspend fun makeNetworkLocalizeCall(image: ByteArray, lat: Double, lon: Double) =
        Dispatchers.Default {

            var result: LocalizationResult? = null
            val webService = apiClient.createService(LocalizerApi::class.java)
            val gps = ImageDescriptionGps(lat.toFloat(), lon.toFloat())
            val imageDesc = ImageDescription(gps)
            val mp = createMultipartBody(image)
            val callResult: Call<LocalizationResult> = webService.localize(imageDesc, mp)
            try {
                val response: Response<LocalizationResult> = callResult.execute()
                result = response.body()
                Log.d("DBG", "LocalizationResult: $result")
            } catch (ex: Exception) {
                Log.d("DBG", "localize error: ${ex.message}")
            }
            return@Default result
        }
}

fun createRequestBody(data: Any): RequestBody {
    return when (data) {
        is ByteArray -> {
            data.toRequestBody("multipart/form-data".toMediaTypeOrNull(), 0, data.size)
        }
        is File -> {
            data.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        }
        is RequestBody -> {
            return data
        }
        else -> throw IllegalArgumentException("Should be File or ByteArray")
    }
}

fun createMultipartBody(
    byteArray: ByteArray
): MultipartBody.Part {
    return MultipartBody.Part.createFormData("image", null, createRequestBody(byteArray))
}

fun Image.toByteArray(): ByteArray {
    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    //U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    // No need to ratate image on device
    //val rotated = rotateNV21(nv21, this.width, this.height, rotationDegrees)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 95, out)
    return out.toByteArray()
}



