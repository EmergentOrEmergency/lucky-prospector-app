# Requisiti

Stati: `MVP`, `LATER`, `OPEN`. Le parole DEVE/DOVREBBE indicano priorita normativa interna.

## Detection e guida live

- **DET-001 (MVP)** L'app DEVE analizzare in tempo reale i frame della fotocamera posteriore sul dispositivo.
- **DET-002 (MVP)** L'overlay DEVE mostrare posizione e stabilita del candidato senza dipendere solo dal colore.
- **DET-003 (MVP)** La pipeline DEVE scartare frame arretrati e mantenere responsiva l'anteprima.
- **DET-004 (MVP)** Una segnalazione positiva DEVE richiedere conferma temporale su piu frame, con parametri configurabili.
- **DET-005 (MVP)** L'app DEVE dare suggerimenti contestuali almeno per: troppo lontano, troppo vicino/sfocato, movimento eccessivo, luce insufficiente e nessun trifoglio visibile.
- **DET-006 (MVP)** L'utente DEVE poter correggere o rifiutare la classificazione.
- **DET-007 (MVP)** Previsione originale, confidenza, versione modello e correzione DEVONO restare distinguibili.
- **DET-008 (LATER)** L'utente PUO inviare volontariamente esempi corretti al server, con anteprima e consenso separato.
- **DET-009 (OPEN)** La detection di `clover_patch` richiede una classe/modello validato; non va simulata usando semplicemente l'assenza di quadrifogli.

Accettazione MVP: anteprima senza accumulo visibile; overlay coerente con rotazione/crop; nessun frame trasmesso; suggerimenti testati con casi riproducibili.

## Acquisizione e archivio

- **CAP-001 (MVP)** L'utente DEVE poter salvare una foto da una detection o come prato di trifogli.
- **CAP-002 (MVP)** La foto DEVE essere conservata nell'archivio dell'app e pubblicata anche nella galleria di sistema tramite MediaStore.
- **CAP-003 (MVP)** Il record DEVE contenere ID, tipo, data/ora, percorso/URI, coordinate opzionali, accuratezza GPS, desiderio opzionale, previsione e correzione.
- **CAP-004 (MVP)** Il salvataggio DEVE riuscire senza posizione quando il permesso e negato o il fix non arriva.
- **CAP-005 (MVP)** L'utente DEVE poter modificare desiderio e classificazione ed eliminare un ritrovamento.
- **CAP-006 (MVP)** La galleria interna DEVE distinguere quadrifogli e prati e mostrare lo stato di sincronizzazione futuro.

Accettazione MVP: dopo riavvio i record persistono; entrambe le gallerie mostrano la foto; una correzione non sovrascrive la previsione originaria.

## Posizione e mappa

- **MAP-001 (MVP)** Ogni ritrovamento geolocalizzato DEVE apparire come singolo marker.
- **MAP-002 (MVP)** Marker di quadrifogli e prati DEVONO essere distinguibili anche senza affidarsi solo al colore.
- **MAP-003 (MVP)** I marker e i dettagli gia salvati DEVONO essere consultabili offline.
- **MAP-004 (MVP)** L'assenza della cartografia di sfondo offline NON DEVE impedire l'accesso al ritrovamento.
- **MAP-005 (MVP)** L'app DEVE spiegare perche richiede la posizione e chiedere solo il permesso foreground.
- **MAP-006 (LATER)** Il download esplicito di aree offline potra essere aggiunto solo con un provider/licenza che lo consenta.

## Account, backup e condivisione

- **AUTH-001 (LATER)** L'MVP DEVE essere utilizzabile senza account.
- **AUTH-002 (LATER)** Il login Google facoltativo DEVE abilitare backup e ripristino.
- **AUTH-003 (LATER)** Su iOS va previsto Sign in with Apple quando richiesto dalle regole dello store.
- **SYNC-001 (LATER)** La sincronizzazione DEVE essere offline-first, idempotente e tollerante a interruzioni.
- **SYNC-002 (LATER)** Foto e metadati DEVONO essere recuperabili dopo reinstallazione o cambio dispositivo per utenti autenticati.
- **SHARE-001 (LATER)** La pubblicazione di un ritrovamento DEVE richiedere opt-in per singolo record.
- **SHARE-002 (LATER)** Il desiderio DEVE restare privato salvo scelta esplicita futura separata.
- **SHARE-003 (LATER)** Prima della mappa pubblica va definita una riduzione della precisione geografica e una policy anti-abuso/ecologica.
- **SHARE-004 (LATER)** Contenuti pubblici e campioni ML richiedono consensi distinti.

## Privacy, sicurezza e accessibilita

- **PRI-001 (MVP)** Nessun frame live DEVE lasciare il dispositivo.
- **PRI-002 (MVP)** Desideri e coordinate precise DEVONO essere privati per impostazione predefinita.
- **PRI-003 (MVP)** I dati locali sensibili DOVREBBERO essere protetti usando le API di sicurezza del sistema.
- **PRI-004 (LATER)** L'utente autenticato DEVE poter esportare o cancellare i dati cloud.
- **ACC-001 (MVP)** Controlli e feedback DEVONO avere etichette per screen reader.
- **ACC-002 (MVP)** Conferme importanti DEVONO poter usare combinazioni di grafica, testo, vibrazione e suono configurabili.

## Qualita e prestazioni

- **QLT-001 (MVP)** Preprocessing, decoder, mapping coordinate e filtro temporale DEVONO avere test unitari.
- **QLT-002 (MVP)** Il modello mobile DEVE superare test di parita contro immagini golden prima del rilascio.
- **QLT-003 (MVP)** Devono essere misurati FPS, latenza p50/p95, memoria, temperatura e consumo batteria su dispositivi reali.
- **QLT-004 (MVP)** L'app DEVE mostrare versione e licenza del modello.
- **QLT-005 (MVP)** Una build di release NON DEVE contenere chiavi provider non ristrette all'app/package e firma.

