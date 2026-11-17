package com.example.data

import android.content.Context
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ScriptRepository(
    private val appContext: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val prefs = appContext.getSharedPreferences("script_hub_prefs", Context.MODE_PRIVATE)

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    fun observeScripts(userId: String): Flow<List<ScriptItem>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/scripts"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("scripts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScriptItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureSeededForUser(userId: String): Result<Unit> {
        val collectionPath = "users/$userId/scripts"
        return try {
            val uid = requireUserId(userId)
            val seedKey = "has_seeded_samples_$uid"
            if (prefs.getBoolean(seedKey, false)) {
                return Result.success(Unit)
            }

            val scriptsRef = db.collection("users").document(uid).collection("scripts")
            val existingSnapshot = scriptsRef.limit(1).get().await()
            if (existingSnapshot.isEmpty) {
                val batch = db.batch()
                SampleScripts.buildSamples().forEach { sample ->
                    val docRef = scriptsRef.document(sample.docId)
                    val payload = mapOf(
                        "userId" to uid,
                        "title" to sample.title,
                        "scriptContent" to sample.scriptContent,
                        "sampleDrawableName" to sample.sampleDrawableName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                }
                batch.commit().await()
            }
            prefs.edit().putBoolean(seedKey, true).apply()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, collectionPath)
            Result.failure(e)
        }
    }

    suspend fun createScript(
        userId: String,
        title: String,
        scriptContent: String,
        pickedPhotoUri: Uri?
    ): Result<Unit> {
        var docPath = "users/$userId/scripts"
        return try {
            val uid = requireUserId(userId)
            val trimmedTitle = title.trim()
            val trimmedScript = scriptContent.trim()
            require(trimmedTitle.length in 1..MAX_TITLE_LENGTH) {
                "Title must be between 1 and $MAX_TITLE_LENGTH characters."
            }
            require(trimmedScript.length in 1..MAX_SCRIPT_LENGTH) {
                "Script must be between 1 and $MAX_SCRIPT_LENGTH characters."
            }

            val savedPath = if (pickedPhotoUri != null) {
                withContext(Dispatchers.IO) { copyUriToInternalFile(pickedPhotoUri) }
            } else null

            val docRef = db.collection("users").document(uid).collection("scripts").document()
            docPath = docRef.path

            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("title", trimmedTitle)
                put("scriptContent", trimmedScript)
                savedPath?.take(MAX_PATH_LENGTH)?.let { put("localImagePath", it) }
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteScript(userId: String, script: ScriptItem): Result<Unit> {
        var docPath = "users/$userId/scripts/${script.id}"
        return try {
            val uid = requireUserId(userId)
            require(script.id.isNotBlank()) { "Script ID must not be blank for delete." }
            val docRef = db.collection("users").document(uid)
                .collection("scripts").document(script.id)
            docPath = docRef.path
            docRef.delete().await()

            script.localImagePath?.let { path ->
                withContext(Dispatchers.IO) {
                    runCatching { File(path).delete() }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun copyUriToInternalFile(uri: Uri): String? {
        return runCatching {
            val dir = File(appContext.filesDir, "script_covers").apply { mkdirs() }
            val dest = File(dir, "cover_${UUID.randomUUID()}.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            dest.absolutePath
        }.getOrNull()
    }

    companion object {
        const val MAX_TITLE_LENGTH = 120
        const val MAX_SCRIPT_LENGTH = 50000
        const val MAX_PATH_LENGTH = 500
    }
}
