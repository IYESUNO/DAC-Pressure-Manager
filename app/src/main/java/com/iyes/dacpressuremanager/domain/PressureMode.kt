package com.iyes.dacpressuremanager.domain

enum class PressureMode(
    val storageValue: String,
    val defaultValueCenti: Int,
    val minimumValueCenti: Int,
    val maximumValueCenti: Int,
    val unit: String,
    val profilePrefix: String,
) {
    DIAMOND(
        storageValue = "diamond",
        defaultValueCenti = 133_300,
        minimumValueCenti = 100_000,
        maximumValueCenti = 299_999,
        unit = "cm⁻¹",
        profilePrefix = "Diamond",
    ),
    RUBY(
        storageValue = "ruby",
        defaultValueCenti = 69_424,
        minimumValueCenti = 60_000,
        maximumValueCenti = 79_999,
        unit = "nm",
        profilePrefix = "Ruby",
    ),
    ;

    companion object {
        fun fromStorage(value: String?): PressureMode =
            entries.firstOrNull { it.storageValue == value } ?: DIAMOND
    }
}

enum class MeasurementField {
    REFERENCE,
    MEASURED,
}

