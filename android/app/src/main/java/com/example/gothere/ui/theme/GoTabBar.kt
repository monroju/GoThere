package com.example.gothere.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class GoTabItem(
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val PillWidth = 64.dp
private val PillHeight = 32.dp

/**
 * Bottom tab bar with one highlight pill that slides between tabs on a spring,
 * a hairline top edge and a soft lift shadow. [selectedIndex] is -1 when the
 * current screen is not a tab (e.g. Settings): the pill then fades out in place.
 */
@Composable
fun GoTabBar(
    items: List<GoTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val hairline = MaterialTheme.colorScheme.outlineVariant
    // Remember where the pill was so it does not jump when leaving the tabs.
    val lastIndex = remember { mutableIntStateOf(0) }
    if (selectedIndex >= 0) lastIndex.intValue = selectedIndex

    val pillPos by animateFloatAsState(
        targetValue = lastIndex.intValue.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "pillPos"
    )
    val pillAlpha by animateFloatAsState(if (selectedIndex >= 0) 1f else 0f, label = "pillAlpha")

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .shadow(16.dp, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .drawWithContent {
                drawContent()
                drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .height(72.dp)
        ) {
            val itemWidth = maxWidth / items.size
            val density = LocalDensity.current
            val itemPx = with(density) { itemWidth.toPx() }
            val insetPx = with(density) { ((itemWidth - PillWidth) / 2).toPx() }

            Box(
                Modifier
                    .graphicsLayer {
                        translationX = itemPx * pillPos + insetPx
                        translationY = 10.dp.toPx()
                        alpha = pillAlpha
                    }
                    .size(PillWidth, PillHeight)
                    .background(primary.copy(alpha = 0.16f), CircleShape)
            )

            Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tabTint"
                    )
                    val pop by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
                        label = "tabPop"
                    )
                    val source = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                onClick = { onSelect(index) },
                                role = Role.Tab,
                                interactionSource = source,
                                // Ripple is drawn on the pill below, not across the whole cell.
                                indication = null
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .size(PillWidth, PillHeight)
                                .clip(CircleShape)
                                .indication(source, rememberRipple(color = primary)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
