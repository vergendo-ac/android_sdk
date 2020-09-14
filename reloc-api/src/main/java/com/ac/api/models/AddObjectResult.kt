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

import com.ac.api.models.ARObject
import com.ac.api.models.AddObjectStatus

import com.squareup.moshi.Json

/**
 * 
 * @param status 
 * @param objectsInfo 
 */

data class AddObjectResult (
    @Json(name = "status")
    val status: AddObjectStatus,
    @Json(name = "objects_info")
    val objectsInfo: kotlin.collections.List<ARObject>? = null
)
