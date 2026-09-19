-- ============================================================
-- RateBench — Schéma backend partagé (Supabase / Postgres)
-- Tables : profiles, benches, reviews
-- Stockage : buckets bench-photos, review-photos (publics en lecture)
-- Sécurité : RLS lecture publique, écriture réservée aux connectés
-- Exécuter UNE FOIS dans l'éditeur SQL du projet Supabase.
-- ============================================================

-- 0. Extension pour gen_random_uuid()
create extension if not exists "pgcrypto";

-- 1. Profils publics (1 ligne par utilisateur auth)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  avatar_url text,
  created_at timestamptz not null default now()
);

-- 2. Bancs / spots
create table if not exists public.benches (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text not null default '',
  latitude double precision not null,
  longitude double precision not null,
  author_id uuid references public.profiles(id) on delete set null,
  author_name text not null default 'Communauté',
  photo_url text not null default '',
  rating_avg numeric not null default 0,
  review_count integer not null default 0,
  created_at timestamptz not null default now()
);
create index if not exists benches_geo_idx on public.benches (latitude, longitude);
create index if not exists benches_created_idx on public.benches (created_at desc);

-- 3. Avis
create table if not exists public.reviews (
  id uuid primary key default gen_random_uuid(),
  bench_id uuid not null references public.benches(id) on delete cascade,
  user_id uuid references public.profiles(id) on delete set null,
  user_name text not null default 'Visiteur',
  rating integer not null check (rating between 1 and 10),
  comment text not null default '',
  photo_url text,
  created_at timestamptz not null default now()
);
create index if not exists reviews_bench_idx on public.reviews (bench_id, created_at desc);

-- 4. Recalcul automatique note moyenne + compteur d'un banc
create or replace function public.refresh_bench_stats()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.benches b
  set
    review_count = (select count(*) from public.reviews r where r.bench_id = coalesce(new.bench_id, old.bench_id)),
    rating_avg = coalesce(
      (select round(avg(r.rating)::numeric, 1) from public.reviews r where r.bench_id = coalesce(new.bench_id, old.bench_id)),
      0
    )
  where b.id = coalesce(new.bench_id, old.bench_id);
  return coalesce(new, old);
end;
$$;

drop trigger if exists trg_reviews_refresh_stats on public.reviews;
create trigger trg_reviews_refresh_stats
  after insert or update of bench_id, rating or delete on public.reviews
  for each row execute function public.refresh_bench_stats();

-- 5. Création auto du profil à l'inscription
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, split_part(new.email, '@', 1))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists trg_auth_user_created on auth.users;
create trigger trg_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 6. Row Level Security
alter table public.profiles enable row level security;
alter table public.benches enable row level security;
alter table public.reviews enable row level security;

-- Profils : lecture publique, chacun gère le sien
drop policy if exists "Lecture publique des profils" on public.profiles;
create policy "Lecture publique des profils"
  on public.profiles for select using (true);
drop policy if exists "Création de son profil" on public.profiles;
create policy "Création de son profil"
  on public.profiles for insert to authenticated with check (auth.uid() = id);
drop policy if exists "MAJ de son profil" on public.profiles;
create policy "MAJ de son profil"
  on public.profiles for update to authenticated
  using (auth.uid() = id) with check (auth.uid() = id);

-- Bancs : lecture publique, création connectée, modif/suppression par l'auteur
drop policy if exists "Lecture publique des bancs" on public.benches;
create policy "Lecture publique des bancs"
  on public.benches for select using (true);
drop policy if exists "Création de banc connecté" on public.benches;
create policy "Création de banc connecté"
  on public.benches for insert to authenticated with check (auth.uid() = author_id);
drop policy if exists "Modif de son banc" on public.benches;
create policy "Modif de son banc"
  on public.benches for update to authenticated
  using (auth.uid() = author_id) with check (auth.uid() = author_id);
drop policy if exists "Suppression de son banc" on public.benches;
create policy "Suppression de son banc"
  on public.benches for delete to authenticated using (auth.uid() = author_id);

-- Avis : lecture publique, création connectée, modif/suppression par l'auteur
drop policy if exists "Lecture publique des avis" on public.reviews;
create policy "Lecture publique des avis"
  on public.reviews for select using (true);
drop policy if exists "Création d'avis connecté" on public.reviews;
create policy "Création d'avis connecté"
  on public.reviews for insert to authenticated with check (auth.uid() = user_id);
drop policy if exists "Modif de son avis" on public.reviews;
create policy "Modif de son avis"
  on public.reviews for update to authenticated
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "Suppression de son avis" on public.reviews;
create policy "Suppression de son avis"
  on public.reviews for delete to authenticated using (auth.uid() = user_id);

-- 7. Stockage images (buckets publics en lecture)
insert into storage.buckets (id, name, public)
values ('bench-photos', 'bench-photos', true),
       ('review-photos', 'review-photos', true)
on conflict (id) do nothing;

drop policy if exists "Lecture publique des photos" on storage.objects;
create policy "Lecture publique des photos"
  on storage.objects for select
  using (bucket_id in ('bench-photos', 'review-photos'));
drop policy if exists "Upload connecté des photos" on storage.objects;
create policy "Upload connecté des photos"
  on storage.objects for insert to authenticated
  with check (bucket_id in ('bench-photos', 'review-photos'));
