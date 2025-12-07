package com.dms2350.iptvapp.data.api.dto

import com.google.gson.annotations.SerializedName

data class HeartbeatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

