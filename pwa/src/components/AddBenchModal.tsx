import React, { useState, useRef } from 'react';
import { Camera, Image as ImageIcon, Star, X, Loader2, MapPin } from 'lucide-react';
import { UserLocation, NewBenchData } from '../types';
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
  const [rating, setRating] = useState<number>(8); // Valeur par défaut agréable
  const [comment, setComment] = useState('');
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [latitude, setLatitude] = useState<number>(userLocation?.lat || 48.8566);
  const [longitude, setLongitude] = useState<number>(userLocation?.lng || 2.3522);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setPhotoFile(file);
      const previewUrl = URL.createObjectURL(file);
      setPhotoPreview(previewUrl);
    }
  };

  const handleRemovePhoto = () => {
    setPhotoFile(null);
    if (photoPreview) {
      URL.revokeObjectURL(photoPreview);
      setPhotoPreview(null);
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      let photoUrl: string | null = null;

      // 1. Compression et upload vers Supabase Storage si une photo est fournie
      if (photoFile) {
        photoUrl = await uploadBenchPhoto(photoFile, userId);
      }

      // 2. Insertion du banc dans la table 'benches'
      const { data: benchData, error: benchError } = await supabase
        .from('benches')
        .insert({
          title: title.trim() || 'Banc de repos',
          latitude,
          longitude,
          created_by: userId || null,
        })
        .select('id')
        .single();

      if (benchError) throw benchError;

      // 3. Insertion de la première évaluation dans la table 'reviews'
      const { error: reviewError } = await supabase
        .from('reviews')
        .insert({
          bench_id: benchData.id,
          user_id: userId || null,
          rating,
          comment: comment.trim() || null,
          photo_url: photoUrl,
        });

      if (reviewError) throw reviewError;

      // Réinitialisation et notification
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
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-slate-900/50 backdrop-blur-xs transition-opacity p-0 sm:p-4">
      {/* Drawer bas mobile / modal centré tablette & desktop */}
      <div 
        className="w-full max-w-lg bg-white rounded-t-3xl sm:rounded-2xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden animate-in slide-in-from-bottom duration-200"
      >
        {/* Barre de glissement tactile (handle) */}
        <div className="w-full pt-3 pb-1 flex justify-center sm:hidden">
          <div className="w-12 h-1.5 bg-slate-300 rounded-full"></div>
        </div>

        {/* En-tête */}
        <div className="flex items-center justify-between px-6 py-3 border-b border-slate-100">
          <div>
            <h2 className="text-lg font-semibold text-slate-800">Ajouter un banc</h2>
            <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
              <MapPin className="w-3.5 h-3.5 text-slate-400" />
              {latitude.toFixed(4)}, {longitude.toFixed(4)}
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Formulaire défilant */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {errorMessage && (
            <div className="p-3 bg-red-50 text-red-600 rounded-xl text-xs font-medium border border-red-100">
              {errorMessage}
            </div>
          )}

          {/* Photo & Prévisualisation */}
          <div>
            <label className="block text-xs font-medium text-slate-600 uppercase tracking-wider mb-2">
              Photo du spot
            </label>
            {photoPreview ? (
              <div className="relative rounded-2xl overflow-hidden aspect-video bg-slate-100 border border-slate-200 group">
                <img
                  src={photoPreview}
                  alt="Aperçu du banc"
                  className="w-full h-full object-cover"
                />
                <button
                  type="button"
                  onClick={handleRemovePhoto}
                  className="absolute top-2 right-2 p-1.5 bg-slate-900/70 hover:bg-slate-900 text-white rounded-full transition"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <div
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-200 hover:border-slate-400 rounded-2xl p-6 flex flex-col items-center justify-center cursor-pointer bg-slate-50 hover:bg-slate-100/60 transition group text-center"
              >
                <div className="w-12 h-12 rounded-full bg-slate-200/80 flex items-center justify-center text-slate-600 mb-2 group-hover:scale-105 transition">
                  <Camera className="w-6 h-6" />
                </div>
                <span className="text-sm font-medium text-slate-700">Prendre ou choisir une photo</span>
                <span className="text-xs text-slate-400 mt-0.5">Compression auto &lt; 300 Ko</span>
              </div>
            )}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/*"
              capture="environment"
              onChange={handlePhotoSelect}
              className="hidden"
            />
          </div>

          {/* Titre optionnel */}
          <div>
            <label className="block text-xs font-medium text-slate-600 uppercase tracking-wider mb-1.5">
              Nom ou description brève
            </label>
            <input
              type="text"
              placeholder="Ex: Banc ombragé face à la rivière"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-700 focus:bg-white transition"
            />
          </div>

          {/* Système de notation tactile sobre (0 à 10) */}
          <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-medium text-slate-600 uppercase tracking-wider">
                Note de confort & vue
              </span>
              <div className="flex items-center gap-1 font-bold text-base text-slate-800">
                <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                <span>{rating}</span>
                <span className="text-xs font-normal text-slate-400">/10</span>
              </div>
            </div>
            {/* Slider épuré */}
            <input
              type="range"
              min="0"
              max="10"
              step="1"
              value={rating}
              onChange={(e) => setRating(Number(e.target.value))}
              className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-slate-800"
            />
            <div className="flex justify-between text-[11px] text-slate-400 mt-1">
              <span>0 (Inconfortable)</span>
              <span>5 (Moyen)</span>
              <span>10 (Exceptionnel)</span>
            </div>
          </div>

          {/* Commentaire / Ambiance */}
          <div>
            <label className="block text-xs font-medium text-slate-600 uppercase tracking-wider mb-1.5">
              Avis, ambiance, panorama
            </label>
            <textarea
              rows={3}
              placeholder="Calme, belle exposition au coucher de soleil, dossier confortable..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-slate-700 focus:bg-white transition resize-none"
            />
          </div>

          {/* Bouton de soumission */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 px-4 bg-slate-800 hover:bg-slate-900 disabled:bg-slate-400 text-white rounded-xl font-medium shadow-md flex items-center justify-center gap-2 transition active:scale-[0.99]"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
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
