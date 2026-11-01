package com.dsmod.probe.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * YunSeek launcher components, Nexus-like: white grouped cards on a light grey page, monochrome
 * line icons, one blue accent, big page titles and almost no motion. Nothing here animates on
 * its own; the only motion is a short press feedback.
 */

/** Kept for call-site compatibility; entrances no longer animate (quiet UI). */
@Suppress("UNUSED_PARAMETER")
fun Modifier.staggeredEntrance(index: Int): Modifier = this

/** Very small, fast press feedback (no spring bounce). */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.985f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Large left-aligned page title (Nexus style). Scrolls away with the page content. */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = 6.dp, top = 4.dp, bottom = 10.dp),
    )
}

/**
 * Activation card: solid blue with white text when active, plain white card with a red title when
 * not. A large faint icon sits half outside the right edge as a watermark. No animation.
 */
@Composable
fun StatusHeroCard(
    active: Boolean,
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (active) scheme.primary else scheme.surfaceContainer
    val titleColor = if (active) Color.White else scheme.error
    val lineColor = if (active) Color.White.copy(alpha = 0.86f) else scheme.onSurfaceVariant
    val markColor = if (active) Color.White.copy(alpha = 0.22f) else scheme.error.copy(alpha = 0.10f)
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(shape)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Icon(
            imageVector = if (active) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = markColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 28.dp)
                .size(140.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium, color = lineColor)
            }
        }
    }
}

/** Small grey group label above a white card ("功能", "帮助"). */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal,
        modifier = modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp),
    )
}

/** White rounded group card; rows inside are separated by spacing only (no dividers). */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 6.dp),
        content = content,
    )
}

/** One row of a [GroupCard]: monochrome line icon, bold title, optional grey description, chevron. */
@Composable
fun GroupRow(
    icon: ImageVector?,
    title: String,
    description: String = "",
    trailing: String = "",
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else Color.Transparent,
        tween(90), label = "rowPress",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            if (description.isNotEmpty()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing.isNotEmpty()) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** Single-row white card (used where a category stands alone). */
@Composable
fun CategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GroupCard(modifier) { GroupRow(icon, title, description, onClick = onClick) }
}

/** Muted information panel used for notes and explanations. */
@Composable
fun NoteCard(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Floating pill bottom bar; the selected item sits on a light blue pill. */
@Composable
fun FloatingPillBar(
    items: List<Pair<String, ImageVector>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(32.dp), clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(32.dp))
            .background(scheme.surfaceContainer)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (label, icon) ->
            val on = index == selected
            val pill by animateColorAsState(
                if (on) scheme.primaryContainer else Color.Transparent, tween(150), label = "pill",
            )
            val tint = if (on) scheme.primary else scheme.onSurfaceVariant
            val interaction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(pill)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(index) }
                    .padding(horizontal = 22.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 11.sp, color = tint,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
