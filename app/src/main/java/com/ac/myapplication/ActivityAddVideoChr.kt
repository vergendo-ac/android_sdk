package com.ac.myapplication

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import com.google.ar.core.Pose
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable

class ActivityAddVideoChr: ActivityAddVideo() {

    private val CHROMA_KEY_COLOR =
        Color(0.1843f, 1.0f, 0.098f)

    override fun addObject (obj: ArObject, syncPose: Pose) {
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

                val anchorNode = createAnchorNode(obj, syncPose)
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
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
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
            }
    }

}
