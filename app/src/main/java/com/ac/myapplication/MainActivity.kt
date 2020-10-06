package com.ac.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.media.MediaPlayer
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
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response


data class ArObject(
    val position: Vector3d = Vector3d(0.0f, 0.0f, 0.0f),
    val id: String = "",
    var txt: String,
    var node: Node?
)

private enum class ObjectType {
    OBJ_2D, OBJ_3D, OBJ_VID, OBJ_VID_CHR
}


class MainActivity : AppCompatActivity() {
    private val USE_OBJ = ObjectType.OBJ_VID

    companion object {
        const val RESPONSE_STATUS_CODE_OK = 0
        const val GPS_REQUEST = 1001
        const val LOCALIZE_INTERVAL = 15000
        const val DEFAULT_LOCATION_UPDATE_INTERVAL = 5000L
        const val REQUEST_PERMISSIONS = 1000
        const val SERVER_URL = "http://developer.augmented.city/api/v2"
        private val CHROMA_KEY_COLOR =
            Color(0.1843f, 1.0f, 0.098f)

        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        const val TAG = "MainActivity"
        const val STICKER_WIDTH_IN_METERS = 1f
        const val VIDEO_HEIGHT_CHR = 1f
        const val VIDEO_HEIGHT = 1f

        const val VIDEO_SOURCE = "https://firebasestorage.googleapis.com/v0/b/video-storage-7afa1.appspot.com/o/BigBuckBunny.mp4?alt=media&token=cd0b0144-0f40-4abe-9a3c-63f85e19698a"

    }

    private lateinit var context: Context
    private val apiClient = ApiClient(SERVER_URL)
    private val webService = apiClient.createService(LocalizerApi::class.java)

    private lateinit var arFragment: ArFragment
    private lateinit var arSceneView: ArSceneView
    private lateinit var syncPose: Pose

    private var localizeDone: Boolean = false
    private var inLocalizeProgressFlag: Boolean = false
    private var lastLocalizeTime: Long = 0
    private var currentLocation: Location? = null

    private lateinit var settingsClient: SettingsClient
    private lateinit var locationManager: LocationManager
    private var prepareLocalizationDone: Boolean = false
    private var sceneObjects = mutableMapOf<String, ArObject>()


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

        arSceneView = arFragment.arSceneView

        arSceneView.planeRenderer.isEnabled = false

