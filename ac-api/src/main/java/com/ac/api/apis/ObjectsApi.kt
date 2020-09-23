package com.ac.api.apis

import com.ac.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody

import com.ac.api.models.AddObjectResult
import com.ac.api.models.ObjectWithPose

interface ObjectsApi {
    /**
     * Add an object by 3D pose
     * Add a new user object into database
     * Responses:
     *  - 200: Object processed
     *  - 400: Bad request
     *  - 500: Internal Server Error
     * 
     * @param objectWithPose  (optional)
    * @return [Call]<[AddObjectResult]>
     */
    @POST("object/pose")
    fun addObjectByPose(@Body objectWithPose: ObjectWithPose? = null): Call<AddObjectResult>

}
