package com.kalogsc.feedbil.ui

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.core.app.ApplicationProvider

/** Reads the shipped pt-BR string so assertions do not duplicate its text. */
internal fun appString(id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id)

internal fun hasRole(role: Role): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

internal fun hasStateDescription(label: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label)
