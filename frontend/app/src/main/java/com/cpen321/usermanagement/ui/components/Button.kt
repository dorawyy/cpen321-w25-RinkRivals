package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cpen321.usermanagement.ui.theme.LocalSpacing

@Composable
fun Button(
    type: String = "primary",
    fullWidth: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = LocalSpacing.current

    val colors = if (type == "secondary") {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.Black
        )
    } else {
        ButtonDefaults.buttonColors()
    }

    var buttonModifier = modifier
    if (fullWidth) {
        buttonModifier = buttonModifier.fillMaxWidth()
    }
    buttonModifier = buttonModifier.height(spacing.extraLarge2)


    MaterialButton(
        colors = colors,
        onClick = onClick,
        enabled = enabled,
        modifier = buttonModifier,
    ) {
        content()
    }
}