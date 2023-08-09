package com.example.musify.ui.screens.homescreen.greetingphrasegenerator

import java.time.LocalTime

/**
 * An extension property that indicates whether an instance of [LocalTime]
 * is within the time period of a day that is considered as "Morning".
 */
val LocalTime.isMorning get() = this in LocalTime.of(0, 0)..LocalTime.of(11, 59)

/**
 * An extension property that indicates whether an instance of [LocalTime]
 * is within the time period of a day that is considered as "Noon".
 */
val LocalTime.isNoon get() = this in LocalTime.of(12, 0)..LocalTime.of(17, 59)
