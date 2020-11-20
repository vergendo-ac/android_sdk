package com.ac.myapplication

import android.location.Location
import android.media.Image
import android.os.Bundle
import android.widget.TextView
import com.ac.api.models.LocalizationResult
import com.ac.api.models.Sticker
import com.ac.api.models.Vector3d
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.FixedWidthViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import retrofit2.Response


data class ArObjectPos(
    val position: Vector3d = Vector3d(0.0f, 0.0f, 0.0f),
    val id: String = "",
    var sticker: Sticker,
    var node: Node?
)

open class ActivityAdd2dObject : ActivityPrepareAndLocalize() {

    var sceneObjects = mutableMapOf<String, ArObjectPos>()

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    open fun createAnchorNode(obj: ArObjectPos, syncPose: Pose): AnchorNode {
        val trPos: Pose = syncPose.compose(
            Pose.makeTranslation(
                obj.position.x,
                obj.position.y,
                obj.position.z
            )
        )
        val anchor: Anchor = arFragment.arSceneView.session!!.createAnchor(trPos)
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)
        return anchorNode
    }

    open fun addObject(obj: ArObjectPos, syncPose: Pose) {
        ViewRenderable.builder()
            .setView(this, R.layout.layout_sticker)
            .build()
            .thenAccept {
                it.view.findViewById<TextView>(R.id.text).text = obj.sticker.stickerText
                val anchorNode = createAnchorNode(obj, syncPose)
                val nodeAr = Node().apply {
                    renderable = it
                    (renderable as ViewRenderable).sizer =
                        FixedWidthViewSizer(STICKER_WIDTH_IN_METERS)
                    setParent(anchorNode)
                    localRotation =
                        com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 0f, 1f), 90f)

                }
                obj.node = nodeAr
            }
    }

    @ExperimentalCoroutinesApi
    override fun onPause() {
        super.onPause()
        clearSceneObjects()
        sceneObjects.clear()
        localizeDone = false
    }

    fun onLocalizationResult(result: LocalizationResult): Array<ArObjectPos> {
        val camera = result.camera
        val points = mutableListOf<ArObjectPos>()
        result.objects?.forEach { obj ->
            result.placeholders?.forEach { place ->
                if (place.placeholderId == obj.placeholder.placeholderId) {
                    val position = place.pose.position.toFloatArray().asList().toVector3d()
                    val ar = ArObjectPos(position, place.placeholderId, obj.sticker, null)
                    points.add(ar)
                }
            }
        }


        val matrix = srvToLocalTransform(
            Pose.IDENTITY.toMat4(),
            Pose(
                camera!!.pose.position.toFloatArray(),
                camera.pose.orientation.toFloatArray()
            ).toMat4(), 1.0f
        )
        val ret = points.map {
            val pos = it.position.toLocal(matrix)
            it.copy(position = pos)
        }.toTypedArray()
        return ret
    }

    @ExperimentalCoroutinesApi
    open fun localization(imageData: ByteArray, syncPose: Pose) {
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
                        val objectsToPlace = onLocalizationResult(result)
                        objectsToPlace.iterator().forEach {
                            //it.txt = getStickerText(result, it.id)
                            val inScene = sceneObjects[it.id]
                            if (inScene == null) {
                                sceneObjects[it.id] = it
                                addObject(it, syncPose)
                            }
                        }
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
        }
    }

    @ExperimentalCoroutinesApi
    override fun onUpdateSceneFrame(frameTime: FrameTime) {
        arFragment.onUpdate(frameTime)
        val frame: Frame = arFragment.arSceneView.arFrame ?: return
        onTrackingState(frame.camera.trackingState)
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            return
        }

        if (frame.camera.trackingState === TrackingState.TRACKING) {
            if ((!localizeDone) and (prepareLocalizationDone) and (!inLocalizeProgressFlag)) {
                inLocalizeProgressFlag = true
                var imageData: ByteArray? = null
                val syncPose = frame.camera.pose
                try {
                    val image: Image = frame.acquireCameraImage()
                    imageData = image.toByteArray()
                    image.close()
                } catch (e: NotYetAvailableException) {
                }
                if (imageData != null) {
                    localization(imageData, syncPose)
                } else {
                    inLocalizeProgressFlag = false
                }
            }
        }
    }

}
