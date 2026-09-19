import React, { useState, useEffect } from 'react';
import { Star, X, MapPin, Navigation, MessageSquare, Plus, Loader2 } from 'lucide-react';
import { Bench, Review, UserLocation } from '../types';
import { supabase } from '../lib/supabase';

interface BenchDetailsSheetProps {
  bench: Bench | null;
  onClose: () => void;
  userLocation: UserLocation | null;
  onReviewAdded: () => void;
  userId?: string;
}

// Calcul de distance euclidienne / haversine en mètres
function calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371e3; // Rayon terre en mètres
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δφ = ((lat2 - lat1) * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return Math.round(R * c);
}

function formatDistance(meters: number): string {
  if (meters < 1000) {
    return `${meters} m`;
  }
  return `${(meters / 1000).toFixed(1)} km`;
}

export const BenchDetailsSheet: React.FC<BenchDetailsSheetProps> = ({
  bench,
  onClose,
  userLocation,
  onReviewAdded,
  userId,
}) => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(false);
  const [isAddingReview, setIsAddingReview] = useState(false);
  const [newRating, setNewRating] = useState(8);
  const [newComment, setNewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  useEffect(() => {
    if (bench) {
      fetchReviews(bench.id);
      setIsAddingReview(false);
    }
  }, [bench]);

  const fetchReviews = async (benchId: string) => {
    setLoadingReviews(true);
    try {
      const { data, error } = await supabase
        .from('reviews')
        .select('*')
        .eq('bench_id', benchId)
        .order('created_at', { ascending: false });

      if (error) throw error;
      setReviews(data || []);
    } catch (err) {
      console.error('Erreur récupération avis:', err);
    } finally {
      setLoadingReviews(false);
    }
  };

  const handleAddReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bench) return;
    setSubmittingReview(true);

    try {
      const { error } = await supabase.from('reviews').insert({
        bench_id: bench.id,
        user_id: userId || null,
        rating: newRating,
        comment: newComment.trim() || null,
      });

      if (error) throw error;

      setNewComment('');
      setIsAddingReview(false);
      fetchReviews(bench.id);
      onReviewAdded();
    } catch (err) {
      console.error("Erreur lors de l'ajout de l'avis:", err);
    } finally {
      setSubmittingReview(false);
    }
  };

  if (!bench) return null;

  const distance = userLocation
    ? calculateDistanceMeters(userLocation.lat, userLocation.lng, bench.latitude, bench.longitude)
    : null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/40 backdrop-blur-xs p-0 sm:p-4">
      <div className="w-full max-w-lg bg-white rounded-t-3xl sm:rounded-2xl max-h-[88vh] flex flex-col shadow-2xl overflow-hidden animate-in slide-in-from-bottom duration-200">
        {/* Handle tactile */}
        <div className="w-full pt-3 pb-1 flex justify-center sm:hidden">
          <div className="w-12 h-1.5 bg-slate-300 rounded-full"></div>
        </div>

        {/* Photo grand format en tête */}
        <div className="relative w-full h-52 sm:h-64 bg-slate-100 flex-shrink-0">
          {bench.cover_photo_url ? (
            <img
              src={bench.cover_photo_url}
              alt={bench.title || 'Banc public'}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center text-slate-400 bg-slate-100">
              <MapPin className="w-10 h-10 stroke-1 mb-1 text-slate-300" />
              <span className="text-xs">Pas encore de photo pour ce banc</span>
            </div>
          )}

          {/* Bouton fermeture */}
          <button
            onClick={onClose}
            className="absolute top-3 right-3 w-9 h-9 flex items-center justify-center rounded-full bg-slate-900/60 hover:bg-slate-900 text-white backdrop-blur-sm transition"
          >
            <X className="w-4 h-4" />
          </button>

          {/* Note moyenne flottante sur l'image */}
          <div className="absolute bottom-3 left-3 bg-slate-900/85 backdrop-blur-md text-white px-3 py-1.5 rounded-xl flex items-center gap-1.5 shadow-md border border-white/20">
            <Star className="w-4 h-4 fill-amber-300 text-amber-300" />
            <span className="font-bold text-sm">
              {bench.avg_rating && bench.avg_rating > 0 ? bench.avg_rating.toFixed(1) : '—'}
            </span>
            <span className="text-slate-300 text-xs">/10</span>
          </div>
        </div>

        {/* Contenu déroulant */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {/* Titre et distance */}
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                {bench.title || 'Banc sans nom'}
              </h2>
              <div className="flex items-center gap-3 text-xs text-slate-500 mt-1">
                {distance !== null && (
                  <span className="flex items-center gap-1 text-slate-700 font-medium">
                    <Navigation className="w-3.5 h-3.5 text-slate-500" />
                    À {formatDistance(distance)}
                  </span>
                )}
                <span>
                  {bench.latitude.toFixed(4)}, {bench.longitude.toFixed(4)}
                </span>
              </div>
            </div>

            {/* Lien Google Maps / itinéraire */}
            <a
              href={`https://www.google.com/maps/dir/?api=1&destination=${bench.latitude},${bench.longitude}`}
              target="_blank"
              rel="noopener noreferrer"
              className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium flex items-center gap-1.5 transition"
            >
              <Navigation className="w-3.5 h-3.5" />
              Itinéraire
            </a>
          </div>

          {/* Section Avis */}
          <div className="border-t border-slate-100 pt-4">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-slate-800 flex items-center gap-1.5">
                <MessageSquare className="w-4 h-4 text-slate-500" />
                Avis de la communauté ({reviews.length})
              </h3>
              {!isAddingReview && (
                <button
                  onClick={() => setIsAddingReview(true)}
                  className="text-xs text-slate-700 hover:text-slate-900 font-medium flex items-center gap-1 px-2.5 py-1 rounded-md bg-slate-100 hover:bg-slate-200 transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Noter ce spot
                </button>
              )}
            </div>

            {/* Formulaire d'ajout d'avis */}
            {isAddingReview && (
              <form onSubmit={handleAddReview} className="bg-slate-50 border border-slate-200 rounded-2xl p-4 mb-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-600">Votre note :</span>
                  <div className="flex items-center gap-1 font-bold text-slate-800 text-sm">
                    <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                    <span>{newRating}/10</span>
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="10"
                  step="1"
                  value={newRating}
                  onChange={(e) => setNewRating(Number(e.target.value))}
                  className="w-full h-1.5 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-slate-800"
                />
                <textarea
                  rows={2}
                  placeholder="Votre ressenti (vue, calme, propreté...)"
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs focus:ring-1 focus:ring-slate-700 outline-none"
                />
                <div className="flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setIsAddingReview(false)}
                    className="px-3 py-1.5 text-xs text-slate-500 hover:text-slate-700"
                  >
                    Annuler
                  </button>
                  <button
                    type="submit"
                    disabled={submittingReview}
                    className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-xs font-medium flex items-center gap-1"
                  >
                    {submittingReview ? <Loader2 className="w-3 h-3 animate-spin" /> : 'Publier'}
                  </button>
                </div>
              </form>
            )}

            {/* Liste des avis */}
            {loadingReviews ? (
              <div className="py-6 flex justify-center text-slate-400">
                <Loader2 className="w-5 h-5 animate-spin" />
              </div>
            ) : reviews.length === 0 ? (
              <p className="text-xs text-slate-400 italic py-2">
                Aucun avis pour l'instant. Soyez le premier à noter ce banc !
              </p>
            ) : (
              <div className="space-y-3">
                {reviews.map((rev) => (
                  <div key={rev.id} className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-1 text-xs font-semibold text-slate-800">
                        <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                        <span>{rev.rating}/10</span>
                      </div>
                      <span className="text-[10px] text-slate-400">
                        {new Date(rev.created_at).toLocaleDateString('fr-FR')}
                      </span>
                    </div>
                    {rev.comment && <p className="text-xs text-slate-600 mt-1">{rev.comment}</p>}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
