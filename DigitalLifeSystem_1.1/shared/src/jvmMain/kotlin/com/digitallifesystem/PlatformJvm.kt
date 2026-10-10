package com.digitallifesystem

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import org.jetbrains.skia.Image
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object PlatformStorage {
    actual fun initialize() = Unit
    private val file = File(System.getProperty("user.home"), "DigitalLifeSystem/users.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    actual fun loadUsers(): MutableMap<String, UserData> = try {
        val source = if (file.exists()) file else File("users.json")
        if (!source.exists()) mutableMapOf() else json.decodeFromString<MutableMap<String, UserData>>(source.readText())
    } catch (_: Exception) { mutableMapOf() }
    actual fun saveUsers(users: Map<String, UserData>) { file.parentFile.mkdirs(); file.writeText(json.encodeToString(users)) }
    private val sessionFile = File(System.getProperty("user.home"), "DigitalLifeSystem/session.txt")
    actual fun loadSessionUsername(): String? = try { sessionFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
    actual fun saveSessionUsername(username: String?) {
        sessionFile.parentFile.mkdirs()
        if (username.isNullOrBlank()) sessionFile.delete() else sessionFile.writeText(username)
    }
}

actual fun platformName(): String = "Desktop"

actual fun pickImage(onPicked: (String) -> Unit) {
    val chooser = javax.swing.JFileChooser()
    chooser.dialogTitle = "Выберите изображение"
    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) onPicked(chooser.selectedFile.absolutePath)
}

actual fun loadPlatformImage(path: String): ImageBitmap? = try {
    Image.makeFromEncoded(File(path).readBytes()).toComposeImageBitmap()
} catch (_: Exception) { null }
