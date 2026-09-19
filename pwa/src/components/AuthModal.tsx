import React, { useState } from 'react';
import { X, Mail, Lock, Loader2, Sparkles } from 'lucide-react';
import { supabase } from '../lib/supabase';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAuthSuccess: (user: any) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ isOpen, onClose, onAuthSuccess }) => {
  const [isSignUp, setIsSignUp] = useState(false);
  const [isMagicLink, setIsMagicLink] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);

    try {
      if (isMagicLink) {
        const { error: magicError } = await supabase.auth.signInWithOtp({
          email,
          options: {
            emailRedirectTo: window.location.origin,
          },
        });
        if (magicError) throw magicError;
        setMessage('Un lien magique de connexion vous a été envoyé par email !');
      } else if (isSignUp) {
        const { data, error: signUpError } = await supabase.auth.signUp({
          email,
          password,
        });
        if (signUpError) throw signUpError;
        if (data.user) {
          onAuthSuccess(data.user);
          onClose();
        } else {
          setMessage('Veuillez vérifier vos emails pour valider votre compte.');
        }
      } else {
        const { data, error: signInError } = await supabase.auth.signInWithPassword({
          email,
          password,
        });
        if (signInError) throw signInError;
        if (data.user) {
          onAuthSuccess(data.user);
          onClose();
        }
      }
    } catch (err: any) {
      setError(err.message || 'Une erreur est survenue.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl p-6 shadow-2xl animate-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-900">
              {isMagicLink ? 'Connexion sans mot de passe' : isSignUp ? 'Rejoindre Rate Bench' : 'Connexion'}
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Partagez et notez les plus beaux bancs
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-xl text-xs mb-3 border border-red-100">
            {error}
          </div>
        )}

        {message && (
          <div className="p-3 bg-emerald-50 text-emerald-700 rounded-xl text-xs mb-3 border border-emerald-100">
            {message}
          </div>
        )}

        <form onSubmit={handleAuth} className="space-y-3">
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 uppercase mb-1">Email</label>
            <div className="relative">
              <Mail className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="email"
                required
                placeholder="votre@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-700 outline-none"
              />
            </div>
          </div>

          {!isMagicLink && (
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 uppercase mb-1">Mot de passe</label>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-slate-700 outline-none"
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-semibold shadow flex items-center justify-center gap-2 mt-2 transition"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : isMagicLink ? (
              'Envoyer le lien magique'
            ) : isSignUp ? (
              "Créer mon compte"
            ) : (
              'Se connecter'
            )}
          </button>
        </form>

        <div className="mt-4 pt-4 border-t border-slate-100 flex flex-col gap-2 text-center text-xs">
          <button
            type="button"
            onClick={() => setIsMagicLink(!isMagicLink)}
            className="text-slate-600 hover:text-slate-900 flex items-center justify-center gap-1.5"
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-500" />
            {isMagicLink ? 'Utiliser mot de passe' : 'Connexion rapide par Magic Link'}
          </button>

          {!isMagicLink && (
            <button
              type="button"
              onClick={() => setIsSignUp(!isSignUp)}
              className="text-slate-500 hover:text-slate-800 underline underline-offset-2"
            >
              {isSignUp ? 'Déjà un compte ? Se connecter' : "Pas encore de compte ? S'inscrire"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
