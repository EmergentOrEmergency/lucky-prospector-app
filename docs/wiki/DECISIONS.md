# Registro decisioni

## Confermate

| ID | Data | Decisione | Motivo |
| --- | --- | --- | --- |
| ADR-001 | 2026-09-03 | Nome di lavoro `Lucky Prospector` | Comunica ricerca, rarita e scoperta internazionale. |
| ADR-002 | 2026-09-03 | Android prima, iOS dopo | Riduce il perimetro della prima release. |
| ADR-003 | 2026-09-03 | Android nativo Kotlin/Compose | Camera e inference live beneficiano delle API native; coincide col piano del model lab. |
| ADR-004 | 2026-09-03 | Offline-first e account non obbligatorio | Privacy, resilienza e costo iniziale minimo. |
| ADR-005 | 2026-09-03 | Desiderio privato nell'MVP | La condivisione richiede una decisione separata e moderazione. |
| ADR-006 | 2026-09-03 | Prato salvato come singolo punto | Modello dati e UX semplici. |
| ADR-007 | 2026-09-03 | Foto in archivio app e galleria sistema | Soddisfa memoria privata e accesso personale comune. |
| ADR-008 | 2026-09-03 | Correzione preserva previsione originale | Audit e futuri campioni di miglioramento. |
| ADR-009 | 2026-09-03 | Provider mappa astratto | Evita lock-in prima di misurare utilizzo e requisiti offline. |
| ADR-010 | 2026-09-03 | Demo detector separato dal modello reale | Permette UX e test senza distribuire pesi AGPL non ancora approvati. |
| ADR-011 | 2026-09-03 | Mappa demo schematica offline | Consente una demo priva di API key mantenendo sostituibile il provider. |
| ADR-012 | 2026-09-22 | Firebase come backend (Auth + Firestore) | Copre accesso Google e dati condivisi senza scrivere e mantenere un server. Chiude la scelta Supabase/Firebase. |
| ADR-013 | 2026-09-22 | Dati in `eur3` (Europa), modalita produzione | Utenza italiana; evita trasferimenti fuori UE. **La regione non e piu modificabile** dopo la creazione del database. |
| ADR-014 | 2026-09-22 | Coordinate pubbliche arrotondate, mai esatte | Chiude la voce aperta "coordinate esatte o approssimate". Cella minima 100 m, predefinita 150 m, imposta sia dal codice sia dalle regole server. |
| ADR-015 | 2026-09-22 | **Pin condivisi anonimi**: nessuna identita sulla mappa | Un identificativo stabile cucirebbe i pin di una persona in una traccia, e da una traccia si ricava dove abita. L'arrotondamento protegge il singolo pin, non la sequenza. |
| ADR-016 | 2026-09-22 | Proprieta tramite rivendicazione privata, non campo pubblico | Consente di ritirare un pin senza esporre chi l'ha pubblicato. L'autore resta risalibile dall'amministratore del progetto: anonimo verso gli altri utenti, non impunito. |
| ADR-017 | 2026-09-22 | Condivisione esplicita per singolo ritrovamento | Privato e il valore predefinito; non esiste un interruttore globale "condividi tutto", che si attiva per errore una volta sola e pubblica anche il passato. |
| ADR-018 | 2026-09-22 | Nome utente mai derivato dall'email | `mario.rossi@` diventerebbe nome e cognome reali. Resta comunque locale: con ADR-015 non compare su nessun pin. |
| ADR-019 | 2026-09-22 | Accesso Google via Credential Manager | `GoogleSignInClient` e deprecato. Il client id si legge dalla risorsa generata, non si incolla. |
| ADR-020 | 2026-09-22 | ~~Prati e quadrifogli decadono invece di essere segnalati~~ **Sostituita da ADR-025.** | Un prato falciato non e un abuso, e un dato scaduto. Le segnalazioni restano per l'abuso vero. |
| ADR-021 | 2026-09-24 | Modello on-device `flc-yolox-tiny-v0.1.0-rc1`, importato dal laboratorio tramite bundle verificato | YOLOX e Apache-2.0, il dynamic INT8 ha superato parita e release gate sul Galaxy S23 Ultra. Il suo contratto letterbox/BGR e separato dall'adapter SSD storico stretch/RGB: mischiarli invaliderebbe le metriche. Il binario resta fuori da Git e arriva tramite `scripts/import-model.ps1`, che verifica release e SHA-256. |
| ADR-022 | 2026-09-26 | Lingua automatica del dispositivo con inglese di fallback e scelta manuale nel Profilo | Italiano, inglese, francese, tedesco e spagnolo sono supportati. Se nessuna lingua di sistema e supportata, l'app usa inglese; la scelta manuale persistente include anche il ritorno a “Lingua del dispositivo”. Evita interfacce miste e mantiene l'app utilizzabile fuori dai mercati tradotti. |
| ADR-023 | 2026-09-26 | MapLibre con OpenFreeMap Liberty soltanto per sviluppo e collaudo | Il primo stile ufficiale MapLibre ha verificato renderer, pin e gesture ma mostrava solo Paesi; OpenFreeMap aggiunge citta e strade senza chiave. L'utente ha approvato esplicitamente entrambi i provider dopo l'avviso che vedono area richiesta, IP e orario. OpenFreeMap dichiara di non conservare IP nei log ordinari, puo abilitarli fino a 30 giorni durante incidenti e puo usare Cloudflare. Marker, foto, desideri e identita non vengono inviati al provider; i pin condivisi arrivano al renderer gia arrotondati. Questa scelta resta di sviluppo, non approvata automaticamente per beta o produzione. |
| ADR-024 | 2026-09-26 | Segnalazioni private per account e auto-nascondimento a tre segnalazioni (per i luoghi spariti vedi ADR-025) | La data della fonte e il decadimento gestiscono i luoghi vecchi o rimossi; la segnalazione resta per pin non pertinenti, falsi, spam o offensivi. Ogni account ha una ricevuta privata immutabile, mentre il pin pubblico espone solo un conteggio anonimo. L'autore non puo auto-segnalarsi e tre account indipendenti sono necessari per nascondere il pin. |
| ADR-025 | 2026-09-26 | I pin non scadono mai: escono dalla mappa quando **2 persone diverse** dicono "non c'è più". Rimozione solo logica (`hidden`) | Un prato puo durare anni: l'eta da sola non dice che un luogo non esiste piu. Due persone e non una, perche un solo account non basti a togliere un pin. "Non c'è più" resta separato dalle segnalazioni di abuso (ADR-020 conserva il suo motivo). La freschezza resta solo informazione visiva. Ricevute private e contatore anonimo `goneCount`, come per conferme e segnalazioni. |
| ADR-026 | 2026-09-26 | Massimo 20 pin condivisi al giorno per account, imposti dalle regole | Contro lo spam. Contatore privato in `users/{uid}/limits/publish` che deve avanzare nello stesso batch del pin. |
| ADR-027 | 2026-09-26 | ~~Foto come anteprima solo sulla mappa privata~~ **Sostituita da ADR-028.** | Sulla condivisa le foto restano escluse: EXIF, sfondo riconoscibile, moderazione e costi richiedono una decisione a parte (vedi SOCIAL.md, Foto condivise). |
| ADR-028 | 2026-09-26 | Foto anche sulla mappa condivisa: ritaglio attorno ai trifogli rilevati, ricodificato senza EXIF, mostrato all'utente prima dell'invio. Il tipo pubblico lo decide il classificatore (quadrifoglio ≥ 0,7, prato ≥ 0,5, altrimenti niente condivisione) | La foto rende la mappa utile e credibile. Il ritaglio limita lo sfondo che rivelerebbe il luogo esatto; la ricodifica elimina il GPS. Il classificatore evita che un pin prometta un quadrifoglio che il modello non vede. Documento Firestore separato (`shared_photos`), servito solo finché il pin non è nascosto. |
| ADR-029 | 2026-09-26 | Suggerimenti dal Profilo in una collezione Firestore di sola scrittura, senza account né nome, massimo 10 al giorno per account | Le recensioni dello store raramente danno spunti concreti; domande mirate per categoria sì. Nessun server da mantenere; il contatto resta facoltativo. |

