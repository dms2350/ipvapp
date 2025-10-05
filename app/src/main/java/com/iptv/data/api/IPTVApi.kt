package com.iptv.data.api

import com.iptv.data.api.dto.ChannelDto
import com.iptv.data.api.dto.CategoryDto
import retrofit2.Response
import retrofit2.http.GET
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
}