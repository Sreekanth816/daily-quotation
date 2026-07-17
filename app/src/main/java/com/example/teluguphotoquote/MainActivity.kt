package com.example.teluguphotoquote

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PhotoQuoteScreen()
                }
            }
        }
    }
}

@Composable
fun PhotoQuoteScreen() {
    val context = LocalContext.current

    var quoteText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var speechLanguage by remember { mutableStateOf(SpeechLanguage.TELUGU) }
    var position by remember { mutableStateOf(TextPosition.BOTTOM) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- Photo Picker (gallery) ---
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            sourceBitmap = loadBitmapFromUri(context, uri)
            renderedBitmap = null
        }
    }

    // --- Camera capture ---
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            selectedImageUri = pendingCameraUri
            sourceBitmap = loadBitmapFromUri(context, pendingCameraUri!!)
            renderedBitmap = null
        }
    }

    // --- Speech-to-text ---
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = SpeechInputHelper.extractResultText(result.data)
        if (!spokenText.isNullOrBlank()) {
            quoteText = if (quoteText.isBlank()) spokenText else "$quoteText $spokenText"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Telugu Photo Quote",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Text input
        OutlinedTextField(
            value = quoteText,
            onValueChange = { quoteText = it },
            label = { Text("Type Telugu or English text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Voice language toggle + mic button
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedLanguageToggle(
                selected = speechLanguage,
                onSelect = { speechLanguage = it }
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = {
                if (SpeechInputHelper.isAvailable(context as android.app.Activity)) {
                    speechLauncher.launch(SpeechInputHelper.createRecognizerIntent(speechLanguage))
                } else {
                    Toast.makeText(context, "No speech recognizer found on this device", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.Mic, contentDescription = "Speak")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Photo pick / camera buttons
        Row {
            Button(onClick = {
                pickImageLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }) {
                Icon(Icons.Filled.Photo, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = {
                val uri = createCameraOutputUri(context)
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            }) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text position selector
        if (sourceBitmap != null) {
            Text("Text position", style = MaterialTheme.typography.labelLarge)
            Row {
                TextPosition.values().forEach { pos ->
                    FilterChip(
                        selected = position == pos,
                        onClick = { position = pos },
                        label = { Text(pos.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val bmp = sourceBitmap ?: return@Button
                    renderedBitmap = renderQuoteOnPhoto(
                        context = context,
                        sourceBitmap = bmp,
                        quoteText = quoteText,
                        style = QuoteStyle(position = position)
                    )
                },
                enabled = quoteText.isNotBlank()
            ) {
                Text("Generate Image")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Preview
        val previewBitmap = renderedBitmap ?: sourceBitmap
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save / Share
        if (renderedBitmap != null) {
            Row {
                Button(onClick = {
                    val uri = saveBitmapToGallery(context, renderedBitmap!!)
                    if (uri != null) {
                        Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = {
                    val uri = saveBitmapToCache(context, renderedBitmap!!)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                }) {
                    Text("Share")
                }
            }
        }
    }
}

@Composable
fun SegmentedLanguageToggle(
    selected: SpeechLanguage,
    onSelect: (SpeechLanguage) -> Unit
) {
    Row {
        SpeechLanguage.values().forEach { lang ->
            FilterChip(
                selected = selected == lang,
                onClick = { onSelect(lang) },
                label = { Text(lang.label) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

// --- Helper functions ---

private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    } catch (e: Exception) {
        null
    }
}

private fun createCameraOutputUri(context: android.content.Context): Uri {
    val imagesDir = File(context.getExternalFilesDir("images"), "")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val fileName = "camera_${System.currentTimeMillis()}.jpg"
    val file = File(imagesDir, fileName)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap): Uri? {
    val filename = "TeluguQuote_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TeluguPhotoQuote")
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return null
    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return uri
}

private fun saveBitmapToCache(context: android.content.Context, bitmap: Bitmap): Uri {
    val cacheDir = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val file = File(cacheDir, "share_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
