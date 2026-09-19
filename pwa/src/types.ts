export interface Bench {
  id: string;
  created_at: string;
  created_by?: string | null;
  title?: string | null;
  latitude: number;
  longitude: number;
  avg_rating?: number;
  review_count?: number;
  cover_photo_url?: string | null;
  distance_meters?: number | null;
}

export interface Review {
  id: string;
  bench_id: string;
  user_id?: string | null;
  user_email?: string | null;
  rating: number; // 0 to 10
  comment?: string | null;
  photo_url?: string | null;
  created_at: string;
}

export interface NewBenchData {
  title?: string;
  latitude: number;
  longitude: number;
  rating: number;
  comment?: string;
  photoFile?: File | null;
}

export interface UserLocation {
  lat: number;
  lng: number;
}
