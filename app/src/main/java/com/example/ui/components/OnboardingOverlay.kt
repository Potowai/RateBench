package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

private data class OnboardingPage(
  val icon: ImageVector,
  val title: String,
  val text: String
)

private val onboardingPages = listOf(
  OnboardingPage(
    icon = Icons.Default.Weekend,
    title = "Les bancs remarquables",
    text = "RateBench répertorie les bancs publics et spots de repos : découvrez les mieux notés autour de vous."
  ),
  OnboardingPage(
    icon = Icons.Default.Explore,
    title = "Explorez en rayon 100 km",
    text = "Autorisez la position pour voir les spots proches sur la carte, ou parcourez la liste et la recherche."
  ),
  OnboardingPage(
    icon = Icons.Default.PersonAdd,
    title = "Partagez un spot",
    text = "Touchez + pour ajouter un banc avec photo ou vidéo. Avec un compte ou en anonyme : votre pseudo est conservé."
  )
)

/** Écran d'accueil affiché une seule fois (installation → premier spot). */
@Composable
fun OnboardingOverlay(onDone: () -> Unit) {
  var page by remember { mutableStateOf(0) }
  val last = page >= onboardingPages.size - 1
  val current = onboardingPages[page]

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f)),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color.White,
      shadowElevation = 8.dp,
      modifier = Modifier
        .padding(horizontal = 32.dp)
        .widthIn(max = 420.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = Slate100,
          modifier = Modifier.size(64.dp)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              imageVector = current.icon,
              contentDescription = null,
              tint = Slate900,
              modifier = Modifier.size(30.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = current.title,
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          color = Slate900,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = current.text,
          fontSize = 14.sp,
          color = Slate700,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          onboardingPages.indices.forEach { i ->
            Box(
              modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(if (i == page) 10.dp else 7.dp)
                .clip(CircleShape)
                .background(if (i == page) Slate900 else Slate100)
            )
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = {
            if (last) onDone() else page++
          },
          colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(if (last) "Commencer" else "Suivant")
        }
        if (!last) {
          TextButton(onClick = onDone) {
            Text("Passer", color = Slate500, fontSize = 13.sp)
          }
        } else {
          Spacer(modifier = Modifier.width(8.dp))
        }
      }
    }
  }
}
