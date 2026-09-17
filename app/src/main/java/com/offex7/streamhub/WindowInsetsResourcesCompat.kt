package com.offex7.streamhub

import android.content.res.Resources
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.WindowInsets

/** Allows the existing v4 code to query an inset using Android Resources. */
fun WindowInsets.getBottom(resources: Resources): Int =
    getBottom(Density(resources.displayMetrics.density, resources.configuration.fontScale))
