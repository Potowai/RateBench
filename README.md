# Rate Bench

> *« Il faut rater le bench, c'est bien. »*

> **English:** RateBench is a free and open-source, mobile-first app for finding, rating and photographing remarkable public benches and resting spots. Maps, community reviews, photos — Android app, installable PWA and a shared Supabase backend.

**RateBench** est une application libre et open-source pour repérer, noter et photographier les bancs publics et les spots de repos remarquables. Elle est pensée « mobile first », fonctionne hors ligne en mode démonstration, et se synchronise avec un backend Supabase partagé dès que les clés sont fournies.

Créé par **Alexis Fiolleau** et ouvert aux contributions de la communauté.

![Android](https://img.shields.io/badge/Android-Kotlin%20%2F%20Compose-3DDC84?style=flat-square)
![PWA](https://img.shields.io/badge/PWA-React%20%2F%20Vite-646CFF?style=flat-square)
![Backend](https://img.shields.io/badge/Backend-Supabase%20%2F%20Postgres-3FCF8E?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## Sommaire

- [Pourquoi ?](#pourquoi-)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Démo en ligne](#démo-en-ligne)
- [Démarrage rapide](#démarrage-rapide)
- [Configuration](#configuration)
- [Backend / base de données](#backend--base-de-données)
- [Contribuer](#contribuer)
- [Sécurité](#sécurité)
- [Feuille de route](#feuille-de-route)
- [Licence](#licence)

## Pourquoi ?

Un banc, c'est bien plus qu'un meuble urbain : c'est un endroit où l'on s'assoit, respire, regarde passer la vie. RateBench veut cartographier ces petits lieux — le banc face au coucher de soleil, le spot ombragé sous les platanes, le banc en fer forgé d'un square caché — et permettre à la communauté de les partager avec des notes et des photos.

Le projet est **open-source** : vous pouvez l'utiliser, l'étudier, le modifier et le redistribuer librement sous licence MIT.

## Fonctionnalités

- 🗺️ **Carte plein écran** avec position GPS et recentrage (Leaflet / OpenStreetMap)
- 📍 **Ajout de spots** : titre, description, position, note et **jusqu'à 5 photos** (compressées côté client)
- ⭐ **Avis notés de 1 à 10**, avec photo optionnelle et moyenne recalculée automatiquement
- 🔐 **Comptes Supabase** (inscription automatique au premier login) ; lecture publique et écriture avec compte
- 👤 **Pseudonyme anonyme** conservé localement pour les publications sans compte (application Android)
- 📴 **Mode démonstration** fonctionnel sans configuration (55 bancs réels intégrés)
- 📲 **PWA installable** (Android / iOS) et **application Android native** en Kotlin
- 🧪 **CI GitHub Actions** : tests unitaires JVM, build debug sur PR, release APK signé sur tag `v*`

## Architecture

| Composant | Stack | Emplacement |
|---|---|---|
| Application Android | Kotlin, Jetpack Compose, KSP, Coil | `app/` |
| PWA / web | React 18, TypeScript, Vite, Tailwind, Leaflet | `pwa/` |
| Backend partagé | Supabase (Postgres, Auth, Storage, RLS) | `supabase/` |
| CI / release | GitHub Actions | `.github/workflows/` |
| Documentation backend | Guide d'installation Supabase | `supabase/README.md` |

Les deux clients (Android et PWA) parlent au **même backend Supabase** : le schéma canonique de `supabase/migrations/` et sa vue `benches_with_stats` (migration 005). La lecture est publique (RLS), l'écriture avec compte est protégée, et chaque utilisateur ne modifie que ses propres contenus. L'application Android propose en plus une publication anonyme avec pseudonyme.

## Démo en ligne

Une instance de démonstration (mode local, sans backend configuré) tourne sur :
**https://ratebench.potowai.tech** — accessible depuis le tailnet.

Elle contient **55 bancs réels** répartis dans le monde : **France (25)**,
**Nouvelle-Zélande (15)** (Queenstown, Wellington, Christchurch, lac Tekapo, Rotorua)
et **reste du monde (15)** (Londres, Central Park, Kyoto, Vancouver, Sydney).

Sources des données (voir `pwa/src/data/benches.ts` et la migration `006`) :
- **Coordonnées réelles** : nœuds `amenity=bench` extraits d'**OpenStreetMap** via l'API officielle.
- **Photos réelles** : images de bancs sous licence libre issues de **Wikimedia Commons**.
- **Plusieurs photos par banc** : **3 photos réelles** par banc (galerie swipeable, visionneuse plein écran).
- **Avis** : **74 avis de démonstration**, **au moins un avis pour chaque banc**, avec note, auteur,
  commentaire et photo. Ajoutés localement en mode démo, persistés dans Supabase en mode cloud.
- **Notes et compteurs** : valeurs de démonstration (à remplacer par les vraies données
  de la communauté quand le backend Supabase sera connecté).

Elle sert le contenu de `pwa/dist` via Caddy. Pour la mettre à jour après un changement :

```bash
cd pwa && npm run build
```

Caddy sert le dossier statique en lecture seule ; aucun redémarrage du serveur web n'est nécessaire.

### Accès global (sans Tailscale)

Une seconde route est exposée publiquement via un **Cloudflare Quick Tunnel**
(service `cloudflared-ratebench.service`) : elle renvoie vers l'origine locale
`http://127.0.0.1:4097` servie par Caddy. Aucun port entrant n'est ouvert sur le serveur.

Récupérer l'URL publique actuelle :

```bash
journalctl --user -u cloudflared-ratebench.service --no-pager | grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -1
```

⚠️ Un *quick tunnel* reçoit un nom aléatoire qui **change à chaque redémarrage**
du service. Pour une URL stable (`ratebench.potowai.tech` en public), il faut un
tunnel nommé Cloudflare — ce qui nécessite un jeton API avec la permission
`Tunnel:Edit` ou un `cloudflared tunnel login` interactif. La configuration Caddy
et le DNS sont déjà prêts ; il ne manque que cette autorisation.

## Démarrage rapide

### PWA (web)

```bash
cd pwa
npm install
npm run dev
```

Ouvrez l'URL affichée par Vite. Sans configuration Supabase, l'application démarre en **mode démonstration** avec les bancs réels intégrés — aucune clé requise.

Pour activer le mode cloud, copiez `pwa/.env.example` vers `pwa/.env` et renseignez vos clés :

```bash
cp pwa/.env.example pwa/.env
# VITE_SUPABASE_URL=https://votre-projet.supabase.co
# VITE_SUPABASE_ANON_KEY=votre_cle_anon_publique
```

Puis `npm test` et `npm run build` pour générer la PWA dans `pwa/dist`.

### Application Android

Prérequis : **JDK 17**, Android SDK (plateforme 36, build-tools 36.0.0).

```bash
./gradlew testDebugUnitTest   # tests unitaires
./gradlew assembleDebug       # APK debug non signé
```

Pour un build de release, configurez les secrets GitHub `SUPABASE_URL` et `SUPABASE_ANON_KEY` (voir `.env.example`) et poussez un tag `v*` : le workflow `.github/workflows/release-apk.yml` construit et publie l'APK.

## Configuration

Les deux fichiers d'exemple sont versionnés et ne contiennent **aucune clé réelle** :

- `.env.example` — variables pour l'application Android (injectées via le plugin Secrets)
- `pwa/.env.example` — variables `VITE_*` pour la PWA

Ne committez jamais de fichier `.env` rempli : ils sont ignorés par Git. Utilisez les secrets GitHub Actions pour la CI.

> ℹ️ La clé `anon` de Supabase est **publique par conception** ; la sécurité repose sur les politiques RLS, pas sur le secret de cette clé. Ne placez jamais une clé `service_role` dans le client.

## Backend / base de données

Le schéma et les migrations sont dans `supabase/migrations/` :

| Fichier | Rôle |
|---|---|
| `001_schema.sql` | Tables `profiles`, `benches`, `reviews`, buckets Storage, RLS, triggers de statistiques |
| `002_anonymous.sql` | Publication anonyme avec pseudo (sans compte) |
| `003_seed_benches.sql` | Bancs initiaux (Paris) |
| `004_seed_world.sql` | Bancs complémentaires (monde) |
| `005_pwa_view.sql` | Vue `benches_with_stats` partagée par la PWA et l'Android |
| `006_seed_real_benches.sql` | 55 bancs réels (OSM + Wikimedia Commons), France / NZ / monde |
| `007_bench_photos_and_reviews.sql` | Table `bench_photos` (galerie multi-photos) + au moins un avis par banc |

Voir [`supabase/README.md`](supabase/README.md) pour la procédure complète de création du projet et d'application des migrations.

## Contribuer

Les contributions sont les bienvenues ! Consultez [`CONTRIBUTING.md`](CONTRIBUTING.md) pour le workflow, les conventions et la méthode de test. Merci de respecter le [`Code de conduite`](CODE_OF_CONDUCT.md).

## Sécurité

Si vous trouvez une vulnérabilité, **ne l'ouvrez pas publiquement** : suivez la procédure décrite dans [`SECURITY.md`](SECURITY.md).

## Feuille de route

Le projet est ouvert et les contributions sont bienvenues (voir [CONTRIBUTING.md](CONTRIBUTING.md)). Pistes prioritaires :

- Étendre les tests (repositories Android, composants PWA) — les tests actuels se concentrent sur la géolocalisation et le modèle JVM.
- Internationalisation de l'interface (anglais, puis autres langues).
- Synchronisation hors ligne plus complète (file d'attente des publications).
- Moderniser l'outillage de développement PWA (Vite/Vitest) : `npm audit` signale des avis de sécurité sur les dépendances **de développement** uniquement, qui nécessitent une montée de version majeure.
- Durcir encore le stockage des jetons Android (EncryptedSharedPreferences) et la modération des publications anonymes.

## Licence

Distribué sous licence [MIT](LICENSE). © 2026 Alexis Fiolleau et les contributeurs RateBench.

---

*Fait avec amour pour les bancs, les siestes au soleil et les villes plus accueillantes.*
