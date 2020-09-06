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

import com.doors.api.models.Pose
import com.doors.api.models.Vector2d

import com.squareup.moshi.Json

/**
 * 
 * @param pose 
 * @param frame 
 */

data class Surface (
    @Json(name = "pose")
    val pose: Pose,
    @Json(name = "frame")
    val frame: kotlin.collections.List<Vector2d>
)