drop policy if exists "Gestion de ses photos" on storage.objects;
create policy "Gestion de ses photos"
  on storage.objects for update to authenticated
  using (bucket_id in ('bench-photos', 'review-photos') and auth.uid() = owner)
  with check (bucket_id in ('bench-photos', 'review-photos') and auth.uid() = owner);
drop policy if exists "Suppression de ses photos" on storage.objects;
create policy "Suppression de ses photos"
  on storage.objects for delete to authenticated
  using (bucket_id in ('bench-photos', 'review-photos') and auth.uid() = owner);

-- 8. Données initiales (seed parisien, rejouable sans doublon)
insert into public.benches (id, title, description, latitude, longitude, author_name, photo_url)
values
  ('10000000-0000-4000-8000-000000000001', 'Banc face au coucher de soleil sur la Seine',
   'Vue imprenable sur l''eau et les péniches. Parfait au crépuscule, bois verni très bien entretenu.',
   48.8575, 2.3514, 'Communauté',
   'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80'),
  ('10000000-0000-4000-8000-000000000002', 'Spot ombragé sous les platanes du canal',
   'Banc double en pierre et fonte. Fraîcheur naturelle appréciable en plein été pour lire.',
   48.8682, 2.3644, 'Communauté',
   'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=800&q=80'),
  ('10000000-0000-4000-8000-000000000003', 'Belvédère de Montmartre (vue panoramique)',
   'En retrait de la foule, banc en bois patiné avec horizon dégagé sur les toits parisiens.',
   48.8867, 2.3431, 'Communauté',
   'https://images.unsplash.com/photo-1508050919630-b135583b3ae8?auto=format&fit=crop&w=800&q=80'),
  ('10000000-0000-4000-8000-000000000004', 'Havre de paix dans la cour du cloître',
   'Silencieux, entouré de rosiers et de vieilles pierres. Idéal pour le télétravail nomade ou la méditation.',
   48.8520, 2.3580, 'Communauté',
   'https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=800&q=80'),
  ('10000000-0000-4000-8000-000000000005', 'Banc royal des jardins de Versailles',
   'À ~18 km du centre. Banc en marbre et bois sculpté offrant une perspective grandiose sur le Grand Canal.',
   48.8048, 2.1203, 'Communauté',
   'https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=800&q=80'),
  ('10000000-0000-4000-8000-000000000006', 'Banc des rochers de Fontainebleau',
   'À ~55 km de Paris. Banc taillé dans le grès au sommet d''une gorge sablonneuse.',
   48.4047, 2.7016, 'Communauté',
   'https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=800&q=80'),
  ('10000000-0000-4000-8000-000000000007', 'Banc des hauteurs de Fourvière (Lyon)',
   'Banc situé à Lyon (~390 km de Paris), filtré lors du refresh dans le rayon de 100 km.',
   45.7624, 4.8223, 'Communauté',
   'https://images.unsplash.com/photo-1524397030763-9a9163e79391?auto=format&fit=crop&w=800&q=80')
on conflict (id) do nothing;

insert into public.reviews (bench_id, user_name, rating, comment, photo_url, created_at)
select * from (values
  ('10000000-0000-4000-8000-000000000001'::uuid, 'Camille D.', 10,
   'L''un des meilleurs spots de Paris pour décompresser. Regardez cette vue !',
   'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=600&q=80',
   now() - interval '2 days'),
  ('10000000-0000-4000-8000-000000000001'::uuid, 'Thomas R.', 9,
   'Très calme vers 19h, ombre bienvenue sous le saule.', null,
   now() - interval '7 days'),
  ('10000000-0000-4000-8000-000000000002'::uuid, 'Sophie M.', 9,
   'Idéal pour une pause lecture au frais au bord de l''eau.',
   'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=600&q=80',
   now() - interval '3 days'),
  ('10000000-0000-4000-8000-000000000002'::uuid, 'Julien K.', 8,
   'Quelques cyclistes mais cadre relaxant.', null,
   now() - interval '10 days'),
  ('10000000-0000-4000-8000-000000000003'::uuid, 'Alexandre V.', 10,
   'Mon banc secret préféré pour contempler le lever de soleil.',
   'https://images.unsplash.com/photo-1508050919630-b135583b3ae8?auto=format&fit=crop&w=600&q=80',
   now() - interval '1 day'),
  ('10000000-0000-4000-8000-000000000004'::uuid, 'Léa B.', 9,
   'Le chant des oiseaux en plein Paris, un pur bonheur.', null,
   now() - interval '5 days'),
  ('10000000-0000-4000-8000-000000000005'::uuid, 'Marc P.', 10,
   'Majestueux et paisible en fin d''après-midi.',
   'https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=600&q=80',
   now() - interval '7 days'),
  ('10000000-0000-4000-8000-000000000006'::uuid, 'Clara G.', 9,
   'Parfait après une randonnée sur le circuit des 25 bosses !', null,
   now() - interval '14 days'),
  ('10000000-0000-4000-8000-000000000007'::uuid, 'Bastien L.', 10,
   'Magnifique panorama sur le Rhône.', null,
   now() - interval '30 days')
) as seed(bench_id, user_name, rating, comment, photo_url, created_at)
where not exists (
  select 1 from public.reviews r
  where r.bench_id = seed.bench_id and r.user_name = seed.user_name and r.comment = seed.comment
);
