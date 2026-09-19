import React, { useState, useEffect } from 'react';
import { Plus, Navigation, User, LogOut, Compass, MapPin } from 'lucide-react';
import { BenchMap } from './components/BenchMap';
import { AddBenchModal } from './components/AddBenchModal';
import { BenchDetailsSheet } from './components/BenchDetailsSheet';
import { AuthModal } from './components/AuthModal';
import { Bench, UserLocation } from './types';
import { supabase, isSupabaseConfigured } from './lib/supabase';

// Données de démonstration réalistes pour test immédiat hors-ligne ou avant configuration Supabase
const INITIAL_DEMO_BENCHES: Bench[] = [
  {
    id: 'demo-1',
    created_at: new Date().toISOString(),
    title: 'Banc face au coucher de soleil sur la Seine',
    latitude: 48.8575,
    longitude: 2.3514,
    avg_rating: 9.2,
    review_count: 14,
    cover_photo_url: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'demo-2',
    created_at: new Date().toISOString(),
    title: 'Spot ombragé sous les platanes du canal',
    latitude: 48.8682,
    longitude: 2.3644,
    avg_rating: 8.5,
    review_count: 8,
    cover_photo_url: 'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'demo-3',
    created_at: new Date().toISOString(),
    title: 'Banc en fer forgé du square caché',
    latitude: 48.8530,
    longitude: 2.3499,
    avg_rating: 7.8,
    review_count: 5,
    cover_photo_url: 'https://images.unsplash.com/photo-1519671482749-fd09be7ccebf?auto=format&fit=crop&w=800&q=80',
  },
];

