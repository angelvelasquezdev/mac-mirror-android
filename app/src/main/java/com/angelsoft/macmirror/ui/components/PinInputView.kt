package com.angelsoft.macmirror.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angelsoft.macmirror.ui.theme.AppleBlue

@Composable
fun PinInputView(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    pinLength: Int = 6,
    onPinComplete: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Auto focus on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Invisible input capturing system keyboard events cleanly
        BasicTextField(
            value = pin,
            onValueChange = { newValue ->
                val digitsOnly = newValue.filter { it.isDigit() }.take(pinLength)
                if (digitsOnly != pin) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPinChange(digitsOnly)
                    if (digitsOnly.length == pinLength) {
                        keyboardController?.hide()
                        onPinComplete?.invoke(digitsOnly)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (pin.length == pinLength) {
                        onPinComplete?.invoke(pin)
                    }
                }
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp) // Invisible target
        )

        // 6 iOS-style Digit Cells
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        ) {
            for (index in 0 until pinLength) {
                val digit = pin.getOrNull(index)?.toString() ?: ""
                val isFocused = pin.length == index || (pin.length == pinLength && index == pinLength - 1)

                val borderColor by animateColorAsState(
                    targetValue = if (isFocused) AppleBlue else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(150),
                    label = "PinBorder"
                )

                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = borderColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                // Add a subtle middle separator between digit 3 and 4 (3+3 grouping)
                if (index == (pinLength / 2) - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}
