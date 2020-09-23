# ReconstructionApi

All URIs are relative to *https://developer.augmented.city/api/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createReconstructionTask**](ReconstructionApi.md#createReconstructionTask) | **POST** series | Create reconstruction task
[**getAllCities**](ReconstructionApi.md#getAllCities) | **GET** get_cities_all | Get augmented cities list
[**getCityByGps**](ReconstructionApi.md#getCityByGps) | **GET** get_city | Get augmented city by gps
[**getReconstructionStatus**](ReconstructionApi.md#getReconstructionStatus) | **GET** series | Get reconstruction task status
[**updateReconstructionTask**](ReconstructionApi.md#updateReconstructionTask) | **PUT** series | Upload images for reconstruction



Create reconstruction task

Create a new task to reconstruct a series of images. The task passes to the status of waiting for image upload after creation. After receiving a signal that the images have been uploaded, the task is added to the queue for processing.

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ReconstructionApi::class.java)
val scanSeriesDescription : ScanSeriesDescription =  // ScanSeriesDescription | 

val result : ReconstructionTaskStatus = webService.createReconstructionTask(scanSeriesDescription)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scanSeriesDescription** | [**ScanSeriesDescription**](ScanSeriesDescription.md)|  | [optional]

### Return type

[**ReconstructionTaskStatus**](ReconstructionTaskStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


Get augmented cities list

Get the list of augmented cities. Localization is possible only inside scanned and reconstructed areas. Information about reconstructed areas will be provided in the future.

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ReconstructionApi::class.java)

val result : kotlin.collections.List<AugmentedCity> = webService.getAllCities()
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**kotlin.collections.List&lt;AugmentedCity&gt;**](AugmentedCity.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Get augmented city by gps

Get augmented city by gps point

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ReconstructionApi::class.java)
val pLatitude : kotlin.Double = 1.2 // kotlin.Double | GPS latitude
val pLongitude : kotlin.Double = 1.2 // kotlin.Double | GPS longitude

val result : AugmentedCity = webService.getCityByGps(pLatitude, pLongitude)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **pLatitude** | **kotlin.Double**| GPS latitude |
 **pLongitude** | **kotlin.Double**| GPS longitude |

### Return type

[**AugmentedCity**](AugmentedCity.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Get reconstruction task status

Get series reconstruction task status by task id. Several task ids could be specified. Return status for known task ids. Return nothing for unknown task ids.

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ReconstructionApi::class.java)
val taskId : kotlin.collections.List<java.util.UUID> =  // kotlin.collections.List<java.util.UUID> | Task id

val result : kotlin.collections.List<ReconstructionTaskStatus> = webService.getReconstructionStatus(taskId)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **taskId** | [**kotlin.collections.List&lt;java.util.UUID&gt;**](java.util.UUID.md)| Task id |

### Return type

[**kotlin.collections.List&lt;ReconstructionTaskStatus&gt;**](ReconstructionTaskStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


Upload images for reconstruction

Upload images for reconstruction. You can upload one image or a group of images at a time. To send a signal that the upload is complete, call the method without image data. After receiving such a signal that images are being uploaded, the service adds the task to the queue and changes its status to IN_QUEUE. When the status is changed, new images cannot be uploaded.

### Example
```kotlin
// Import classes:
//import com.ac.api.*
//import com.ac.api.infrastructure.*
//import com.ac.api.models.*

val apiClient = ApiClient()
val webService = apiClient.createWebservice(ReconstructionApi::class.java)
val taskId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | Reconstruction task id. Only one task_id could be specified
val image : java.io.File = BINARY_DATA_HERE // java.io.File | A JPEG-encoded image, must include GPS data in EXIF tags

val result : ReconstructionTaskStatus = webService.updateReconstructionTask(taskId, image)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **taskId** | [**java.util.UUID**](.md)| Reconstruction task id. Only one task_id could be specified |
 **image** | **java.io.File**| A JPEG-encoded image, must include GPS data in EXIF tags | [optional]

### Return type

[**ReconstructionTaskStatus**](ReconstructionTaskStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

