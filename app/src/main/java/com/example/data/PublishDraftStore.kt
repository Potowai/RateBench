package com.example.data

import android.content.Context
import org.json.JSONObject

/** Brouillon d'un spot en cours de publication (non connecté). */
data class SpotDraft(
  val title: String,
  val rating: Float,
  val comment: String,
  val photoUrl: String
)

/** Brouillon d'un avis en cours de publication (non connecté). */
data class ReviewDraft(
  val benchId: String,
  val rating: Float,
  val comment: String,
  val photoUrl: String?
)

/**
 * Conserve les formulaires pré-remplis quand la publication exige une
 * connexion : le brouillon survit à la fermeture du dialogue et est
 * rechargé à la réouverture (ou publié en anonyme avec un pseudo).
 */
class PublishDraftStore(context: Context) {

  private val prefs = context.getSharedPreferences("rate_bench_drafts", Context.MODE_PRIVATE)

  // ---------- Spot ----------
  fun saveSpot(title: String, rating: Float, comment: String, photoUrl: String) {
    prefs.edit()
      .putString("spot", JSONObject()
        .put("title", title)
        .put("rating", rating.toDouble())
        .put("comment", comment)
        .put("photoUrl", photoUrl)
        .toString())
      .apply()
  }

  fun getSpot(): SpotDraft? {
    val raw = prefs.getString("spot", null) ?: return null
    return try {
      val o = JSONObject(raw)
      SpotDraft(
        title = o.optString("title", ""),
        rating = o.optDouble("rating", 8.0).toFloat(),
        comment = o.optString("comment", ""),
        photoUrl = o.optString("photoUrl", "")
      )
    } catch (_: Exception) {
      null
    }
  }

  fun clearSpot() {
    prefs.edit().remove("spot").apply()
  }

  // ---------- Avis ----------
  fun saveReview(benchId: String, rating: Float, comment: String, photoUrl: String?) {
    prefs.edit()
      .putString("review_$benchId", JSONObject()
        .put("rating", rating.toDouble())
        .put("comment", comment)
        .put("photoUrl", photoUrl ?: JSONObject.NULL)
        .toString())
      .apply()
  }

  fun getReview(benchId: String): ReviewDraft? {
    val raw = prefs.getString("review_$benchId", null) ?: return null
    return try {
      val o = JSONObject(raw)
      ReviewDraft(
        benchId = benchId,
        rating = o.optDouble("rating", 8.0).toFloat(),
        comment = o.optString("comment", ""),
        photoUrl = o.optString("photoUrl", null)?.takeIf { it.isNotBlank() }
      )
    } catch (_: Exception) {
      null
    }
  }

  fun clearReview(benchId: String) {
    prefs.edit().remove("review_$benchId").apply()
  }

  // ---------- Pseudo anonyme (conservé entre les sessions) ----------
  /** Même pseudo anonyme réutilisé à chaque publication sans compte. */
  fun getAnonPseudo(): String {
    prefs.getString("anon_pseudo", null)?.takeIf { it.isNotBlank() }?.let { return it }
    return randomAnonymousPseudo().also { setAnonPseudo(it) }
  }

  fun setAnonPseudo(pseudo: String) {
    if (pseudo.isNotBlank()) {
      prefs.edit().putString("anon_pseudo", pseudo).apply()
    }
  }
}

/** Pseudo anonyme français (modifiable par l'utilisateur). */
fun randomAnonymousPseudo(): String {
  val names = listOf("Explorateur", "Flâneur", "Marcheur", "Randonneur", "Rêveur", "Promeneur", "Contemplateur")
  return "${names.random()}-${(1000..9999).random()}"
}
