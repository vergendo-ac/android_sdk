package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.PrepareResult
import io.kotlintest.specs.StringSpec
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import retrofit2.Call
import retrofit2.Response
import java.lang.ArithmeticException


class PrepareLocalizationTest: StringSpec({
    "Prepare should respond" {
        val apiClient = ApiClient("http://developer.augmented.city/api/v2")
//        apiClient.setLogger {
//            println(it)
//        }
        val webService = apiClient.createService(LocalizerApi::class.java)
        val result : Call<PrepareResult> = webService.prepare(0.0, 0.0, 0.0, 0.0)
        try {
            val response: Response<PrepareResult> = result.execute()
            println("Prepare localization test PASS")
        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})
