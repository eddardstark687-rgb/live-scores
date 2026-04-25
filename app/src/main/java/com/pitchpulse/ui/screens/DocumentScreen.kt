package com.pitchpulse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    fileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf(AnnotatedString("Loading...")) }

    LaunchedEffect(fileName) {
        val rawText = withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(fileName)
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.readText()
            } catch (e: Exception) {
                "Error loading document: ${e.localizedMessage}"
            }
        }
        content = parseMarkdownToAnnotatedString(rawText)
    }

    val title = when (fileName) {
        "privacy_policy.md" -> "Privacy Policy"
        "terms.md" -> "Terms & Conditions"
        else -> "Document"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        SelectionContainer {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Default
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        for (line in lines) {
            var currentLine = line
            
            // Handle Headers
            when {
                currentLine.startsWith("# ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                        append(currentLine.removePrefix("# "))
                    }
                    append("\n")
                    continue
                }
                currentLine.startsWith("## ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        append(currentLine.removePrefix("## "))
                    }
                    append("\n")
                    continue
                }
                currentLine.startsWith("### ") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                        append(currentLine.removePrefix("### "))
                    }
                    append("\n")
                    continue
                }
            }

            // Handle Bullet points
            if (currentLine.startsWith("- ")) {
                currentLine = "• " + currentLine.removePrefix("- ")
            } else if (currentLine.startsWith("* ")) {
                currentLine = "• " + currentLine.removePrefix("* ")
            }

            // Handle Bold Text
            var remaining = currentLine
            while (remaining.contains("**")) {
                val start = remaining.indexOf("**")
                val end = remaining.indexOf("**", start + 2)
                
                if (end != -1) {
                    append(remaining.substring(0, start))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(remaining.substring(start + 2, end))
                    }
                    remaining = remaining.substring(end + 2)
                } else {
                    break
                }
            }
            append(remaining + "\n")
        }
    }
}
