package com.digitallifesystem

import androidx.compose.ui.graphics.ImageBitmap

expect object PlatformStorage {
    /** Run one-time platform storage initialization off the UI thread. */
    fun initialize()
    fun loadUsers(): MutableMap<String, UserData>
    fun saveUsers(users: Map<String, UserData>)
    fun loadSessionUsername(): String?
    fun saveSessionUsername(username: String?)
}

expect fun platformName(): String
expect fun pickImage(onPicked: (String) -> Unit)
expect fun loadPlatformImage(path: String): ImageBitmap?

/** Opens a text (.txt) file picker and returns the file contents as UTF-8 text. */
expect fun pickTextFile(onPicked: (String) -> Unit)
