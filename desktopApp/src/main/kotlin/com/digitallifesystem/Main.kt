package com.digitallifesystem

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application { Window(onCloseRequest = ::exitApplication, title = "Цифровая система управления жизнью") { App() } }
