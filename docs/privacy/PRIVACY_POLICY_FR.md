# Politique de confidentialité – Lucky Prospector

**Dernière mise à jour :** 29 septembre 2026

Cette information est fournie au titre de l'art. 13 du règlement (UE) 2016/679 (« RGPD »). Elle explique quelles données traite l'application Android Lucky Prospector (`com.luckyprospector.app`), pourquoi et pendant combien de temps. Le texte de référence est la version italienne ; en cas de doute, c'est elle qui fait foi.

## 1. Responsable du traitement

Daniele Bulgarelli, Reggio Emilia, Italie. Contact confidentialité : intervistaconleai@gmail.com.

## 2. En bref

- Vous pouvez chercher et enregistrer des trèfles sans compte et sans connexion.
- Les photos, les vœux et votre position précise restent sur votre téléphone.
- Seul ce que vous choisissez de publier sur la carte partagée est mis en ligne : le type de trouvaille, une zone arrondie à au moins 100 m et, si vous le souhaitez, une photo recadrée sans données GPS.
- L'application ne contient ni publicité ni outils d'analyse ou de suivi.

## 3. Données qui restent sur votre appareil

Le responsable ne reçoit pas ces données et ne peut pas les lire.

- **Images de la caméra.** Pendant la recherche, le modèle de reconnaissance (TensorFlow Lite/LiteRT) analyse les images de la caméra sur le téléphone. Les images ne sont ni enregistrées ni envoyées. Vos photos ne servent pas à entraîner le modèle.
- **Trouvailles.** Pour chaque trouvaille enregistrée, l'application conserve dans une base de données locale : la date et l'heure, le type (trèfle à quatre feuilles ou touffe de trèfles), votre vœu facultatif, la position précise et sa précision (seulement si vous avez autorisé la localisation), le résultat de la reconnaissance (classe, confiance, version du modèle), une éventuelle correction de la classe et l'état du partage.
- **Photos.** Chaque photo est enregistrée dans l'espace privé de l'application et **copiée dans la galerie du téléphone** (« Pictures/Lucky Prospector »). La copie de la galerie est visible par les autres applications ayant accès aux photos et par les services de sauvegarde que vous avez activés. Elle peut rester dans la galerie après la désinstallation de l'application.
- **Session et cache.** Si vous vous connectez, le SDK Firebase conserve sur le téléphone la session et un cache des données de la carte partagée.

La sauvegarde automatique d'Android est désactivée pour l'application : ces données ne sont donc pas copiées dans la sauvegarde cloud du système. Vous pouvez supprimer une trouvaille dans l'application : cela efface la fiche, la photo privée et la copie de la galerie. Si la trouvaille était partagée, le pin public est également retiré.

## 4. Données traitées en ligne

### 4.1 Connexion avec Google (facultative)

La connexion ne sert qu'à publier sur la carte partagée, à la consulter et à envoyer des suggestions. Elle passe par Google (Credential Manager) et Firebase Authentication. Google reçoit la demande de connexion et nous transmet un identifiant de votre compte. Firebase conserve un identifiant utilisateur (UID), votre adresse e-mail, le nom et la photo de votre profil Google, des données techniques de connexion (dates de création et de dernière connexion) et des jetons de session. L'application n'affiche votre nom et votre photo qu'à vous, sur l'écran Profil.

### 4.2 Carte partagée (seulement si vous choisissez de partager)

Une trouvaille n'est publiée que lorsque vous touchez « Partager » et confirmez après avoir vu un aperçu. Le **document public** est visible par tous les utilisateurs connectés et contient uniquement :

- le type, décidé par le modèle de reconnaissance sur le téléphone ;
- la position **arrondie au centre d'une cellule d'au moins 100 m** (150 m par défaut), et un geohash calculé à partir de ce point arrondi ;
- la date de publication et celle de la dernière confirmation ;
- des compteurs anonymes : confirmations, signalements, « Ce n'est plus là » ;
- l'indication qu'une photo est jointe ou non.

Le document public ne contient ni identité, ni vœu, ni position précise.

