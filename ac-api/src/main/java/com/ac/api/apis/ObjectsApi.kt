package com.ac.api.apis

import com.ac.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody

import com.ac.api.models.AddObjectResult
import com.ac.api.models.ObjectWithMarkedImage
import com.ac.api.models.ObjectWithPose

import okhttp3.MultipartBody

interface ObjectsApi {
    /**
     * Add AR object by image
     * Add a custom object by marked image
     * Responses:
     *  - 200: Object processed
     *  - 400: Bad request
     *  - 500: Internal Server Error
     * 
     * @param description  
     * @param image A JPEG-encoded image, must include GPS data in EXIF tags 
     * @return [Call]<[AddObjectResult]>
     */
    @Multipart
    @POST("object")
    fun addObjectByImage(@Part("description") description: ObjectWithMarkedImage, @Part image: MultipartBody.Part): Call<AddObjectResult>

    /**
     * Add AR object by 3D pose
     * Add a custom object by 3d pose
     * Responses:
     *  - 200: Object processed
     *  - 400: Bad request
     *  - 500: Internal Server Error
     * 
     * @param objectWithPose  (optional)
     * @return [Call]<[AddObjectResult]>
     */
@Headers("Content-Type: application/json")
    @POST("object/pose")
    fun addObjectByPose(@Body objectWithPose: ObjectWithPose? = null): Call<AddObjectResult>

}
