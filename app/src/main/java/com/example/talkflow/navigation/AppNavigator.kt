package com.example.talkflow.navigation

import androidx.compose.runtime.*
import com.example.talkflow.ui.ChatPage
import com.example.talkflow.ui.LiveTranslatePage

@Composable
fun AppNavigator() {
    var currentPage by remember { mutableStateOf("chat") }

    when (currentPage) {
        "chat" -> ChatPage(onNavigateToLive = { currentPage = "live" })
        "live" -> LiveTranslatePage(onBackToChat = { currentPage = "chat" })
    }
}
