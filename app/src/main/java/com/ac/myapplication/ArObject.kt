package com.ac.myapplication

import com.doors.api.models.Vector3d
import com.doors.tourist2.utils.kotlinMath.Float3
import com.google.ar.sceneform.math.Vector3


data class ArSticker(
    val float3: Vector3d = Vector3d(0.0f, 0.0f,0.0f),
    val id: String = ""
)
