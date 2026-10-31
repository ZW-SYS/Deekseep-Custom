package com.dsmod.probe.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Fade + slide-up entrance, staggered by [index]. Runs once per composition of the item. */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(45L * index.coerceIn(0, 12))
        progress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

/** Subtle spring scale-down while the bound interaction source is pressed. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.965f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Large status card with a slowly drifting gradient and a soft glow. All animation values are
 * read inside draw/graphicsLayer lambdas, so the infinite transition only triggers redraws, not
 * recompositions.
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
    val base = if (active) scheme.primaryContainer else scheme.errorContainer
    val accent = if (active) scheme.tertiaryContainer else scheme.secondaryContainer
    val glow = if (active) scheme.primary else scheme.error
    val content = if (active) scheme.onPrimaryContainer else scheme.onErrorContainer
    val transition = rememberInfiniteTransition(label = "statusGlow")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(28.dp)
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth().pressScale(interaction, 0.98f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .drawBehind {
                    val p = phase.value
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(base, accent, base),
                            start = Offset(size.width * p, 0f),
                            end = Offset(size.width * (1f - p), size.height),
                        ),
                    )
                    val center = Offset(size.width * (0.15f + 0.7f * p), size.height * 0.25f)
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = 0.28f), Color.Transparent),
                            center = center,
                            radius = size.maxDimension * 0.55f,
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = center,
                    )
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    if (active) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    val v = pulse.value
                                    scaleX = 0.7f + 0.5f * v
                                    scaleY = 0.7f + 0.5f * v
                                    alpha = (1f - v) * 0.55f
                                }
                                .background(glow.copy(alpha = 0.35f), CircleShape),
                        )
                    }
                    Box(
                        Modifier.size(52.dp).background(glow.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = active,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.45f)) +
                                    fadeIn()) togetherWith (scaleOut(targetScale = 0.4f) + fadeOut())
                            },
                            label = "activationIcon",
                        ) { on ->
                            Icon(
                                imageVector = if (on) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = glow,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = content,
                    )
                    lines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = content.copy(alpha = 0.82f),
                        )
                    }
                }
            }
        }
    }
}

/** Category row: tonal icon badge, title, short description and a trailing chevron. */
@Composable
fun CategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth().pressScale(interaction),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
    )
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
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
