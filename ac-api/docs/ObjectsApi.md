# ObjectsApi

All URIs are relative to *https://developer.augmented.city/api/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addObjectByImage**](ObjectsApi.md#addObjectByImage) | **POST** object | Add AR object by image
[**addObjectByPose**](ObjectsApi.md#addObjectByPose) | **POST** object/pose | Add AR object by 3D pose



Add AR object by image

Add a custom object by marked image

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ObjectsApi::class.java)
val description : ObjectWithMarkedImage =  // ObjectWithMarkedImage | 
val image : java.io.File = BINARY_DATA_HERE // java.io.File | A JPEG-encoded image, must include GPS data in EXIF tags

val result : AddObjectResult = webService.addObjectByImage(description, image)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **description** | [**ObjectWithMarkedImage**](ObjectWithMarkedImage.md)|  |
 **image** | **java.io.File**| A JPEG-encoded image, must include GPS data in EXIF tags |

### Return type

[**AddObjectResult**](AddObjectResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json, text/plain


Add AR object by 3D pose

Add a custom object by 3d pose

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

