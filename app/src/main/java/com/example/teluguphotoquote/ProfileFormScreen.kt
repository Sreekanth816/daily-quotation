package com.example.teluguphotoquote

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shared form used both for first-time onboarding and for later profile
 * edits: lets the user upload a profile picture (gallery or camera) and set
 * their display name. [initialName]/[initialImagePath] pre-fill the form
 * when editing an existing profile; leave them null/blank for onboarding.
 */
@Composable
fun ProfileFormScreen(
    title: String,
    initialName: String = "",
    initialImagePath: String? = null,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    onSave: (name: String, imagePath: String) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialName) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val hasExistingImage = initialImagePath != null && File(initialImagePath).exists()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            pickedBitmap = null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            pickedImageUri = pendingCameraUri
            pickedBitmap = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (showBackButton) {
            IconButton(onClick = { onBack?.invoke() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upload a profile picture and set your name.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Circular preview of the currently picked / existing profile picture.
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val previewBitmap: Bitmap? = when {
                    pickedBitmap != null -> pickedBitmap
                    pickedImageUri != null -> remember(pickedImageUri) { loadBitmapFromUri(context, pickedImageUri!!) }
                    hasExistingImage -> remember(initialImagePath) { android.graphics.BitmapFactory.decodeFile(initialImagePath) }
                    else -> null
                }
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                OutlinedButton(onClick = {
                    pickImageLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(onClick = {
                    val uri = createCameraOutputUri(context)
                    pendingCameraUri = uri
                    takePictureLauncher.launch(uri)
                }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val canSave = name.isNotBlank() && (pickedImageUri != null || pickedBitmap != null || hasExistingImage)

        Button(
            onClick = {
                val savedPath = when {
                    pickedBitmap != null -> ProfileImageStore.saveFromBitmap(context, pickedBitmap!!)
                    pickedImageUri != null -> ProfileImageStore.saveFromUri(context, pickedImageUri!!)
                    else -> initialImagePath!!
                }
                onSave(name.trim(), savedPath)
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}

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
    val fileName = "profile_camera_${System.currentTimeMillis()}.jpg"
    val file = File(imagesDir, fileName)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
