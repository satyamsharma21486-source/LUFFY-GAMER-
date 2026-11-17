package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.CreateModalState
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.CodeBoxBg
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceGlassElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateScriptModal(
    state: CreateModalState,
    onDismiss: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onScriptChanged: (String) -> Unit,
    onPhotoSelected: (android.net.Uri?) -> Unit,
    onPublish: (titleErrorMsg: String, scriptErrorMsg: String) -> Unit
) {
    if (state.isOpen) {
        BackHandler(onBack = onDismiss)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoSelected(uri)
        }
    }

    val titleRequiredError = stringResource(R.string.error_title_required)
    val scriptRequiredError = stringResource(R.string.error_script_required)

    AnimatedVisibility(
        visible = state.isOpen,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.74f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = state.isOpen,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(220)),
                exit = scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(160)
                ) + fadeOut(tween(160))
            ) {
                val modalShape = RoundedCornerShape(24.dp)
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .shadow(
                            elevation = 24.dp,
                            shape = modalShape,
                            ambientColor = NeonCyan.copy(alpha = 0.4f),
                            spotColor = NeonCrimson.copy(alpha = 0.4f)
                        )
                        .clip(modalShape)
                        .background(SurfaceGlassElevated)
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    NeonCyan.copy(alpha = 0.65f),
                                    NeonCrimson.copy(alpha = 0.55f)
                                )
                            ),
                            shape = modalShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.modal_create_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.modal_create_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .testTag("close_modal_button")
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close_modal),
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title Input
                    Text(
                        text = stringResource(R.string.label_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onTitleChanged,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_title),
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        isError = state.titleError != null,
                        supportingText = state.titleError?.let { err ->
                            { Text(text = err, color = NeonCrimson) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderGlass,
                            errorBorderColor = NeonCrimson,
                            focusedContainerColor = CodeBoxBg,
                            unfocusedContainerColor = CodeBoxBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        modifier = Modifier
                            .testTag("title_input")
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Photo Upload + Live Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.label_photo),
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan
                        )
                        if (state.selectedPhotoUri != null) {
                            Text(
                                text = "Remove",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCrimson,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPhotoSelected(null) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val pickerShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .testTag("photo_picker_button")
                            .fillMaxWidth()
                            .height(if (state.selectedPhotoUri != null) 168.dp else 96.dp)
                            .clip(pickerShape)
                            .background(CodeBoxBg)
                            .border(
                                width = 1.2.dp,
                                color = if (state.selectedPhotoUri != null) NeonCyan.copy(alpha = 0.6f)
                                else BorderGlass,
                                shape = pickerShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = NeonCyan),
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.selectedPhotoUri != null) {
                            AsyncImage(
                                model = state.selectedPhotoUri,
                                contentDescription = stringResource(R.string.cd_photo_preview),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(DeepSpaceBg.copy(alpha = 0.85f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.action_change_photo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.action_pick_photo),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Script / Text Input
                    Text(
                        text = stringResource(R.string.label_script),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.scriptContent,
                        onValueChange = onScriptChanged,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_script),
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp
                                ),
                                color = TextMuted
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextPrimary
                        ),
                        minLines = 5,
                        maxLines = 8,
                        isError = state.scriptError != null,
                        supportingText = state.scriptError?.let { err ->
                            { Text(text = err, color = NeonCrimson) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderGlass,
                            errorBorderColor = NeonCrimson,
                            focusedContainerColor = CodeBoxBg,
                            unfocusedContainerColor = CodeBoxBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        modifier = Modifier
                            .testTag("script_input")
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = TextSecondary),
                                    onClick = onDismiss
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        val publishShape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .testTag("publish_button")
                                .heightIn(min = 48.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = publishShape,
                                    ambientColor = NeonCyan.copy(alpha = 0.6f),
                                    spotColor = NeonCrimson.copy(alpha = 0.6f)
                                )
                                .clip(publishShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonCyan, Color(0xFF00C2FF))
                                    )
                                )
                                .clickable(
                                    enabled = !state.isPublishing,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = DeepSpaceBg),
                                    onClick = { onPublish(titleRequiredError, scriptRequiredError) }
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Publish,
                                    contentDescription = null,
                                    tint = DeepSpaceBg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_publish),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = DeepSpaceBg
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
