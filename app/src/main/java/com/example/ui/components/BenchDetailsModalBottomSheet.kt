package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BenchItem
import com.example.ui.theme.AmberRating
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

// Formatage de la distance
private fun formatDistance(meters: Int): String {
  return if (meters < 1000) {
    "À $meters m"
  } else {
    String.format("À %.1f km", meters / 1000.0)
  }
}

// Liste de photos prédéfinies de haute qualité pour les avis (au cas où l'utilisateur n'a pas de photo locale)
private val sampleReviewPhotos = listOf(
  "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1508050919630-b135583b3ae8?auto=format&fit=crop&w=800&q=80",
  "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=800&q=80"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchDetailsModalBottomSheet(
  bench: BenchItem,
  distanceMeters: Int,
  currentUserEmail: String?,
  isLoggedIn: Boolean,
  onLoginClick: () -> Unit,
  onDismiss: () -> Unit,
  onAddReview: (rating: Float, comment: String, photoUrl: String?) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  val context = LocalContext.current
  var isReviewing by remember { mutableStateOf(false) }
  var myRating by remember { mutableStateOf(8.0f) }
  var myComment by remember { mutableStateOf("") }
  var selectedReviewPhotoUrl by remember { mutableStateOf<String?>(null) }
  var showPhotoPickerPresets by remember { mutableStateOf(false) }
  val isVideo = isVideoUri(context, selectedReviewPhotoUrl)

  // Galerie : photo, GIF ou vidéo (sans permission requise)
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedReviewPhotoUrl = uri.toString()
    }
  }
  val capturePhoto = rememberCameraCapture { uri ->
    selectedReviewPhotoUrl = uri
  }
  val captureVideo = rememberVideoCapture { uri ->
    selectedReviewPhotoUrl = uri
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color.White,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 10.dp)
          .width(40.dp)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Slate300)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 40.dp)
    ) {
      // 1. Photo grand format du banc
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Slate100)
      ) {
        AsyncImage(
          model = bench.photoUrl,
          contentDescription = bench.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )

        // Note globale en badge incrusté
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate900.copy(alpha = 0.88f),
          contentColor = Color.White,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = AmberRating,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = String.format("%.1f", bench.rating),
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            Text(
              text = " /10",
              fontSize = 11.sp,
              color = Slate300
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Titre, distance et bouton itinéraire
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = bench.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Slate900
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "${formatDistance(distanceMeters)} • ${bench.reviewCount} avis",
            fontSize = 12.sp,
            color = Slate500,
            fontWeight = FontWeight.Medium
          )
        }

        IconButton(
          onClick = {
            val uri = Uri.parse("geo:${bench.latitude},${bench.longitude}?q=${bench.latitude},${bench.longitude}(${Uri.encode(bench.title)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(mapIntent)
          },
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Slate100)
        ) {
          Icon(
            imageVector = Icons.Default.Directions,
            contentDescription = "Itinéraire vers ce banc",
            tint = Slate800
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = bench.description,
        fontSize = 13.sp,
        color = Slate700,
        lineHeight = 18.sp
      )

      Divider(modifier = Modifier.padding(vertical = 14.dp), color = Slate100)

      // 3. Section des Avis & Photos de la communauté
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Avis de la communauté",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Slate900
          )
          Text(
            text = "Chaque avis peut inclure sa propre photo",
            fontSize = 11.sp,
            color = Slate500
          )
        }

        if (!isReviewing) {
          Button(
            onClick = { isReviewing = true },
            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ajouter un avis", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      // Formulaire de notation et d'ajout de photo à l'avis
      if (isReviewing) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Slate100,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Votre évaluation :",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Slate900
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate900,
                contentColor = Color.White
              ) {
                Text(
                  text = "${myRating.toInt()}/10",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Slider(
              value = myRating,
              onValueChange = { myRating = it },
              valueRange = 0f..10f,
              steps = 9,
              colors = SliderDefaults.colors(
                thumbColor = Slate900,
                activeTrackColor = Slate900,
                inactiveTrackColor = Slate300
              )
            )

            // Commentaire : saisie directe si connecté, sinon tap → création de compte
            if (isLoggedIn) {
              OutlinedTextField(
                value = myComment,
                onValueChange = { myComment = it },
                placeholder = { Text("Partagez votre avis sur ce spot (confort, calme, vue...)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
              )
            } else {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(onClick = onLoginClick)
              ) {
                Text(
                  text = "Touchez ici pour vous connecter et écrire votre avis…",
                  fontSize = 12.sp,
                  color = Slate500,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RÈGLE : Chaque avis a le droit d'ajouter une photo
            Text(
              text = "Photo de votre avis (optionnelle) :",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Slate800
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (selectedReviewPhotoUrl != null) {
              // Aperçu du média sélectionné (vidéo = placeholder) avec suppression
              MediaPreviewBox(
                uri = selectedReviewPhotoUrl,
                isVideo = isVideo,
                contentDescription = "Média de l'avis",
                modifier = Modifier
                  .size(100.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color.White),
                onRemove = { selectedReviewPhotoUrl = null }
              )
            } else {
              // Sources : galerie (photo, GIF, vidéo) ou capture live photo/vidéo
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

              Spacer(modifier = Modifier.height(8.dp))

              // Photos d'exemple prédéfinies
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showPhotoPickerPresets = !showPhotoPickerPresets }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Slate800,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Exemples", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate900)
                }
              }

              if (showPhotoPickerPresets) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  sampleReviewPhotos.forEach { url ->
                    AsyncImage(
                      model = url,
                      contentDescription = "Photo d'exemple",
                      modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                          selectedReviewPhotoUrl = url
                          showPhotoPickerPresets = false
                        },
                      contentScale = ContentScale.Crop
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compte requis pour publier l'avis (photos partagées)
            if (!isLoggedIn) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate100,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
              Spacer(modifier = Modifier.height(8.dp))
            }

            // Actions Annuler / Publier l'avis
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = {
                isReviewing = false
                selectedReviewPhotoUrl = null
              }) {
                Text("Annuler", fontSize = 12.sp, color = Slate700)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  if (!isLoggedIn) {
                    onLoginClick()
                    return@Button
                  }
                  val commentToSend = myComment.ifBlank { "Un spot remarquable !" }
                  onAddReview(myRating, commentToSend, selectedReviewPhotoUrl)
                  isReviewing = false
                  myComment = ""
                  selectedReviewPhotoUrl = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(
                  if (isLoggedIn) "Publier l'avis" else "Se connecter pour publier",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 4. Liste de tous les avis avec affichage de la photo de l'avis
      if (bench.reviews.isEmpty()) {
        Text(
          text = "Aucun avis pour le moment. Soyez le premier à noter ce banc !",
          fontSize = 12.sp,
          color = Slate500,
          modifier = Modifier.padding(vertical = 12.dp)
        )
      } else {
        bench.reviews.forEach { rev ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Slate100)
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Slate900,
                  contentColor = Color.White
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Star,
                      contentDescription = null,
                      tint = AmberRating,
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "${rev.rating}/10",
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = rev.userName,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                  color = Slate900
                )
              }
              Text(
                text = rev.date,
                fontSize = 11.sp,
                color = Slate500
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = rev.comment,
              fontSize = 12.sp,
              color = Slate800,
              lineHeight = 16.sp
            )

            // Affichage de la photo jointe à cet avis si présente
            if (!rev.photoUrl.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              AsyncImage(
                model = rev.photoUrl,
                contentDescription = "Photo attachée à l'avis",
                modifier = Modifier
                  .fillMaxWidth()
                  .height(130.dp)
                  .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
              )
            }
          }
        }
      }
    }
  }
}
