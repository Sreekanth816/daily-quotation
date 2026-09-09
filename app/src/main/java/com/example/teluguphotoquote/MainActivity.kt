package com.example.teluguphotoquote

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
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
                    AppRoot()
                }
            }
        }
    }
}

private enum class Screen { ONBOARDING, MAIN, EDIT_PROFILE }

/** Quick-jump presets for the draggable text anchor (center-point fractions of the image). */
private enum class PositionPreset(val label: String, val fx: Float, val fy: Float) {
    TOP("Top", 0.5f, 0.16f),
    CENTER("Center", 0.5f, 0.5f),
    BOTTOM("Bottom", 0.5f, 0.84f)
}

/**
 * Decides whether to show the one-time onboarding form (upload picture +
 * name), the main quote-creation screen, or the profile-edit form, based on
 * whether a profile has already been saved in [UserProfileStore] and on
 * in-memory navigation state.
 */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(UserProfileStore.getProfile(context)) }
    var screen by remember { mutableStateOf(if (profile == null) Screen.ONBOARDING else Screen.MAIN) }

    when (screen) {
        Screen.ONBOARDING -> ProfileFormScreen(
            title = "Welcome!",
            onSave = { name, imagePath ->
                UserProfileStore.saveProfile(context, name, imagePath)
                profile = UserProfile(name, imagePath)
                screen = Screen.MAIN
            }
        )
        Screen.EDIT_PROFILE -> ProfileFormScreen(
            title = "Edit profile",
            initialName = profile?.name.orEmpty(),
            initialImagePath = profile?.profileImagePath,
            showBackButton = true,
            onBack = { screen = Screen.MAIN },
            onSave = { name, imagePath ->
                UserProfileStore.saveProfile(context, name, imagePath)
                profile = UserProfile(name, imagePath)
                screen = Screen.MAIN
            }
        )
        Screen.MAIN -> PhotoQuoteScreen(
            profile = profile!!,
            onEditProfile = { screen = Screen.EDIT_PROFILE }
        )
    }
}

