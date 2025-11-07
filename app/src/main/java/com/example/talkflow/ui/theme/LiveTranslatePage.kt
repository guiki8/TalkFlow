package com.example.talkflow.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.talkflow.R
import com.google.mlkit.nl.translate.*
import com.google.mlkit.common.model.DownloadConditions

@Composable
fun LiveTranslatePage(onBackToChat: () -> Unit) {
    val context = LocalContext.current
    var sourceLang by remember { mutableStateOf("en") } // idioma falado
    var targetLang by remember { mutableStateOf("pt") } // idioma da tradução
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    // Launcher para o microfone
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        spokenText?.let {
            recognizedText = it
            translateText(it, sourceLang, targetLang) { translated ->
                translatedText = translated
            }
        }
        isListening = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Topo
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TalkFlow Microfone", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(onClick = { onBackToChat() }) {
                    Icon(Icons.Default.Chat, contentDescription = "Ir para tradução em tempo real", tint = colorResource(id = R.color.primary))
                }
            }
        }

        // Botão para selecionar idiomas
        Button(
            onClick = { showLanguageMenu = true },
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary))
        ) {
            Icon(Icons.Default.Translate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("${getLangName(sourceLang)} ➜ ${getLangName(targetLang)}")
        }

        // Menu suspenso de idiomas
        if (showLanguageMenu) {
            AlertDialog(
                onDismissRequest = { showLanguageMenu = false },
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
                    TextButton(onClick = { showLanguageMenu = false }) {
                        Text("Fechar")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Área de texto original e traduzido
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                recognizedText.ifBlank { "Fale algo..." },
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
            Divider(color = Color.DarkGray, thickness = 1.dp)
            Text(
                translatedText,
                color = colorResource(id = R.color.primary),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Botão de microfone
        Button(
            onClick = {
                val localeCode = getLocaleTag(sourceLang) // 👈 CORREÇÃO AQUI
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeCode)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale agora...")
                }
                isListening = true
                speechLauncher.launch(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color.Red else colorResource(id = R.color.primary)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isListening) "Ouvindo..." else "Falar")
        }
    }
}

/* ---------------- FUNÇÕES DE SUPORTE ---------------- */

@Composable
fun LanguageDropdown(selectedLang: String, onSelect: (String) -> Unit) {
    val languages = listOf(
        "pt" to "🇧🇷 Português",
        "en" to "🇺🇸 Inglês",
        "es" to "🇪🇸 Espanhol",
        "fr" to "🇫🇷 Francês",
        "de" to "🇩🇪 Alemão",
        "it" to "🇮🇹 Italiano",
        "ja" to "🇯🇵 Japonês",
        "ko" to "🇰🇷 Coreano"
    )

    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(getLangName(selectedLang))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun getLangName(code: String): String {
    return when (code) {
        "pt" -> "🇧🇷 Português"
        "en" -> "🇺🇸 Inglês"
        "es" -> "🇪🇸 Espanhol"
        "fr" -> "🇫🇷 Francês"
        "de" -> "🇩🇪 Alemão"
        "it" -> "🇮🇹 Italiano"
        "ja" -> "🇯🇵 Japonês"
        "ko" -> "🇰🇷 Coreano"
        else -> code
    }
}

// 🔧 Converte código curto (pt, en...) em Locale Tag (pt-BR, en-US...)
fun getLocaleTag(code: String): String {
    return when (code) {
        "pt" -> "pt-BR"
        "en" -> "en-US"
        "es" -> "es-ES"
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "it" -> "it-IT"
        "ja" -> "ja-JP"
        "ko" -> "ko-KR"
        else -> "en-US"
    }
}

// Função que traduz o texto com MLKit
fun translateText(text: String, sourceLang: String, targetLang: String, onResult: (String) -> Unit) {
    val options = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLang)
        .setTargetLanguage(targetLang)
        .build()

    val translator = Translation.getClient(options)
    val conditions = DownloadConditions.Builder().requireWifi().build()

    translator.downloadModelIfNeeded(conditions)
        .addOnSuccessListener {
            translator.translate(text)
                .addOnSuccessListener { translated -> onResult(translated) }
                .addOnFailureListener { onResult("Erro na tradução") }
        }
        .addOnFailureListener { onResult("Erro ao baixar modelo") }
}
