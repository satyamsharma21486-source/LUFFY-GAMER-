package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ScriptRepository(
    private val context: Context,
    private val dao: ScriptDao
) {
    private val prefs = context.getSharedPreferences("script_hub_prefs", Context.MODE_PRIVATE)

    val scriptsFlow: Flow<List<ScriptEntity>> = dao.observeAllScripts()

    suspend fun ensureSeededIfFirstLaunch() {
        withContext(Dispatchers.IO) {
            val alreadySeeded = prefs.getBoolean(KEY_SEEDED, false)
            if (!alreadySeeded) {
                if (dao.countScripts() == 0) {
                    dao.insertScripts(SampleScripts.buildSamples(System.currentTimeMillis()))
                }
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            }
        }
    }

    suspend fun createScript(
        title: String,
        scriptContent: String,
        pickedPhotoUri: Uri?
    ) {
        withContext(Dispatchers.IO) {
            val savedPath = pickedPhotoUri?.let { copyUriToInternalFile(it) }
            dao.insertScript(
                ScriptEntity(
                    title = title.trim(),
                    scriptContent = scriptContent.trim(),
                    imagePath = savedPath,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteScript(script: ScriptEntity) {
        withContext(Dispatchers.IO) {
            dao.deleteScriptById(script.id)
            script.imagePath?.let { path ->
                runCatching { File(path).delete() }
            }
        }
    }

    private fun copyUriToInternalFile(uri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, "script_covers").apply { mkdirs() }
            val dest = File(dir, "cover_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            dest.absolutePath
        }.getOrNull()
    }

    companion object {
        private const val KEY_SEEDED = "has_seeded_samples"
    }
}
