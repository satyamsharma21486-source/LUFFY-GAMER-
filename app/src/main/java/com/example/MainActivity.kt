package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CodeOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ScriptRepository
import com.example.ui.ScriptHubViewModel
import com.example.ui.ScriptsFeedState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.CreateScriptModal
import com.example.ui.components.CyberBackdrop
import com.example.ui.components.FeedSearchBar
import com.example.ui.components.ScriptCard
import com.example.ui.components.ScriptHubTopBar
import com.example.ui.theme.BorderGlass
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ScriptHubTheme
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            ScriptHubTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: ScriptHubViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        ScriptHubViewModel(
                            repository = ScriptRepository(app, db),
                            currentUserId = user.uid
                        )
                    }
                }
            )
            ScriptHubScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun ScriptHubScreen(viewModel: ScriptHubViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val totalCount = when (val feed = uiState.feedState) {
        is ScriptsFeedState.Success -> feed.totalCount
        else -> 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CyberBackdrop()

        Column(modifier = Modifier.fillMaxSize()) {
            ScriptHubTopBar(
                scriptCount = totalCount,
                onCreateClick = viewModel::openCreateModal,
                onSignOutClick = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 800.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = uiState.actionError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ActionErrorBanner(
                            message = uiState.actionError.orEmpty(),
                            onDismiss = viewModel::dismissActionError,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    when (val feed = uiState.feedState) {
                        is ScriptsFeedState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NeonCyan,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        is ScriptsFeedState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                FeedErrorState(
                                    message = feed.message,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                        is ScriptsFeedState.Success -> {
                            if (feed.totalCount > 0) {
                                FeedSearchBar(
                                    query = uiState.searchQuery,
                                    resultCount = feed.scripts.size,
                                    onQueryChange = viewModel::onSearchQueryChanged,
                                    onClearClick = viewModel::clearSearch,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = 6.dp
                                    )
                                )
                            }

                            when {
                                feed.totalCount == 0 -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EmptyFeedState(modifier = Modifier.padding(24.dp))
                                    }
                                }
                                feed.scripts.isEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EmptySearchState(
                                            onClearClick = viewModel::clearSearch,
                                            modifier = Modifier.padding(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 12.dp,
                                            bottom = 36.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        itemsIndexed(
                                            items = feed.scripts,
                                            key = { _, item -> item.id }
                                        ) { index, script ->
                                            ScriptCard(
                                                script = script,
                                                index = index,
                                                isExpanded = script.id in uiState.expandedCardIds,
                                                isScriptRevealed = script.id in uiState.revealedScriptIds,
                                                isCopied = uiState.copiedCardId == script.id,
                                                onCardClick = { viewModel.onCardClicked(script.id) },
                                                onGetScriptClick = { viewModel.onGetScriptClicked(script.id) },
                                                onCopySuccess = { viewModel.onScriptCopied(script.id) },
                                                onDeleteClick = { viewModel.onDeleteScript(script) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        CreateScriptModal(
            state = uiState.modalState,
            onDismiss = viewModel::closeCreateModal,
            onTitleChanged = viewModel::onTitleChanged,
            onScriptChanged = viewModel::onScriptContentChanged,
            onPhotoSelected = viewModel::onPhotoSelected,
            onPublish = viewModel::publishScript
        )
    }
}

@Composable
private fun ActionErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeonCrimson.copy(alpha = 0.16f))
            .border(1.dp, NeonCrimson.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = NeonCrimson,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cd_close_modal),
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FeedErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass)
            .border(1.dp, NeonCrimson.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NeonCrimson.copy(alpha = 0.14f))
                .border(1.dp, NeonCrimson.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = NeonCrimson,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load scripts",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyFeedState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NeonCyan.copy(alpha = 0.12f))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CodeOff,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptySearchState(
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NeonCrimson.copy(alpha = 0.12f))
                .border(1.dp, NeonCrimson.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = NeonCrimson,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_search_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_search_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .testTag("empty_clear_search_button")
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeonCyan.copy(alpha = 0.14f))
                .border(1.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = NeonCyan),
                    onClick = onClearClick
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.action_clear_search),
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan
            )
        }
    }
}
