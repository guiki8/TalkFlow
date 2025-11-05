package com.example.talkflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(onNavigateToLive: () -> Unit) {
    var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var input by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Cabeçalho com botão para mudar de página
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💬 TalkFlow Chat",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = { onNavigateToLive() }) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Ir para tradução em tempo real",
                    tint = Color(0xFF7B61FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lista de mensagens
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { (text, isUser) ->
                ChatBubble(text = text, isFromUser = isUser)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de entrada
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Digite uma mensagem...", color = Color.LightGray) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors( // ✅ Corrected: use .colors instead of .textFieldColors
                    unfocusedContainerColor = Color(0xFF222222), // Use specific states for colors
                    focusedContainerColor = Color(0xFF222222),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        messages = messages + (text to true)
                        messages = messages + (text to false)
                        input = TextFieldValue("")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isFromUser: Boolean) {
    val bubbleColor = if (isFromUser) Color(0xFF7B61FF) else Color(0xFF2E2E2E)
    val alignment = if (isFromUser) Alignment.End else Alignment.Start

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
    }
}
