package com.ac.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.YuvImage
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.doors.api.apis.LocalizerApi
import com.doors.api.infrastructure.ApiClient
import com.doors.api.models.ImageDescription
import com.doors.api.models.ImageDescriptionGps
import com.doors.api.models.LocalizationResult
import com.doors.api.models.PrepareResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.ar.core.*
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
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
import java.util.concurrent.CompletionException


class MainActivity : AppCompatActivity() {

    companion object {
        const val GPS_REQUEST = 1001
        const val LOCALIZE_INTERVAL = 4000
        const val DEFAULT_LOCATION_UPDATE_INTERVAL = 1000L
        const val REQUEST_PERMISSIONS = 1000
        const val SERVER_URL = "http://developer.augmented.city:5000/api/v2"
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        const val MODEL_3D = "Heart.sfb"
    }


    private lateinit var arFragment: ArFragment
    private lateinit var arSceneView: ArSceneView
    private var lastLocalizeTime: Long = 0
    private var inLocalizeProgressFlag: Boolean = false


    private lateinit var settingsClient: SettingsClient
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private val apiClient = ApiClient(SERVER_URL)
    private lateinit var context: Context
    private var prepareLocalizationDone: Boolean = false

    private var modelRenderable: ModelRenderable? = null






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

        createModelRenderable()
        arSceneView.scene.addOnUpdateListener { onUpdateFrame(it) }



    }

    private fun createModelRenderable(){
        ModelRenderable.builder()
            //get the context of the ARFragment and pass the name of your .sfb file
            .setSource(arFragment.context, Uri.parse(MODEL_3D))
            .build()
            //I accepted the CompletableFuture using Async since I created my model on creation of the activity. You could simply use .thenAccept too.
            //Use the returned modelRenderable and save it to a global variable of the same name
            .thenAcceptAsync { modelRenderable -> this@MainActivity.modelRenderable = modelRenderable }
    }

    private suspend fun makeNetworkPrepareLocalizeCall(lat: Double, lon: Double) =
        Dispatchers.Default {
            var result: PrepareResult? = null
            val webService = apiClient.createService(LocalizerApi::class.java)
            val callResult : Call<PrepareResult> = webService.prepare(lat, lon)
            try {
                val response: Response<PrepareResult> = callResult.execute()
                result = response.body()
                Log.d("DBG", "PrepareResult: $result")

            } catch (ex: Exception) {
                Log.d("DBG", "prepare error: ${ex.message}")
            }
            return@Default result
        }



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


    private fun localization(imageData: ByteArray){
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
                lastLocalizeTime = System.currentTimeMillis()
                inLocalizeProgressFlag = false
            }
        }
    }

    private fun prepareLocalization(location: Location){
        CoroutineScope(Dispatchers.Main).launch {
            val result = makeNetworkPrepareLocalizeCall( location.latitude, location.longitude)
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


    private fun onUpdateFrame(frameTime: FrameTime) {
// TODO NOT REMOVE
//        val frame: Frame = arSceneView.arFrame ?: return
//        val time = System.currentTimeMillis()
//        val delta = time - lastLocalizeTime
//        if ( delta >= LOCALIZE_INTERVAL && lastLocation != null && prepareLocalizationDone && !inLocalizeProgressFlag) {
//            inLocalizeProgressFlag = true
//            var imageData: ByteArray? = null
//            try {
//                val image: Image = frame.acquireCameraImage();
//                imageData = image.toByteArray()
//                image.close()
//            } catch (e: NotYetAvailableException) {
//                Log.d("DBG", "onUpdateFrame NotYetAvailableException: $e")
//            }
//            if (imageData != null) {
//                localization(imageData)
//            }else{
//                inLocalizeProgressFlag = false
//            }
//        }

        val frame: Frame = arSceneView.arFrame ?: return
        if (frame != null) {
            //get the trackables to ensure planes are detected
            val var3 = frame.getUpdatedTrackables(Plane::class.java).iterator()
            while (var3.hasNext()) {
                val plane = var3.next() as Plane

                //If a plane has been detected & is being tracked by ARCore
                if (plane.trackingState == TrackingState.TRACKING) {


                    //Get all added anchors to the frame
                    val iterableAnchor = frame.updatedAnchors.iterator()

                    //place the first object only if no previous anchors were added
                    if (!iterableAnchor.hasNext()) {
                        //Perform a hit test at the center of the screen to place an object without tapping

                        val point = getScreenCenter()
                        val hitTest = frame.hitTest(point.x.toFloat(), point.y.toFloat())

                        //iterate through all hits
                        val hitTestIterator = hitTest.iterator()
                        while (hitTestIterator.hasNext()) {
                            val hitResult = hitTestIterator.next()

                            //Create an anchor at the plane hit
                            val modelAnchor = plane.createAnchor(hitResult.hitPose)

                            //Attach a node to this anchor with the scene as the parent
                            val anchorNode = AnchorNode(modelAnchor)
                            anchorNode.setParent(arSceneView.scene)

                            //create a new TranformableNode that will carry our object
                            val transformableNode = TransformableNode(arFragment.transformationSystem)
                            transformableNode.setParent(anchorNode)
                            transformableNode.renderable = this@MainActivity.modelRenderable

                            //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
                            transformableNode.worldPosition = Vector3(
                                modelAnchor.pose.tx(),
                                modelAnchor.pose.compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
                                modelAnchor.pose.tz()
                            )
                        }
                    }
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
            if (lastLocation == null) {

                prepareLocalization(location)
            }
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




    private fun addObject(parse: Uri) {
        val frame = arSceneView.arFrame
        val point = getScreenCenter()
        if (frame != null) {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    placeObject(hit.createAnchor(), parse)
                    break
                }
            }
        }
    }

    private fun placeObject(createAnchor: Anchor, model: Uri) {
        ModelRenderable.builder()
            .setSource(this, model)
            .build()
            .thenAccept {
                addNodeToScene(createAnchor, it)
            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message)
                    .setTitle("error!")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun addNodeToScene(createAnchor: Anchor, renderable: ModelRenderable) {
        val anchorNode = AnchorNode(createAnchor)
        val transformableNode = TransformableNode(arFragment.transformationSystem)
        transformableNode.renderable = renderable
        transformableNode.setParent(anchorNode)
        arFragment.arSceneView.scene.addChild(anchorNode)
        transformableNode.select()
    }

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
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