**Photo facultative :** un recadrage carré autour des trèfles reconnus, de 384 px au plus, réencodé à partir des pixels. Elle ne contient aucune métadonnée EXIF : ni GPS, ni date, ni modèle de téléphone. Le recadrage réduit l'arrière-plan sans le supprimer : l'application vous demande donc de vérifier l'image avant l'envoi.

**Documents privés liés à votre compte.** Ils sont conservés sous votre UID :

- une « revendication » pour chaque pin publié, avec sa date de création, qui vous permet de retirer le pin ;
- les reçus de vos confirmations, de vos signalements (avec le motif) et de vos « Ce n'est plus là » ;
- des compteurs quotidiens qui limitent la publication à 20 pins par jour et les suggestions à 10 messages par jour.

Les autres utilisateurs ne peuvent pas voir ces documents. **Le responsable peut relier votre compte aux pins que vous avez publiés et à vos signalements via la console Firebase.** Au sens du RGPD, le partage est donc anonyme pour les autres utilisateurs mais pseudonyme pour le responsable.

**Retrait.** Vous pouvez retirer à tout moment un pin que vous avez publié : le pin, sa photo et votre revendication sont supprimés. Les pins n'expirent pas d'eux-mêmes. Après 3 signalements d'abus, ou lorsque 2 personnes différentes indiquent « Ce n'est plus là », le pin et sa photo sont masqués pour tous. Le document reste conservé pendant la durée indiquée à la section 7.

### 4.3 Suggestions

Depuis l'écran Profil, vous pouvez envoyer un message avec : la catégorie, le texte (2000 caractères au plus), un contact facultatif, la version de l'application, la langue et la date. Le message ne contient ni votre UID ni votre nom. Toutefois, un compteur quotidien de votre espace privé est mis à jour avec l'heure du dernier envoi. **En comparant cette heure avec la date du message, le responsable pourrait relier le message à votre compte.** N'indiquez pas de données personnelles inutiles dans le texte.

### 4.4 Sécurité (Firebase App Check)

Pour vérifier que les requêtes proviennent de l'application authentique sur un appareil authentique, l'application utilise Firebase App Check avec Google Play Integrity. Google évalue l'intégrité de l'appareil et de l'application et délivre un jeton. Le responsable ne reçoit aucune donnée de l'appareil.

## 5. Cartes (OpenFreeMap et MapLibre)

Les cartes sont dessinées sur le téléphone par MapLibre, avec un style et des tuiles téléchargés depuis OpenFreeMap (`tiles.openfreemap.org`). Le fournisseur reçoit votre adresse IP et les tuiles demandées : il peut donc savoir **quelle zone vous regardez**, y compris sur votre carte privée. Il ne reçoit ni vos trouvailles ni les marqueurs. Selon les informations qu'il publie, OpenFreeMap n'enregistre pas les adresses IP dans ses journaux ordinaires, peut les conserver jusqu'à 30 jours lors d'incidents techniques et peut utiliser Cloudflare. Sa propre politique s'applique pour les détails. Une carte locale simplifiée reste disponible hors connexion.

## 6. Finalités et bases juridiques

| Traitement | Base juridique |
|---|---|
| Connexion et gestion du compte | Exécution du service que vous demandez (art. 6.1.b) |
| Publication sur la carte partagée, photo comprise | Consentement, donné en confirmant le partage et retiré en retirant le pin (art. 6.1.a) |
| Revendications, reçus, limites quotidiennes, seuils de retrait, App Check | Intérêt légitime à prévenir les abus et le spam et à protéger le service (art. 6.1.f) |
| Suggestions et réponse éventuelle | Intérêt légitime à améliorer l'application ; le contact est facultatif (art. 6.1.f) |
| Cartes | Intérêt légitime à afficher la carte que vous ouvrez (art. 6.1.f) |
| Position du téléphone | Autorisation Android facultative ; la donnée reste sur l'appareil |

Il n'y a pas de décision automatisée produisant des effets juridiques. Le modèle de reconnaissance décide seulement comment un pin est affiché (trèfle à quatre feuilles ou touffe) et s'il peut être partagé.

## 7. Conservation

