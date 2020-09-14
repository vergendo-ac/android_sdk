# ReconstructionApi

All URIs are relative to *https://developer.augmented.city/api/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getCities**](ReconstructionApi.md#getCities) | **GET** supported_cities | Get reconstructed cities list



Get reconstructed cities list

List of scanned and reconstructed cities. Localization is possible only inside this zones.

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ReconstructionApi::class.java)

val result : kotlin.collections.List<ReconstructedCity> = webService.getCities()
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**kotlin.collections.List&lt;ReconstructedCity&gt;**](ReconstructedCity.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

