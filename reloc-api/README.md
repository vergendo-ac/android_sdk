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
*ReconstructionApi* | [**getCities**](docs/ReconstructionApi.md#getcities) | **GET** supported_cities | Get reconstructed cities list
*VersionApi* | [**getServerVersion**](docs/VersionApi.md#getserverversion) | **GET** server_version | Get server version


<a name="documentation-for-models"></a>
## Documentation for Models

 - [com.ac.api.models.ARObject](docs/ARObject.md)
 - [com.ac.api.models.AddObjectResult](docs/AddObjectResult.md)
 - [com.ac.api.models.AddObjectStatus](docs/AddObjectStatus.md)
 - [com.ac.api.models.Camera](docs/Camera.md)
 - [com.ac.api.models.ImageDescription](docs/ImageDescription.md)
 - [com.ac.api.models.ImageDescriptionGps](docs/ImageDescriptionGps.md)
 - [com.ac.api.models.InlineObject](docs/InlineObject.md)
 - [com.ac.api.models.LocalizationResult](docs/LocalizationResult.md)
 - [com.ac.api.models.LocalizationStatus](docs/LocalizationStatus.md)
 - [com.ac.api.models.Placeholder](docs/Placeholder.md)
 - [com.ac.api.models.PlaceholderNode3d](docs/PlaceholderNode3d.md)
 - [com.ac.api.models.Pose](docs/Pose.md)
 - [com.ac.api.models.PrepareResult](docs/PrepareResult.md)
 - [com.ac.api.models.PrepareStatus](docs/PrepareStatus.md)
 - [com.ac.api.models.Quaternion](docs/Quaternion.md)
 - [com.ac.api.models.ReconstructedCity](docs/ReconstructedCity.md)
 - [com.ac.api.models.ReconstructedCityGps](docs/ReconstructedCityGps.md)
 - [com.ac.api.models.Surface](docs/Surface.md)
 - [com.ac.api.models.Vector2d](docs/Vector2d.md)
 - [com.ac.api.models.Vector3d](docs/Vector3d.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.
