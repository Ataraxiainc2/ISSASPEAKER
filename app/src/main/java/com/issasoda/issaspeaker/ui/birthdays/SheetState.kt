package com.issasoda.issaspeaker.ui.birthdays

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

/** A bottom sheet that opens fully: no half-open step that could hide its buttons, in landscape above all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun fullSheet(): SheetState =
    rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
