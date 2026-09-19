package com.example.data.supabase

/**
 * Configuration Supabase lue depuis les champs BuildConfig générés par le
 * plugin Secrets (.env / .env.example : SUPABASE_URL, SUPABASE_ANON_KEY).
 *
 * Lecture par réflexion : si les clés ne sont pas configurées, les champs
 * BuildConfig n'existent pas et l'appli bascule en mode local (données
 * simulées) au lieu de planter à la compilation ou au démarrage.
 */
object SupabaseConfig {
  val url: String? = buildConfigField("SUPABASE_URL")
    ?.trim()?.trimEnd('/')?.takeIf { it.startsWith("http") }
  val anonKey: String? = buildConfigField("SUPABASE_ANON_KEY")
    ?.trim()?.takeIf { it.length > 20 }

  val isConfigured: Boolean get() = url != null && anonKey != null

  fun publicStorageUrl(bucket: String, path: String): String? =
    url?.let { "$it/storage/v1/object/public/$bucket/$path" }

  private fun buildConfigField(name: String): String? = try {
    com.example.BuildConfig::class.java.getField(name).get(null) as? String
  } catch (_: Exception) {
    null
  }
}
