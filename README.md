# Mes Actus

Les plus grosses infos du moment, sur une seule page : France, Orléans & Loiret, Canada & Québec, Montréal,
Espagne, Grenade & Andalousie.

## Installer sur le téléphone

1. Depuis le téléphone, téléchargez `MesActus.apk` dans la [dernière release](../../releases/latest).
2. Ouvrez le fichier. Android demande d'autoriser l'installation depuis votre navigateur : acceptez.
3. Lancez **Mes Actus**. Une version plus récente s'installe par-dessus l'ancienne.

## Comment les grands titres sont choisis

L'app lit les unes de chaque zone (Le Monde, Franceinfo, France 3, Radio-Canada, La Presse, Le Devoir, El País,
RTVE, Ideal, Europa Press, Google Actualités…), regroupe les articles qui parlent du même sujet, puis note chaque
sujet selon :

- le nombre de médias qui en parlent ;
- sa place en tête des unes ;
- sa fraîcheur (au-delà de 36 h, il disparaît).

Chaque rubrique affiche le sujet principal en grand, puis 2 à 4 autres titres, un seul titre par événement.

## Utilisation

- **Tirer vers le bas** pour actualiser (automatique si l'app n'a pas été ouverte depuis 20 minutes).
- **Toucher un titre** : article en mode lecture, bouton *Traduire en français* pour l'espagnol, partage.
- **⚙️ Réglages** : rubriques affichées, thème clair ou sombre.

## Limites connues

- Téléphones 64 bits (arm64) sous Android 8 ou plus.
- El País, La Presse et les articles réservés aux abonnés ne se laissent pas afficher dans l'app :
  le bouton « Lire sur … » ouvre le site.

## Recompiler

Il faut Java 17 (`JAVA_HOME`) et le SDK Android 35 (`sdk.dir` dans `local.properties`, ou Android Studio).

```
gradlew.bat assembleRelease
```

L'APK est produit dans `app\build\outputs\apk\release\app-release.apk`.