        settingsClient = LocationServices.getSettingsClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        requestPermissions()
        arSceneView.scene.addOnUpdateListener { onUpdateSceneFrame(it) }
    }

    @ExperimentalCoroutinesApi
    private suspend fun makeNetworkPrepareLocalizeCall(lat: Double, lon: Double) =
        Dispatchers.Default {
            val callResult: Call<PrepareResult> = webService.prepare(lat, lon)
            try {
                return@Default callResult.execute()
            } catch (ex: Exception) {
                Log.d(TAG, "Prepare error: ${ex.message}")
                return@Default "Prepare error: ${ex.message}"
            }
        }


    @ExperimentalCoroutinesApi
    private suspend fun makeNetworkLocalizeCall(image: ByteArray, location: Location) =
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
                Log.d(TAG, "Localize error: ${ex.message}")
                return@Default "Localize error: ${ex.message}"
            }
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

    @ExperimentalCoroutinesApi
    private fun localization(imageData: ByteArray) {
        CoroutineScope(Dispatchers.Main).launch {

            Toast.makeText(
                context,
                "Localization start",
                Toast.LENGTH_SHORT
            ).show()

            val location = Location(currentLocation)
            val response = makeNetworkLocalizeCall(imageData, location)
            if (response is String) {
                Toast.makeText(
                    context,
                    response,
                    Toast.LENGTH_SHORT
                ).show()
            } else if (response is Response<*>) {
                if (response.isSuccessful) {
                    val result = response.body() as LocalizationResult
                    if (result.status.code == RESPONSE_STATUS_CODE_OK) {
                        Toast.makeText(
                            context,
                            "RESPONSE_STATUS_CODE_OK",
                            Toast.LENGTH_SHORT
                        ).show()

                        val camera = result.camera
                        val points: List<ArObject> =
                            result.objects!!.mapNotNull { objectInfo ->
                                val id = objectInfo.placeholder.placeholderId
                                val node =
                                    result.placeholders!!.singleOrNull { it.placeholderId == id }
                                node
                            }.map {
                                val position = it.pose.position.toFloatArray().asList().toVector3d()
                                ArObject(position, it.placeholderId, "", null)
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
                            it.copy(position = pos)
                        }.toTypedArray()

                        objectsToPlace.iterator().forEach {
                            it.txt = getStickerText(result, it.id)
                            val inScene = sceneObjects[it.id]
                            if (inScene == null) {
                                sceneObjects[it.id] = it

                                when(USE_OBJ){
                                    ObjectType.OBJ_2D -> add2dObject(it)
                                    ObjectType.OBJ_3D -> add3dObject(it)
                                    ObjectType.OBJ_VID_CHR -> addVideoChr(it)
                                    ObjectType.OBJ_VID -> addVideo(it)
                                }
                            }
                        }
                        localizeDone = true
                        Toast.makeText(
                            context,
                            "Localization done",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //clearSceneObjects()
                        //sceneObjects.clear()
                        Toast.makeText(
                            context,
                            response.toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    lastLocalizeTime = System.currentTimeMillis()
                    inLocalizeProgressFlag = false

                } else {
                    Toast.makeText(
                        context,
                        response.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }

        }
    }


    private fun getStickerText(resp: LocalizationResult, id: String): String {
        var ret: String = ""
        if (!resp.objects.isNullOrEmpty()) {
            resp.objects!!.iterator().forEach {
                if (it.placeholder.placeholderId == id) {
                    ret =
                        (if (it.sticker["stickerText"] != null) it.sticker["stickerText"] else "null") as String
                    return ret
                }
            }
        }
        return ret
    }

    private fun update2dObjectPos(obj: ArObject) {
        if (obj.node != null) {
            val node = obj.node
            val trPos: Pose = syncPose.compose(
                Pose.makeTranslation(
                    obj.position.x,
                    obj.position.y,
                    obj.position.z
                )
            )
            val anchor: Anchor = arSceneView.session!!.createAnchor(trPos)
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arSceneView.scene)
            node!!.setParent(anchorNode)
        }
    }

    private fun add3dObject(obj: ArObject) {
        ModelRenderable.builder()
            .setSource(this, R.raw.star)
            .build()
            .thenAccept {
                val trPos: Pose = syncPose.compose(
                    Pose.makeTranslation(
                        obj.position.x,
                        obj.position.y,
                        obj.position.z
                    )
                )
                val anchor: Anchor = arSceneView.session!!.createAnchor(trPos)
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arSceneView.scene)

                val node = TransformableNode(arFragment.transformationSystem)
                node.apply {
                    localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 1f, 1f), 90f)
                    renderable = it
                    setParent(anchorNode)
                    select()
                }
            }

    }


    private fun addVideo(obj: ArObject) {
        ModelRenderable.builder()
            .setSource(this, R.raw.video)
            .build()
            .thenAccept {
                val texture = ExternalTexture()
                val mediaPlayer = MediaPlayer()
                mediaPlayer.apply {
                    setSurface(texture.surface)
                    isLooping = true
                    try {
                        mediaPlayer.setDataSource(VIDEO_SOURCE)
                        mediaPlayer.prepare()
                    } catch (e: Exception) {
                        Log.d(TAG, " mediaPlayer.setDataSource error: " + e.message)
                    }
                }

                it.material.setExternalTexture("videoTexture", texture)

                val trPos: Pose = syncPose.compose(
                    Pose.makeTranslation(
                        obj.position.x,
                        obj.position.y,
                        obj.position.z
                    )
                )

                val anchor: Anchor = arSceneView.session!!.createAnchor(trPos)
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arSceneView.scene)

                val videoWidth = mediaPlayer.videoWidth.toFloat()
                val videoHeight = mediaPlayer.videoHeight.toFloat()

                val videoNode = Node()
                videoNode.apply {
                    localScale = Vector3(
                        VIDEO_HEIGHT * (videoWidth / videoHeight),
                        VIDEO_HEIGHT,
                        1.0f
                    )

                    videoNode.localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 0f, 1f), 90f)

                    setParent(anchorNode)
                }

                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    val surfTex = texture.surfaceTexture
                    surfTex.setOnFrameAvailableListener { texture: SurfaceTexture? ->
                        videoNode.renderable = it
                        texture?.setOnFrameAvailableListener(null)
                    }
                } else {
                    videoNode.renderable = it
                }
                obj.node = videoNode
            }
    }

    private fun addVideoChr(obj: ArObject) {
        ModelRenderable.builder()
            .setSource(this, R.raw.video_chr)
            .build()
            .thenAccept {
                val texture = ExternalTexture()
                val mediaPlayer = MediaPlayer.create(context, R.raw.lion_chroma)
                mediaPlayer.setSurface(texture.surface)
                mediaPlayer.isLooping = true

                it.material.setExternalTexture("videoTexture", texture)
                it.material.setFloat4("keyColor", CHROMA_KEY_COLOR)

                val trPos: Pose = syncPose.compose(
                    Pose.makeTranslation(
                        obj.position.x,
                        obj.position.y,
                        obj.position.z
                    )
                )

                val anchor: Anchor = arSceneView.session!!.createAnchor(trPos)
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arSceneView.scene)

                val videoWidth = mediaPlayer.videoWidth.toFloat()
                val videoHeight = mediaPlayer.videoHeight.toFloat()

                val videoNode = Node()
                videoNode.apply {
                    localScale = Vector3(
                        VIDEO_HEIGHT_CHR * (videoWidth / videoHeight),
                        VIDEO_HEIGHT_CHR,
                        1.0f
                    )
                    localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 0f, 1f), 90f)

                    setParent(anchorNode)
                }

                // Start playing the video when the first node is placed.
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    // Wait to set the renderable until the first frame of the  video becomes available.
                    // This prevents the renderable from briefly appearing as a black quad before the video
                    // plays.
                    texture
                        .surfaceTexture
                        .setOnFrameAvailableListener { surface: SurfaceTexture? ->
                            videoNode.renderable = it
                            texture.surfaceTexture.setOnFrameAvailableListener(null)
                        }
                } else {
                    videoNode.renderable = it
                }
                obj.node = videoNode

                Toast.makeText(
                    context,
                    "addVideoLion done",
                    Toast.LENGTH_SHORT
                ).show()

            }.exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message)
                    .setTitle("error!")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun add2dObject(obj: ArObject) {
        ViewRenderable.builder()
            .setView(this, R.layout.layout_sticker)
            .build()
            .thenAccept {
                it.view.findViewById<TextView>(R.id.text).text = obj.txt
                val trPos: Pose = syncPose.compose(
                    Pose.makeTranslation(
                        obj.position.x,
                        obj.position.y,
                        obj.position.z
                    )
                )
                val anchor: Anchor = arSceneView.session!!.createAnchor(trPos)
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arSceneView.scene)
                val nodeAr = Node().apply {
                    renderable = it
                    (renderable as ViewRenderable).sizer =
                        FixedWidthViewSizer(STICKER_WIDTH_IN_METERS)
                    setParent(anchorNode)

                    localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 0f, 1f), 90f)

                }
                obj.node = nodeAr

                Toast.makeText(
                    context,
                    "add2dObject done",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    @ExperimentalCoroutinesApi
    private fun onUpdateSceneFrame(frameTime: FrameTime) {
        arFragment.onUpdate(frameTime)
        val frame: Frame = arSceneView.arFrame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            return
        }
        val time = System.currentTimeMillis()
        val delta = time - lastLocalizeTime
        if (frame.camera.trackingState === TrackingState.TRACKING)
        {
            if ((!localizeDone) and (delta >= LOCALIZE_INTERVAL) and (currentLocation != null) and (prepareLocalizationDone) and (!inLocalizeProgressFlag)) {
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

    @ExperimentalCoroutinesApi
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

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

    @ExperimentalCoroutinesApi
    private fun prepareLocalization(location: Location) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = makeNetworkPrepareLocalizeCall(location.latitude, location.longitude)
            if (result is String) {
                Toast.makeText(
                    context,
                    result,
                    Toast.LENGTH_LONG
                ).show()
            }
            if (result is Response<*>) {
                if (result.isSuccessful) {
                    Toast.makeText(
                        context,
                        result.body().toString(),
                        Toast.LENGTH_LONG
                    ).show()
                    prepareLocalizationDone = true
                } else {
                    Toast.makeText(
                        context,
                        result.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
    }

    private fun prepareDone() {
        Toast.makeText(
            context,
            "prepareLocalization done",
            Toast.LENGTH_LONG
        ).show()
        prepareLocalizationDone = true
    }

    @ExperimentalCoroutinesApi
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (currentLocation == null) {
                prepareLocalization(location)
                //prepareDone()
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