export const App: React.FC = () => {
  const [benches, setBenches] = useState<Bench[]>(INITIAL_DEMO_BENCHES);
  const [selectedBench, setSelectedBench] = useState<Bench | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [user, setUser] = useState<any>(null);
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
  const [mapCenter, setMapCenter] = useState<[number, number] | undefined>(undefined);
  const [isLocating, setIsLocating] = useState(false);

  // 1. Initialisation géolocalisation GPS
  useEffect(() => {
    getUserGeolocation();
  }, []);

  // 2. Gestion de la session utilisateur Supabase
  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setUser(session?.user ?? null);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setUser(session?.user ?? null);
    });

    return () => subscription.unsubscribe();
  }, []);

  // 3. Chargement des bancs
  useEffect(() => {
    fetchBenches();
  }, []);

  const getUserGeolocation = () => {
    if ('geolocation' in navigator) {
      setIsLocating(true);
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const loc = {
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          };
          setUserLocation(loc);
          setMapCenter([loc.lat, loc.lng]);
          setIsLocating(false);
        },
        (error) => {
          console.warn('Géolocalisation refusée ou non disponible:', error);
          setIsLocating(false);
          // Position de repli centrée
          setUserLocation({ lat: 48.8566, lng: 2.3522 });
        },
        { enableHighAccuracy: true, timeout: 10000 }
      );
    }
  };

  const fetchBenches = async () => {
    try {
      if (isSupabaseConfigured) {
        const { data, error } = await supabase
          .from('benches_with_stats')
          .select('*');

        if (!error && data && data.length > 0) {
          setBenches(data);
          return;
        }
      }
      // Conserver ou actualiser les bancs avec les données de démo si base vide
      setBenches(INITIAL_DEMO_BENCHES);
    } catch (err) {
      console.warn('Utilisation des données locales de démo:', err);
      setBenches(INITIAL_DEMO_BENCHES);
    }
  };

  const handleRecenter = () => {
    if (userLocation) {
      setMapCenter([userLocation.lat, userLocation.lng]);
    } else {
      getUserGeolocation();
    }
  };

  const handleSignOut = async () => {
    await supabase.auth.signOut();
    setUser(null);
  };

  return (
    <div className="relative w-screen h-[100dvh] overflow-hidden bg-slate-100 font-sans">
      {/* 1. Carte plein écran */}
      <BenchMap
        benches={benches}
        userLocation={userLocation}
        selectedBench={selectedBench}
        onSelectBench={(bench) => setSelectedBench(bench)}
        centerCoords={mapCenter}
      />

      {/* 2. Barre supérieure minimaliste */}
      <div className="absolute top-4 left-4 right-4 z-20 flex items-center justify-between pointer-events-none">
        {/* Logo / Titre sobre */}
        <div className="pointer-events-auto bg-slate-900/80 backdrop-blur-md text-white px-3.5 py-1.5 rounded-full shadow-lg border border-white/10 flex items-center gap-2">
          <Compass className="w-4 h-4 text-slate-300" />
          <span className="text-xs font-semibold tracking-wide">Rate Bench</span>
        </div>

        {/* Bouton profil / connexion */}
        <div className="pointer-events-auto">
          {user ? (
            <div className="flex items-center gap-1.5 bg-white/90 backdrop-blur-md px-3 py-1.5 rounded-full shadow-md border border-slate-200 text-slate-800 text-xs">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              <span className="max-w-[100px] truncate">{user.email?.split('@')[0]}</span>
              <button
                onClick={handleSignOut}
                title="Se déconnecter"
                className="ml-1 p-1 hover:bg-slate-100 rounded-full text-slate-500 hover:text-red-500 transition"
              >
                <LogOut className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setIsAuthModalOpen(true)}
              className="bg-white/95 hover:bg-white backdrop-blur-md text-slate-800 text-xs font-medium px-3.5 py-2 rounded-full shadow-md border border-slate-200/80 flex items-center gap-1.5 transition active:scale-95"
            >
              <User className="w-3.5 h-3.5 text-slate-600" />
              Connexion
            </button>
          )}
        </div>
      </div>

      {/* 3. Bouton flottant GPS (Recentrage) en bas à droite */}
      <div className="absolute bottom-24 right-4 z-20">
        <button
          onClick={handleRecenter}
          title="Ma position"
          className="w-12 h-12 bg-white/95 hover:bg-white active:bg-slate-50 backdrop-blur-md text-slate-800 rounded-full shadow-lg border border-slate-200/80 flex items-center justify-center transition active:scale-95 group"
        >
          <Navigation className={`w-5 h-5 text-slate-700 transition ${isLocating ? 'animate-spin' : 'group-hover:rotate-45'}`} />
        </button>
      </div>

      {/* 4. Bouton principal d'action : "+ Ajouter un spot" (Centré en bas) */}
      <div className="absolute bottom-6 left-0 right-0 z-20 flex justify-center px-4 pointer-events-none">
        <button
          onClick={() => {
            if (!user && isSupabaseConfigured) {
              setIsAuthModalOpen(true);
            } else {
              setIsAddModalOpen(true);
            }
          }}
          className="pointer-events-auto bg-slate-900 hover:bg-slate-800 active:bg-black text-white px-6 py-3.5 rounded-full shadow-xl flex items-center gap-2.5 transition transform active:scale-95 border border-white/10"
        >
          <div className="w-6 h-6 rounded-full bg-white/15 flex items-center justify-center">
            <Plus className="w-4 h-4 text-white" />
          </div>
          <span className="text-sm font-semibold tracking-tight">Ajouter un spot</span>
        </button>
      </div>

      {/* 5. Modale / Drawer d'ajout de banc */}
      <AddBenchModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        userLocation={userLocation}
        onBenchAdded={() => fetchBenches()}
        userId={user?.id}
      />

      {/* 6. Fiche détaillée du banc sélectionné */}
      <BenchDetailsSheet
        bench={selectedBench}
        onClose={() => setSelectedBench(null)}
        userLocation={userLocation}
        onReviewAdded={() => fetchBenches()}
        userId={user?.id}
      />

      {/* 7. Modale d'authentification */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onAuthSuccess={(u) => setUser(u)}
      />
    </div>
  );
};

export default App;
