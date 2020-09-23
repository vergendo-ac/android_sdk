# com.ac.api - Kotlin client library for Augmented City API

## Requires

* Kotlin 1.3.61
* Gradle 4.9

## Build

First, create the gradle wrapper script:

```
gradle wrapper
```

Then, run:

```
./gradlew check assemble
```

This runs all tests and packages the library.

## Features/Implementation Notes

* Supports JSON inputs/outputs, File inputs, and Form inputs.
* Supports collection formats for query parameters: csv, tsv, ssv, pipes.
* Some Kotlin and Java types are fully qualified to avoid conflicts with types defined in OpenAPI definitions.
* Implementation of ApiClient is intended to reduce method counts, specifically to benefit Android targets.

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *https://developer.augmented.city/api/v2*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*LocalizerApi* | [**localize**](docs/LocalizerApi.md#localize) | **POST** localizer/localize | Localize camera
*LocalizerApi* | [**prepare**](docs/LocalizerApi.md#prepare) | **GET** localizer/prepare | Prepare localization session
*ObjectsApi* | [**addObjectByPose**](docs/ObjectsApi.md#addobjectbypose) | **POST** object/pose | Add an object by 3D pose
*ReconstructionApi* | [**createReconstructionTask**](docs/ReconstructionApi.md#createreconstructiontask) | **POST** series | Create reconstruction task
*ReconstructionApi* | [**getAllCities**](docs/ReconstructionApi.md#getallcities) | **GET** get_cities_all | Get augmented cities list
*ReconstructionApi* | [**getCityByGps**](docs/ReconstructionApi.md#getcitybygps) | **GET** get_city | Get augmented city by gps
*ReconstructionApi* | [**getReconstructionStatus**](docs/ReconstructionApi.md#getreconstructionstatus) | **GET** series | Get reconstruction task status
*ReconstructionApi* | [**updateReconstructionTask**](docs/ReconstructionApi.md#updatereconstructiontask) | **PUT** series | Upload images for reconstruction
*VersionApi* | [**getServerVersion**](docs/VersionApi.md#getserverversion) | **GET** server_version | Get server version


<a name="documentation-for-models"></a>
## Documentation for Models

 - [com.ac.api.models.ARObject](docs/ARObject.md)
 - [com.ac.api.models.AddObjectResult](docs/AddObjectResult.md)
 - [com.ac.api.models.AddObjectStatus](docs/AddObjectStatus.md)
 - [com.ac.api.models.AugmentedCity](docs/AugmentedCity.md)
 - [com.ac.api.models.AugmentedCityDescription](docs/AugmentedCityDescription.md)
 - [com.ac.api.models.AugmentedCityDescriptionCircle](docs/AugmentedCityDescriptionCircle.md)
 - [com.ac.api.models.Camera](docs/Camera.md)
 - [com.ac.api.models.CameraIntrinsics](docs/CameraIntrinsics.md)
 - [com.ac.api.models.GeoPoint](docs/GeoPoint.md)
 - [com.ac.api.models.ImageDescription](docs/ImageDescription.md)
 - [com.ac.api.models.ImageDescriptionGps](docs/ImageDescriptionGps.md)
 - [com.ac.api.models.InlineObject](docs/InlineObject.md)
 - [com.ac.api.models.InlineObject1](docs/InlineObject1.md)
 - [com.ac.api.models.LocalizationHint](docs/LocalizationHint.md)
 - [com.ac.api.models.LocalizationResult](docs/LocalizationResult.md)
 - [com.ac.api.models.LocalizationStatus](docs/LocalizationStatus.md)
 - [com.ac.api.models.ObjectWithPose](docs/ObjectWithPose.md)
 - [com.ac.api.models.ObjectWithPoseDescription](docs/ObjectWithPoseDescription.md)
 - [com.ac.api.models.ObjectWithPoseDescriptionSticker](docs/ObjectWithPoseDescriptionSticker.md)
 - [com.ac.api.models.Placeholder](docs/Placeholder.md)
 - [com.ac.api.models.PlaceholderNode3d](docs/PlaceholderNode3d.md)
 - [com.ac.api.models.Pose](docs/Pose.md)
 - [com.ac.api.models.PrepareResult](docs/PrepareResult.md)
 - [com.ac.api.models.PrepareStatus](docs/PrepareStatus.md)
 - [com.ac.api.models.Quaternion](docs/Quaternion.md)
 - [com.ac.api.models.ReconstructionStage](docs/ReconstructionStage.md)
 - [com.ac.api.models.ReconstructionTaskStatus](docs/ReconstructionTaskStatus.md)
 - [com.ac.api.models.ScanDaytime](docs/ScanDaytime.md)
 - [com.ac.api.models.ScanPassage](docs/ScanPassage.md)
 - [com.ac.api.models.ScanSeriesDescription](docs/ScanSeriesDescription.md)
 - [com.ac.api.models.ScanStyle](docs/ScanStyle.md)
 - [com.ac.api.models.Surface](docs/Surface.md)
 - [com.ac.api.models.Vector2d](docs/Vector2d.md)
 - [com.ac.api.models.Vector3d](docs/Vector3d.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.
