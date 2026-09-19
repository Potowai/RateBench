import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet';
import L from 'leaflet';
import { Bench, UserLocation } from '../types';

interface BenchMapProps {
  benches: Bench[];
  userLocation: UserLocation | null;
  selectedBench: Bench | null;
  onSelectBench: (bench: Bench) => void;
  centerCoords?: [number, number];
}

// Contrôleur pour animer et recentrer la carte
function MapRecenterController({ center }: { center?: [number, number] }) {
  const map = useMap();
  useEffect(() => {
    if (center) {
      map.flyTo(center, Math.max(map.getZoom(), 15), { duration: 0.8 });
    }
  }, [center, map]);
  return null;
}

// Générateur d'icône personnalisée sobre pour les bancs (pastille ardoise + note)
function createBenchIcon(rating?: number, isSelected = false) {
  const displayRating = rating && rating > 0 ? rating.toFixed(1) : '—';
  
  // Palette ardoise avec accentuation discrète selon la note
  const bgClass = isSelected
    ? 'bg-slate-900 border-white text-white ring-2 ring-slate-800 ring-offset-2'
    : 'bg-slate-800/95 border-white/90 text-white hover:bg-slate-900';

  const html = `
    <div class="relative flex items-center justify-center transform -translate-x-1/2 -translate-y-1/2 transition-all duration-200">
      <div class="flex items-center gap-1 px-2.5 py-1 rounded-full shadow-md border ${bgClass} backdrop-blur-sm">
        <svg class="w-3 h-3 fill-amber-300 text-amber-300 inline-block" viewBox="0 0 24 24">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
        <span class="text-xs font-semibold tracking-tight">${displayRating}</span>
      </div>
      <div class="absolute -bottom-1 w-2 h-2 bg-slate-800 rotate-45 border-r border-b border-white/80"></div>
    </div>
  `;

  return L.divIcon({
    className: 'custom-bench-pin',
    html,
    iconSize: [46, 32],
    iconAnchor: [23, 28],
  });
}

// Icône sobre pour la position GPS de l'utilisateur
function createUserLocationIcon() {
  const html = `
    <div class="relative flex items-center justify-center">
      <div class="w-4 h-4 rounded-full bg-slate-700 border-2 border-white shadow-md z-10"></div>
      <div class="absolute w-8 h-8 rounded-full bg-slate-400/30 animate-ping"></div>
    </div>
  `;
  return L.divIcon({
    className: 'user-loc-pin',
    html,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
}

export const BenchMap: React.FC<BenchMapProps> = ({
  benches,
  userLocation,
  selectedBench,
  onSelectBench,
  centerCoords,
}) => {
  const defaultCenter: [number, number] = userLocation
    ? [userLocation.lat, userLocation.lng]
    : [48.8566, 2.3522]; // Paris par défaut

  return (
    <div className="w-full h-full relative z-0">
      <MapContainer
        center={defaultCenter}
        zoom={15}
        zoomControl={false}
        className="w-full h-full"
        attributionControl={false}
      >
        {/* Tuiles cartographiques propres et sobres (CartoDB Positron / OSM style) */}
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          maxZoom={19}
        />

        {/* Contrôleur de recentrage */}
        <MapRecenterController center={centerCoords} />

        {/* Marqueur position utilisateur */}
        {userLocation && (
          <Marker
            position={[userLocation.lat, userLocation.lng]}
            icon={createUserLocationIcon()}
          />
        )}

        {/* Marqueurs des bancs */}
        {benches.map((bench) => {
          const isSelected = selectedBench?.id === bench.id;
          return (
            <Marker
              key={bench.id}
              position={[bench.latitude, bench.longitude]}
              icon={createBenchIcon(bench.avg_rating, isSelected)}
              eventHandlers={{
                click: () => onSelectBench(bench),
              }}
            />
          );
        })}
      </MapContainer>
    </div>
  );
};
