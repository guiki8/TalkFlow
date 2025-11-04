package com.example.talkflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.talkflow.ui.theme.TalkFlowTheme
import com.example.talkflow.ui.theme.components.ChatBubble
import com.example.talkflow.viewmodel.ChatViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        setContent {
            TalkFlowTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "chat") {
                    composable("chat") {
                        ChatScreen(viewModel, onNavigateToLive = {
                            navController.navigate("live")
                        })
                    }
                    composable("live") {
                        LiveTranslateScreen(onBack = {
                            navController.popBackStack()
                        })
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------
// 🧩 ChatScreen
// ------------------------------------------------------
@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateToLive: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("💬 TalkFlow Chat", color = Color.White, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { onNavigateToLive() }) {
                Icon(Icons.Default.Translate, contentDescription = "Ir para tradução em tempo real", tint = Color(0xFF7B61FF))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.reversed(), key = { it.id }) { msg ->
                ChatBubble(msg.text, msg.isFromUser)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Digite sua mensagem...") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    viewModel.sendMessage(input)
                    input = ""
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))) {
                Text("Enviar")
            }
        }
    }
}

// ------------------------------------------------------
// 🎧 LiveTranslateScreen (corrigido com reconhecimento real)
// ------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var originalText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var inputLanguage by remember { mutableStateOf("en") }
    var outputLanguage by remember { mutableStateOf("pt") }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // Idiomas
    val languageList = listOf(
        "en" to "Inglês",
        "pt" to "Português",
        "es" to "Espanhol",
        "fr" to "Francês",
        "de" to "Alemão",
        "it" to "Italiano",
        "ja" to "Japonês",
        "ko" to "Coreano",
        "ru" to "Russo"
    )

    val translator = remember(inputLanguage, outputLanguage) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(inputLanguage) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(outputLanguage) ?: TranslateLanguage.PORTUGUESE)
            .build()
        Translation.getClient(options)
    }

    fun startListening() {
        translator.downloadModelIfNeeded()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, inputLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { listening = false }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                text?.let {
                    originalText = it
                    translator.translate(it)
                        .addOnSuccessListener { res -> translatedText = res }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) originalText = partial
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    DisposableEffect(listening) {
        if (listening) startListening()
        else speechRecognizer.stopListening()
        onDispose { speechRecognizer.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎧 Tradução em tempo real") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF7B61FF),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF101010))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Idioma de entrada:", color = Color.White)
            DropdownMenuBox(languageList, inputLanguage) { inputLanguage = it }

            Text("Idioma de saída:", color = Color.White)
            DropdownMenuBox(languageList, outputLanguage) { outputLanguage = it }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Text("Original:", color = Color.Gray)
                    Text(originalText, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    Divider(color = Color.DarkGray)
                    Text("Traduzido:", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    Text(translatedText, color = Color(0xFF7BFF7B))
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { listening = !listening },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (listening) Color.Red else Color(0xFF7B61FF)
                )
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (listening) "Parar" else "Iniciar")
            }
        }
    }
}

// ------------------------------------------------------
// 🔽 DropdownMenuBox (idiomas)
// ------------------------------------------------------
@Composable
fun DropdownMenuBox(
    languages: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = languages.find { it.first == selected }?.second ?: selected

    Box {
        Button(onClick = { expanded = true }) {
            Icon(Icons.Default.Language, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
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