@Composable
fun PhotoQuoteScreen(profile: UserProfile, onEditProfile: () -> Unit) {
    val context = LocalContext.current

    var quoteText by remember { mutableStateOf("") }
    var backgroundResId by remember { mutableStateOf(ImagePool.randomResId()) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var speechLanguage by remember { mutableStateOf(SpeechLanguage.TELUGU) }

    // Center-point of the quote text as a fraction (0f..1f) of the actual
    // image dimensions — draggable in the preview, baked in on Generate.
    var textAnchor by remember { mutableStateOf(Offset(PositionPreset.TOP.fx, PositionPreset.TOP.fy)) }

    var showImagePicker by remember { mutableStateOf(false) }

    if (showImagePicker) {
        ImagePickerBottomSheet(
            onDismiss = { showImagePicker = false },
            onImageSelected = { resId ->
                backgroundResId = resId
                renderedBitmap = null
                showImagePicker = false
            }
        )
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
        // Header row: title on the left, small circular profile avatar on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JS Today Quote",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onEditProfile() }
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                val avatarBitmap = remember(profile.profileImagePath) {
                    android.graphics.BitmapFactory.decodeFile(profile.profileImagePath)
                }
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Your profile picture (tap to edit)",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
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

        // Background photo preview with the draggable quote text overlaid.
        Text("Drag & Drop quotation position", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        DraggableQuotePreview(
            backgroundResId = backgroundResId,
            quoteText = quoteText,
            anchor = textAnchor,
            onAnchorChange = { textAnchor = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            OutlinedButton(onClick = {
                backgroundResId = ImagePool.randomResId(exclude = backgroundResId)
                renderedBitmap = null
            }) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shuffle image")
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = { showImagePicker = true }) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick-jump position presets — dragging still works after tapping one.
        Text("Jump to position", style = MaterialTheme.typography.labelLarge)
        Row {
            PositionPreset.values().forEach { preset ->
                val isSelected = textAnchor.x == preset.fx && textAnchor.y == preset.fy
                FilterChip(
                    selected = isSelected,
                    onClick = { textAnchor = Offset(preset.fx, preset.fy) },
                    label = { Text(preset.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val bmp = drawableToBitmap(context, backgroundResId)
                renderedBitmap = renderQuoteOnPhoto(
                    context = context,
                    sourceBitmap = bmp,
                    quoteText = quoteText,
                    profile = profile,
                    anchorFractionX = textAnchor.x,
                    anchorFractionY = textAnchor.y
                )
            },
            enabled = quoteText.isNotBlank()
        ) {
            Text("Generate Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview of the final rendered quote image
        if (renderedBitmap != null) {
            Image(
                bitmap = renderedBitmap!!.asImageBitmap(),
                contentDescription = "Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(16.dp))

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

/**
 * Shows the chosen background image (scaled to fit, so what's visible maps
 * 1:1 onto the full-resolution photo) with the quote text overlaid as a
 * draggable label. [anchor] is the text's center point as a fraction
 * (0f..1f) of the *image itself* — not of this composable's box — so the
 * same fraction can be handed straight to [renderQuoteOnPhoto].
 */
@Composable
private fun DraggableQuotePreview(
    backgroundResId: Int,
    quoteText: String,
    anchor: Offset,
    onAnchorChange: (Offset) -> Unit
) {
    val painter = painterResource(id = backgroundResId)
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var textSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onGloballyPositioned { boxSize = it.size }
    ) {
        Image(
            painter = painter,
            contentDescription = "Background",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (quoteText.isNotBlank() && boxSize.width > 0 && boxSize.height > 0) {
            val intrinsic = painter.intrinsicSize
            val boxW = boxSize.width.toFloat()
            val boxH = boxSize.height.toFloat()
            val scale = if (intrinsic.width > 0f && intrinsic.height > 0f) {
                minOf(boxW / intrinsic.width, boxH / intrinsic.height)
            } else 1f
            val drawnW = if (intrinsic.width > 0f) intrinsic.width * scale else boxW
            val drawnH = if (intrinsic.height > 0f) intrinsic.height * scale else boxH
            val imageOffsetX = (boxW - drawnW) / 2f
            val imageOffsetY = (boxH - drawnH) / 2f

            // Read the latest sizes inside the gesture without restarting it.
            val drawnWState = rememberUpdatedState(drawnW)
            val drawnHState = rememberUpdatedState(drawnH)
            val anchorState = rememberUpdatedState(anchor)

            val density = LocalDensity.current
            val maxTextWidthDp = with(density) { (drawnW * 0.85f).toDp() }

            Box(
                // Extra padding = bigger, more forgiving drag handle around the text.
                modifier = Modifier
                    .widthIn(max = maxTextWidthDp + 32.dp)
                    .onGloballyPositioned { textSize = it.size }
                    .offset {
                        val centerX = imageOffsetX + anchor.x * drawnW
                        val centerY = imageOffsetY + anchor.y * drawnH
                        IntOffset(
                            (centerX - textSize.width / 2f).toInt(),
                            (centerY - textSize.height / 2f).toInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = drawnWState.value
                                val h = drawnHState.value
                                if (w > 0f && h > 0f) {
                                    val current = anchorState.value
                                    val newX = (current.x + dragAmount.x / w).coerceIn(0f, 1f)
                                    val newY = (current.y + dragAmount.y / h).coerceIn(0f, 1f)
                                    onAnchorChange(Offset(newX, newY))
                                }
                            }
                        )
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quoteText,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall.copy(
                        shadow = Shadow(color = Color.Black, blurRadius = 10f, offset = Offset(0f, 2f))
                    )
                )
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

/** Decodes a bundled drawable resource into a mutable ARGB_8888 bitmap ready for canvas drawing. */
private fun drawableToBitmap(context: android.content.Context, resId: Int): Bitmap {
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)!!
    val bitmap = drawable.toBitmap()
    return bitmap.copy(Bitmap.Config.ARGB_8888, true)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    onDismiss: () -> Unit,
    onImageSelected: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Background Image",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(ImagePool.drawableResIds) { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageSelected(resId) }
                    )
                }
            }
        }
    }
}

private fun saveBitmapToCache(context: android.content.Context, bitmap: Bitmap): Uri {
    val cacheDir = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val file = File(cacheDir, "share_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}