package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ScriptItem
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.CodeBoxBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreenSuccess
import com.example.ui.theme.NeonGreenSuccessDim
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ScriptCard(
    script: ScriptItem,
    index: Int,
    isExpanded: Boolean,
    isScriptRevealed: Boolean,
    isCopied: Boolean,
    onCardClick: () -> Unit,
    onGetScriptClick: () -> Unit,
    onCopySuccess: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Staggered fade-in and slide-up entrance animation
    val entranceProgress = remember(script.id) { Animatable(0f) }
    LaunchedEffect(script.id) {
        val staggerMs = (index.coerceAtMost(6) * 65L)
        if (staggerMs > 0L) delay(staggerMs)
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
    }

    // 3D Tilt & Hover/Press glow state
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var targetTiltX by remember { mutableFloatStateOf(0f) }
    var targetTiltY by remember { mutableFloatStateOf(0f) }
    var isActiveGlow by remember { mutableStateOf(false) }

    val tiltX by animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tilt_x"
    )
    val tiltY by animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tilt_y"
    )
    val scaleFactor by animateFloatAsState(
        targetValue = if (isActiveGlow) 1.012f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "hover_scale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isExpanded -> NeonCyan.copy(alpha = 0.72f)
            isActiveGlow -> NeonCrimson.copy(alpha = 0.75f)
            else -> BorderGlass
        },
        animationSpec = tween(220),
        label = "card_border"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(250),
        label = "chevron_rotation"
    )

    val cardShape = RoundedCornerShape(20.dp)
    val displayTimestamp = script.updatedAt ?: script.createdAt ?: Timestamp.now()
    val relativeTime = remember(displayTimestamp) {
        DateUtils.getRelativeTimeSpanString(
            displayTimestamp.toDate().time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    val previewLine = remember(script.scriptContent) {
        script.scriptContent.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it }
            .graphicsLayer {
                val p = entranceProgress.value
                alpha = p
                translationY = (1f - p) * 44f
                scaleX = scaleFactor
                scaleY = scaleFactor
                rotationX = tiltX
                rotationY = tiltY
                cameraDistance = 14f * density
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move, PointerEventType.Press -> {
                                isActiveGlow = true
                                if (pos != null && cardSize.width > 0 && cardSize.height > 0) {
                                    val nx = (pos.x / cardSize.width - 0.5f) * 2f
                                    val ny = (pos.y / cardSize.height - 0.5f) * 2f
                                    targetTiltY = (nx * 3.5f).coerceIn(-4f, 4f)
                                    targetTiltX = (-ny * 3.5f).coerceIn(-4f, 4f)
                                }
                            }
                            PointerEventType.Exit, PointerEventType.Release -> {
                                isActiveGlow = false
                                targetTiltX = 0f
                                targetTiltY = 0f
                            }
                        }
                    }
                }
            }
            .shadow(
                elevation = if (isExpanded || isActiveGlow) 18.dp else 8.dp,
                shape = cardShape,
                ambientColor = if (isExpanded) NeonCyan.copy(alpha = 0.35f) else NeonCrimson.copy(alpha = 0.2f),
                spotColor = if (isExpanded) NeonCyan.copy(alpha = 0.45f) else NeonCrimson.copy(alpha = 0.3f)
            )
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SurfaceGlassElevated,
                        SurfaceGlass
                    )
                )
            )
            .border(width = 1.2.dp, color = borderColor, shape = cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main clickable row (line-by-line showcase item)
            Row(
                modifier = Modifier
                    .testTag("script_card_${script.id}")
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = NeonCyan),
                        onClick = onCardClick
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScriptThumbnail(
                    imagePath = script.localImagePath,
                    sampleDrawableName = script.sampleDrawableName
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = script.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .testTag("delete_card_${script.id}")
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_card),
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = previewLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextMuted
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isExpanded) NeonCyan.copy(alpha = 0.14f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isExpanded) NeonCyan.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    if (isExpanded) R.string.hint_expanded else R.string.hint_click_to_view
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = if (isExpanded) NeonCyan else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isExpanded) NeonCyan else TextSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(chevronRotation)
                            )
                        }
                    }
                }
            }

            // Step 1: Smooth accordion drawer with glowing "Get Script" button
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(220)),
                exit = shrinkVertically(
                    animationSpec = tween(220)
                ) + fadeOut(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    HorizontalDivider(color = BorderGlass, thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SCRIPT DRAWER READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                            Text(
                                text = "${script.scriptContent.lines().size} lines • Tap to reveal source",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val getBtnShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .testTag("get_script_button_${script.id}")
                                .heightIn(min = 48.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = getBtnShape,
                                    ambientColor = NeonCrimson.copy(alpha = 0.6f),
                                    spotColor = NeonCrimson.copy(alpha = 0.8f)
                                )
                                .clip(getBtnShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = if (isScriptRevealed) {
                                            listOf(Color(0xFF23263B), Color(0xFF1A1C2E))
                                        } else {
                                            listOf(NeonCrimson, Color(0xFFB5179E))
                                        }
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isScriptRevealed) NeonCyan.copy(alpha = 0.5f)
                                    else Color.White.copy(alpha = 0.25f),
                                    shape = getBtnShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    onClick = onGetScriptClick
                                )
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        if (isScriptRevealed) R.string.action_hide_script
                                        else R.string.action_get_script
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Step 2: Reveal exact Script/Text in sleek dark code box + Copy Script button
                    AnimatedVisibility(
                        visible = isScriptRevealed,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(260)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                    ) {
                        RevealedScriptBox(
                            scriptId = script.id,
                            scriptContent = script.scriptContent,
                            isCopied = isCopied,
                            onCopyClick = {
                                copyToClipboard(context, script.title, script.scriptContent)
                                onCopySuccess()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptThumbnail(
    imagePath: String?,
    sampleDrawableName: String?
) {
    val context = LocalContext.current
    val sampleResId = remember(sampleDrawableName) {
        if (sampleDrawableName != null) {
            context.resources.getIdentifier(sampleDrawableName, "drawable", context.packageName)
        } else 0
    }

    val thumbShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(width = 92.dp, height = 82.dp)
            .clip(thumbShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.14f),
                        NeonCrimson.copy(alpha = 0.14f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.45f),
                        NeonCrimson.copy(alpha = 0.35f)
                    )
                ),
                shape = thumbShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            !imagePath.isNullOrBlank() -> {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = stringResource(R.string.cd_script_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            sampleResId != 0 -> {
                Image(
                    painter = painterResource(id = sampleResId),
                    contentDescription = stringResource(R.string.cd_script_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = stringResource(R.string.cd_script_thumbnail),
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun RevealedScriptBox(
    scriptId: String,
    scriptContent: String,
    isCopied: Boolean,
    onCopyClick: () -> Unit
) {
    val copyBtnBg by animateColorAsState(
        targetValue = if (isCopied) NeonGreenSuccessDim else NeonCyan.copy(alpha = 0.16f),
        animationSpec = tween(220),
        label = "copy_btn_bg"
    )
    val copyBtnBorder by animateColorAsState(
        targetValue = if (isCopied) NeonGreenSuccess else NeonCyan.copy(alpha = 0.65f),
        animationSpec = tween(220),
        label = "copy_btn_border"
    )
    val copyBtnTextColor by animateColorAsState(
        targetValue = if (isCopied) NeonGreenSuccess else NeonCyan,
        animationSpec = tween(220),
        label = "copy_btn_text"
    )

    val boxShape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .clip(boxShape)
            .background(CodeBoxBg)
            .border(
                width = 1.dp,
                color = if (isCopied) NeonGreenSuccess.copy(alpha = 0.65f) else NeonCyan.copy(alpha = 0.25f),
                shape = boxShape
            )
    ) {
        // Header strip with Copy Script button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NeonCrimson)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SOURCE // PAYLOAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            val copyShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .testTag("copy_script_button_$scriptId")
                    .heightIn(min = 40.dp)
                    .shadow(
                        elevation = if (isCopied) 14.dp else 4.dp,
                        shape = copyShape,
                        ambientColor = if (isCopied) NeonGreenSuccess else NeonCyan,
                        spotColor = if (isCopied) NeonGreenSuccess else NeonCyan
                    )
                    .clip(copyShape)
                    .background(copyBtnBg)
                    .border(width = 1.dp, color = copyBtnBorder, shape = copyShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = if (isCopied) NeonGreenSuccess else NeonCyan),
                        onClick = onCopyClick
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isCopied,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                    label = "copy_label_transition"
                ) { copied ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = copyBtnTextColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (copied) R.string.action_copied else R.string.action_copy_script
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            color = copyBtnTextColor
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = scriptContent,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
