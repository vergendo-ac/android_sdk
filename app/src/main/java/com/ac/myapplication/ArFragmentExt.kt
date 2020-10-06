package com.ac.myapplication

import android.util.Log
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.*




private enum class ImageResolution {
    LOW_RESOLUTION, MEDIUM_RESOLUTION, HIGH_RESOLUTION
}



class ArFragmentExt : ArFragment() {
    private val USE_RESOLUTION = ImageResolution.MEDIUM_RESOLUTION
    private var cpuLowResolutionCameraConfig: CameraConfig? = null
    private var cpuMediumResolutionCameraConfig: CameraConfig? = null
    private var cpuHighResolutionCameraConfig: CameraConfig? = null

    companion object {
        const val TAG = "ArFragmentExt"
    }

    private fun getCameraConfigWithSelectedResolution(
        cameraConfigs: List<CameraConfig>, resolution: ImageResolution
    ): CameraConfig? {
        val cameraConfigsByResolution: List<CameraConfig> = ArrayList(
            cameraConfigs.subList(0, Math.min(cameraConfigs.size, 3))
        )
        Collections.sort(
            cameraConfigsByResolution
        ) { p1: CameraConfig, p2: CameraConfig ->
            Integer.compare(
                p1.imageSize.height, p2.imageSize.height
            )
        }
        var cameraConfig = cameraConfigsByResolution[0]
        when (resolution) {
            ImageResolution.LOW_RESOLUTION -> cameraConfig = cameraConfigsByResolution[0]
            ImageResolution.MEDIUM_RESOLUTION -> cameraConfig = cameraConfigsByResolution[1]
            ImageResolution.HIGH_RESOLUTION -> cameraConfig = cameraConfigsByResolution[2]
        }
        return cameraConfig
    }

    private fun printResolutionInfoInfo(config :CameraConfig?){
        if(config!=null) {
            val imageSize = config.imageSize
            Log.i(TAG, "W=" + imageSize.width + ", H=" + imageSize.height)
        }
    }

    override fun getSessionConfiguration(session: Session): Config {
        val cameraConfigFilter = CameraConfigFilter(session)
            .setTargetFps(
                EnumSet.of(
                    CameraConfig.TargetFps.TARGET_FPS_30, CameraConfig.TargetFps.TARGET_FPS_60
                )
            )
        val cameraConfigs =
            session.getSupportedCameraConfigs(cameraConfigFilter)

        cpuLowResolutionCameraConfig =
            getCameraConfigWithSelectedResolution(
                cameraConfigs, ImageResolution.LOW_RESOLUTION)

        printResolutionInfoInfo(cpuLowResolutionCameraConfig)


        cpuMediumResolutionCameraConfig =
            getCameraConfigWithSelectedResolution(
                cameraConfigs, ImageResolution.MEDIUM_RESOLUTION)

        printResolutionInfoInfo(cpuMediumResolutionCameraConfig)

        cpuHighResolutionCameraConfig =
            getCameraConfigWithSelectedResolution(
                cameraConfigs, ImageResolution.HIGH_RESOLUTION)

        printResolutionInfoInfo(cpuHighResolutionCameraConfig)

        when(USE_RESOLUTION){
            ImageResolution.LOW_RESOLUTION -> session.cameraConfig = cpuLowResolutionCameraConfig
            ImageResolution.MEDIUM_RESOLUTION -> session.cameraConfig = cpuMediumResolutionCameraConfig
            ImageResolution.HIGH_RESOLUTION -> session.cameraConfig = cpuHighResolutionCameraConfig
        }

        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        return config
    }
}
