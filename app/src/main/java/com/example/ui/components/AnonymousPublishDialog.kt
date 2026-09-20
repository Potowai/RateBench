package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

/**
 * Publication sans compte : pseudo aléatoire (relançable au dé) ou
 * personnalisé, ou redirection vers la connexion (le brouillon est gardé).
 */
@Composable
fun AnonymousPublishDialog(
  pseudo: String,
  onPseudoChange: (String) -> Unit,
  onDiceClick: () -> Unit,
  hasLocalMedia: Boolean,
  onLoginClick: () -> Unit,
  onPublishAnonymous: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White,
    title = {
      Text(
        text = "Publier sans compte",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Slate900
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Votre contenu sera partagé avec la communauté sous ce pseudo.",
          fontSize = 13.sp,
          color = Slate700
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = pseudo,
            onValueChange = { onPseudoChange(it.take(24)) },
            label = { Text("Pseudo") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(onClick = onDiceClick) {
            Icon(
              imageVector = Icons.Default.Casino,
              contentDescription = "Pseudo aléatoire",
              tint = Slate900,
              modifier = Modifier.size(22.dp)
            )
          }
        }
        if (hasLocalMedia) {
          Text(
            text = "Sans compte, les photos/vidéos ne sont pas envoyées (compte requis).",
            fontSize = 12.sp,
            color = Slate500
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onPublishAnonymous,
        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Publier en anonyme")
      }
    },
    dismissButton = {
      TextButton(onClick = onLoginClick) {
        Text("Se connecter", color = Slate900, fontWeight = FontWeight.Bold)
      }
    }
  )
}
