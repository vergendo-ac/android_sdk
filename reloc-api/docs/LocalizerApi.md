# LocalizerApi

All URIs are relative to *https://developer.augmented.city/api/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**localize**](LocalizerApi.md#localize) | **POST** localizer/localize | Localize camera
[**prepare**](LocalizerApi.md#prepare) | **GET** localizer/prepare | Prepare localization session



Localize camera

Localize uploaded image. Return camera pose and optional placeholders scene, surfaces scene and objects content. Camera, placeholders and surfaces coordinates are local coordinates in reconstruction coordinate system identified by reconstruction id.

### Example
```kotlin
// Import classes:
//import com.doors.api.*
//import com.doors.api.infrastructure.*
//import com.doors.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(LocalizerApi::class.java)
val description : ImageDescription =  // ImageDescription | 
val image : java.io.File = BINARY_DATA_HERE // java.io.File | A JPEG-encoded image

val result : LocalizationResult = webService.localize(description, image)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **description** | [**ImageDescription**](ImageDescription.md)|  |
 **image** | **java.io.File**| A JPEG-encoded image |

### Return type

[**LocalizationResult**](LocalizationResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data, image/jpeg
 - **Accept**: application/json, text/plain


Prepare localization session

Prepare for localization for given geolocation. Causes server to load nearby reconstructions for localization. Returns an error when localization in this location is not possible.

### Example
```kotlin
// Import classes:
//import com.doors.api.*
//import com.doors.api.infrastructure.*
//import com.doors.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(LocalizerApi::class.java)
val lat : kotlin.Double = 1.2 // kotlin.Double | GPS latitude
val lon : kotlin.Double = 1.2 // kotlin.Double | GPS longitude
val alt : kotlin.Double = 1.2 // kotlin.Double | GPS altitude (optional)
val dop : kotlin.Double = 1.2 // kotlin.Double | GPS HDOP (optional)

val result : PrepareResult = webService.prepare(lat, lon, alt, dop)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **lat** | **kotlin.Double**| GPS latitude |
 **lon** | **kotlin.Double**| GPS longitude |
 **alt** | **kotlin.Double**| GPS altitude (optional) | [optional]
 **dop** | **kotlin.Double**| GPS HDOP (optional) | [optional]

### Return type

[**PrepareResult**](PrepareResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, text/plain

