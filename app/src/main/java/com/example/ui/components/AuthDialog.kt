package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun AuthDialog(
  currentUserEmail: String?,
  onDismiss: () -> Unit,
  onLogin: (String) -> Unit,
  onLogout: () -> Unit
) {
  var emailInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White,
    title = {
      Text(
        text = if (currentUserEmail != null) "Mon profil Rate Bench" else "Rejoindre la communauté",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Slate900
      )
    },
    text = {
      if (currentUserEmail != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Connecté avec :", fontSize = 12.sp, color = Slate500)
          Text(text = currentUserEmail, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
          Text(
            text = "Vos avis, photos et nouveaux bancs sont synchronisés dans la base externe partagée.",
            fontSize = 12.sp,
            color = Slate700
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Connectez-vous pour ajouter des avis avec photo et répertorier des spots.",
            fontSize = 13.sp,
            color = Slate700
          )
          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            placeholder = { Text("votre@email.com") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )
        }
      }
    },
    confirmButton = {
      if (currentUserEmail != null) {
        TextButton(onClick = onLogout) {
          Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
        }
      } else {
        Button(
          onClick = { onLogin(emailInput.ifBlank { "utilisateur@ratebench.app" }) },
          colors = ButtonDefaults.buttonColors(containerColor = Slate900),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Se connecter")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Fermer", color = Slate700)
      }
    }
  )
}
