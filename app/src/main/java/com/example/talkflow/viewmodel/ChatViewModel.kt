package com.example.talkflow.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.talkflow.data.Message

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun sendMessage(text: String) {
        val newMsg = Message(text = text, isFromUser = true)
        val botReply = Message(text = "Você disse: $text", isFromUser = false)
        _messages.value += listOf(newMsg, botReply)
    }
}
