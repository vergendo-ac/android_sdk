package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.ReconstructedCity
import io.kotlintest.specs.StringSpec
import retrofit2.Call
import retrofit2.Response

class CitiesTest : StringSpec({
    "getCities should respond" {
        val apiClient = ApiClient("http://developer.augmented.city/api/v2")
//        apiClient.setLogger {
//            println(it)
//        }
        val webService = apiClient.createService(ReconstructionApi::class.java)
        val result : Call<List<ReconstructedCity>> = webService.getCities()
        try {
            val response: Response<List<ReconstructedCity>> = result.execute()
            println("getCities test PASS")
        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})
