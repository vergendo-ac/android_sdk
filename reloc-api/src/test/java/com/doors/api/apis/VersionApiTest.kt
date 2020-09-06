package com.doors.api.apis

import com.doors.api.infrastructure.ApiClient
import io.kotlintest.specs.StringSpec
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.Assertions.assertEquals
import retrofit2.Call
import retrofit2.Response

class VersionApiTest : StringSpec({
    "VersionApi should respond and has valid version" {
        val apiClient = ApiClient("http://developer.augmented.city:15000/api/v2")
//        apiClient.setLogger {
//            println(it)
//        }
        val webService = apiClient.createService(VersionApi::class.java)
        val result: Call<String> = webService.getServerVersion()
        try {
            val response: Response<String> = result.execute()
            val apiResponse: String? = response.body()
            println(apiResponse)
            assertEquals("2.9-1-g1c6d739a HEAD 20.09.01 23:10", apiResponse)
            println("VersionApiTest PASS")
        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})
