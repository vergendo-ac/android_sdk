package com.ac.api.apis

import com.ac.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody

import com.ac.api.models.ReconstructedCity

interface ReconstructionApi {
    /**
     * Get reconstructed cities list
     * List of scanned and reconstructed cities. Localization is possible only inside this zones.
     * Responses:
     *  - 200: OK
     * 
    * @return [Call]<[kotlin.collections.List<ReconstructedCity>]>
     */
    @Deprecated("This api was deprecated")
    @GET("supported_cities")
    fun getCities(): Call<kotlin.collections.List<ReconstructedCity>>

}
