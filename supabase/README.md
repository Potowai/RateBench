# Base partagée RateBench (Supabase)

Ce dossier contient tout le backend mutualisé : tous les utilisateurs de
l'appli partagent les mêmes bancs, avis et photos.

## 1. Créer le projet (2 min, gratuit)

1. Aller sur https://supabase.com → **New project**
2. Nom : `ratebench` — mot de passe DB : à noter précieusement
3. Région la plus proche (ex. `West EU (Ireland)`)
4. Attendre la fin du provisionnement (~1 min)

## 2. Appliquer le schéma (tables + RLS + stockage + seed)

1. Dans le projet : **SQL Editor → New query**
2. Copier-coller tout le contenu de `migrations/001_schema.sql`
3. **Run** → succès = 7 bancs + 9 avis insérés, buckets créés
4. Puis copier-coller `migrations/002_anonymous.sql` → **Run**
   (publication anonyme avec pseudo, sans compte)

Vérification : **Table Editor** → `benches` (7 lignes), `reviews` (9 lignes) ;
**Storage** → buckets `bench-photos` et `review-photos`.

## 3. Récupérer les clés et configurer l'appli

1. **Project Settings → API** : copier `Project URL` et la clé `anon public`
2. À la racine du repo, créer un fichier `.env` (jamais commité) :
   ```
   SUPABASE_URL=https://xyzcompany.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOi...
   ```
3. Rebuilder l'appli : le mode cloud s'active automatiquement.
   Sans ces clés, l'appli fonctionne en **mode local** (données simulées).

## 4. Authentification

- **Lecture** (bancs, avis, photos) : publique, sans compte
- **Écriture** (publier un banc, un avis, une photo) : compte email + mot de
  passe via le bouton profil de l'appli (inscription automatique au 1er login)
- Les utilisateurs ne modifient/suppriment que leurs propres contenus (RLS)

## 5. Fichiers

- `migrations/001_schema.sql` — schéma complet rejouable (seed idempotent)
- Côté appli : `app/src/main/java/com/example/data/supabase/`
  (`SupabaseConfig`, `SupabaseApi`, `SupabaseAuthRepository`,
  `SupabaseBenchRepository` avec repli local automatique)
