package com.example.data.tariff

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TariffSearchResponse(
    @Json(name = "elements") val elements: List<TariffItemDto>? = null,
    @Json(name = "totalElements") val totalElements: Int? = null
)

@JsonClass(generateAdapter = true)
data class TariffItemDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "tariffCode") val tariffCode: String? = null,
    @Json(name = "tariffTitle") val tariffTitle: String? = null,
    @Json(name = "suq") val suq: String? = null,
    @Json(name = "customsDuty") val customsDuty: Double? = null,
    @Json(name = "commercialBenefit") val commercialBenefit: Double? = null,
    @Json(name = "importDuty") val importDuty: Double? = null,
    val commercialBenefitNote: String? = null
)