- **Données locales :** jusqu'à ce que vous supprimiez la trouvaille ou désinstalliez l'application. La copie de la galerie reste jusqu'à ce que vous la supprimiez.
- **Compte et documents privés :** tant que le compte existe. Ils sont supprimés avec le compte, sauf les compteurs techniques décrits à la section 8.
- **Pins publics et photos :** jusqu'à ce que vous les retiriez ou supprimiez votre compte. Pins et photos masqués à la suite de signalements : ils sont supprimés dans les 120 jours suivant leur masquage.
- **Suggestions :** jusqu'à 24 mois après leur envoi, puis supprimées.
- **Journaux techniques de Google/Firebase :** selon les règles de Google.

## 8. Suppression du compte

Sur l'écran Profil, dans « Infos et confidentialité », « Supprimer le compte et les données » efface : les pins que vous avez publiés avec leurs photos et revendications ; les confirmations, signalements et « Ce n'est plus là » que vous avez donnés à d'autres pins, qui sont retirés (le compteur public diminue de un) avec leurs reçus ; enfin votre compte Firebase. Il ne reste que deux compteurs techniques quotidiens (nombre d'envois et heure du dernier), sans contenu et qui ne peuvent plus être reliés à vous après la suppression du compte : s'ils pouvaient être effacés, n'importe qui pourrait remettre à zéro les limites anti-spam. Un pin déjà masqué par la communauté reste masqué même si votre vote est retiré. Si vous n'avez plus l'application, vous pouvez demander la suppression sur https://emergentoremergency.github.io/lucky-prospector/delete-account-fr.html ou en écrivant à intervistaconleai@gmail.com. Les données locales s'effacent en supprimant les trouvailles ou en désinstallant l'application. Les suggestions ne sont pas liées au compte : nous pouvons les supprimer sur demande si vous nous aidez à les retrouver.

## 9. Destinataires et transferts

- **Google Ireland Ltd / Google LLC** (Firebase Authentication, Cloud Firestore, App Check, Play Integrity), en qualité de sous-traitant selon les conditions de Firebase. La base Firestore se trouve dans la région **`eur3` (Union européenne)**. L'authentification, App Check et certains services d'assistance peuvent aussi traiter des données hors de l'UE, notamment aux États-Unis. Dans ces cas, Google s'appuie sur les clauses contractuelles types de la Commission européenne et/ou sur le EU-U.S. Data Privacy Framework.
- **OpenFreeMap** (et son éventuel CDN), pour les tuiles de la carte (section 5).
- Les **autres utilisateurs** ne voient que les documents publics décrits à la section 4.2.

Vos données ne sont ni vendues ni utilisées à des fins publicitaires.

## 10. Vos droits

Dans les limites des art. 15 à 22 du RGPD, vous pouvez demander l'accès, la rectification, l'effacement, la limitation et la portabilité de vos données, et vous opposer aux traitements fondés sur l'intérêt légitime. Vous pouvez retirer votre consentement à tout moment en retirant vos pins partagés, sans effet sur les traitements antérieurs. Écrivez à intervistaconleai@gmail.com. Vous avez aussi le droit d'introduire une réclamation auprès du **Garante per la protezione dei dati personali** (www.garanteprivacy.it) ou de l'autorité de votre pays de résidence (en France, la CNIL : www.cnil.fr).

## 11. Mineurs

L'application ne s'adresse pas aux personnes de moins de 14 ans, l'âge à partir duquel on peut, en Italie, consentir seul aux services en ligne (art. 2-quinquies du Code italien de la vie privée). Si la loi de votre pays fixe un âge plus élevé, c'est celui-ci qui s'applique. En dessous de cet âge, il ne faut ni se connecter ni partager sans le consentement d'un titulaire de l'autorité parentale.

## 12. Autorisations Android

- **Caméra** (obligatoire) : pour la recherche et les photos.
- **Localisation précise/approximative** (facultative) : pour placer une trouvaille sur votre carte.
- **Internet et état du réseau :** pour la connexion, la carte partagée, les tuiles de la carte et les suggestions.
- **Écriture du stockage** (Android 9 et versions antérieures uniquement) : pour copier les photos dans la galerie.

## 13. Modifications

Toute modification sera publiée ici avec la date mise à jour. Si elle est importante, nous vous en informerons aussi dans l'application.
