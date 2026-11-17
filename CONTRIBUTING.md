# Contribuer à RateBench

Merci de votre intérêt ! Toutes les contributions sont appréciées : correction de bugs, nouvelles fonctionnalités, documentation, traductions, design ou tests.

## Code de conduite

En participant, vous acceptez de respecter notre [Code de conduite](CODE_OF_CONDUCT.md).

## Signaler un problème

- Utilisez les [issues GitHub](https://github.com/Potowai/RateBench/issues) et le modèle « Bug report ».
- Décrivez le comportement attendu / observé, la plateforme (Android / PWA / backend), la version et les étapes de reproduction.
- Pour une vulnérabilité de sécurité, suivez [SECURITY.md](SECURITY.md) **au lieu** d'ouvrir une issue publique.

## Mettre en place l'environnement

```bash
git clone https://github.com/Potowai/RateBench.git
cd RateBench

# PWA
cd pwa && npm install && npm run dev

# Android (JDK 17 + SDK Android 36)
cd .. && ./gradlew testDebugUnitTest
```

Aucune clé n'est nécessaire pour démarrer : l'application fonctionne en mode démonstration. Pour tester le mode cloud, copiez les `.env.example` et renseignez vos propres clés (ne les commitez jamais).

## Workflow

1. Créez une branche depuis `main` : `git checkout -b feat/ma-fonctionnalite` ou `fix/mon-bug`.
2. Faites des commits atomiques avec des messages clairs en français ou en anglais
   (ex. `feat(map): ajoute un filtre par note`, `fix(auth): corrige la déconnexion`).
3. Vérifiez avant d'ouvrir une PR :
   - `cd pwa && npm test` (tests unitaires PWA)
   - `cd pwa && npm run build` (TypeScript + Vite doivent passer)
   - `./gradlew testDebugUnitTest` (tests JVM)
4. Ouvrez une PR en décrivant **ce qui change**, **pourquoi**, et comment vous l'avez testé.
5. La CI (GitHub Actions) lance les tests et un build debug ; une PR n'est fusionnée que si tout est vert.

## Conventions

- **Langue** : le code, les commentaires et la documentation existants sont majoritairement en français. Gardez cette cohérence, ou utilisez l'anglais si vous touchez une zone déjà en anglais.
- **PWA** : TypeScript strict, composants fonctionnels, Tailwind pour le style, pas de dépendance inutile.
- **Android** : Kotlin + Jetpack Compose, architectures existantes (`data/`, `model/`, `ui/`), pas de logique métier dans les composables.
- **SQL** : migrations idempotentes (`if not exists`, `drop policy if exists`), RLS activée sur toute nouvelle table.
- **Sécurité** : aucune clé ou donnée personnelle dans le dépôt ; uniquement des `.env.example` avec des valeurs fictives.

## Idées de contributions

- Améliorer l'accessibilité (libellés, navigation clavier, contraste) de la PWA et de l'app Android.
- Ajouter des filtres / tris sur la carte (note, distance, photos).
- Écrire des tests sur les repositories et les composants.
- Améliorer le support hors ligne (cache des bancs et des photos).
- Traduire l'interface en d'autres langues.
- Enrichir la documentation et les captures d'écran.

## Licence

En contribuant, vous acceptez que votre contribution soit publiée sous licence [MIT](LICENSE).
