package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

private val sampleBenchPhotos = listOf(
  "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1508050919630-b135583b3ae8?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=800&q=80"
)

@Composable
fun AddBenchModalDialog(
  userLat: Double,
  userLng: Double,
  isLoggedIn: Boolean,
  onLoginClick: () -> Unit,
  onDismiss: () -> Unit,
  onBenchAdded: (title: String, rating: Float, comment: String, photoUrl: String) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var rating by remember { mutableStateOf(8.0f) }
  var comment by remember { mutableStateOf("") }
  var photoUrl by remember { mutableStateOf(sampleBenchPhotos.first()) }
  var showPhotoOptions by remember { mutableStateOf(false) }
  val isVideo = isVideoUri(context, photoUrl)

  // Galerie : photo, GIF ou vidéo (sans permission requise)
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      photoUrl = uri.toString()
      showPhotoOptions = false
    }
  }
  val capturePhoto = rememberCameraCapture { uri ->
    photoUrl = uri
    showPhotoOptions = false
  }
  val captureVideo = rememberVideoCapture { uri ->
    photoUrl = uri
    showPhotoOptions = false
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White,
    title = {
      Text(
        text = "Ajouter un spot de repos",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Slate900
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Compte requis pour publier (photos partagées avec la communauté)
        if (!isLoggedIn) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate100,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = "Compte gratuit requis pour publier.",
                fontSize = 12.sp,
                color = Slate800,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = onLoginClick) {
                Text("Se connecter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
              }
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Nom du spot") },
          placeholder = { Text("Ex: Banc ensoleillé face au canal") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Note de confort & vue",
              fontSize = 12.sp,
              color = Slate700
            )
            Text(
              text = "${rating.toInt()}/10",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = Slate900
            )
          }
          Slider(
            value = rating,
            onValueChange = { rating = it },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
              thumbColor = Slate900,
              activeTrackColor = Slate900,
              inactiveTrackColor = Slate300
            )
          )
        }

        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          label = { Text("Description du banc") },
          placeholder = { Text("Calme, vue dégagée, dossier confortable...") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          shape = RoundedCornerShape(12.dp)
        )

        // Média du banc : photo, GIF ou vidéo (galerie ou caméra)
        Text(
          text = "Photo ou vidéo du spot :",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Slate800
        )

        MediaPreviewBox(
          uri = photoUrl,
          isVideo = isVideo,
          contentDescription = "Aperçu média du banc",
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Slate100)
        ) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
              .clickable { showPhotoOptions = !showPhotoOptions }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = "Changer le média",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Modifier", fontSize = 11.sp, color = Color.White)
            }
          }
        }

        if (showPhotoOptions) {
          MediaSourceRow(
            onGallery = {
              galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
              )
            },
            onPhoto = { capturePhoto() },
            onVideo = { captureVideo() },
            modifier = Modifier.fillMaxWidth()
          )
        }

        Text(
          text = "Coordonnées GPS: ${String.format("%.4f", userLat)}, ${String.format("%.4f", userLng)}",
          fontSize = 10.sp,
          color = Slate500
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val safeTitle = title.ifBlank { "Nouveau banc remarquable" }
          val safeComment = comment.ifBlank { "Spot confortable et agréable pour se reposer." }
          onBenchAdded(safeTitle, rating, safeComment, photoUrl)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("confirm_add_bench_button")
      ) {
        Text("Enregistrer le banc")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Annuler", color = Slate700)
      }
    }
  )
}
