package com.example.talkflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.colorResource
import com.example.talkflow.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(onNavigateToLive: () -> Unit) {
    var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) } // (texto, é usuário?)
    var input by remember { mutableStateOf(TextFieldValue("")) }

    // Idiomas (códigos curtos usados pelo ML Kit: TranslateLanguage.XXX)
    var sourceLang by remember { mutableStateOf("pt") } // idioma da entrada (usuário)
    var targetLang by remember { mutableStateOf("en") } // idioma da resposta (bot)

    var showLangDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar: título + botão ir pra LiveTranslate
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TalkFlow Chat", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(onClick = { onNavigateToLive() }) {
                    Icon(Icons.Default.Mic, contentDescription = "Ir para tradução em tempo real", tint = colorResource(id = R.color.primary))
                }
            }
        }

        // Botão para selecionar idiomas
        Button(
            onClick = { showLangDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary))
        ) {
            Icon(Icons.Default.Translate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("${getLangName(sourceLang)} ➜ ${getLangName(targetLang)}")
        }

        // Menu suspenso de idiomas
        if (showLangDialog) {
            AlertDialog(
                onDismissRequest = { showLangDialog = false },
                title = { Text("Escolher idiomas") },
                text = {
                    Column {
                        Text("Idioma da fala:")
                        LanguageDropdown(selectedLang = sourceLang, onSelect = { sourceLang = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Idioma da tradução:")
                        LanguageDropdown(selectedLang = targetLang, onSelect = { targetLang = it })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLangDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mensagens
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = messages.reversed()) { (text, isUser) ->
                ChatBubble(text = text, isFromUser = isUser)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input + enviar
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Digite uma mensagem...", color = colorResource(id = R.color.secondary)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = colorResource(id = R.color.secondary),
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val userText = input.text.trim()
                    if (userText.isNotEmpty()) {
                        // mostrar mensagem do usuário
                        messages = messages + (userText to true)
                        input = TextFieldValue("")

                        // traduz e mostra resposta
                        coroutineScope.launch {
                            val translated = safeTranslateMLKit(userText, sourceLang, targetLang)
                            messages = messages + (translated to false)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary))
            ) {
                Text("Enviar")
            }
        }
    }
}

/* ---------- Helpers: Lang Dropdown, Bubble, ML Kit translation ---------- */

@Composable
fun LangDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Lista simples — os códigos devem ser suportados pelo ML Kit Translate
    val languages = listOf(
        "pt" to "Português",
        "en" to "Inglês",
        "es" to "Espanhol",
        "fr" to "Francês",
        "de" to "Alemão",
        "it" to "Italiano",
        "ja" to "Japonês",
        "ko" to "Coreano",
        "zh" to "Chinês"
    )

    Box {
        Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E))) {
            Text(languages.find { it.first == selected }?.second ?: selected, color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(onClick = {
                    onSelect(code)
                    expanded = false
                }, text = { Text(name) })
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isFromUser: Boolean) {
    val bubbleColor = if (isFromUser) colorResource(id = R.color.primary) else Color(0xFF2E2E2E)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
    }
}

/**
 * Tradução com ML Kit (on-device). Retorna uma string segura mesmo em erro.
 * Usa DownloadConditions sem forçar Wi-Fi para maior compatibilidade.
 */
suspend fun safeTranslateMLKit(text: String, sourceCode: String, targetCode: String): String {
    return try {
        withContext(Dispatchers.IO) {
            val source = when (sourceCode) {
                "pt" -> TranslateLanguage.PORTUGUESE
                "en" -> TranslateLanguage.ENGLISH
                "es" -> TranslateLanguage.SPANISH
                "fr" -> TranslateLanguage.FRENCH
                "de" -> TranslateLanguage.GERMAN
                "it" -> TranslateLanguage.ITALIAN
                "ja" -> TranslateLanguage.JAPANESE
                "ko" -> TranslateLanguage.KOREAN
                "zh" -> TranslateLanguage.CHINESE
                else -> TranslateLanguage.ENGLISH
            }
            val target = when (targetCode) {
                "pt" -> TranslateLanguage.PORTUGUESE
                "en" -> TranslateLanguage.ENGLISH
                "es" -> TranslateLanguage.SPANISH
                "fr" -> TranslateLanguage.FRENCH
                "de" -> TranslateLanguage.GERMAN
                "it" -> TranslateLanguage.ITALIAN
                "ja" -> TranslateLanguage.JAPANESE
                "ko" -> TranslateLanguage.KOREAN
                "zh" -> TranslateLanguage.CHINESE
                else -> TranslateLanguage.ENGLISH
            }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
            val translator = Translation.getClient(options)

            val conditions = DownloadConditions.Builder().build() // permite download por rede celular também
            translator.downloadModelIfNeeded(conditions).awaitMLKit()
            val translated = translator.translate(text).awaitMLKit()
            translator.close() // libera recurso
            translated
        }
    } catch (e: Exception) {
        // Em caso de erro, devolve o texto original com indicação
        "Erro tradução — original: $text"
    }
}

// helper para aguardar Task<T> como suspending function
suspend fun <T> Task<T>.awaitMLKit(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exc -> cont.resumeWithException(exc) }
}
