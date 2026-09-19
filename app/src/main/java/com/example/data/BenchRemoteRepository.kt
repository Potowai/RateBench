package com.example.data

import android.content.Context
import com.example.model.BenchItem
import com.example.model.BenchReview
import com.example.model.calculateDistanceKm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BenchRemoteRepository(private val context: Context) {
  private val prefs = context.getSharedPreferences("rate_bench_remote_db", Context.MODE_PRIVATE)

  // Base de données distante simulée / connecteur API externe
  // Contient des bancs dans le rayon de 100 km et d'autres hors rayon (ex: Lyon à ~400 km)
  private val defaultRemoteBenches: List<BenchItem> = listOf(
    BenchItem(
      id = "bench-1",
      title = "Banc face au coucher de soleil sur la Seine",
      latitude = 48.8575,
      longitude = 2.3514,
      rating = 9.2f,
      reviewCount = 14,
      description = "Vue imprenable sur l'eau et les péniches. Parfait au crépuscule, bois verni très bien entretenu.",
      photoUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r1",
          userName = "Camille D.",
          rating = 10,
          comment = "L'un des meilleurs spots de Paris pour décompresser. Regardez cette vue !",
          date = "Il y a 2 jours",
          photoUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=600&q=80"
        ),
        BenchReview(
          id = "r2",
          userName = "Thomas R.",
          rating = 9,
          comment = "Très calme vers 19h, ombre bienvenue sous le saule.",
          date = "La semaine passée",
          photoUrl = null
        )
      )
    ),
    BenchItem(
      id = "bench-2",
      title = "Spot ombragé sous les platanes du canal",
      latitude = 48.8682,
      longitude = 2.3644,
      rating = 8.5f,
      reviewCount = 8,
      description = "Banc double en pierre et fonte. Fraîcheur naturelle appréciable en plein été pour lire.",
      photoUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r3",
          userName = "Sophie M.",
          rating = 9,
          comment = "Idéal pour une pause lecture au frais au bord de l'eau.",
          date = "Il y a 3 jours",
          photoUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=600&q=80"
        ),
        BenchReview(
          id = "r4",
          userName = "Julien K.",
          rating = 8,
          comment = "Quelques cyclistes mais cadre relaxant.",
          date = "Il y a 10 jours",
          photoUrl = null
        )
      )
    ),
    BenchItem(
      id = "bench-3",
      title = "Belvédère de Montmartre (vue panoramique)",
      latitude = 48.8867,
      longitude = 2.3431,
      rating = 9.7f,
      reviewCount = 29,
      description = "En retrait de la foule, banc en bois patiné avec horizon dégagé sur les toits parisiens.",
      photoUrl = "https://images.unsplash.com/photo-1508050919630-b135583b3ae8?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r5",
          userName = "Alexandre V.",
          rating = 10,
          comment = "Mon banc secret préféré pour contempler le lever de soleil.",
          date = "Hier",
          photoUrl = "https://images.unsplash.com/photo-1508050919630-b135583b3ae8?auto=format&fit=crop&w=600&q=80"
        )
      )
    ),
    BenchItem(
      id = "bench-4",
      title = "Havre de paix dans la cour du cloître",
      latitude = 48.8520,
      longitude = 2.3580,
      rating = 9.0f,
      reviewCount = 6,
      description = "Silencieux, entouré de rosiers et de vieilles pierres. Idéal pour le télétravail nomade ou la méditation.",
      photoUrl = "https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r6",
          userName = "Léa B.",
          rating = 9,
          comment = "Le chant des oiseaux en plein Paris, un pur bonheur.",
          date = "Il y a 5 jours",
          photoUrl = null
        )
      )
    ),
    BenchItem(
      id = "bench-5",
      title = "Banc royal des jardins de Versailles",
      latitude = 48.8048,
      longitude = 2.1203,
      rating = 9.4f,
      reviewCount = 18,
      description = "À ~18 km du centre. Banc en marbre et bois sculpté offrant une perspective grandiose sur le Grand Canal.",
      photoUrl = "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r7",
          userName = "Marc P.",
          rating = 10,
          comment = "Majestueux et paisible en fin d'après-midi.",
          date = "Il y a 1 semaine",
          photoUrl = "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=600&q=80"
        )
      )
    ),
    BenchItem(
      id = "bench-6",
      title = "Banc des rochers de Fontainebleau",
      latitude = 48.4047,
      longitude = 2.7016,
      rating = 9.1f,
      reviewCount = 11,
      description = "À ~55 km de Paris. Banc taillé dans le grès au sommet d'une gorge sablonneuse.",
      photoUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r8",
          userName = "Clara G.",
          rating = 9,
          comment = "Parfait après une randonnée sur le circuit des 25 bosses !",
          date = "Il y a 2 semaines",
          photoUrl = null
        )
      )
    ),
    // BANC HORS RAYON (> 100 km) pour valider le filtre strict de 100 km demandé par l'utilisateur
    BenchItem(
      id = "bench-lyon",
      title = "Banc des hauteurs de Fourvière (Lyon)",
      latitude = 45.7624,
      longitude = 4.8223,
      rating = 9.6f,
      reviewCount = 42,
      description = "Banc situé à Lyon (~390 km de Paris), filtré lors du refresh dans le rayon de 100 km.",
      photoUrl = "https://images.unsplash.com/photo-1524397030763-9a9163e79391?auto=format&fit=crop&w=800&q=80",
      reviews = listOf(
        BenchReview(
          id = "r-lyon",
          userName = "Bastien L.",
          rating = 10,
          comment = "Magnifique panorama sur le Rhône.",
          date = "Il y a 1 mois",
          photoUrl = null
        )
      )
    )
  )

  // Récupération de tous les bancs stockés dans la base distante
  fun getAllRemoteBenches(): List<BenchItem> {
    val jsonString = prefs.getString("remote_benches_db", null) ?: return defaultRemoteBenches
    return try {
      deserializeBenches(jsonString)
    } catch (e: Exception) {
      defaultRemoteBenches
    }
  }

  // Sauvegarde dans la base externe
  private fun saveRemoteBenches(benches: List<BenchItem>) {
    val json = serializeBenches(benches)
    prefs.edit().putString("remote_benches_db", json).apply()
  }

  /**
   * Rafraîchissement via appel API externe.
   * Filtre STRICTEMENT dans un rayon de 100 km par rapport à la géolocalisation
   * de l'utilisateur (et NON là où regarde la caméra/carte).
   */
  suspend fun syncNearbyBenches(
    userLat: Double,
    userLng: Double,
    radiusKm: Double = 100.0
  ): List<BenchItem> = withContext(Dispatchers.IO) {
    // Simuler le délai réseau d'un appel HTTP à l'API de base de données externe
    delay(650)

    val allBenches = getAllRemoteBenches()

    // Filtrage strict : distance <= radiusKm (100 km par défaut) depuis la géolocalisation utilisateur
    val nearby = allBenches.filter { bench ->
      val distKm = calculateDistanceKm(userLat, userLng, bench.latitude, bench.longitude)
      distKm <= radiusKm
    }

    // Sauvegarder dans le cache local synchronisé
    saveRemoteBenches(allBenches)
    nearby
  }

  // Ajout d'un nouveau banc dans la base de données distante
  suspend fun insertBench(
    bench: BenchItem,
    userLat: Double,
    userLng: Double
  ): List<BenchItem> = withContext(Dispatchers.IO) {
    delay(400)
    val current = getAllRemoteBenches().toMutableList()
    current.add(0, bench)
    saveRemoteBenches(current)
    // Retourner la liste rafraîchie dans les 100 km
    current.filter { calculateDistanceKm(userLat, userLng, it.latitude, it.longitude) <= 100.0 }
  }

  // Ajout d'un avis (avec support d'une photo d'avis) sur un banc dans la base de données distante
  suspend fun addReviewToBench(
    benchId: String,
    review: BenchReview,
    userLat: Double,
    userLng: Double
  ): Pair<BenchItem?, List<BenchItem>> = withContext(Dispatchers.IO) {
    delay(400)
    val current = getAllRemoteBenches().toMutableList()
    val index = current.indexOfFirst { it.id == benchId }
    var updatedBench: BenchItem? = null

    if (index != -1) {
      val existing = current[index]
      val updatedReviews = listOf(review) + existing.reviews
      val newAvgRating = updatedReviews.map { it.rating }.average().toFloat()

      updatedBench = existing.copy(
        reviews = updatedReviews,
        reviewCount = updatedReviews.size,
        rating = Math.round(newAvgRating * 10f) / 10f
      )
      current[index] = updatedBench
      saveRemoteBenches(current)
    }

    val nearby = current.filter { calculateDistanceKm(userLat, userLng, it.latitude, it.longitude) <= 100.0 }
    Pair(updatedBench, nearby)
  }

  private fun serializeBenches(benches: List<BenchItem>): String {
    val arr = JSONArray()
    for (b in benches) {
      val obj = JSONObject().apply {
        put("id", b.id)
        put("title", b.title)
        put("latitude", b.latitude)
        put("longitude", b.longitude)
        put("rating", b.rating.toDouble())
        put("reviewCount", b.reviewCount)
        put("description", b.description)
        put("photoUrl", b.photoUrl)
        put("author", b.author)

        val reviewsArr = JSONArray()
        for (r in b.reviews) {
          val rObj = JSONObject().apply {
            put("id", r.id)
            put("userName", r.userName)
            put("rating", r.rating)
            put("comment", r.comment)
            put("date", r.date)
            if (r.photoUrl != null) {
              put("photoUrl", r.photoUrl)
            }
          }
          reviewsArr.put(rObj)
        }
        put("reviews", reviewsArr)
      }
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun deserializeBenches(jsonString: String): List<BenchItem> {
    val result = mutableListOf<BenchItem>()
    val arr = JSONArray(jsonString)
    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)
      val reviewsList = mutableListOf<BenchReview>()
      if (obj.has("reviews")) {
        val rArr = obj.getJSONArray("reviews")
        for (j in 0 until rArr.length()) {
          val rObj = rArr.getJSONObject(j)
          reviewsList.add(
            BenchReview(
              id = rObj.getString("id"),
              userName = if (rObj.has("userName")) rObj.getString("userName") else "Visiteur",
              rating = rObj.getInt("rating"),
              comment = rObj.getString("comment"),
              date = rObj.getString("date"),
              photoUrl = if (rObj.has("photoUrl") && !rObj.isNull("photoUrl")) rObj.getString("photoUrl") else null
            )
          )
        }
      }

      result.add(
        BenchItem(
          id = obj.getString("id"),
          title = obj.getString("title"),
          latitude = obj.getDouble("latitude"),
          longitude = obj.getDouble("longitude"),
          rating = obj.getDouble("rating").toFloat(),
          reviewCount = obj.getInt("reviewCount"),
          description = obj.getString("description"),
          photoUrl = obj.getString("photoUrl"),
          reviews = reviewsList,
          author = if (obj.has("author")) obj.getString("author") else "Communauté"
        )
      )
    }
    return result
  }
}
