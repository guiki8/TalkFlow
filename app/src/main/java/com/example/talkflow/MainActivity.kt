package com.example.talkflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.talkflow.ui.theme.TalkFlowTheme
import com.example.talkflow.ui.theme.components.ChatBubble
import com.example.talkflow.viewmodel.ChatViewModel
import com.google.mlkit.nl.languageid.LanguageIdentification
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicita permissão de microfone
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        setContent {
            TalkFlowTheme {
                ChatScreen(viewModel)
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var input by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("auto") }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val languageIdentifier = remember { LanguageIdentification.getClient() }

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ComponentActivity.RESULT_OK) {
                val spokenText =
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                if (!spokenText.isNullOrBlank()) {
                    if (selectedLanguage == "auto") {
                        // Detecta o idioma automaticamente
                        languageIdentifier.identifyLanguage(spokenText)
                            .addOnSuccessListener { code ->
                                val idioma = when (code) {
                                    "pt" -> "Português"
                                    "en" -> "Inglês"
                                    "es" -> "Espanhol"
                                    "fr" -> "Francês"
                                    "de" -> "Alemão"
                                    "it" -> "Italiano"
                                    "ja" -> "Japonês"
                                    "ko" -> "Coreano"
                                    "zh" -> "Chinês"
                                    "und" -> "Indefinido"
                                    else -> code.uppercase()
                                }
                                viewModel.sendMessage("Idioma detectado: $idioma")
                                viewModel.sendMessage("Você disse: $spokenText")
                            }
                            .addOnFailureListener {
                                viewModel.sendMessage("Não foi possível identificar o idioma.")
                            }
                    } else {
                        // Usa o idioma selecionado manualmente
                        viewModel.sendMessage("Você disse ($selectedLanguage): $spokenText")
                    }
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .statusBarsPadding()
            .padding(8.dp)
    ) {
        // Mostrar idioma atual
        Text(
            text = "Idioma atual: ${idiomaNome(selectedLanguage)}",
            color = Color.LightGray,
            modifier = Modifier.padding(4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed(), key = { it.id }) { message ->
                ChatBubble(message.text, message.isFromUser)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de idioma
            IconButton(onClick = { showLanguageMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Selecionar idioma",
                    tint = Color(0xFF7B61FF)
                )
            }

            DropdownMenu(
                expanded = showLanguageMenu,
                onDismissRequest = { showLanguageMenu = false }
            ) {
                val languages = listOf(
                    "Automático" to "auto",
                    "🇧🇷 Português" to "pt-BR",
                    "🇺🇸 Inglês" to "en-US",
                    "🇪🇸 Espanhol" to "es-ES",
                    "🇫🇷 Francês" to "fr-FR",
                    "🇩🇪 Alemão" to "de-DE",
                    "🇮🇹 Italiano" to "it-IT",
                    "🇯🇵 Japonês" to "ja-JP"
                )
                languages.forEach { (name, code) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedLanguage = code
                            showLanguageMenu = false
                            // Remove o emoji antes de exibir a mensagem de confirmação
                            val languageNameOnly = name.substringAfter(" ")
                            if (code == "auto") {
                                viewModel.sendMessage("Idioma alterado para: Automático")
                            } else {
                                viewModel.sendMessage("Idioma alterado para: $languageNameOnly")
                            }
                        }
                    )
                }
            } // This closing brace was missing

            Spacer(modifier = Modifier.width(4.dp))

            // Botão de microfone
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE,
                            if (selectedLanguage == "auto") Locale.getDefault().toString() else selectedLanguage
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale agora…")
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Reconhecimento de voz não suportado neste dispositivo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Falar",
                    tint = Color(0xFF7B61FF)
                )
            }

            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Digite sua mensagem...") }
            )

            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.sendMessage(input)
                        input = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
            ) {
                Text("Enviar")
            }
        }
    }
}

// ✅ Moved this function to be at the top-level of the file
private fun idiomaNome(code: String): String {
    return when (code) {
        "auto" -> "Automático"
        "pt-BR" -> "Português"
        "en-US" -> "Inglês"
        "es-ES" -> "Espanhol"
        "fr-FR" -> "Francês"
        "de-DE" -> "Alemão"
        "it-IT" -> "Italiano"
        "ja-JP" -> "Japonês"
        else -> code
    }
}
