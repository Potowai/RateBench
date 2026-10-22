# Rate Bench

> *« Il faut rater le bench, c'est bien. »*

Rate Bench est une application mobile-first pour **répertorier, noter et photographier
les bancs publics et les spots de repos remarquables** — une carte collaborative des
endroits où s'asseoir.

L'application est composée de deux clients qui partagent la même base Supabase :

- **Application Android** (Kotlin) — `app/`
- **PWA web** (Vite + TypeScript + Tailwind) — `pwa/`

## Fonctionnalités

- Catalogue communautaire de bancs et de spots de repos
- Notation et photographies
- Backend Supabase (schéma dans `supabase/`)
- Métadonnées de l'application Android dans `metadata.json`

## Stack

| Client | Technologies |
| :--- | :--- |
| Android | Kotlin, Gradle, Jetpack |
| PWA | Vite, TypeScript, Tailwind CSS |
| Données | Supabase |

## Démarrage

### PWA

```bash
cd pwa
npm install
npm run dev
```

### Android

```bash
cd app
./gradlew assembleDebug
```

Les variables sensibles sont gérées via un `.env` — le dépôt ne contient qu'un
`.env.example` à copier.

## Licence

MIT
