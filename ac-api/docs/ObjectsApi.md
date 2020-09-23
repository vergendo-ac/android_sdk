# ObjectsApi

All URIs are relative to *https://developer.augmented.city/api/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addObjectByPose**](ObjectsApi.md#addObjectByPose) | **POST** object/pose | Add an object by 3D pose



Add an object by 3D pose

Add a new user object into database

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ObjectsApi::class.java)
val objectWithPose : ObjectWithPose =  // ObjectWithPose | 

val result : AddObjectResult = webService.addObjectByPose(objectWithPose)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **objectWithPose** | [**ObjectWithPose**](ObjectWithPose.md)|  | [optional]

### Return type

[**AddObjectResult**](AddObjectResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, text/plain

