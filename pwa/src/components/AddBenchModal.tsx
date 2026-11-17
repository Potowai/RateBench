import React, { useState, useEffect, useRef } from 'react';
import { Camera, Star, X, Loader2, MapPin } from 'lucide-react';
import { UserLocation } from '../types';
import { uploadBenchPhoto } from '../services/imageService';
import { supabase } from '../lib/supabase';

interface AddBenchModalProps {
  isOpen: boolean;
  onClose: () => void;
  userLocation: UserLocation | null;
  onBenchAdded: () => void;
  userId?: string;
}

export const AddBenchModal: React.FC<AddBenchModalProps> = ({
  isOpen,
  onClose,
  userLocation,
  onBenchAdded,
  userId,
}) => {
  const [title, setTitle] = useState('');
  const [rating, setRating] = useState<number>(8); // 1 à 10 (contrainte DB)
  const [comment, setComment] = useState('');
  const [photoFiles, setPhotoFiles] = useState<File[]>([]);
  const [photoPreviews, setPhotoPreviews] = useState<string[]>([]);
  const [latitude, setLatitude] = useState<number>(userLocation?.lat ?? 48.8566);
  const [longitude, setLongitude] = useState<number>(userLocation?.lng ?? 2.3522);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // La géolocalisation peut arriver après l'ouverture de la modale : on synchronise
  // les coordonnées dès qu'une position est disponible (bug historique : Paris figé).
  useEffect(() => {
    if (userLocation) {
      setLatitude(userLocation.lat);
      setLongitude(userLocation.lng);
    }
  }, [userLocation]);

  // Fermeture au clavier (accessibilité) et verrouillage du scroll
  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []).slice(0, 5);
    if (files.length) {
      setPhotoFiles((prev) => [...prev, ...files].slice(0, 5));
      setPhotoPreviews((prev) => [...prev, ...files.map((f) => URL.createObjectURL(f))].slice(0, 5));
    }
  };

  const handleRemovePhoto = (index: number) => {
    URL.revokeObjectURL(photoPreviews[index]);
    setPhotoFiles((prev) => prev.filter((_, i) => i !== index));
    setPhotoPreviews((prev) => prev.filter((_, i) => i !== index));
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleUseMyPosition = () => {
    if (userLocation) {
      setLatitude(userLocation.lat);
      setLongitude(userLocation.lng);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      // Plusieurs photos du même banc (max 5), compressées puis téléversées.
      const photoUrls: string[] = [];
      for (const file of photoFiles) {
        photoUrls.push(await uploadBenchPhoto(file, userId));
      }
      const coverUrl = photoUrls[0] ?? null;

      // Schéma canonique (supabase/migrations/001_schema.sql) :
      // benches.author_id, benches.photo_url ; reviews.user_id, reviews.photo_url
      const { data: benchData, error: benchError } = await supabase
        .from('benches')
        .insert({
          title: title.trim() || 'Banc de repos',
          latitude,
          longitude,
          author_id: userId || null,
          photo_url: coverUrl ?? '',
        })
        .select('id')
        .single();

      if (benchError) throw benchError;

      // Galerie : une ligne par photo supplémentaire (migration 007).
      if (photoUrls.length > 1) {
        const { error: galleryError } = await supabase.from('bench_photos').insert(
          photoUrls.map((url, i) => ({ bench_id: benchData.id, photo_url: url, sort_order: i }))
        );
        if (galleryError) throw galleryError;
      }

      const { error: reviewError } = await supabase
        .from('reviews')
        .insert({
          bench_id: benchData.id,
          user_id: userId || null,
          rating,
          comment: comment.trim() || null,
          photo_url: coverUrl,
        });

      if (reviewError) throw reviewError;

      onBenchAdded();
      onClose();
    } catch (err: any) {
      console.error("Erreur lors de l'enregistrement du banc:", err);
      setErrorMessage(err.message || "Une erreur est survenue lors de l'enregistrement.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-slate-900/50 backdrop-blur-xs p-0 sm:p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-bench-title"
        className="w-full max-w-lg bg-white rounded-t-3xl sm:rounded-2xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden"
      >
        <div className="w-full pt-3 pb-1 flex justify-center sm:hidden">
          <div className="w-12 h-1.5 bg-slate-300 rounded-full"></div>
        </div>

        <div className="flex items-center justify-between px-6 py-3 border-b border-slate-100">
          <div>
            <h2 id="add-bench-title" className="text-lg font-semibold text-slate-800">
              Ajouter un banc
            </h2>
            <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
              <MapPin className="w-3.5 h-3.5 text-slate-400" aria-hidden="true" />
              <span>{latitude.toFixed(4)}, {longitude.toFixed(4)}</span>
              <button
                type="button"
                onClick={handleUseMyPosition}
                className="ml-1 text-slate-700 underline underline-offset-2 hover:text-slate-900"
              >
                utiliser ma position
              </button>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer la fenêtre d'ajout"
            className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {errorMessage && (
            <div role="alert" className="p-3 bg-red-50 text-red-600 rounded-xl text-xs font-medium border border-red-100">
              {errorMessage}
            </div>
          )}

          <div>
            <span className="block text-xs font-medium text-slate-600 uppercase tracking-wider mb-2">
              Photos du spot <span className="normal-case text-slate-400">(jusqu'à 5)</span>
            </span>
            {photoPreviews.length > 0 && (
              <div className="grid grid-cols-3 gap-2 mb-2">
                {photoPreviews.map((src, i) => (
                  <div key={i} className="relative rounded-xl overflow-hidden aspect-square bg-slate-100 border border-slate-200">
                    <img src={src} alt={`Aperçu ${i + 1}`} className="w-full h-full object-cover" />
                    <button
                      type="button"
                      onClick={() => handleRemovePhoto(i)}
                      aria-label={`Retirer la photo ${i + 1}`}
                      className="absolute top-1 right-1 p-1 bg-slate-900/70 hover:bg-slate-900 text-white rounded-full transition"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
            {photoPreviews.length < 5 && (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-200 hover:border-slate-400 rounded-2xl p-5 flex flex-col items-center justify-center cursor-pointer bg-slate-50 hover:bg-slate-100/60 transition text-center w-full"
              >
                <div className="w-10 h-10 rounded-full bg-slate-200/80 flex items-center justify-center text-slate-600 mb-1.5">
                  <Camera className="w-5 h-5" aria-hidden="true" />
                </div>
                <span className="text-sm font-medium text-slate-700">Ajouter des photos</span>
                <span className="text-xs text-slate-400 mt-0.5">Compression auto &lt; 300 Ko</span>
              </button>
            )}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/*"
              multiple
              onChange={handlePhotoSelect}
              aria-label="Photo du banc"
              className="hidden"
            />
          </div>

          <div>
            <label htmlFor="bench-title" className="block text-xs font-medium text-slate-600 uppercase tracking-wider mb-1.5">
              Nom ou description brève
            </label>
            <input
              id="bench-title"
              type="text"
              placeholder="Ex: Banc ombragé face à la rivière"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-700 focus:bg-white transition"
            />
          </div>

          <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4">
            <div className="flex items-center justify-between mb-2">
              <label htmlFor="bench-rating" className="text-xs font-medium text-slate-600 uppercase tracking-wider">
                Note de confort & vue
              </label>
              <div className="flex items-center gap-1 font-bold text-base text-slate-800">
                <Star className="w-4 h-4 fill-amber-400 text-amber-400" aria-hidden="true" />
                <span>{rating}</span>
                <span className="text-xs font-normal text-slate-400">/10</span>
              </div>
            </div>
            <input
              id="bench-rating"
              type="range"
              min="1"
              max="10"
              step="1"
              value={rating}
              onChange={(e) => setRating(Number(e.target.value))}
              aria-valuetext={`${rating} sur 10`}
              className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-slate-800"
            />
            <div className="flex justify-between text-[11px] text-slate-400 mt-1">
              <span>1 (Inconfortable)</span>
              <span>5 (Moyen)</span>
              <span>10 (Exceptionnel)</span>
            </div>
          </div>

          <div>
            <label htmlFor="bench-comment" className="block text-xs font-medium text-slate-600 uppercase tracking-wider mb-1.5">
              Avis, ambiance, panorama
            </label>
            <textarea
              id="bench-comment"
              rows={3}
              placeholder="Calme, belle exposition au coucher de soleil, dossier confortable..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-700 focus:bg-white transition resize-none"
            />
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 px-4 bg-slate-800 hover:bg-slate-900 disabled:bg-slate-400 text-white rounded-xl font-medium shadow-md flex items-center justify-center gap-2 transition"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                  <span>Compression & Publication...</span>
                </>
              ) : (
                <span>Enregistrer ce spot de repos</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
