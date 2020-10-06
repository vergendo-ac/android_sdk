package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.*
import io.kotlintest.specs.StringSpec
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response

const val TEST_FILE_NAME = "image_1601637381817.jpg"

class AddObjectByImage :   StringSpec({
    "AddObjectByImage" {
        val apiClient = ApiClient("http://developer.augmented.city/api/v2")
        apiClient.setLogger {
            println(it)
        }
        val objectsApi = apiClient.createService(ObjectsApi::class.java)
        val sticker = StickerData("coffee house", "restaurant", "path", null, "sticker_id")
        val list = mutableListOf<Vector2i>()
        list.add(Vector2i(0, 0))
        list.add(Vector2i(100, 0))
        list.add(Vector2i(100, 100))
        list.add(Vector2i(0, 100))
        val impr = ImageProjections(list,TEST_FILE_NAME)
        val place = PlaceholderImage(listOf(impr))
        val obj = ObjectWithMarkedImage(place, sticker)

        val image = AddObjectByImage::class.java.getResource(TEST_FILE_NAME)!!.readBytes()
        val mp = MultipartBody.Part.createFormData("image", TEST_FILE_NAME, createRequestBody(image))
        val addCall: Call<AddObjectResult> = objectsApi.addObjectByImage(obj, mp)
        try {
            val response: Response<AddObjectResult> = addCall.execute()
            val result = response.body()
            println(result)
            assert(response.isSuccessful)
        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})

