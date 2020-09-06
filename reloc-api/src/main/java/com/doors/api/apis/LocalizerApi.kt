package com.doors.api.apis

import com.doors.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody

import com.doors.api.models.ImageDescription
import com.doors.api.models.LocalizationResult
import com.doors.api.models.PrepareResult

import okhttp3.MultipartBody

interface LocalizerApi {
    /**
     * Localize camera
     * Localize uploaded image. Return camera pose and optional placeholders scene, surfaces scene and objects content. Camera, placeholders and surfaces coordinates are local coordinates in reconstruction coordinate system identified by reconstruction id.
     * Responses:
     *  - 200: Localization result
     *  - 400: Bad request
     *  - 500: Internal Server Error
     * 
     * @param description  
     * @param image A JPEG-encoded image 
    * @return [Call]<[LocalizationResult]>
     */
    @Multipart
    @POST("localizer/localize")
    fun localize(@Part("description") description: ImageDescription, @Part image: MultipartBody.Part): Call<LocalizationResult>

    /**
     * Prepare localization session
     * Prepare for localization for given geolocation. Causes server to load nearby reconstructions for localization. Returns an error when localization in this location is not possible.
     * Responses:
     *  - 200: Status
     *  - 400: Bad request
     *  - 500: Internal Server Error
     * 
     * @param lat GPS latitude 
     * @param lon GPS longitude 
     * @param alt GPS altitude (optional) (optional)
     * @param dop GPS HDOP (optional) (optional)
    * @return [Call]<[PrepareResult]>
     */
    @GET("localizer/prepare")
    fun prepare(@Query("lat") lat: kotlin.Double, @Query("lon") lon: kotlin.Double, @Query("alt") alt: kotlin.Double? = null, @Query("dop") dop: kotlin.Double? = null): Call<PrepareResult>

}
