package com.iyes.dacpressuremanager.domain

import kotlin.math.floor

object RubyTemperature {
    const val ROOM_K = 298
    const val MIN_K = 150
    const val MAX_K = 400

    fun isValid(temperatureK: Int): Boolean =
        temperatureK in MIN_K..MAX_K

    fun toRoundedCelsius(temperatureK: Int): Int =
        floor(temperatureK - 273.15 + 0.5).toInt()
}
