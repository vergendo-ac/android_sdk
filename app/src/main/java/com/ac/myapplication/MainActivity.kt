package com.ac.myapplication

import android.Manifest
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
import android.widget.TextView
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
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.*
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.FixedWidthViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File


data class Placeholder(val position: Vector3d = Vector3d(0.0f, 0.0f,0.0f),
                       val id: String = "")


class MainActivity : AppCompatActivity()
{
    companion object {
        const val RESPONSE_STATUS_CODE_OK = 0
        const val GPS_REQUEST = 1001
        const val LOCALIZE_INTERVAL = 3000
        const val DEFAULT_LOCATION_UPDATE_INTERVAL = 1000L
        const val REQUEST_PERMISSIONS = 1000
        const val SERVER_URL = "http://developer.augmented.city:5000/api/v2"
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        const val TAG = "MainActivity"
        const val STICKER_WIDTH_IN_METERS = 0.3f
    }

    private lateinit var context: Context
    private val apiClient = ApiClient(SERVER_URL)
    private lateinit var arFragment: ArFragment
    private lateinit var arSceneView: ArSceneView
    private lateinit var syncPose: Pose

    private var inLocalizeProgressFlag: Boolean = false
    private var lastLocalizeTime: Long = 0
    private var currentLocation: Location? = null

    private lateinit var settingsClient: SettingsClient
    private lateinit var locationManager: LocationManager
    private var prepareLocalizationDone: Boolean = false


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
        arSceneView.planeRenderer.isEnabled = false

