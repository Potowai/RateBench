-- ============================================================
-- RateBench — 002 : publication anonyme (pseudo, sans compte)
-- Les visiteurs non connectés peuvent publier bancs et avis en
-- indiquant un pseudo. author_id / user_id restent NULL.
-- (L'upload d'images reste réservé aux comptes : voir Storage.)
-- Exécuter dans l'éditeur SQL APRES 001_schema.sql.
-- ============================================================

-- Bancs : insertion anonyme sans auteur lié
drop policy if exists "Création de banc anonyme" on public.benches;
create policy "Création de banc anonyme"
  on public.benches for insert to anon
  with check (author_id is null);

-- Avis : insertion anonyme sans auteur lié
drop policy if exists "Création d'avis anonyme" on public.reviews;
create policy "Création d'avis anonyme"
  on public.reviews for insert to anon
  with check (user_id is null);
