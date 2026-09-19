package com.example.data.supabase

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit

/**
 * Authentification Supabase (email + mot de passe), session persistée en local.
 * Lecture publique sans compte ; écriture (bancs, avis, photos) connectée.
 */
class SupabaseAuthRepository(private val context: Context) {

  private val prefs = context.getSharedPreferences("rate_bench_supabase_auth", Context.MODE_PRIVATE)
  private val jsonMediaType = "application/json".toMediaType()

  private val api: SupabaseApi? by lazy {
    val url = SupabaseConfig.url ?: return@lazy null
    Retrofit.Builder().baseUrl("$url/").build().create(SupabaseApi::class.java)
  }

  val isConfigured: Boolean get() = SupabaseConfig.isConfigured && api != null
  val isLoggedIn: Boolean get() = prefs.getString("access_token", null) != null
  fun sessionEmail(): String? = prefs.getString("email", null)
  fun userId(): String? = prefs.getString("user_id", null)
  fun authToken(): String? = prefs.getString("access_token", null)

  /** Inscription, ou connexion si le compte existe déjà. */
  suspend fun signInOrUp(email: String, password: String): String = withContext(Dispatchers.IO) {
    val client = api ?: throw IllegalStateException("supabase_not_configured")
    val body = JSONObject().put("email", email.trim()).put("password", password)
      .toString().toRequestBody(jsonMediaType)
    val anonKey = SupabaseConfig.anonKey!!

    val signUpRes = client.signUp(anonKey, body)
    if (signUpRes.isSuccessful) {
      val session = JSONObject(signUpRes.stringBody()).optJSONObject("session")
      if (session != null) {
        saveSession(email, signUpRes.stringBody())
        return@withContext email
      }
      // Inscription sans session immédiate (confirmation email activée) -> tentative de connexion
    } else if (!signUpRes.isUserAlreadyRegistered()) {
      throw IllegalStateException("signup_failed:${signUpRes.code()}")
    }
    signIn(email, password)
  }

  suspend fun signIn(email: String, password: String): String = withContext(Dispatchers.IO) {
    val client = api ?: throw IllegalStateException("supabase_not_configured")
    val body = JSONObject().put("email", email.trim()).put("password", password)
      .toString().toRequestBody(jsonMediaType)
    val res = client.signIn(SupabaseConfig.anonKey!!, body)
    if (!res.isSuccessful) throw IllegalStateException("signin_failed:${res.code()}")
    saveSession(email, res.stringBody())
    email
  }

  fun signOut() {
    prefs.edit().clear().apply()
  }

  /** Rafraîchit le token d'accès via le refresh_token stocké. */
  suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
    val client = api ?: return@withContext false
    val refresh = prefs.getString("refresh_token", null) ?: return@withContext false
    val email = prefs.getString("email", null) ?: return@withContext false
    return@withContext try {
      val body = JSONObject().put("refresh_token", refresh)
        .toString().toRequestBody(jsonMediaType)
      val res = client.refreshToken(SupabaseConfig.anonKey!!, body)
      if (!res.isSuccessful) {
        signOut()
        false
      } else {
        saveSession(email, res.stringBody())
        true
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun saveSession(email: String, payload: String) {
    val json = JSONObject(payload)
    // signUp renvoie { user, session } ; signIn/refresh renvoient la session directement
    val session = json.optJSONObject("session") ?: json
    val user = json.optJSONObject("user")
      ?: session.optJSONObject("user")
    prefs.edit()
      .putString("access_token", session.optString("access_token", null))
      .putString("refresh_token", session.optString("refresh_token", null))
      .putString("user_id", user?.optString("id", null))
      .putString("email", email)
      .apply()
  }

  private fun Response<ResponseBody>.isUserAlreadyRegistered(): Boolean = try {
    val err = errorBody()?.string().orEmpty()
    code() == 422 && (err.contains("already registered", ignoreCase = true) ||
      err.contains("already exists", ignoreCase = true))
  } catch (_: Exception) {
    false
  }

  private fun Response<ResponseBody>.stringBody(): String = body()?.string().orEmpty()
}
