package com.example.data.supabase

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Accès direct aux API REST Supabase (PostgREST + GoTrue Auth) via Retrofit,
 * sans dépendance supplémentaire (ResponseBody + org.json pour le parsing).
 */
interface SupabaseApi {

  // ---------- Auth (GoTrue) ----------
  @POST("auth/v1/signup")
  suspend fun signUp(
    @Header("apikey") apiKey: String,
    @Body body: RequestBody
  ): Response<ResponseBody>

  @POST("auth/v1/token?grant_type=password")
  suspend fun signIn(
    @Header("apikey") apiKey: String,
    @Body body: RequestBody
  ): Response<ResponseBody>

  @POST("auth/v1/token?grant_type=refresh_token")
  suspend fun refreshToken(
    @Header("apikey") apiKey: String,
    @Body body: RequestBody
  ): Response<ResponseBody>

  // ---------- PostgREST ----------
  @GET("rest/v1/benches")
  suspend fun getBenches(
    @Header("apikey") apiKey: String,
    @Header("Authorization") auth: String,
    @Query("select") select: String = "*,reviews(*)",
    @Query("order") order: String = "created_at.desc",
    @Query("limit") limit: Int = 500
  ): Response<ResponseBody>

  @POST("rest/v1/benches")
  suspend fun insertBench(
    @Header("apikey") apiKey: String,
    @Header("Authorization") auth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: RequestBody
  ): Response<ResponseBody>

  @POST("rest/v1/reviews")
  suspend fun insertReview(
    @Header("apikey") apiKey: String,
    @Header("Authorization") auth: String,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: RequestBody
  ): Response<ResponseBody>
}
