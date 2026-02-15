package com.dms2350.iptvapp.data.api.dto

import com.google.gson.annotations.SerializedName

data class DeviceInfoDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("manufacturer") val manufacturer: String,
    @SerializedName("model") val model: String,
    @SerializedName("os_version") val osVersion: String,
    @SerializedName("sdk_int") val sdkInt: Int,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("user_full_name") val userFullName: String,
    @SerializedName("user_id_number") val userIdNumber: String
)

