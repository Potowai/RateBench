package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.BenchRemoteRepository
import com.example.data.supabase.SupabaseAuthRepository
import com.example.data.supabase.SupabaseBenchRepository
import com.example.model.BenchItem
import com.example.model.BenchReview
import com.example.model.calculateDistanceMeters
import com.example.ui.components.AddBenchModalDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.BenchDetailsModalBottomSheet
import com.example.ui.components.OpenStreetMapWebView
import com.example.ui.components.benchesToJson
import com.example.ui.theme.AmberRating
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        RateBenchMainScreen()
      }
    }
  }
}

private fun formatDistance(meters: Int): String {
  return if (meters < 1000) {
    "À $meters m"
  } else {
    String.format("À %.1f km", meters / 1000.0)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateBenchMainScreen() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  // Backend : Supabase partagé si configuré (.env), sinon données locales simulées.
  // SupabaseBenchRepository expose les mêmes signatures et bascule seul en repli local.
  val authRepository = remember { SupabaseAuthRepository(context) }
  val repository = remember {
    SupabaseBenchRepository(context, authRepository, BenchRemoteRepository(context))
  }

  // Position réelle (GPS) avec repli Paris centre si indisponible ou refusée
  var userLat by remember { mutableStateOf(48.8575) }
  var userLng by remember { mutableStateOf(2.3514) }
  var gpsActive by remember { mutableStateOf(false) }

  val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

  fun hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

  var benches by remember { mutableStateOf<List<BenchItem>>(emptyList()) }
  var isRefreshing by remember { mutableStateOf(false) }
  var syncToastMessage by remember { mutableStateOf<String?>(null) }

  var isListView by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedBench by remember { mutableStateOf<BenchItem?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }
  var showAuthDialog by remember { mutableStateOf(false) }
  var currentUserEmail by remember { mutableStateOf<String?>(authRepository.sessionEmail()) }
  var authError by remember { mutableStateOf<String?>(null) }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isMapReady by remember { mutableStateOf(false) }

  // Fonction centrale de synchronisation avec l'API de la base de données externe
  // RÈGLE : Filtrage strict dans un rayon de 100 km par rapport à la géolocalisation
  // de l'utilisateur (et NON là où regarde la carte)
  fun syncWithExternalDatabase(silent: Boolean = false) {
    if (isRefreshing) return
    coroutineScope.launch {
      isRefreshing = true
      try {
        val syncedBenches = repository.syncNearbyBenches(
          userLat = userLat,
          userLng = userLng,
          radiusKm = 100.0
        )
        benches = syncedBenches

        // Rafraîchir les marqueurs de la carte Leaflet
        if (webViewRef != null && isMapReady) {
          val json = benchesToJson(syncedBenches)
          webViewRef?.evaluateJavascript("setBenches('$json');", null)
          webViewRef?.evaluateJavascript("setUserLocation($userLat, $userLng);", null)
        }

        syncToastMessage = "${syncedBenches.size} bancs synchronisés (Rayon de 100 km)"
        delay(3000)
        syncToastMessage = null
      } catch (e: Exception) {
        syncToastMessage = "Synchronisé avec les données locales"
        delay(2500)
        syncToastMessage = null
      } finally {
        isRefreshing = false
      }
    }
  }

  // Demande une position fraîche, recentre la carte et resynchronise le rayon
  fun fetchFreshLocation() {
    if (!hasLocationPermission()) return
    try {
      fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
          if (location != null) {
            userLat = location.latitude
            userLng = location.longitude
            gpsActive = true
            webViewRef?.evaluateJavascript("centerMap($userLat, $userLng, 15);", null)
            syncWithExternalDatabase(silent = true)
          } else {
            fusedClient.lastLocation.addOnSuccessListener { last ->
              if (last != null) {
                userLat = last.latitude
                userLng = last.longitude
                gpsActive = true
                webViewRef?.evaluateJavascript("centerMap($userLat, $userLng, 15);", null)
                syncWithExternalDatabase(silent = true)
              } else {
                syncToastMessage = "Position indisponible, Paris par défaut"
                coroutineScope.launch { delay(2500); syncToastMessage = null }
              }
            }
          }
        }
        .addOnFailureListener {
          syncToastMessage = "GPS indisponible, Paris par défaut"
          coroutineScope.launch { delay(2500); syncToastMessage = null }
        }
    } catch (_: SecurityException) {
      gpsActive = false
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    if (grants.values.any { it }) {
      fetchFreshLocation()
    } else {
      syncToastMessage = "GPS refusé, Paris par défaut"
      coroutineScope.launch { delay(2500); syncToastMessage = null }
    }
  }

  fun requestLocationOrFetch() {
    if (hasLocationPermission()) {
      fetchFreshLocation()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  // Chargement initial depuis la base de données distante + demande GPS
  LaunchedEffect(Unit) {
    syncWithExternalDatabase(silent = true)
    requestLocationOrFetch()
  }

  // Mise à jour de la carte dès qu'elle est prête ou que les bancs changent
  LaunchedEffect(benches, isMapReady) {
    if (isMapReady && webViewRef != null && benches.isNotEmpty()) {
      val json = benchesToJson(benches)
      webViewRef?.evaluateJavascript("setBenches('$json');", null)
      webViewRef?.evaluateJavascript("setUserLocation($userLat, $userLng);", null)
    }
  }

  // Filtrage local selon la recherche textuelle
  val filteredBenches = remember(benches, searchQuery) {
    if (searchQuery.isBlank()) {
      benches
    } else {
      benches.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
          it.description.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  val pullToRefreshState = rememberPullToRefreshState()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("rate_bench_main_screen"),
    topBar = {
      // Barre supérieure avec recherche, filtre 100 km et bascules
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White)
          .statusBarsPadding()
          .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Champ de recherche moderne
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher un spot, une vue...", fontSize = 13.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Recherche",
                tint = Slate500,
                modifier = Modifier.size(18.dp)
              )
            },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White,
              disabledContainerColor = Color.White,
              focusedIndicatorColor = Slate900,
              unfocusedIndicatorColor = Slate300
            ),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("search_text_field"),
            singleLine = true
          )

          // Bouton de bascule Carte / Liste
          IconButton(
            onClick = { isListView = !isListView },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (isListView) Slate900 else Slate100)
              .testTag("toggle_view_button")
          ) {
            Icon(
              imageVector = if (isListView) Icons.Default.Map else Icons.Default.FormatListBulleted,
              contentDescription = "Basculer la vue",
              tint = if (isListView) Color.White else Slate800,
              modifier = Modifier.size(20.dp)
            )
          }

          // Bouton profil / connexion (pastille verte = connecté)
          BadgedBox(
            badge = {
              Badge(
                containerColor = if (currentUserEmail != null) Color(0xFF10B981) else Slate300,
                modifier = Modifier.size(10.dp)
              )
            },
            modifier = Modifier.testTag("auth_button")
          ) {
            IconButton(
              onClick = { showAuthDialog = true },
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Slate100)
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = if (currentUserEmail != null) "Profil ($currentUserEmail)" else "Profil (non connecté)",
                tint = if (currentUserEmail != null) Slate900 else Slate500,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        // Bandeau d'information et bouton explicite de rafraîchissement 100 km
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Slate900.copy(alpha = 0.08f),
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Text(
                text = if (gpsActive) "Rayon 100 km (GPS)" else "Rayon 100 km (Paris)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate800,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Text(
              text = "${filteredBenches.size} spots trouvés",
              fontSize = 11.sp,
              color = Slate500
            )
          }

          // Bouton de rafraîchissement manuel
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate100,
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .clickable { syncWithExternalDatabase() }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              if (isRefreshing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  strokeWidth = 1.5.dp,
                  color = Slate900
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Rafraîchir",
                  modifier = Modifier.size(13.dp),
                  tint = Slate700
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isRefreshing) "Actualisation..." else "Rafraîchir",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate800
              )
            }
          }
        }
      }
    },
    floatingActionButton = {
      // FAB central pour ajouter un banc & bouton recentrage GPS
      Row(
        modifier = Modifier
          .navigationBarsPadding()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Bouton Recentrage GPS (position fraîche réelle)
        FloatingActionButton(
          onClick = { requestLocationOrFetch() },
          containerColor = Color.White,
          contentColor = Slate900,
          shape = CircleShape,
          modifier = Modifier
            .size(48.dp)
            .testTag("recenter_gps_button")
        ) {
          Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = "Ma position",
            modifier = Modifier.size(22.dp)
          )
        }

        // Bouton principal Ajouter un banc (fond clair, texte noir)
        FloatingActionButton(
          onClick = { showAddDialog = true },
          containerColor = Color.White,
          contentColor = Slate900,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.testTag("add_bench_fab")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ajouter un spot", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          }
        }
      }
    }
  ) { paddingValues ->
    // RÈGLE : PullToRefreshBox - un simple scroll du haut vers le bas rafraîchit la carte
    // et fait un appel à l'API de la base de données externe
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = { syncWithExternalDatabase() },
      state = pullToRefreshState,
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          // Détection du geste de glissement du haut vers le bas sur la carte
          .pointerInput(Unit) {
            var totalDragY = 0f
            detectVerticalDragGestures(
              onDragStart = { totalDragY = 0f },
              onVerticalDrag = { _, dragAmount ->
                totalDragY += dragAmount
                if (totalDragY > 180f && !isRefreshing) {
                  totalDragY = 0f
                  syncWithExternalDatabase()
                }
              }
            )
          }
      ) {
        // Écrans larges (tablette/paysage ≥ 600dp) : carte + liste côte à côte
        val wideLayout = maxWidth >= 600.dp

        // 1. CARTE OPENSTREETMAP PLEIN ÉCRAN
        OpenStreetMapWebView(
          onWebViewCreated = { webView ->
            webViewRef = webView
          },
          onMapReady = {
            isMapReady = true
            val json = benchesToJson(benches)
            webViewRef?.evaluateJavascript("setBenches('$json');", null)
            webViewRef?.evaluateJavascript("setUserLocation($userLat, $userLng);", null)
          },
          onBenchClicked = { benchId ->
            val found = benches.find { it.id == benchId }
            if (found != null) {
              selectedBench = found
            }
          },
          onMapClicked = { _, _ -> }
        )

        // Indicateur discret de chargement d'OpenStreetMap (non-bloquant)
        AnimatedVisibility(
          visible = !isMapReady && !isListView,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 3.dp
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              CircularProgressIndicator(
                color = Slate900,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Initialisation d'OpenStreetMap...",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate800
              )
            }
          }
        }

        // Notification de synchronisation externe réussie (Toast animé)
        AnimatedVisibility(
          visible = syncToastMessage != null,
          enter = slideInVertically { -it } + fadeIn(),
          exit = slideOutVertically { -it } + fadeOut(),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 12.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            contentColor = Slate900,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Slate300)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = syncToastMessage ?: "",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // 2. VUE LISTE ALTERNATIVE (Animée, écrans étroits uniquement)
        AnimatedVisibility(
          visible = isListView && !wideLayout,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          BenchListPanel(
            benches = filteredBenches,
            userLat = userLat,
            userLng = userLng,
            onBenchClick = { selectedBench = it }
          )
        }

        // 2b. ÉCRANS LARGES : panneau liste persistant à droite de la carte
        if (wideLayout) {
          Box(
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .width(360.dp)
              .fillMaxHeight()
              .background(Slate100)
          ) {
            BenchListPanel(
              benches = filteredBenches,
              userLat = userLat,
              userLng = userLng,
              onBenchClick = { selectedBench = it }
            )
          }
        }
      }
    }
  }

  // 3. TIROIR DÉTAILS DU BANC, AVIS ET AJOUT D'AVIS AVEC PHOTO
  selectedBench?.let { bench ->
    val dist = calculateDistanceMeters(userLat, userLng, bench.latitude, bench.longitude).toInt()

    BenchDetailsModalBottomSheet(
      bench = bench,
      distanceMeters = dist,
      currentUserEmail = currentUserEmail,
      isLoggedIn = currentUserEmail != null,
      onLoginClick = { selectedBench = null; showAuthDialog = true },
      onDismiss = { selectedBench = null },
      onAddReview = { rating, comment, photoUrl ->
        coroutineScope.launch {
          val newReview = BenchReview(
            id = "rev-${UUID.randomUUID().toString().take(8)}",
            userName = currentUserEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Visiteur",
            rating = rating.toInt(),
            comment = comment,
            date = "À l'instant",
            photoUrl = photoUrl // Support de la photo d'avis !
          )

          val (updatedBench, updatedList) = try {
            repository.addReviewToBench(
              benchId = bench.id,
              review = newReview,
              userLat = userLat,
              userLng = userLng
            )
          } catch (e: Exception) {
            if (e.message == "login_required") {
              syncToastMessage = "Connectez-vous pour publier un avis"
            } else {
              syncToastMessage = "Échec d'envoi, réessayez"
            }
            delay(2500)
            syncToastMessage = null
            return@launch
          }

          benches = updatedList
          if (updatedBench != null) {
            selectedBench = updatedBench
          }

          // Mise à jour de la carte
          if (webViewRef != null && isMapReady) {
            val json = benchesToJson(updatedList)
            webViewRef?.evaluateJavascript("setBenches('$json');", null)
          }

          syncToastMessage = "Avis & photo enregistrés dans la base externe !"
          delay(2500)
          syncToastMessage = null
        }
      }
    )
  }

  // 4. MODALE AJOUT D'UN NOUVEAU BANC
  if (showAddDialog) {
    AddBenchModalDialog(
      userLat = userLat,
      userLng = userLng,
      isLoggedIn = currentUserEmail != null,
      onLoginClick = { showAddDialog = false; showAuthDialog = true },
      onDismiss = { showAddDialog = false },
      onBenchAdded = { title, rating, comment, photoUrl ->
        coroutineScope.launch {
          val newBench = BenchItem(
            id = "bench-${UUID.randomUUID().toString().take(8)}",
            title = title,
            latitude = userLat + (Math.random() - 0.5) * 0.015,
            longitude = userLng + (Math.random() - 0.5) * 0.015,
            rating = rating,
            reviewCount = 1,
            description = comment,
            photoUrl = photoUrl,
            author = currentUserEmail ?: "Anonyme",
            reviews = listOf(
              BenchReview(
                id = "rev-initial-${UUID.randomUUID().toString().take(6)}",
                userName = currentUserEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Visiteur",
                rating = rating.toInt(),
                comment = comment,
                date = "À l'instant",
                photoUrl = photoUrl
              )
            )
          )

          val updatedList = try {
            repository.insertBench(newBench, userLat, userLng)
          } catch (e: Exception) {
            if (e.message == "login_required") {
              syncToastMessage = "Connectez-vous pour ajouter un spot"
            } else {
              syncToastMessage = "Échec d'envoi, réessayez"
            }
            delay(2500)
            syncToastMessage = null
            return@launch
          }
          benches = updatedList
          showAddDialog = false

          if (webViewRef != null && isMapReady) {
            val json = benchesToJson(updatedList)
            webViewRef?.evaluateJavascript("setBenches('$json');", null)
            webViewRef?.evaluateJavascript("centerMap(${newBench.latitude}, ${newBench.longitude}, 17);", null)
          }

          syncToastMessage = "Nouveau spot ajouté à la base de données !"
          delay(2500)
          syncToastMessage = null
        }
      }
    )
  }

  // 5. MODALE AUTHENTIFICATION
  if (showAuthDialog) {
    AuthDialog(
      currentUserEmail = currentUserEmail,
      authError = authError,
      onDismiss = { showAuthDialog = false; authError = null },
      onLogin = { email, password ->
        coroutineScope.launch {
          try {
            authError = null
            if (password.length < 6) {
              authError = "Mot de passe : 6 caractères minimum"
              return@launch
            }
            authRepository.signInOrUp(email, password)
            currentUserEmail = email
            showAuthDialog = false
            syncToastMessage = "Connecté, vos contenus sont partagés !"
            delay(2500)
            syncToastMessage = null
          } catch (e: Exception) {
            authError = when {
              e.message?.startsWith("signin_failed:400") == true ->
                "Email ou mot de passe incorrect"
              e.message?.startsWith("supabase_not_configured") == true ->
                "Backend non configuré (mode local)"
              else -> "Connexion impossible, réessayez"
            }
          }
        }
      },
      onLogout = {
        authRepository.signOut()
        currentUserEmail = null
        authError = null
        showAuthDialog = false
      }
    )
  }
}

