package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.ImageDescription
import com.ac.api.models.ImageDescriptionGps
import com.ac.api.models.LocalizationResult
import com.ac.api.models.PrepareResult
import io.kotlintest.specs.StringSpec
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Assertions
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.InputStream

const val EXP = "LocalizationResult(status=LocalizationStatus(code=0, message=Image has been localized in recId: <data/DB/series/series_2020-09-12_12-32-36.858874844.140051090499328/0>), camera=Camera(pose=Pose(position=Vector3d(x=-3.2284274, y=2.4958546, z=-0.5517299), orientation=Quaternion(w=0.9951804, x=0.078717366, y=0.05244493, z=0.025866553))), reconstructionId=14572, placeholders=[PlaceholderNode3d(placeholderId=objects/object_6050, pose=Pose(position=Vector3d(x=-3.012573, y=-4.9047594, z=17.850176), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-1.2192755, y=-1.2429489, z=0.5275387), Vector3d(x=1.1272248, y=-1.0014473, z=-0.6660276), Vector3d(x=1.1478487, y=1.2124363, z=-0.4931274), Vector3d(x=-1.0557982, y=1.0319598, z=0.6316163)])], surfaces=null, objects=[ARObject(placeholder=Placeholder(placeholderId=objects/object_6050), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1599904583918, description=, path=test_1, sticker_id=objects/object_6050, sticker_subtype=, sticker_text=тест_1, sticker_type=other})])"

class LocalizeTest : StringSpec({
    "Localize should respond" {
        val apiClient = ApiClient("http://developer.vergendo.com/api")
        apiClient.setLogger {
            println(it)
        }
        val webService = apiClient.createService(LocalizerApi::class.java)
        val gps = ImageDescriptionGps(60.0309083F, 30.2414354F, 68.5F)
        val imageDesc = ImageDescription(gps, null, null, ImageDescription.Rotation._90)
        val image = LocalizeTest::class.java.getResource("image.jpg")!!.readBytes()
        val mp = createMultipartBody(image)

        val result: Call<LocalizationResult> = webService.localize(imageDesc, mp)
        try {
            val response: Response<LocalizationResult> = result.execute()
            println("LocalizationResult:" + response.body())
            Assertions.assertEquals(
                EXP, response.body()
            )
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
    return MultipartBody.Part.createFormData("image", null, createRequestBody(byteArray))
}