        settingsClient = LocationServices.getSettingsClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        requestPermissions()
        arSceneView.scene.addOnUpdateListener { onUpdateSceneFrame(it) }
    }

    private suspend fun makeNetworkPrepareLocalizeCall(lat: Double, lon: Double) =
        Dispatchers.Default {
            var result: PrepareResult? = null
            val webService = apiClient.createService(LocalizerApi::class.java)
            val callResult: Call<PrepareResult> = webService.prepare(lat, lon)
            try {
                val response: Response<PrepareResult> = callResult.execute()
                result = response.body()
                Log.d(TAG, "PrepareResult: $result")

            } catch (ex: Exception) {
                Log.d(TAG, "prepare error: ${ex.message}")
            }
            return@Default result
        }


    private suspend fun makeNetworkLocalizeCall(image: ByteArray, location: Location) =
        Dispatchers.Default {
            var result: LocalizationResult? = null
            val webService = apiClient.createService(LocalizerApi::class.java)
            val gps = ImageDescriptionGps(location.latitude, location.longitude, location.altitude)
            val imageDesc = ImageDescription(gps, null, null, 90)
            val mp = createMultipartBody(image)
            val callResult: Call<LocalizationResult> = webService.localize(imageDesc, mp)
            try {
                val response: Response<LocalizationResult> = callResult.execute()
                result = response.body()
                Log.d(TAG, "LocalizationResult: $result")
            } catch (ex: Exception) {
                Log.d(TAG, "localize error: ${ex.message}")
            }
            return@Default result
        }

    private fun clearSceneObjects() {
        val children: List<Node> = ArrayList(arSceneView.scene.children)
        for (node in children) {
            if (node is AnchorNode) {
                node.anchor?.detach()
            }
            if (node !is Camera && node !is Sun) {
                node.setParent(null)
            }
        }
    }

    private fun localization(imageData: ByteArray) {
        CoroutineScope(Dispatchers.Main).launch {
            val location = Location(currentLocation)
            val response = makeNetworkLocalizeCall(imageData, location)
            if (response != null) {
                if (response.status.code == RESPONSE_STATUS_CODE_OK) {
                    val camera = response.camera

                    val points: List<Placeholder> =
                        response.objects!!.mapNotNull { objectInfo ->
                            val id = objectInfo.placeholder.placeholderId
                            val node =
                                response.placeholders!!.singleOrNull { it.placeholderId == id }
                            node
                        }.map {
                            val position = it.pose.position.toFloatArray().asList().toVector3d()
                            Placeholder(position, it.placeholderId)
                        }.toList()

                    val matrix = srvToLocalTransform(
                        Pose.IDENTITY.toMat4(),
                        Pose(
                            camera!!.pose.position.toFloatArray(),
                            camera.pose.orientation.toFloatArray()
                        ).toMat4(), 1.0f
                    )

                    val objectsToPlace = points.map {
                        val pos = it.position.toLocal(matrix)
                        when (it) {
                            is Placeholder -> it.copy(position = pos)
                            else -> IllegalStateException()
                        } as Placeholder
                    }.toTypedArray()

                    clearSceneObjects()

                    getStickerText(response, "")

                    var cnt = 0
                    objectsToPlace.iterator().forEach {
                        Log.d(TAG, "object[" + cnt + "] = " + it.position)
                        add2dObjectPos(getStickerText(response,it.id), it.position)
                        cnt++
                    }
                    Toast.makeText(
                        context,
                        "Localization done",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        response.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
                lastLocalizeTime = System.currentTimeMillis()
                inLocalizeProgressFlag = false
            }
        }
    }

    private fun prepareLocalization(location: Location) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = makeNetworkPrepareLocalizeCall(location.latitude, location.longitude)
            if (result != null) {
                Toast.makeText(
                    context,
                    result.toString(),
                    Toast.LENGTH_LONG
                ).show()
                prepareLocalizationDone = true
            }
        }
    }

    private fun getStickerText(resp: LocalizationResult, id: String): String{
        var ret: String = ""
        if(!resp.objects.isNullOrEmpty()){
            resp.objects!!.iterator().forEach {
                if(it.placeholder.placeholderId == id){
                    ret = (if(it.sticker.get("sticker_text")!=null) it.sticker.get("sticker_text") else "") as String
                    return ret
                }
            }
        }
        return ret;
    }

    private fun add2dObjectPos(text: String, pos: Vector3d) {
        ViewRenderable.builder()
            .setView(this, R.layout.layout_sticker)
            .build()
            .thenAccept { it ->
                it.view.findViewById<TextView>(R.id.text).text = text
                val pos: Pose = syncPose.compose(
                    Pose.makeTranslation(
                        pos.x,
                        pos.y,
                        pos.z
                    )
                )
                val anchor: Anchor = arSceneView.session!!.createAnchor(pos)
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arSceneView.scene)
                Node().apply {
                    renderable = it
                    (renderable as ViewRenderable).sizer = FixedWidthViewSizer(STICKER_WIDTH_IN_METERS)
                    setParent(anchorNode)
                    localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 0f, 1f), 90f)

                }
            }
    }

    private fun onUpdateSceneFrame(frameTime: FrameTime) {
        val frame: Frame = arSceneView.arFrame ?: return
        val time = System.currentTimeMillis()
        val delta = time - lastLocalizeTime
        if (frame.camera.trackingState === TrackingState.TRACKING) {
            if (delta >= LOCALIZE_INTERVAL && currentLocation != null && prepareLocalizationDone && !inLocalizeProgressFlag) {
                inLocalizeProgressFlag = true
                var imageData: ByteArray? = null
                try {
                    val image: Image = frame.acquireCameraImage()
                    imageData = image.toByteArray()
                    image.close()
                    syncPose = frame.camera.pose
                } catch (e: NotYetAvailableException) {
                    Log.d(TAG, "NotYetAvailableException: $e")
                }
                if (imageData != null) {
                    localization(imageData)
                } else {
                    inLocalizeProgressFlag = false
                }
            }
        }
    }

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

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

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

    private fun requestPermissions() {
        if (!isAllPermissionsGranted()) {
            requestPermissions(this)
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

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (currentLocation == null) {
                prepareLocalization(location)
            }
            currentLocation = location
            Log.d(TAG, "onLocationChanged: $location")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.d(TAG, "onStatusChanged: $provider")

        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "onProviderEnabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "onProviderDisabled: $provider")
        }
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


