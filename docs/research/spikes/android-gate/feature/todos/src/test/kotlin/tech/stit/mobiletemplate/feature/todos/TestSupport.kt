package tech.stit.mobiletemplate.feature.todos

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick

/** Robolectric-графика native-режим стабильно рендерит только на этих API. */
internal const val ROBOLECTRIC_SDK = 35

internal fun SemanticsNodeInteraction.performClickCompat(): SemanticsNodeInteraction = performClick()