## Aperte ma non bloccanti per la fondazione

- Package/application ID definitivo e proprietario del dominio.
- Provider tile/stile per la beta. La decisione deve includere non solo resa e costo, ma anche log e regione: le richieste di tile rivelano al provider l'area geografica visualizzata, anche quando i marker restano soltanto sul dispositivo.
- ~~Supabase oppure Firebase per il cloud.~~ Chiusa da ADR-012.
- ~~Coordinate pubbliche esatte oppure approssimate.~~ Chiusa da ADR-014.
- Condivisione futura dei desideri: bacheca **senza posizione** (vedi [SOCIAL.md](SOCIAL.md)), non desideri attaccati ai pin.
- Cella piu ampia per `clover_patch` che per il singolo quadrifoglio: un prato e gia diffuso, un singolo ritrovamento e un punto preciso. Mai piu stretta di 100 m in nessun caso.
- ~~Soglia di segnalazioni indipendenti oltre la quale un pin si auto-nasconde.~~ Chiusa da ADR-024: tre.
- Email di assistenza pubblica: sostituire l'indirizzo personale prima di aprire l'app oltre la cerchia di amici.
- ~~Strategia commerciale/licenza del modello YOLO26n AGPL-3.0.~~ Chiusa da ADR-021 scegliendo YOLOX Apache-2.0.
- Come riconoscere in modo affidabile `clover_patch`: seconda classe, secondo modello o conferma manuale.
- Budget prestazionale minimo e matrice dispositivi reali.

## Vincoli scoperti

- Il modello selezionato e YOLOX-Tiny 640 dynamic INT8, release candidate `flc-yolox-tiny-v0.1.0-rc1`, con bundle e test on-device completi.
- Il vecchio modello Apache-2.0 `detect4clover` e utilizzabile tecnicamente ma ha precisione insufficiente per una buona ricerca autonoma.
- Android Studio 2026.1.4.7, il suo JDK, Android SDK 37.0 e Gradle 9.4.1 sono installati; `JAVA_HOME` globale punta ancora a un vecchio Java e viene sovrascritto nel comando di build documentato.
