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


import com.squareup.moshi.Json

/**
 * 
 * @param x 
 * @param y 
 */

data class Vector2d (
    @Json(name = "x")
    val x: kotlin.Float,
    @Json(name = "y")
    val y: kotlin.Float
)

