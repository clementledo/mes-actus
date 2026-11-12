# Mes Actus

Les plus grosses infos du moment, sur une seule page : France, Orléans & Loiret, Canada & Québec, Montréal,
Espagne, Grenade & Andalousie.

## Installer sur le téléphone

1. Depuis le téléphone, téléchargez `MesActus.apk` dans la [dernière release](../../releases/latest).
2. Ouvrez le fichier. Android demande d'autoriser l'installation depuis votre navigateur : acceptez.
3. Lancez **Mes Actus**.

## Mises à jour

Quand une nouvelle version est publiée, un bandeau vert **« Nouvelle version »** apparaît en haut de la page.
Touchez **Installer** : l'app télécharge la nouvelle version et ouvre l'installateur d'Android. La première
fois, Android demande d'autoriser Mes Actus à installer des applis : acceptez, puis touchez à nouveau Installer.

## Comment les grands titres sont choisis

L'app lit les unes de chaque zone (Le Monde, Franceinfo, France 3, Radio-Canada, La Presse, Le Devoir, El País,
RTVE, Ideal, Europa Press, Google Actualités…), regroupe les articles qui parlent du même sujet, puis note chaque
sujet selon :

- le nombre de médias qui en parlent ;
- sa place en tête des unes ;
- sa fraîcheur (au-delà de 36 h, il disparaît).

Pour ne pas se noyer, chaque rubrique se limite à 3 sujets (2 pour les rubriques locales) : le sujet principal
en grand, avec son image, puis les suivants. Un seul titre par événement. Si le flux ne donne pas d'image,
l'app va chercher celle de l'article.

## Utilisation

- **Tirer vers le bas** pour actualiser (automatique si l'app n'a pas été ouverte depuis 20 minutes).
- **Toucher un titre** : article en mode lecture, bouton *Traduire en français* pour l'espagnol, partage.
- **⚙️ Réglages** : rubriques affichées, thème clair ou sombre.

## Limites connues

- Téléphones 64 bits (arm64) sous Android 8 ou plus.
- El País, La Presse et les articles réservés aux abonnés ne se laissent pas afficher dans l'app :
  le bouton « Lire sur … » ouvre le site.

## Fabrication

Chaque envoi sur la branche `main` lance GitHub Actions (`.github/workflows/release.yml`), qui fabrique l'APK
signé, lance les tests et publie la release `v1.N` avec `MesActus.apk`. La clé de signature est créée au
premier passage et rangée dans une release brouillon privée `signing-key` (ne pas la supprimer, sinon les
mises à jour ne s'installeraient plus par-dessus).

## Recompiler sur un ordinateur

Il faut Java 17 (`JAVA_HOME`) et le SDK Android 35 (`sdk.dir` dans `local.properties`, ou Android Studio).

```
gradlew.bat assembleRelease
```

L'APK est produit dans `app\build\outputs\apk\release\app-release.apk`.
