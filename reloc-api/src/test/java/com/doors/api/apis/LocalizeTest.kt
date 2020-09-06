package com.doors.api.apis

import com.doors.api.infrastructure.ApiClient
import com.doors.api.models.ImageDescription
import com.doors.api.models.ImageDescriptionGps
import com.doors.api.models.LocalizationResult
import com.doors.api.models.PrepareResult
import io.kotlintest.specs.StringSpec
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.InputStream

class LocalizeTest: StringSpec({
    "Localize should respond" {
        val apiClient = ApiClient("http://developer.augmented.city:15000/api/v2")
//        apiClient.setLogger {
//            println(it)
//        }
        val webService = apiClient.createService(LocalizerApi::class.java)
        val gps = ImageDescriptionGps(59.941868f, 30.223455f)
        val imageDesc = ImageDescription(gps)
        val image = LocalizeTest::class.java.getResource("image.jpg")!!.readBytes()
        val mp = createMultipartBody(image)

        val result : Call<LocalizationResult> = webService.localize(imageDesc, mp)
        try {
            val response: Response<LocalizationResult> = result.execute()
            println("Localize camera test PASS")
        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})

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
    return MultipartBody.Part.createFormData("image",null, createRequestBody(byteArray))
}
