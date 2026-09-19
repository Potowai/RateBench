package com.example.data.supabase

import android.content.Context
import android.net.Uri
import com.example.data.BenchRemoteRepository
import com.example.model.BenchItem
import com.example.model.BenchReview
import com.example.model.calculateDistanceKm
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit

/**
 * Repository distant adossé à Supabase, avec repli automatique sur le
 * repository local simulé si Supabase n'est pas configuré ou injoignable.
 *
 * Mêmes signatures que [BenchRemoteRepository] pour un câblage minimal :
 * - syncNearbyBenches : lecture publique (même sans compte)
 * - insertBench / addReviewToBench : nécessitent un compte (RLS)
 * - toute photo "content://" est téléversée vers le Storage avant écriture
 */
class SupabaseBenchRepository(
  private val context: Context,
  private val auth: SupabaseAuthRepository,
  private val localFallback: BenchRemoteRepository
) {
  companion object {
    const val BENCH_BUCKET = "bench-photos"
    const val REVIEW_BUCKET = "review-photos"
  }

  private val jsonMediaType = "application/json".toMediaType()
  private val http = OkHttpClient()

  private val api: SupabaseApi? by lazy {
    val url = SupabaseConfig.url ?: return@lazy null
    Retrofit.Builder().baseUrl("$url/").build().create(SupabaseApi::class.java)
  }

  val isCloudEnabled: Boolean get() = SupabaseConfig.isConfigured && api != null
  fun isLoggedIn(): Boolean = auth.isLoggedIn

  private fun anonAuth(): Pair<String, String> {
    val key = SupabaseConfig.anonKey!!
    return key to "Bearer $key"
  }

  private suspend fun userAuth(): Pair<String, String> {
    val key = SupabaseConfig.anonKey!!
    var token = auth.authToken()
    if (token == null) throw IllegalStateException("login_required")
    return key to "Bearer $token"
  }

  // ---------- Lecture ----------

  suspend fun syncNearbyBenches(
    userLat: Double,
    userLng: Double,
    radiusKm: Double = 100.0
  ): List<BenchItem> {
    if (!isCloudEnabled) return localFallback.syncNearbyBenches(userLat, userLng, radiusKm)
    return try {
      val (key, bearer) = anonAuth()
      val res = withContext(Dispatchers.IO) { api!!.getBenches(key, bearer) }
      if (!res.isSuccessful) throw IllegalStateException("fetch_failed:${res.code()}")
      val all = parseBenches(JSONArray(res.body()?.string().orEmpty()))
      all.filter { calculateDistanceKm(userLat, userLng, it.latitude, it.longitude) <= radiusKm }
    } catch (e: Exception) {
      if (e is IllegalStateException && e.message == "login_required") throw e
      // Repli local en cas de panne réseau
      localFallback.syncNearbyBenches(userLat, userLng, radiusKm)
    }
  }

  // ---------- Écritures (compte requis) ----------

  suspend fun insertBench(
    bench: BenchItem,
    userLat: Double,
    userLng: Double
  ): List<BenchItem> {
    if (!isCloudEnabled) return localFallback.insertBench(bench, userLat, userLng)
    val userId = auth.userId() ?: throw IllegalStateException("login_required")
    val (key, bearer) = userAuth()

    val photoUrl = uploadIfLocal(bench.photoUrl, BENCH_BUCKET) ?: bench.photoUrl

    val payload = JSONObject()
      .put("title", bench.title)
      .put("description", bench.description)
      .put("latitude", bench.latitude)
      .put("longitude", bench.longitude)
      .put("author_id", userId)
      .put("author_name", bench.author.ifBlank { "Communauté" })
      .put("photo_url", photoUrl)
      .toString().toRequestBody(jsonMediaType)

    val inserted = postWithAuthRetry(
      call = { token -> api!!.insertBench(key, "Bearer $token", body = payload) },
      bearer = bearer
    )
    val benchId = JSONArray(inserted).optJSONObject(0)?.optString("id")
      ?: throw IllegalStateException("insert_failed")

    // Premier avis (note + commentaire de création)
    val first = bench.reviews.firstOrNull()
    if (first != null) {
      val reviewPhoto = uploadIfLocal(first.photoUrl, REVIEW_BUCKET)
      val reviewPayload = JSONObject()
        .put("bench_id", benchId)
        .put("user_id", userId)
        .put("user_name", first.userName)
        .put("rating", first.rating)
        .put("comment", first.comment)
        .put("photo_url", reviewPhoto ?: JSONObject.NULL)
        .toString().toRequestBody(jsonMediaType)
      postWithAuthRetry(
        call = { token -> api!!.insertReview(key, "Bearer $token", body = reviewPayload) },
        bearer = bearer
      )
    }
    return syncNearbyBenches(userLat, userLng, 100.0)
  }

  suspend fun addReviewToBench(
    benchId: String,
    review: BenchReview,
    userLat: Double,
    userLng: Double
  ): Pair<BenchItem?, List<BenchItem>> {
    if (!isCloudEnabled) return localFallback.addReviewToBench(benchId, review, userLat, userLng)
    val userId = auth.userId() ?: throw IllegalStateException("login_required")
    val (key, bearer) = userAuth()

    val photoUrl = uploadIfLocal(review.photoUrl, REVIEW_BUCKET)
    val payload = JSONObject()
      .put("bench_id", benchId)
      .put("user_id", userId)
      .put("user_name", review.userName)
      .put("rating", review.rating)
      .put("comment", review.comment)
      .put("photo_url", photoUrl ?: JSONObject.NULL)
      .toString().toRequestBody(jsonMediaType)

    postWithAuthRetry(
      call = { token -> api!!.insertReview(key, "Bearer $token", body = payload) },
      bearer = bearer
    )
    val nearby = syncNearbyBenches(userLat, userLng, 100.0)
    return nearby.firstOrNull { it.id == benchId } to nearby
  }

  // ---------- Storage ----------

  /** Téléverse une image "content://" vers le bucket, renvoie l'URL publique (ou null). */
  suspend fun uploadIfLocal(uriString: String?, bucket: String): String? {
    if (uriString == null || !uriString.startsWith("content://")) return uriString?.takeIf { it.startsWith("http") }
    val base = SupabaseConfig.url ?: return null
    val key = SupabaseConfig.anonKey ?: return null
    val token = try {
      val (_, bearer) = userAuth()
      bearer.removePrefix("Bearer ")
    } catch (_: Exception) {
      return null // upload anonyme interdit par RLS -> on garde l'URI locale
    }
    return withContext(Dispatchers.IO) {
      try {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
          ?: return@withContext null
        val mime = context.contentResolver.getType(Uri.parse(uriString)) ?: "image/jpeg"
        val mimeLower = mime.lowercase()
        val ext = when {
          mimeLower.contains("png") -> "png"
          mimeLower.contains("gif") -> "gif"
          mimeLower.contains("mp4") -> "mp4"
          mimeLower.contains("quicktime") -> "mov"
          mimeLower.contains("webm") -> "webm"
          mimeLower.contains("3gpp") -> "3gp"
          else -> "jpg"
        }
        val path = "${UUID.randomUUID()}.$ext"
        val req = Request.Builder()
          .url("$base/storage/v1/object/$bucket/$path")
          .addHeader("apikey", key)
          .addHeader("Authorization", "Bearer $token")
          .addHeader("Content-Type", mime)
          .post(bytes.toRequestBody(mime.toMediaType()))
          .build()
        http.newCall(req).execute().use { res ->
          if (!res.isSuccessful) return@withContext null
        }
        SupabaseConfig.publicStorageUrl(bucket, path)
      } catch (_: Exception) {
        null
      }
    }
  }

  // ---------- Helpers réseau ----------

  /** POST avec un seul retry après refresh du token si 401. */
  private suspend fun postWithAuthRetry(
    call: suspend (token: String) -> retrofit2.Response<okhttp3.ResponseBody>,
    bearer: String
  ): String = withContext(Dispatchers.IO) {
    var res = call(bearer.removePrefix("Bearer "))
    if (res.code() == 401 && auth.refreshSession()) {
      val fresh = auth.authToken() ?: throw IllegalStateException("login_required")
      res = call(fresh)
    }
    if (!res.isSuccessful) {
      if (res.code() == 401 || res.code() == 403) throw IllegalStateException("login_required")
      throw IllegalStateException("write_failed:${res.code()}")
    }
    res.body()?.string().orEmpty()
  }

  // ---------- Mapping JSON -> modèles ----------

  private fun parseBenches(arr: JSONArray): List<BenchItem> {
    val out = mutableListOf<BenchItem>()
    for (i in 0 until arr.length()) {
      val o = arr.optJSONObject(i) ?: continue
      val reviewsJson = o.optJSONArray("reviews") ?: JSONArray()
      val dated = mutableListOf<Pair<String, BenchReview>>()
      for (j in 0 until reviewsJson.length()) {
        val r = reviewsJson.optJSONObject(j) ?: continue
        val createdAt = r.optString("created_at", "")
        dated.add(
          createdAt to BenchReview(
            id = r.optString("id"),
            userName = r.optString("user_name", "Visiteur"),
            rating = r.optInt("rating", 5),
            comment = r.optString("comment", ""),
            date = relativeFr(createdAt),
            photoUrl = r.optString("photo_url", null)?.takeIf { it.isNotBlank() }
          )
        )
      }
      val reviews = dated.sortedByDescending { it.first }.map { it.second }
      out.add(
        BenchItem(
          id = o.optString("id"),
          title = o.optString("title"),
          latitude = o.optDouble("latitude"),
          longitude = o.optDouble("longitude"),
          rating = o.optDouble("rating_avg", 0.0).toFloat(),
          reviewCount = o.optInt("review_count", reviews.size),
          description = o.optString("description", ""),
          photoUrl = o.optString("photo_url", ""),
          reviews = reviews,
          author = o.optString("author_name", "Communauté")
        )
      )
    }
    return out
  }

  /** "2026-09-19T12:00:00+00:00" -> "Il y a 2 jours". */
  private fun relativeFr(iso: String): String {
    if (iso.isBlank()) return "Récemment"
    return try {
      val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRANCE).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
      val clean = iso.substringBefore("+").substringBefore("Z")
      val date = fmt.parse(clean) ?: return "Récemment"
      val diffMin = ((System.currentTimeMillis() - date.time) / 60000).coerceAtLeast(0)
      when {
        diffMin < 1 -> "À l'instant"
        diffMin < 60 -> "Il y a ${diffMin} min"
        diffMin < 1440 -> "Il y a ${diffMin / 60} h"
        diffMin < 43200 -> "Il y a ${diffMin / 1440} j"
        else -> "Il y a ${diffMin / 43200} mois"
      }
    } catch (_: Exception) {
      "Récemment"
    }
  }
}
