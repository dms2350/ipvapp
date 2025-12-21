package com.dms2350.iptvapp.data.api

import com.dms2350.iptvapp.data.api.dto.ChannelDto
import com.dms2350.iptvapp.data.api.dto.CategoryDto
import com.dms2350.iptvapp.data.api.dto.DeviceInfoDto
import com.dms2350.iptvapp.data.api.dto.HeartbeatResponse
import com.dms2350.iptvapp.data.api.dto.NotificationsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IPTVApi {
    @GET("channels")
    suspend fun getChannels(): Response<List<ChannelDto>>

    @GET("channels/{id}")
    suspend fun getChannelById(@Path("id") id: Int): Response<ChannelDto>

    @GET("channels")
    suspend fun getChannelsByCategory(@Query("category_id") categoryId: Int): Response<List<ChannelDto>>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Int): Response<CategoryDto>

    @POST("devices/heartbeat")
    suspend fun sendHeartbeat(@Body deviceInfo: DeviceInfoDto): Response<HeartbeatResponse>

    @GET("devices/check-status/{device_id}")
    suspend fun getDeviceNotifications(@Path("device_id") deviceId: String): Response<NotificationsResponse>
}
