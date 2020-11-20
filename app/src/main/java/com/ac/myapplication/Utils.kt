package com.ac.myapplication

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.location.Location
import android.media.Image
import android.os.Environment.DIRECTORY_PICTURES
import com.ac.api.models.Quaternion
import com.ac.api.models.Vector3d
import com.ac.myapplication.math.Float3
import com.ac.myapplication.math.Float4
import com.ac.myapplication.math.Mat4
import com.ac.myapplication.math.transpose
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

operator fun Vector3.plus(other: Vector3): Vector3 {
    return Vector3.add(this, other)
}

operator fun Vector3.minus(other: Vector3): Vector3 {
    return Vector3.subtract(this, other)
}

fun Quaternion.toFloatArray(): FloatArray {
    return floatArrayOf(x, y, z, w)
}

fun Vector3d.toFloatArray(): FloatArray {
    return floatArrayOf(x, y, z)
}

fun List<Float>.toVector3d(): Vector3d {
    return Vector3d(this[0], this[1], this[2])
}

fun inverseMatrix(matrix: Mat4): Mat4 {
    val rotation = matrix.upperLeft
    val position = matrix.position

    val newPosition: Float3 = transpose(-rotation) * position
    val newRotation = transpose(rotation)
    return Mat4(
        Float4(newRotation.x, 0.0f),
        Float4(newRotation.y, 0.0f),
        Float4(newRotation.z, 0.0f),
        Float4(newPosition, 1.0f)
    )
}

fun srvToLocalTransform(local: Mat4, server: Mat4, scaleScalar: Float): Mat4 {
    val scale = Mat4.diagonal(Float4(scaleScalar, scaleScalar, scaleScalar, 1.0f))

    val tf_cb_ca = Mat4(
        Float4(0.0f, 1.0f, 0.0f, 0.0f),
        Float4(1.0f, 0.0f, 0.0f, 0.0f),
        Float4(0.0f, 0.0f, -1.0f, 0.0f),
        Float4(0.0f, 0.0f, 0.0f, 1.0f)
    )

    val tf_b_cb = inverseMatrix(server)
    val tf_ca_a = local
    val tf_b_a = (tf_ca_a * tf_cb_ca) * scale * tf_b_cb

    return tf_b_a
}


fun Pose.toMat4(): Mat4 {
    val array = FloatArray(16)
    this.toMatrix(array, 0)
    return Mat4(array)
}

fun Vector3d.toLocal(matrix: Mat4): Vector3d {
    val pointVec3 = Float4(this.x, this.y, this.z, 1.0f)
    val pointAr: Float4 = matrix * pointVec3
    return Vector3d(pointAr.x, pointAr.y, pointAr.z)
}

fun saveImageToFile(byteArray: ByteArray, location: Location, context: Context) {

    val sb = StringBuilder("image_")
    sb.append(System.currentTimeMillis().toString() + "_")
    sb.append("lat_" + location.latitude.toString())
    sb.append("_lon_" + location.longitude.toString())
    sb.append("_alt_" + location.altitude.toString())
    sb.append(".jpg")

    val file = File(context.getExternalFilesDir(DIRECTORY_PICTURES), sb.toString())
    file.writeBytes(byteArray)
}

fun createMultipartBody(
    byteArray: ByteArray
): MultipartBody.Part {
    return MultipartBody.Part.createFormData("image", "file.jpg", createRequestBody(byteArray))
}

// convert scene image to jpg
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
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 60, out)
    return out.toByteArray()
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
