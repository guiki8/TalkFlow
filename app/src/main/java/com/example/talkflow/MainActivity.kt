package com.example.talkflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.talkflow.navigation.AppNavigator
import com.example.talkflow.ui.theme.TalkFlowTheme // <-- Add this import

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TalkFlowTheme {
                AppNavigator()
            }
        }
    }
}