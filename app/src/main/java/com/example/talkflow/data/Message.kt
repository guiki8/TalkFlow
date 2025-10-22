package com.example.talkflow.data

import java.util.UUID

// The `isFromUser` property can be added if you need to distinguish
// between user and bot/other person messages for styling.
data class Message(
    val id: String = UUID.randomUUID().toString(), // Unique ID for each message
    val text: String,
    val isFromUser: Boolean
)
