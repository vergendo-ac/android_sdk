# VersionApi

All URIs are relative to *https://developer.augmented.city/api/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getServerVersion**](VersionApi.md#getServerVersion) | **GET** server_version | Get server version



Get server version

Get server version

### Example
```kotlin
// Import classes:
//import com.doors.api.*
//import com.doors.api.infrastructure.*
//import com.doors.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(VersionApi::class.java)

val result : kotlin.String = webService.getServerVersion()
```

### Parameters
This endpoint does not need any parameter.

### Return type

**kotlin.String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/plain

