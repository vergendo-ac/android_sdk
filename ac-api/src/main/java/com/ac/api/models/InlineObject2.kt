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

import com.ac.api.models.ImageDescription
import com.ac.api.models.LocalizationHint

import com.squareup.moshi.Json

/**
 * 
 * @param description 
 * @param image A JPEG-encoded image
 * @param hint 
 */

data class InlineObject2 (
    @Json(name = "description")
    val description: ImageDescription,
    /* A JPEG-encoded image */
    @Json(name = "image")
    val image: java.io.File,
    @Json(name = "hint")
    val hint: LocalizationHint? = null
)

