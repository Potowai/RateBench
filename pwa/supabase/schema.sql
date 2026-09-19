-- ==============================================================================
-- RATE BENCH - SCHÉMA DE BASE DE DONNÉES SUPABASE (PostgreSQL + PostGIS)
-- ==============================================================================

-- 1. Activation de l'extension PostGIS pour la gestion spatiale avancée
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Création de la table 'benches' (Bancs publics et spots de repos)
CREATE TABLE IF NOT EXISTS public.benches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    title VARCHAR(255),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geom GEOGRAPHY(Point, 4326) GENERATED ALWAYS AS (
        ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
    ) STORED
);

-- Index spatial GiST pour des requêtes de distance ultra-rapides
CREATE INDEX IF NOT EXISTS benches_geom_idx ON public.benches USING GIST (geom);
CREATE INDEX IF NOT EXISTS benches_created_at_idx ON public.benches (created_at DESC);

-- 3. Création de la table 'reviews' (Avis, notes de 0 à 10 et photos)
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bench_id UUID NOT NULL REFERENCES public.benches(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    user_email VARCHAR(255),
    rating INTEGER NOT NULL CHECK (rating >= 0 AND rating <= 10),
    comment TEXT,
    photo_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Index pour accélérer les jointures et tris d'avis par banc
CREATE INDEX IF NOT EXISTS reviews_bench_id_idx ON public.reviews (bench_id);
CREATE INDEX IF NOT EXISTS reviews_created_at_idx ON public.reviews (created_at DESC);

-- 4. Vue pratique pour récupérer les bancs avec leur note moyenne et première photo
CREATE OR REPLACE VIEW public.benches_with_stats AS
SELECT 
    b.id,
    b.title,
    b.latitude,
    b.longitude,
    b.created_at,
    b.created_by,
    COALESCE(ROUND(AVG(r.rating)::numeric, 1), 0) AS avg_rating,
    COUNT(r.id)::integer AS review_count,
    (
        SELECT photo_url 
        FROM public.reviews r2 
        WHERE r2.bench_id = b.id AND r2.photo_url IS NOT NULL 
        ORDER BY r2.created_at DESC 
        LIMIT 1
    ) AS cover_photo_url
FROM public.benches b
LEFT JOIN public.reviews r ON r.bench_id = b.id
GROUP BY b.id;

-- 5. Activation de Row Level Security (RLS)
ALTER TABLE public.benches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

-- Politiques RLS pour 'benches'
-- Lecture publique pour tous (authentifiés ou visiteurs anonymes)
CREATE POLICY "Les bancs sont visibles par tout le monde"
    ON public.benches
    FOR SELECT
    USING (true);

-- Création réservée aux utilisateurs authentifiés
CREATE POLICY "Les utilisateurs connectés peuvent créer un banc"
    ON public.benches
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = created_by);

-- Modification/Suppression par le créateur uniquement
CREATE POLICY "Le créateur peut modifier son banc"
    ON public.benches
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = created_by);

-- Politiques RLS pour 'reviews'
-- Lecture publique de tous les avis
CREATE POLICY "Les avis sont consultables par tous"
    ON public.reviews
    FOR SELECT
    USING (true);

-- Création d'avis par les utilisateurs connectés
CREATE POLICY "Les utilisateurs connectés peuvent poster un avis"
    ON public.reviews
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

-- Modification de son propre avis
CREATE POLICY "Chacun peut modifier son avis"
    ON public.reviews
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id);

-- 6. Configuration du bucket Supabase Storage : 'bench-photos'
-- Insertion du bucket s'il n'existe pas encore
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'bench-photos',
    'bench-photos',
    true,
    5242880, -- 5 Mo max
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/heic']
)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Politiques de sécurité pour le Storage 'bench-photos'
-- Tout le monde peut voir les photos
CREATE POLICY "Photos publiques en lecture"
    ON storage.objects
    FOR SELECT
    USING (bucket_id = 'bench-photos');

-- Seuls les utilisateurs authentifiés peuvent téléverser des photos
CREATE POLICY "Téléversement réservé aux utilisateurs connectés"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = 'bench-photos');

-- 7. Fonction SQL pratique pour la recherche de bancs par proximité (PostGIS)
CREATE OR REPLACE FUNCTION public.get_nearby_benches(
    user_lat DOUBLE PRECISION,
    user_lng DOUBLE PRECISION,
    radius_meters DOUBLE PRECISION DEFAULT 10000
)
RETURNS TABLE (
    id UUID,
    title VARCHAR,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    avg_rating NUMERIC,
    review_count INTEGER,
    cover_photo_url TEXT,
    distance_meters DOUBLE PRECISION
)
LANGUAGE sql
STABLE
AS $$
    SELECT 
        b.id,
        b.title,
        b.latitude,
        b.longitude,
        COALESCE(ROUND(AVG(r.rating)::numeric, 1), 0) AS avg_rating,
        COUNT(r.id)::integer AS review_count,
        (
            SELECT photo_url 
            FROM public.reviews r2 
            WHERE r2.bench_id = b.id AND r2.photo_url IS NOT NULL 
            ORDER BY r2.created_at DESC 
            LIMIT 1
        ) AS cover_photo_url,
        ST_Distance(
            b.geom,
            ST_SetSRID(ST_MakePoint(user_lng, user_lat), 4326)::geography
        ) AS distance_meters
    FROM public.benches b
    LEFT JOIN public.reviews r ON r.bench_id = b.id
    WHERE ST_DWithin(
        b.geom,
        ST_SetSRID(ST_MakePoint(user_lng, user_lat), 4326)::geography,
        radius_meters
    )
    GROUP BY b.id
    ORDER BY distance_meters ASC;
$$;
