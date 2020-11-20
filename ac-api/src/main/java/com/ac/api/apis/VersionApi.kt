package com.ac.api.apis

import com.ac.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody


interface VersionApi {
    /**
     * Get server version
     * Get server version
     * Responses:
     *  - 200: OK
     *  - 404: Not Found
     *  - 500: Internal Server Error
     * 
     * @return [Call]<[kotlin.String]>
     */
    @GET("server_version")
    fun getServerVersion(): Call<kotlin.String>

}
