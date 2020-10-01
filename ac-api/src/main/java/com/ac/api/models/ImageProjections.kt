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
package com.ac.api.models

import com.ac.api.models.Vector2i

import com.squareup.moshi.Json

/**
 * 
 * @param points 
 * @param filename 
 */

data class ImageProjections (
    @Json(name = "points")
    val points: kotlin.collections.List<Vector2i>,
    @Json(name = "filename")
    val filename: kotlin.String
)

