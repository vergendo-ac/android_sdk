package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.*
import io.kotlintest.specs.StringSpec
import retrofit2.Call
import retrofit2.Response

enum class StickerType {
    REST,
    SHOP,
    PLACE,
    OTHER,
    TEXT;

    override fun toString() =
        when (this) {
            REST -> "restaurant"
            SHOP -> "shop"
            PLACE -> "place"
            OTHER -> "other"
            TEXT -> "text"
        }

    companion object {
        fun fromString(string: String): StickerType {
            return when (string) {
                "restaurant" -> REST
                "shop" -> SHOP
                "place" -> PLACE
                "text" -> TEXT
                "other" -> OTHER
                else -> OTHER
            }
        }
    }
}

class AddObjectByPoseTest :   StringSpec({
    "AddObjectByPoseTest" {
        val apiClient = ApiClient("http://developer.augmented.city:15000/api/v2")
//        apiClient.setLogger {
//            println(it)
//        }
        val localizerApi = apiClient.createService(LocalizerApi::class.java)
        val objectsApi = apiClient.createService(ObjectsApi::class.java)
        val gps = ImageDescriptionGps(59.9927546, 30.3807287, 67.9000015258789)
        val imageDesc = ImageDescription(gps, null, null,false, 90)
        val image = AddObjectByPoseTest::class.java.getResource("image_1601626418723_lat_59.9927546_lon_30.3807287_alt_67.9000015258789.jpg")!!.readBytes()
        val mp = createMultipartBody(image)
        val hint = LocalizationHint( emptyList(), null)
        val localizeCall: Call<LocalizationResult> = localizerApi.localize(imageDesc, mp, hint)
        try {
            val response: Response<LocalizationResult> = localizeCall.execute()
            val result = response.body()
            println(result)
            if(result!!.status.code==0){
                val recId = result.reconstructionId
                val position = Vector3d(0f, 0f, -1f)
                val rotation = Quaternion(x=0f, y=0f, z=0f, w=1f)
                val pose = Pose(position, rotation)
                val sticker = StickerData("coffee house", "restaurant", "path")
                val obj = ObjectWithPoseDescription(sticker)

                val objectWithPose = ObjectWithPose(recId!!, pose, obj, null)

                val objectWithPoseCall: Call<AddObjectResult> = objectsApi.addObjectByPose(objectWithPose)
                val addResponse: Response<AddObjectResult>  = objectWithPoseCall.execute()

                println("RSP:"+addResponse)
            }



        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})



