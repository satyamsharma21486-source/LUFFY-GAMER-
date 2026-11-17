package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScriptDatabase
import com.example.data.ScriptEntity
import com.example.data.ScriptRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val scripts: List<ScriptEntity> = emptyList(),
    val totalScriptCount: Int = 0,
    val searchQuery: String = "",
    val expandedCardIds: Set<Long> = emptySet(),
    val revealedScriptIds: Set<Long> = emptySet(),
    val copiedCardId: Long? = null,
    val modalState: CreateModalState = CreateModalState()
)

class ScriptHubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScriptRepository = ScriptRepository(
        context = application,
        dao = ScriptDatabase.getInstance(application).scriptDao()
    )

    private val searchQuery = MutableStateFlow("")
    private val expandedCardIds = MutableStateFlow<Set<Long>>(emptySet())
    private val revealedScriptIds = MutableStateFlow<Set<Long>>(emptySet())
    private val copiedCardId = MutableStateFlow<Long?>(null)
    private val modalState = MutableStateFlow(CreateModalState())
    private var copyResetJob: Job? = null

    private val filteredScriptsFlow = combine(
        repository.scriptsFlow,
        searchQuery
    ) { scripts, query ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            scripts
        } else {
            scripts.filter { it.title.contains(trimmed, ignoreCase = true) }
        }
        Triple(filtered, scripts.size, query)
    }

    val uiState: StateFlow<ScriptHubUiState> = combine(
        filteredScriptsFlow,
        expandedCardIds,
        revealedScriptIds,
        copiedCardId,
        modalState
    ) { (filteredScripts, totalCount, query), expanded, revealed, copiedId, modal ->
        ScriptHubUiState(
            scripts = filteredScripts,
            totalScriptCount = totalCount,
            searchQuery = query,
            expandedCardIds = expanded,
            revealedScriptIds = revealed,
            copiedCardId = copiedId,
            modalState = modal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScriptHubUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededIfFirstLaunch()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun onCardClicked(scriptId: Long) {
        expandedCardIds.update { current ->
            if (scriptId in current) {
                revealedScriptIds.update { it - scriptId }
                current - scriptId
            } else {
                current + scriptId
            }
        }
    }

    fun onGetScriptClicked(scriptId: Long) {
        revealedScriptIds.update { current ->
            if (scriptId in current) current - scriptId else current + scriptId
        }
    }

    fun onScriptCopied(scriptId: Long) {
        copyResetJob?.cancel()
        copiedCardId.value = scriptId
        copyResetJob = viewModelScope.launch {
            delay(2200L)
            if (copiedCardId.value == scriptId) {
                copiedCardId.value = null
            }
        }
    }

    fun onDeleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            expandedCardIds.update { it - script.id }
            revealedScriptIds.update { it - script.id }
            repository.deleteScript(script)
        }
    }

    fun openCreateModal() {
        modalState.value = CreateModalState(isOpen = true)
    }

    fun closeCreateModal() {
        modalState.update { it.copy(isOpen = false) }
    }

    fun onTitleChanged(newTitle: String) {
        modalState.update { it.copy(title = newTitle, titleError = null) }
    }

    fun onScriptContentChanged(newContent: String) {
        modalState.update { it.copy(scriptContent = newContent, scriptError = null) }
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
            repository.createScript(
                title = trimmedTitle,
                scriptContent = trimmedScript,
                pickedPhotoUri = current.selectedPhotoUri
            )
            modalState.value = CreateModalState(isOpen = false)
        }
    }
}
