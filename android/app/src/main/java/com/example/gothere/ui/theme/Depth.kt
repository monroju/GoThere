package com.example.gothere.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

// Subtle depth: light and motion instead of heavy shadows, so it reads in both
// themes. Only primary actions get the gradient; everything else gets press
// feedback and a raised edge at most.

/** Shrinks slightly while pressed and springs back. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Raised surface for cards. Dark mode: a 1px lighter top edge that catches the
 * light. Light mode: a soft, low shadow.
 */
@Composable
fun Modifier.raisedSurface(shape: Shape = MaterialTheme.shapes.medium): Modifier =
    if (isDarkTheme()) {
        this.border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.Transparent)),
            shape
        )
    } else {
        this.shadow(
            elevation = 3.dp, shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.12f)
        )
    }

/** Teal gradient body, light top rim and a teal glow underneath. */
@Composable
private fun Modifier.raisedPrimary(shape: Shape, glow: Float): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    return this
        .shadow(elevation = glow.dp, shape = shape, ambientColor = primary, spotColor = primary)
        .background(Brush.verticalGradient(listOf(lerp(primary, Color.White, 0.16f), primary)), shape)
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
            shape
        )
}

/** Primary call to action. Use for the one main action on a screen. */
@Composable
fun RaisedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val shape = ButtonDefaults.shape
    Button(
        onClick = onClick,
        modifier = modifier
            .pressScale(source)
            .alpha(if (enabled) 1f else 0.5f)
            .raisedPrimary(shape, glow = if (enabled) 8f else 0f),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

/** Floating action button with the same raised treatment. */
@Composable
fun RaisedFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.pressScale(source, 0.92f).raisedPrimary(shape, glow = 12f),
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        interactionSource = source,
        content = content
    )
}
