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
 * @param code | State   | Code   |  Description  |   | -- | -- | -- |   | Success | 0 | Object is added |   | Fail | 1 | Failed to add object |  
 * @param message 
 */

data class AddObjectStatus (
    /* | State   | Code   |  Description  |   | -- | -- | -- |   | Success | 0 | Object is added |   | Fail | 1 | Failed to add object |   */
    @Json(name = "code")
    val code: AddObjectStatus.Code,
    @Json(name = "message")
    val message: kotlin.String
) {

    /**
    * | State   | Code   |  Description  |   | -- | -- | -- |   | Success | 0 | Object is added |   | Fail | 1 | Failed to add object |  
    * Values: _0,_1
    */
    
    enum class Code(val value: kotlin.Int){
        @Json(name = "0") _0(0),
        @Json(name = "1") _1(1);
    }
}

