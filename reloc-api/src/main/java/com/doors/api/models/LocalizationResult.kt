/**
* Augmented City API
* ## Description This is an API for the Augmented City platform ## Other resources For more information, please visit our website [https://www.augmented.city](https://www.augmented.city/) 
*
* The version of the OpenAPI document: 2.0.0
* Contact: support@vergendo.com
*
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package com.doors.api.models

import com.doors.api.models.ARObject
import com.doors.api.models.Camera
import com.doors.api.models.LocalizationStatus
import com.doors.api.models.PlaceholderNode3d
import com.doors.api.models.Surface

import com.squareup.moshi.Json

/**
 * 
 * @param status 
 * @param camera 
 * @param reconstructionId 
 * @param placeholders Pose describes position and orientation in reconstruction coordinate system. Frame describes 4 points in placeholder coordinate system.
 * @param surfaces 
 * @param objects 
 */

data class LocalizationResult (
    @Json(name = "status")
    val status: LocalizationStatus,
    @Json(name = "camera")
    val camera: Camera? = null,
    @Json(name = "reconstruction_id")
    val reconstructionId: kotlin.Int? = null,
    /* Pose describes position and orientation in reconstruction coordinate system. Frame describes 4 points in placeholder coordinate system. */
    @Json(name = "placeholders")
    val placeholders: kotlin.collections.List<PlaceholderNode3d>? = null,
    @Json(name = "surfaces")
    val surfaces: kotlin.collections.List<Surface>? = null,
    @Json(name = "objects")
    val objects: kotlin.collections.List<ARObject>? = null
)

