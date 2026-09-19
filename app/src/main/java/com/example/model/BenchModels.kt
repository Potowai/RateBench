package com.example.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class BenchReview(
  val id: String,
  val userName: String = "Visiteur",
  val rating: Int,
  val comment: String,
  val date: String,
  val photoUrl: String? = null // Chaque avis a le droit d'ajouter une photo
)

data class BenchItem(
  val id: String,
  val title: String,
  val latitude: Double,
  val longitude: Double,
  val rating: Float,
  val reviewCount: Int,
  val description: String,
  val photoUrl: String,
  val reviews: List<BenchReview> = emptyList(),
  val author: String = "Communauté"
)

fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val r = 6371000.0
  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)
  val a = sin(dLat / 2) * sin(dLat / 2) +
    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
    sin(dLon / 2) * sin(dLon / 2)
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  return r * c
}

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  return calculateDistanceMeters(lat1, lon1, lat2, lon2) / 1000.0
}