// Panneau liste des spots (plein écran sur téléphone, latéral sur tablette)
@Composable
fun BenchListPanel(
  benches: List<BenchItem>,
  userLat: Double,
  userLng: Double,
  onBenchClick: (BenchItem) -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Slate100)
  ) {
    if (benches.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Aucun spot trouvé dans un rayon de 100 km.",
          color = Slate500,
          fontSize = 14.sp
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(benches, key = { it.id }) { bench ->
          BenchListItemCard(
            bench = bench,
            userLat = userLat,
            userLng = userLng,
            onClick = { onBenchClick(bench) }
          )
        }
      }
    }
  }
}

// Carte d'un banc dans la vue liste
@Composable
fun BenchListItemCard(
  bench: BenchItem,
  userLat: Double,
  userLng: Double,
  onClick: () -> Unit
) {
  val dist = calculateDistanceMeters(userLat, userLng, bench.latitude, bench.longitude).toInt()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("bench_list_item_${bench.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = bench.photoUrl,
        contentDescription = bench.title,
        modifier = Modifier
          .size(72.dp)
          .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            contentColor = Color.White
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AmberRating,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = String.format("%.1f", bench.rating),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }

          Text(
            text = formatDistance(dist),
            fontSize = 11.sp,
            color = Slate500,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = bench.title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = Slate900,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Text(
          text = bench.description,
          fontSize = 12.sp,
          color = Slate700,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
