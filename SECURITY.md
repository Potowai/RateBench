# Politique de sécurité

## Versions prises en charge

| Version | Prise en charge |
|---|---|
| `main` (dernière version) | ✅ Oui |
| Tags `v*` récents | ✅ Correctifs de sécurité |
| Anciennes versions | ❌ Non |

## Signaler une vulnérabilité

**Ne signalez jamais une vulnérabilité dans une issue publique.**

Utilisez le signalement privé de GitHub :
1. Ouvrez la page du dépôt → onglet **Security** → **Report a vulnerability** (« Signaler une vulnérabilité »).
2. Décrivez le problème, son impact, les étapes de reproduction et, si possible, un correctif suggéré.

À défaut, contactez les responsables du dépôt via les canaux privés de GitHub.

Merci de ne pas tester de vulnérabilités sur des services hébergés par des tiers sans autorisation.

## Ce qui est considéré comme une vulnérabilité

- Contournement des politiques RLS Supabase (accès aux données d'un autre utilisateur, écriture non autorisée)
- Injection SQL via les migrations ou les requêtes client
- Fuite de clés ou de jetons (`service_role`, clés API privées)
- XSS, téléchargement de fichiers arbitraires, abus du stockage de photos
- Problèmes d'authentification ou de session

## Ce qui n'est pas une vulnérabilité

- La clé `anon` de Supabase est **publique par conception** : la protection repose sur les politiques RLS côté serveur, pas sur le secret de cette clé.
- Les données de démonstration intégrées (bancs d'exemple, URLs Unsplash) sont publiques et sans risque.

## Délais de réponse

- Accusé de réception : sous 7 jours
- Évaluation et première réponse : sous 14 jours
- Correctif et publication : selon la gravité, dès que possible

Nous vous remercions de votre aide pour garder RateBench sûr pour tous.
