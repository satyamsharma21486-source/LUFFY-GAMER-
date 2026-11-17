package com.example.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScriptItem
import com.example.data.ScriptRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScriptsFeedState {
    data object Loading : ScriptsFeedState
    data class Success(
        val scripts: List<ScriptItem>,
        val totalCount: Int
    ) : ScriptsFeedState
    data class Error(val message: String) : ScriptsFeedState
}

data class CreateModalState(
    val isOpen: Boolean = false,
    val title: String = "",
    val scriptContent: String = "",
    val selectedPhotoUri: Uri? = null,
    val titleError: String? = null,
    val scriptError: String? = null,
    val isPublishing: Boolean = false
)

data class ScriptHubUiState(
    val feedState: ScriptsFeedState = ScriptsFeedState.Loading,
    val searchQuery: String = "",
    val expandedCardIds: Set<String> = emptySet(),
    val revealedScriptIds: Set<String> = emptySet(),
    val copiedCardId: String? = null,
    val actionError: String? = null,
    val modalState: CreateModalState = CreateModalState()
)

class ScriptHubViewModel(
    private val repository: ScriptRepository,
    private val currentUserId: String
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val expandedCardIds = MutableStateFlow<Set<String>>(emptySet())
    private val revealedScriptIds = MutableStateFlow<Set<String>>(emptySet())
    private val copiedCardId = MutableStateFlow<String?>(null)
    private val actionError = MutableStateFlow<String?>(null)
    private val modalState = MutableStateFlow(CreateModalState())
    private var copyResetJob: Job? = null

    private sealed interface UpstreamSnapshot {
        data object Loading : UpstreamSnapshot
        data class Loaded(val items: List<ScriptItem>) : UpstreamSnapshot
        data class Failed(val message: String) : UpstreamSnapshot
    }

    private val upstreamScripts: StateFlow<UpstreamSnapshot> =
        repository.observeScripts(currentUserId)
            .map<List<ScriptItem>, UpstreamSnapshot> { UpstreamSnapshot.Loaded(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing scripts", rootError)
                emit(UpstreamSnapshot.Failed(rootError.localizedMessage ?: "Failed to load scripts"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UpstreamSnapshot.Loading
            )

    private val filteredFeedFlow = combine(upstreamScripts, searchQuery) { snapshot, query ->
        val feedState = when (snapshot) {
            is UpstreamSnapshot.Loading -> ScriptsFeedState.Loading
            is UpstreamSnapshot.Failed -> ScriptsFeedState.Error(snapshot.message)
            is UpstreamSnapshot.Loaded -> {
                val trimmed = query.trim()
                val filtered = if (trimmed.isEmpty()) {
                    snapshot.items
                } else {
                    snapshot.items.filter { it.title.contains(trimmed, ignoreCase = true) }
                }
                ScriptsFeedState.Success(scripts = filtered, totalCount = snapshot.items.size)
            }
        }
        feedState to query
    }

    private val interactionFlow = combine(
        expandedCardIds,
        revealedScriptIds,
        copiedCardId,
        actionError,
        modalState
    ) { expanded, revealed, copiedId, err, modal ->
        InteractionSnapshot(expanded, revealed, copiedId, err, modal)
    }

    private data class InteractionSnapshot(
        val expandedCardIds: Set<String>,
        val revealedScriptIds: Set<String>,
        val copiedCardId: String?,
        val actionError: String?,
        val modalState: CreateModalState
    )

    val uiState: StateFlow<ScriptHubUiState> = combine(
        filteredFeedFlow,
        interactionFlow
    ) { (feedState, query), interaction ->
        ScriptHubUiState(
            feedState = feedState,
            searchQuery = query,
            expandedCardIds = interaction.expandedCardIds,
            revealedScriptIds = interaction.revealedScriptIds,
            copiedCardId = interaction.copiedCardId,
            actionError = interaction.actionError,
            modalState = interaction.modalState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ScriptHubUiState()
    )

    init {
        viewModelScope.launch {
            val result = repository.ensureSeededForUser(currentUserId)
            result.onFailure { err ->
                actionError.value = err.localizedMessage ?: "Could not initialize sample scripts"
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun dismissActionError() {
        actionError.value = null
    }

    fun onCardClicked(scriptId: String) {
        expandedCardIds.update { current ->
            if (scriptId in current) {
                revealedScriptIds.update { it - scriptId }
                current - scriptId
            } else {
                current + scriptId
            }
        }
    }

    fun onGetScriptClicked(scriptId: String) {
        revealedScriptIds.update { current ->
            if (scriptId in current) current - scriptId else current + scriptId
        }
    }

    fun onScriptCopied(scriptId: String) {
        copyResetJob?.cancel()
        copiedCardId.value = scriptId
        copyResetJob = viewModelScope.launch {
            delay(2200L)
            if (copiedCardId.value == scriptId) {
                copiedCardId.value = null
            }
        }
    }

    fun onDeleteScript(script: ScriptItem) {
        viewModelScope.launch {
            expandedCardIds.update { it - script.id }
            revealedScriptIds.update { it - script.id }
            val result = repository.deleteScript(currentUserId, script)
            result.onFailure { err ->
                actionError.value = err.localizedMessage ?: "Failed to delete script"
            }
        }
    }

    fun openCreateModal() {
        modalState.value = CreateModalState(isOpen = true)
    }

    fun closeCreateModal() {
        modalState.update { it.copy(isOpen = false) }
    }

    fun onTitleChanged(newTitle: String) {
        val capped = newTitle.take(ScriptRepository.MAX_TITLE_LENGTH)
        modalState.update { it.copy(title = capped, titleError = null) }
    }

    fun onScriptContentChanged(newContent: String) {
        val capped = newContent.take(ScriptRepository.MAX_SCRIPT_LENGTH)
        modalState.update { it.copy(scriptContent = capped, scriptError = null) }
    }

    fun onPhotoSelected(uri: Uri?) {
        modalState.update { it.copy(selectedPhotoUri = uri) }
    }

    fun publishScript(titleErrorMsg: String, scriptErrorMsg: String) {
        val current = modalState.value
        val trimmedTitle = current.title.trim()
        val trimmedScript = current.scriptContent.trim()

        val titleErr = if (trimmedTitle.isEmpty()) titleErrorMsg else null
        val scriptErr = if (trimmedScript.isEmpty()) scriptErrorMsg else null

        if (titleErr != null || scriptErr != null) {
            modalState.update { it.copy(titleError = titleErr, scriptError = scriptErr) }
            return
        }

        if (current.isPublishing) return

        modalState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            val result = repository.createScript(
                userId = currentUserId,
                title = trimmedTitle,
                scriptContent = trimmedScript,
                pickedPhotoUri = current.selectedPhotoUri
            )
            result.onSuccess {
                modalState.value = CreateModalState(isOpen = false)
            }.onFailure { err ->
                modalState.update { it.copy(isPublishing = false) }
                actionError.value = err.localizedMessage ?: "Failed to publish script"
            }
        }
    }

    private companion object {
        const val TAG = "ScriptHubVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
