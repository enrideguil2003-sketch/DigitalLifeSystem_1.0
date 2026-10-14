package com.digitallifesystem

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private lateinit var appContext: Context
private const val PROGRESS_RESET_PREF = "progress_reset_v1"

fun initPlatform(context: Context) {
    // Only capture the application context here. Disk access and the legacy
    // one-time reset are deferred until App launches its IO coroutine.
    appContext = context.applicationContext
}

private fun resetAllProgressOnce() {
    val prefs = appContext.getSharedPreferences("digital_life_system", Context.MODE_PRIVATE)
    if (prefs.getBoolean(PROGRESS_RESET_PREF, false)) return
    try {
        val users = PlatformStorage.loadUsers()
        if (users.isNotEmpty()) {
            users.values.forEach { user ->
                // Reset achieved/progression state, while preserving accounts,
                // passwords, profile appearance and task definitions.
                user.level = 0
                user.level_xp = 0
                user.reputation_xp = 0
                user.reputation_rank = 0
                user.achievements.clear()
                user.calendar_marks.clear()
                user.training_completions.clear()
                user.custom_trainings.clear()
                user.training_segment_overrides.clear()
                user.training_start_date = todayIso()
                user.tasks.forEach { task ->
                    task.done = false
                    task.penalized = false
                    task.hard_missed = false
                    task.xp_awarded_amount = 0
                    task.completed_date = ""
                    task.remind_sent = false
                    task.start_notification_sent = false
                }
            }
            PlatformStorage.saveUsers(users)
        }
        prefs.edit().putBoolean(PROGRESS_RESET_PREF, true).apply()
    } catch (_: Exception) {
        // If storage is temporarily unavailable, do not mark the reset complete.
    }
}
fun initImagePickerCallback(callback: (String?) -> Unit) { pendingPickerResult = callback }
private var pendingPickerResult: ((String?) -> Unit)? = null

fun onPickedUri(uri: android.net.Uri?) {
    if (uri == null) { pendingPickerResult?.invoke(null); pendingPickerResult = null; return }
    try {
        val ext = appContext.contentResolver.getType(uri)?.substringAfterLast('/') ?: "png"
        val file = File(appContext.filesDir, "user_image_${System.currentTimeMillis()}.$ext")
        appContext.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        pendingPickerResult?.invoke(file.absolutePath)
    } catch (_: Exception) { pendingPickerResult?.invoke(null) }
    pendingPickerResult = null
}

actual object PlatformStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    actual fun initialize() { resetAllProgressOnce() }
    actual fun loadUsers(): MutableMap<String, UserData> = try {
        val f=appContext.getFileStreamPath("users.json")
        if(f.exists()) json.decodeFromString<MutableMap<String, UserData>>(f.readText())
        else appContext.assets.open("users.json").bufferedReader().use { json.decodeFromString<MutableMap<String, UserData>>(it.readText()) }
    } catch(_:Exception){ mutableMapOf() }
    actual fun saveUsers(users: Map<String, UserData>) { appContext.openFileOutput("users.json", Context.MODE_PRIVATE).use { it.write(json.encodeToString(users).toByteArray()) } }
    actual fun loadSessionUsername(): String? {
        val value = appContext.getSharedPreferences("digital_life_system_session", Context.MODE_PRIVATE)
            .getString("username", null)
        return value?.takeIf { it.isNotBlank() }
    }
    actual fun saveSessionUsername(username: String?) {
        val editor = appContext.getSharedPreferences("digital_life_system_session", Context.MODE_PRIVATE).edit()
        if (username.isNullOrBlank()) editor.remove("username") else editor.putString("username", username)
        editor.apply()
    }
}

actual fun platformName(): String = "Android"

actual fun pickImage(onPicked: (String) -> Unit) {
    pendingPickerResult = { path -> if (path != null) onPicked(path) }
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
    }
    ImagePickerHost.start(intent)
}

actual fun pickTextFile(onPicked: (String) -> Unit) {
    pendingPickerResult = { path ->
        if (path != null) {
            try {
                val f = File(path)
                val text = f.readText(Charsets.UTF_8)
                f.delete()
                onPicked(text)
            } catch (_: Exception) { }
        }
    }
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
    }
    ImagePickerHost.start(intent)
}

actual fun loadPlatformImage(path: String): ImageBitmap? = try {
    BitmapFactory.decodeFile(path)?.asImageBitmap()
} catch (_: Exception) { null }

object ImagePickerHost {
    lateinit var start: (Intent) -> Unit
}
