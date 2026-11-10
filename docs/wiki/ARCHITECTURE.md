# Architettura tecnica

## Scelta iniziale

Android nativo con Kotlin e Jetpack Compose. Il repository ML aveva gia definito CameraX + LiteRT e la pipeline live beneficia dell'accesso diretto alle API Android. iOS verra implementato in SwiftUI/AVFoundation riusando contratto modello, dominio, UX e casi di test, non il codice UI.

## Moduli previsti

- `app`: composizione, navigazione e dependency injection.
- `camera`: CameraX, analisi frame e suggerimenti di qualita.
- `inference`: preprocessing, LiteRT, decoder, tracking e coordinate overlay.
- `domain`: entita e casi d'uso indipendenti dalla UI.
- `data-local`: Room/SQLite, file privati e MediaStore.
- `map`: visualizzazione marker tramite un adapter indipendente dal provider.
- `sync`: interfacce e coda locale; implementazione cloud successiva.
- `design-system`: colori, tipografia, componenti e accessibilita.

## Flusso dati live

`CameraX -> quality analyzer -> throttling/latest frame -> LiteRT -> decoder -> temporal confirmation -> overlay -> user confirmation -> local repository`

I suggerimenti non devono dipendere soltanto dal detector. Si combinano nitidezza, luminosita, moto tra frame, dimensione del soggetto e presenza di verde/trifogli quando disponibile.

## Modello

Candidato attuale: YOLO26n FP32 TFLite, input 640x640, output `[1,300,6]` con box, confidenza e classe, circa 9.3 MB. Classi di training: `clover_3`, `clover_4`, `clover_5_plus`; il target prodotto primario e `clover_4`. Il checkpoint ha mostrato sul test `clover_4` precision 0.952 e recall 0.986 a confidenza 0.25.

Attenzione: l'artefatto YOLO26n deriva dal pacchetto Ultralytics AGPL-3.0. Prima di distribuire l'app va risolta la strategia di licenza oppure scelto/addestrato un modello con licenza compatibile. Il vecchio `detect4clover` e Apache-2.0 ma ha bassa precisione e una sola classe positiva.

Il modello non va copiato nell'app finche non esiste un release bundle approvato con metadata completi, hash, licenze e test golden.

## Dati locali

Entita minima `Finding`:

```text
id, createdAt, type, privateImageUri, galleryImageUri,
latitude?, longitude?, locationAccuracyM?, wish?, notes?,
predictedClass?, correctedClass?, confidence?, modelVersion?,
syncState, sharingState, deletedAt?, updatedAt
```

Le modifiche usano timestamp e tombstone per rendere futura la sincronizzazione senza migrazioni distruttive.

## Backend futuro

Contratto astratto iniziale: autenticazione, tabella ritrovamenti, object storage foto, coda sync e policy row-level. Prima candidata: Supabase; Firebase resta alternativa naturale se si privilegia l'integrazione Google. Nessuna SDK cloud deve entrare nel core offline.

