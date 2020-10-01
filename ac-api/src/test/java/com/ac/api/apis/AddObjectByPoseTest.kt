package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.*
import io.kotlintest.specs.StringSpec
import retrofit2.Call
import retrofit2.Response

class AddObjectByPoseTest :   StringSpec({
    "AddObjectByPoseTest" {
        val apiClient = ApiClient("http://developer.vergendo.com:5000/api/v2")
        apiClient.setLogger {
            println(it)
        }
        val localizerApi = apiClient.createService(LocalizerApi::class.java)
        val objectsApi = apiClient.createService(ObjectsApi::class.java)
        val gps = ImageDescriptionGps(59.9927142, 30.3807979, 67.9000015258789)
        val imageDesc = ImageDescription(gps, null, null,false, 90)
        val image = AddObjectByPoseTest::class.java.getResource("image_1601547820001_lat_59.9927142_lon_30.3807979_alt_67.9000015258789.jpg")!!.readBytes()
        val mp = createMultipartBody(image)
        val hint = LocalizationHint( emptyList(), null)
        val localizeCall: Call<LocalizationResult> = localizerApi.localize(imageDesc, mp, hint)
        try {
            val response: Response<LocalizationResult> = localizeCall.execute()
            val result = response.body()
            if(result!!.status.code==0){
                val recId = result.reconstructionId


                val position = Vector3d(0f, 0f, -1f)
                val rotation = Quaternion(x=0f, y=0f, z=0f, w=1f)
                val pose = Pose(position, rotation)
                val sticker = ObjectWithPoseDescriptionSticker("", "!!!!")
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


