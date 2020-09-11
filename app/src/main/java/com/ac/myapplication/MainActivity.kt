package com.ac.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
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
import android.util.Log
import android.view.View
import android.widget.TextView
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
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import kotlinx.android.synthetic.main.activity_main.*
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
        const val LOCALIZE_INTERVAL = 3000
        const val DEFAULT_LOCATION_UPDATE_INTERVAL = 1000L
        const val REQUEST_PERMISSIONS = 1000
        const val SERVER_URL = "http://developer.augmented.city:5000/api/v2"
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        const val TEXT_FOR_STICKER =  "This is 2D"
        const val MODEL_3D = "object.sfb"
        const val TAG = "MainActivity"
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

    private fun createModelRenderable(){
        ModelRenderable.builder()
            .setSource(arFragment.context, Uri.parse(MODEL_3D))
            .build()
            .thenAcceptAsync { modelRenderable -> this@MainActivity.modelRenderable = modelRenderable }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this

        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        arFragment.planeDiscoveryController.apply {
            hide()
            setInstructionView(null)
        }

        buttonAdd2d.setOnClickListener {
            add2dObject(TEXT_FOR_STICKER)
        }
        buttonAdd3d.setOnClickListener {
            add3dObject()
        }

        createModelRenderable()
        arSceneView = arFragment.arSceneView
        settingsClient = LocationServices.getSettingsClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        requestPermissions()
        arSceneView.scene.addOnUpdateListener { onUpdateFrame(it) }
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
                Log.d(TAG, "LocalizationResult: $result")
            } catch (ex: Exception) {
                Log.d(TAG, "localize error: ${ex.message}")
            }
            return@Default result
        }


    private fun localization(imageData: ByteArray) {
        CoroutineScope(Dispatchers.Main).launch {
            val lat: Double =
                (if (lastLocation != null) lastLocation?.latitude else 0.0) as Double
            val lon: Double =
                (if (lastLocation != null) lastLocation?.longitude else 0.0) as Double
            val result = makeNetworkLocalizeCall(imageData, lat, lon)
            if (result != null) {
                if(result.status.code==0){
                    if(buttonsRow.visibility == View.GONE){
                        Toast.makeText(
                            context,
                            "Localize done",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    buttonsRow.visibility = View.VISIBLE
                }else{
                    Toast.makeText(
                        context,
                        result.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                    buttonsRow.visibility = View.GONE
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

    private fun add2dObject(text: String) {
        ViewRenderable.builder()
            .setView(this, R.layout.sticker)
            .build()
            .thenAccept { viewRenderable ->
                viewRenderable.view.findViewById<TextView>(R.id.text).text = text
                val forward = arSceneView.scene.camera.forward
                val cameraPosition = arSceneView.scene.camera.worldPosition
                val position = cameraPosition + forward
                val direction = cameraPosition - position
                direction.y = position.y

                Node().apply {
                    worldPosition = position
                    renderable = viewRenderable
                    setParent(arSceneView.scene)
                    setLookDirection(direction)
                }
            }
    }

    private fun makeTransformationSystem(): TransformationSystem {
        val footprintSelectionVisualizer = FootprintSelectionVisualizer()
        return TransformationSystem(resources.displayMetrics, footprintSelectionVisualizer)
    }


    private fun add3dObject() {
        ModelRenderable.builder()
            .setSource(arFragment.context, Uri.parse(MODEL_3D))
            .build()
            .thenAcceptAsync { modelRenderable ->
                //val forward = arSceneView.scene.camera.forward
                //val cameraPosition = arSceneView.scene.camera.worldPosition
                //val position = cameraPosition + forward
                //val direction = cameraPosition - position
                //direction.y = position.y


                val forward: Vector3 = arSceneView.scene.camera.forward
                val worldPosition: Vector3 = arSceneView.scene.camera.worldPosition
                val positon = Vector3.add(forward, worldPosition)

                val direction = Vector3.subtract(worldPosition, positon)
                direction.y = positon.y

                val transformationSystem = makeTransformationSystem()
                val transformableNode = TransformableNode(transformationSystem).apply {
                    rotationController.isEnabled = true
                    scaleController.isEnabled = true
                    translationController.isEnabled = false // not support
                    setParent(arSceneView.scene)
                    this.renderable = modelRenderable // Build using CompletableFuture
                }
                transformableNode.select()

                arSceneView.scene.addOnPeekTouchListener { hitTestResult, motionEvent ->
                    transformationSystem.onTouch(hitTestResult, motionEvent)
                }
            }
    }


    private fun onUpdateFrame(frameTime: FrameTime) {
        val frame: Frame = arSceneView.arFrame ?: return
        val time = System.currentTimeMillis()
        val delta = time - lastLocalizeTime
        if (delta >= LOCALIZE_INTERVAL && lastLocation != null && prepareLocalizationDone && !inLocalizeProgressFlag) {
            inLocalizeProgressFlag = true
            var imageData: ByteArray? = null
            try {
                val image: Image = frame.acquireCameraImage();
                imageData = image.toByteArray()
                image.close()
            } catch (e: NotYetAvailableException) {
                Log.d(TAG, "NotYetAvailableException: $e")
            }
            if (imageData != null) {
                localization(imageData)
            } else {
                inLocalizeProgressFlag = false
            }
        }

//TODO NOT REMOVE
//            //get the trackables to ensure planes are detected
//            val var3 = frame.getUpdatedTrackables(Plane::class.java).iterator()
//            while (var3.hasNext()) {
//                val plane = var3.next() as Plane
//
//                //If a plane has been detected & is being tracked by ARCore
//                if (plane.trackingState == TrackingState.TRACKING) {
//
//
//                    //Get all added anchors to the frame
//                    val iterableAnchor = frame.updatedAnchors.iterator()
//
//                    //place the first object only if no previous anchors were added
//                    if (!iterableAnchor.hasNext()) {
//                        //Perform a hit test at the center of the screen to place an object without tapping
//
//                        val point = getScreenCenter()
//                        val hitTest = frame.hitTest(point.x.toFloat(), point.y.toFloat())
//
//                        //iterate through all hits
//                        val hitTestIterator = hitTest.iterator()
//                        while (hitTestIterator.hasNext()) {
//                            val hitResult = hitTestIterator.next()
//
//                            //Create an anchor at the plane hit
//                            val modelAnchor = plane.createAnchor(hitResult.hitPose)
//
//                            //Attach a node to this anchor with the scene as the parent
//                            val anchorNode = AnchorNode(modelAnchor)
//                            anchorNode.setParent(arSceneView.scene)
//
//                            //create a new TranformableNode that will carry our object
//                            val transformableNode = TransformableNode(arFragment.transformationSystem)
//                            transformableNode.setParent(anchorNode)
//                            transformableNode.renderable = this@MainActivity.modelRenderable
//
//                            //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
//                            transformableNode.worldPosition = Vector3(
//                                modelAnchor.pose.tx(),
//                                modelAnchor.pose.compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(),
//                                modelAnchor.pose.tz()
//                            )
//                        }
//                    }
//                }
//            }

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

    private fun getScreenCenter(): Point {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
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
            if (lastLocation == null) {
                prepareLocalization(location)
            }
            lastLocation = location
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

fun createMultipartBody(
    byteArray: ByteArray
): MultipartBody.Part {
    return MultipartBody.Part.createFormData("image", null, createRequestBody(byteArray))
}

fun Image.toByteArray(): ByteArray {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 95, out)
    return out.toByteArray()
}



operator fun Vector3.plus(other: Vector3): Vector3 {
    return Vector3.add(this, other)
}

operator fun Vector3.minus(other: Vector3): Vector3 {
    return Vector3.subtract(this, other)
}
