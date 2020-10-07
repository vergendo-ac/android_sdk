package com.ac.myapplication

import android.graphics.SurfaceTexture
import android.location.Location
import android.media.MediaPlayer
import com.ac.api.models.LocalizationResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import retrofit2.Response

open class ActivityAddVideo: ActivityAdd2dObject() {


    override fun addObject(obj: ArObject, syncPose: Pose) {
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
                    }
                }
                it.material.setExternalTexture("videoTexture", texture)
                val anchorNode = createAnchorNode(obj, syncPose)
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


    @ExperimentalCoroutinesApi
    override fun localization(imageData: ByteArray, syncPose: Pose) {
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
                            val inScene = sceneObjects[it.id]
                            if (inScene == null) {
                                sceneObjects[it.id] = it
                                addObject(it, syncPose)
                                return@forEach
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

}
