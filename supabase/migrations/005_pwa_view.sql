-- ============================================================
-- RateBench — Vue compatible PWA sur le schéma canonique
--
-- Le PWA (pwa/src/App.tsx) interroge `benches_with_stats`, une vue qui
-- n'existait que dans l'ancien schéma `pwa/supabase/schema.sql`. Ce fichier
-- la recrée sur les tables canoniques de 001_schema.sql afin que les deux
-- clients (Android et PWA) partagent UN SEUL backend.
--
-- Exécuter après 001_schema.sql (et après 002_anonymous.sql si utilisé).
-- Idempotent : peut être rejoué sans effet de bord.
-- ============================================================

create or replace view public.benches_with_stats as
select
  b.id,
  b.created_at,
  b.author_id              as created_by,
  b.title,
  b.description,
  b.latitude,
  b.longitude,
  b.rating_avg             as avg_rating,
  b.review_count,
  coalesce(
    nullif(b.photo_url, ''),
    (
      select r.photo_url
      from public.reviews r
      where r.bench_id = b.id
        and r.photo_url is not null
        and r.photo_url <> ''
      order by r.created_at desc
      limit 1
    )
  )                        as cover_photo_url,
  null::double precision   as distance_meters
from public.benches b;

-- La vue est en lecture seule par défaut ; l'accès en lecture reste public,
-- comme les tables sous-jacentes (RLS de 001_schema.sql).
comment on view public.benches_with_stats is
  'Vue canonique des bancs avec statistiques, utilisée par la PWA et l''application Android.';
