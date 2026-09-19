package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/** Vrai si l'URI désigne une vidéo (aperçu adapté au lieu de l'image). */
fun isVideoUri(context: Context, uriString: String?): Boolean {
  if (uriString == null || !uriString.startsWith("content://")) return false
  return try {
    context.contentResolver.getType(Uri.parse(uriString))?.startsWith("video") == true
  } catch (_: Exception) {
    false
  }
}

/** Sauvegarde un bitmap capturé dans la galerie, renvoie l'URI content:// (ou null). */
fun saveCameraBitmap(context: Context, bitmap: Bitmap): String? = try {
  val name = "RateBench_${System.currentTimeMillis()}.jpg"
  val values = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, name)
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RateBench")
      put(MediaStore.Images.Media.IS_PENDING, 1)
    }
  }
  val collection =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
  val uri = context.contentResolver.insert(collection, values) ?: return null
  context.contentResolver.openOutputStream(uri)?.use { out ->
    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) return null
  }
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    context.contentResolver.update(uri, values, null, null)
  }
  uri.toString()
} catch (_: Exception) {
  null
}

/**
 * Déclencheur de capture caméra (sans permission CAMERA : appli externe).
 * Gère WRITE_EXTERNAL_STORAGE sur Android < 10 pour la sauvegarde galerie.
 */
@Composable
fun rememberCameraCapture(onCaptured: (String) -> Unit): () -> Unit {
  val context = LocalContext.current
  val takePicture = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    if (bitmap != null) {
      saveCameraBitmap(context, bitmap)?.let { onCaptured(it) }
    }
  }
  val writePermission = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) takePicture.launch(null)
  }
  return remember {
    {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED
      ) {
        writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      } else {
        takePicture.launch(null)
      }
    }
  }
}

/** Crée une URI MediaStore pour enregistrer une vidéo capturée en direct. */
fun createVideoCaptureUri(context: Context): Uri? = try {
  val name = "RateBench_${System.currentTimeMillis()}.mp4"
  val values = ContentValues().apply {
    put(MediaStore.Video.Media.DISPLAY_NAME, name)
    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/RateBench")
      put(MediaStore.Video.Media.IS_PENDING, 1)
    }
  }
  val collection =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
  context.contentResolver.insert(collection, values)
} catch (_: Exception) {
  null
}

private fun markVideoReady(context: Context, uri: Uri) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
  try {
    val values = ContentValues().apply {
      put(MediaStore.Video.Media.IS_PENDING, 0)
    }
    context.contentResolver.update(uri, values, null, null)
  } catch (_: Exception) {
  }
}

/**
 * Déclencheur de capture vidéo en direct (appli caméra externe via
 * ACTION_VIDEO_CAPTURE + URI MediaStore, compatible tous niveaux d'API).
 * Gère WRITE_EXTERNAL_STORAGE sur Android < 10.
 */
@Composable
fun rememberVideoCapture(onCaptured: (String) -> Unit): () -> Unit {
  val context = LocalContext.current
  var pendingUri by remember { androidx.compose.runtime.mutableStateOf<Uri?>(null) }
  fun launchCapture(uri: Uri, launcher: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>) {
    pendingUri = uri
    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri)
    launcher.launch(intent)
  }
  val takeVideo = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val uri = pendingUri
    if (result.resultCode == Activity.RESULT_OK && uri != null) {
      markVideoReady(context, uri)
      onCaptured(uri.toString())
    }
    pendingUri = null
  }
  val writePermission = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      createVideoCaptureUri(context)?.let { launchCapture(it, takeVideo) }
    }
  }
  return remember {
    {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED
      ) {
        writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      } else {
        createVideoCaptureUri(context)?.let { launchCapture(it, takeVideo) }
      }
    }
  }
}

/** Ligne de choix de source : galerie (photo, GIF, vidéo), photo live ou vidéo live. */
@Composable
fun MediaSourceRow(
  onGallery: () -> Unit,
  onPhoto: () -> Unit,
  onVideo: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
  ) {
    MediaSourceButton(
      icon = Icons.Default.PhotoLibrary,
      label = "Galerie",
      onClick = onGallery,
      modifier = Modifier.weight(1f)
    )
    MediaSourceButton(
      icon = Icons.Default.PhotoCamera,
      label = "Photo",
      onClick = onPhoto,
      modifier = Modifier.weight(1f)
    )
    MediaSourceButton(
      icon = Icons.Default.Videocam,
      label = "Vidéo",
      onClick = onVideo,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun MediaSourceButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = Color.White,
    shadowElevation = 1.dp,
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Slate800,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate900)
    }
  }
}

/** Aperçu média : image/GIF via Coil, placeholder sombre pour les vidéos. */
@Composable
fun MediaPreviewBox(
  uri: String?,
  isVideo: Boolean,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onRemove: (() -> Unit)? = null,
  overlay: @Composable BoxScope.() -> Unit = {}
) {
  Box(modifier = modifier) {
    if (isVideo) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Slate900),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
          Text("Vidéo sélectionnée", fontSize = 11.sp, color = Color.White)
        }
      }
    } else {
      AsyncImage(
        model = uri,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        error = rememberVectorPainter(Icons.Default.BrokenImage)
      )
    }
    if (onRemove != null) {
      IconButton(
        onClick = onRemove,
        modifier = Modifier
          .size(24.dp)
          .align(Alignment.TopEnd)
          .background(Color.Black.copy(alpha = 0.6f), CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Retirer le média",
          tint = Color.White,
          modifier = Modifier.size(14.dp)
        )
      }
    }
    overlay()
  }
}
