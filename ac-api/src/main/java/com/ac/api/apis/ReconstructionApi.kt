package com.ac.api.apis

import com.ac.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody

import com.ac.api.models.AugmentedCity
import com.ac.api.models.ReconstructionTaskStatus
import com.ac.api.models.ScanSeriesDescription

import okhttp3.MultipartBody

interface ReconstructionApi {
    /**
     * Create reconstruction task
     * Create a new task to reconstruct a series of images. The task passes to the status of waiting for image upload after creation. After receiving a signal that the images have been uploaded, the task is added to the queue for processing.
     * Responses:
     *  - 200: Series reconstruction task created
     * 
     * @param scanSeriesDescription  (optional)
     * @return [Call]<[ReconstructionTaskStatus]>
     */
    @POST("series")
    fun createReconstructionTask(@Body scanSeriesDescription: ScanSeriesDescription? = null): Call<ReconstructionTaskStatus>

    /**
     * Get augmented cities list
     * Get the list of augmented cities. Localization is possible only inside scanned and reconstructed areas. Information about reconstructed areas will be provided in the future.
     * Responses:
     *  - 200: OK
     * 
     * @return [Call]<[kotlin.collections.List<AugmentedCity>]>
     */
    @GET("get_cities_all")
    fun getAllCities(): Call<kotlin.collections.List<AugmentedCity>>

    /**
     * Get augmented city by gps
     * Get augmented city by gps point
     * Responses:
     *  - 200: OK
     * 
     * @param pLatitude GPS latitude 
     * @param pLongitude GPS longitude 
     * @return [Call]<[AugmentedCity]>
     */
    @GET("get_city")
    fun getCityByGps(@Query("p_latitude") pLatitude: kotlin.Double, @Query("p_longitude") pLongitude: kotlin.Double): Call<AugmentedCity>

    /**
     * Get reconstruction task status
     * Get series reconstruction task status by task id. Several task ids could be specified. Return status for known task ids. Return nothing for unknown task ids.
     * Responses:
     *  - 200: Series processing statuses list
     * 
     * @param taskId Task id 
     * @return [Call]<[kotlin.collections.List<ReconstructionTaskStatus>]>
     */
    @GET("series")
    fun getReconstructionStatus(@Query("task_id") taskId: kotlin.collections.List<java.util.UUID>): Call<kotlin.collections.List<ReconstructionTaskStatus>>

    /**
     * Upload images for reconstruction
     * Upload images for reconstruction. You can upload one image or a group of images at a time. To send a signal that the upload is complete, call the method without image data. After receiving such a signal that images are being uploaded, the service adds the task to the queue and changes its status to IN_QUEUE. When the status is changed, new images cannot be uploaded.
     * Responses:
     *  - 200: Images uploaded
     * 
     * @param taskId Reconstruction task id. Only one task_id could be specified 
     * @param image A JPEG-encoded image, must include GPS data in EXIF tags (optional)
     * @return [Call]<[ReconstructionTaskStatus]>
     */
    @Multipart
    @PUT("series")
    fun updateReconstructionTask(@Query("task_id") taskId: java.util.UUID, @Part image: MultipartBody.Part): Call<ReconstructionTaskStatus>

}
