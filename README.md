# OptimPhone

Appli Android + script ADB pour nettoyer, désactiver et optimiser votre téléphone.

## Contenu

- **`app/`** : appli Android (Kotlin) avec :
  - liste des applications installées (utilisateur + système) avec boutons **Ouvrir / Désactiver / Désinstaller** ;
  - état du **stockage**, de la **RAM** et de la **batterie** ;
  - raccourcis vers les réglages (stockage, batterie, notifications, accessibilité) ;
  - **Assistant IA** : collez votre clé API (compatible Mistral/OpenAI) et l'appli analyse votre téléphone (stockage, RAM, batterie, applis) puis vous donne des recommandations personnalisées.
- **`scripts/debloat.sh`** : script ADB interactif pour désactiver le bloatware système (Samsung, Xiaomi, générique) sans root, réversible.

## Compiler l'APK

1. Installez [Android Studio](https://developer.android.com/studio) (ou le SDK Android + Gradle 8.2).
2. Ouvrez le dossier `optimphone` dans Android Studio.
3. `Build > Build Bundle(s)/APK(s) > Build APK(s)`.
4. Transférez l'APK sur le téléphone et installez-le (autorisez « installer des applis inconnues »).

Configuration IA dans l'appli : bouton **Assistant IA > Configurer la clé API**.
- URL de base : `https://api.mistral.ai/v1` (Mistral) ou `https://api.openai.com/v1` (OpenAI) — toute API compatible OpenAI fonctionne.
- Clé API : votre clé personnelle (stockée uniquement sur le téléphone).
- Modèle : ex. `mistral-small-latest`.

## Utiliser le script ADB (bloatware système)

1. Sur le téléphone : Paramètres > À propos > appuyez 7 fois sur « numéro de build » pour activer le mode développeur, puis activez **Débogage USB**.
2. Sur l'ordinateur, installez [platform-tools](https://developer.android.com/tools/releases/platform-tools) (contient `adb`), branchez le téléphone en USB et acceptez la fenêtre d'autorisation.
3. Puis :

```bash
chmod +x scripts/debloat.sh
./scripts/debloat.sh list    # voir le bloatware détecté
./scripts/debloat.sh menu    # mode interactif (désactivation réversible)
./scripts/debloat.sh disable com.samsung.android.bixby.agent   # un paquet précis
./scripts/debloat.sh restore com.samsung.android.bixby.agent   # annuler
```

Sous Windows sans WSL, les mêmes commandes fonctionnent directement avec `adb` :

```
adb shell pm disable-user --user 0 com.samsung.android.bixby.agent
adb shell cmd package install-existing com.samsung.android.bixby.agent
```

## Limites importantes

- Android ne permet pas à une appli normale de désinstaller les applis **système** : d'où le script ADB (aucun root requis, tout est réversible).
- La clé API est stockée dans les SharedPreferences locales de l'appli (non chiffrée) : n'utilisez pas une clé sensible sur un téléphone partagé.
- Vérifiez toujours les paquets avant désactivation : ne désactivez pas la messagerie, le téléphone, le clavier ou les services Google Play.
