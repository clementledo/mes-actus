# Mes Actus

Application Android d'actualités : France (Orléans & métropole), Canada (Québec, île de Montréal), Espagne (Grenade & agglo).

## Installer sur le téléphone

1. Depuis le téléphone, téléchargez `MesActus.apk` dans la [dernière release](../../releases/latest).
2. Ouvrez le fichier depuis le téléphone. Android demande d'autoriser l'installation depuis cette application
   (« Sources inconnues ») : acceptez pour cette fois.
3. Lancez **Mes Actus**.

Ou, le téléphone branché en USB avec le débogage USB activé :

```
adb install -r MesActus.apk
```

## Utilisation

- **Barre du bas** : France, Canada, Espagne, Favoris.
- **Onglets du haut** : National, Région, Ville. Les zones précises se masquent dans *Réglages → Zones affichées*.
- **Puces de thèmes** : Politique, Économie, Sport, Culture, Faits divers, Société, Sciences, Santé, Environnement.
  Le classement se fait automatiquement par mots-clés.
- **Tirer vers le bas** pour actualiser.
- **Article** : mode lecture épuré. ⭐ pour l'ajouter aux favoris (le texte est enregistré et reste lisible sans réseau),
  bouton *Traduire en français* pour les articles en espagnol ou en anglais (traduction sur le téléphone ; le modèle de
  ~30 Mo est téléchargé une seule fois).
- **Loupe** : recherche dans toutes les zones.
- **Réglages → Gérer les sources** : activer, désactiver, ajouter un flux RSS (testé avant l'ajout) ou revenir à la liste d'origine.

## Limites connues

- L'APK est compilé pour les téléphones 64 bits (arm64, la quasi-totalité des téléphones depuis 2017) sous Android 8 ou plus.
  Pour un vieux téléphone 32 bits, ajoutez `"armeabi-v7a"` à `abiFilters` dans `app/build.gradle.kts`.
- El País, La Presse et les articles réservés aux abonnés bloquent l'extraction : l'app affiche le résumé.

- Les articles venant de *Google Actualités* ne s'affichent qu'avec leur titre : le bouton « Lire l'article sur le site »
  ouvre la page du média.
- Certains sites (articles payants, pages très dynamiques) ne se laissent pas extraire : l'app affiche alors le résumé RSS.

## Recompiler

Il faut Java 17 (`JAVA_HOME`) et le SDK Android 35 (`sdk.dir` dans `local.properties`, ou Android Studio).

```
gradlew.bat assembleRelease
```

L'APK est produit dans `app\build\outputs\apk\release\app-release.apk`. Le projet s'ouvre aussi dans Android Studio.
