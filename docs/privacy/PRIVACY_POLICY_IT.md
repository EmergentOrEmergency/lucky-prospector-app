# Informativa privacy – Lucky Prospector

**Ultimo aggiornamento:** 29 settembre 2026

Questa informativa, resa ai sensi dell'art. 13 del Regolamento (UE) 2016/679 ("GDPR"), spiega quali dati tratta l'app Android Lucky Prospector (`com.luckyprospector.app`), perché e per quanto tempo.

## 1. Titolare del trattamento

Daniele Bulgarelli, Reggio Emilia, Italia. Contatto privacy: intervistaconleai@gmail.com.

## 2. In breve

- Puoi cercare e salvare quadrifogli senza account e senza connessione.
- Foto, desideri e posizione precisa restano sul tuo telefono.
- Online va solo ciò che scegli di pubblicare sulla mappa condivisa: il tipo di ritrovamento, una zona arrotondata ad almeno 100 m e, se lo vuoi, un ritaglio della foto senza dati GPS.
- Nell'app non ci sono pubblicità né strumenti di analisi o di tracciamento.

## 3. Dati che restano sul tuo dispositivo

Il titolare non riceve questi dati e non può leggerli.

- **Immagini della fotocamera.** Durante la ricerca il modello di riconoscimento (TensorFlow Lite/LiteRT) analizza i fotogrammi direttamente sul telefono. I fotogrammi non vengono salvati né inviati. Le tue foto non servono ad addestrare il modello.
- **Ritrovamenti.** Per ogni ritrovamento salvato l'app conserva in un database locale: data e ora, tipo (quadrifoglio o prato), il desiderio facoltativo, la posizione precisa con la sua accuratezza (solo se hai concesso la localizzazione), il risultato del riconoscimento (classe, confidenza, versione del modello), un'eventuale correzione della classe e lo stato di condivisione.
- **Foto.** Ogni foto viene salvata nell'archivio privato dell'app e **copiata nella galleria del telefono** (cartella "Pictures/Lucky Prospector"). La copia in galleria è visibile alle altre app con accesso alle foto e ai servizi di backup che hai eventualmente attivato. Se disinstalli l'app, può restare nella galleria.
- **Sessione e cache.** Se accedi, l'SDK Firebase conserva sul telefono la sessione e una cache dei dati della mappa condivisa.

Il backup automatico di Android è disattivato per l'app, quindi questi dati non vengono copiati nel cloud di sistema. Puoi eliminare un ritrovamento dall'app: vengono cancellati il record, la foto privata e la copia in galleria. Se il ritrovamento era condiviso, viene ritirato anche il pin pubblico.

## 4. Dati trattati online

### 4.1 Accesso con Google (facoltativo)

L'accesso serve solo per pubblicare sulla mappa condivisa, vederla e inviare suggerimenti. Avviene tramite Google (Credential Manager) e Firebase Authentication. Google riceve la richiesta di accesso e ci trasmette un identificativo dell'account. Firebase conserva un identificativo utente (UID), l'indirizzo email, il nome e la foto del profilo Google, i dati tecnici di accesso (date di creazione e ultimo accesso) e i token di sessione. L'app mostra il tuo nome e la tua foto solo a te, nella schermata Profilo.

### 4.2 Mappa condivisa (solo se decidi di condividere)

Un ritrovamento viene pubblicato solo quando tocchi "Condividi" e confermi dopo aver visto l'anteprima. Il **documento pubblico**, visibile a tutti gli utenti che hanno effettuato l'accesso, contiene soltanto:

- il tipo, stabilito dal riconoscimento sul telefono;
- la posizione **arrotondata al centro di una cella di almeno 100 m** (150 m di default) e un geohash calcolato su quel punto arrotondato;
- la data di pubblicazione e quella dell'ultima conferma;
- contatori anonimi: conferme, segnalazioni, "non c'è più";
- un'indicazione sulla presenza della foto.

Il documento pubblico non contiene identità, desiderio né posizione precisa.

**Foto facoltativa:** un ritaglio quadrato attorno ai trifogli riconosciuti, di 384 px al massimo, ricodificato dai pixel. Non contiene metadati EXIF: niente GPS, data o modello del telefono. Il ritaglio riduce lo sfondo ma non lo elimina, per questo l'app ti chiede di controllare l'immagine prima di inviarla.

**Documenti privati collegati all'account.** Sotto il tuo UID vengono conservati:

- la "rivendicazione" di ciascun pin che hai pubblicato, con la data di creazione, che ti permette di ritirarlo;
- le ricevute delle tue conferme, delle segnalazioni (con il motivo) e dei "non c'è più";
- i contatori giornalieri, che limitano le pubblicazioni a 20 al giorno e i suggerimenti a 10 al giorno.

Gli altri utenti non vedono questi documenti. **Il titolare, dalla console Firebase, può collegare il tuo account ai pin che hai pubblicato e alle tue segnalazioni.** Ai fini del GDPR, quindi, la condivisione è anonima verso gli altri utenti ma pseudonima verso il titolare.

**Rimozione.** Puoi ritirare in qualunque momento un pin che hai pubblicato: pin, foto e rivendicazione vengono cancellati. I pin non scadono da soli. Dopo 3 segnalazioni di abuso, o quando 2 persone diverse indicano "non c'è più", un pin viene nascosto a tutti insieme alla sua foto. Il documento però resta memorizzato (vedi sezione 7).

### 4.3 Suggerimenti

Dalla schermata Profilo puoi inviare un messaggio con queste informazioni: categoria, testo (massimo 2000 caratteri), un contatto facoltativo, versione dell'app, lingua e data. Il messaggio non contiene il tuo UID né il tuo nome. Nel tuo spazio privato viene però aggiornato un contatore giornaliero con l'orario dell'ultimo invio. **Confrontando quell'orario con la data del messaggio, il titolare potrebbe risalire all'account.** Non inserire nel testo dati personali che non servono.

### 4.4 Sicurezza (Firebase App Check)

Per verificare che le richieste arrivino dall'app originale su un dispositivo autentico, l'app usa Firebase App Check con Google Play Integrity. Google valuta l'integrità del dispositivo e dell'app e rilascia un token. Il titolare non riceve dati del dispositivo.

## 5. Mappe (OpenFreeMap e MapLibre)

Le mappe sono disegnate sul telefono da MapLibre, con stile e tessere scaricate da OpenFreeMap (`tiles.openfreemap.org`). Il fornitore riceve il tuo indirizzo IP e le tessere richieste, quindi può sapere **quale zona stai guardando**, anche sulla mappa privata. Non riceve i tuoi ritrovamenti né i marker. Secondo le informazioni che pubblica, OpenFreeMap non registra gli IP nei log ordinari, può conservarli fino a 30 giorni durante incidenti tecnici e può usare Cloudflare. Per i dettagli vale la sua informativa. Senza connessione resta disponibile una mappa semplificata locale.

## 6. Finalità e basi giuridiche

| Trattamento | Base giuridica |
|---|---|
| Accesso e gestione dell'account | Esecuzione del servizio che richiedi (art. 6.1.b) |
| Pubblicazione sulla mappa condivisa, foto inclusa | Consenso, espresso con la conferma di condivisione e revocabile ritirando il pin (art. 6.1.a) |
| Rivendicazioni, ricevute, limiti giornalieri, soglie di rimozione, App Check | Legittimo interesse a prevenire abusi e spam e a proteggere il servizio (art. 6.1.f) |
| Suggerimenti ed eventuale risposta | Legittimo interesse a migliorare l'app; il contatto è facoltativo (art. 6.1.f) |
| Mappe | Legittimo interesse a mostrare la mappa che apri (art. 6.1.f) |
| Posizione del telefono | Permesso Android facoltativo; il dato resta sul dispositivo |

Non ci sono decisioni automatizzate con effetti giuridici. Il riconoscimento decide solo come appare un pin (quadrifoglio o prato) e se può essere condiviso.

## 7. Conservazione

- **Dati locali:** finché non elimini il ritrovamento o disinstalli l'app. La copia in galleria resta finché non la cancelli tu.
- **Account e documenti privati:** finché l'account esiste. Vengono cancellati con l'eliminazione dell'account, tranne i contatori tecnici descritti nella sezione 8.
- **Pin pubblici e foto:** finché non li ritiri o non elimini l'account. Pin e foto nascosti a seguito di segnalazioni: vengono cancellati entro 120 giorni da quando sono stati nascosti.
- **Suggerimenti:** fino a 24 mesi dall'invio, poi vengono cancellati.
- **Log tecnici di Google/Firebase:** secondo le policy di Google.

## 8. Eliminazione dell'account

Dal Profilo, in "Informazioni e privacy", "Elimina account e dati" rimuove: i pin che hai pubblicato con le relative foto e le rivendicazioni; le conferme, le segnalazioni e i "non c'è più" che hai dato ad altri pin, che vengono ritirati (il conteggio pubblico scende di uno) insieme alle ricevute; infine l'account Firebase. Restano solo due contatori tecnici giornalieri (numero di invii e orario dell'ultimo), privi di contenuto e non più collegabili a te dopo l'eliminazione dell'account: se potessero essere cancellati, chiunque potrebbe azzerare i limiti anti-spam. Un pin già nascosto dalla comunità resta nascosto anche se il tuo voto viene ritirato. Se non hai più l'app, puoi chiedere l'eliminazione da https://emergentoremergency.github.io/lucky-prospector/delete-account-it.html o scrivendo a intervistaconleai@gmail.com. I dati locali si cancellano eliminando i ritrovamenti o disinstallando l'app. I suggerimenti non sono collegati all'account: possiamo cancellarli su richiesta se ci aiuti a individuarli.

## 9. Destinatari e trasferimenti

- **Google Ireland Ltd / Google LLC** (Firebase Authentication, Cloud Firestore, App Check, Play Integrity), in qualità di responsabile del trattamento secondo i termini Firebase. Il database Firestore si trova nella regione **`eur3` (Unione europea)**. Autenticazione, App Check e alcuni servizi di supporto possono trattare dati anche fuori dall'UE, in particolare negli Stati Uniti. In questi casi Google usa le Clausole contrattuali standard della Commissione europea e/o l'EU-U.S. Data Privacy Framework.
- **OpenFreeMap** (e l'eventuale CDN), per le tessere della mappa (sezione 5).
- Gli **altri utenti** vedono solo i documenti pubblici descritti nella sezione 4.2.

I dati non vengono venduti né usati per pubblicità.

## 10. I tuoi diritti

Nei limiti degli artt. 15–22 GDPR puoi chiedere accesso, rettifica, cancellazione, limitazione e portabilità dei dati, e opporti ai trattamenti basati sul legittimo interesse. Puoi revocare il consenso in qualunque momento ritirando i pin condivisi, senza effetti sui trattamenti precedenti. Scrivi a intervistaconleai@gmail.com. Hai anche diritto di proporre reclamo al **Garante per la protezione dei dati personali** (www.garanteprivacy.it) o all'autorità di controllo del paese in cui risiedi.

## 11. Minori

L'app non è rivolta a persone con meno di 14 anni, l'età da cui in Italia si può dare in autonomia il consenso ai servizi online (art. 2-quinquies, Codice privacy). Sotto questa età non si deve accedere né condividere senza il consenso di chi esercita la responsabilità genitoriale.

## 12. Permessi Android

- **Fotocamera** (obbligatoria): per la ricerca e le foto.
- **Posizione precisa/approssimativa** (facoltativa): per collocare il ritrovamento sulla tua mappa.
- **Internet e stato della rete:** per l'accesso, la mappa condivisa, le mappe e i suggerimenti.
- **Scrittura nella memoria** (solo Android 9 o precedenti): per copiare la foto nella galleria.

## 13. Modifiche

Pubblicheremo qui ogni modifica, aggiornando la data. Se una modifica è rilevante, te lo segnaleremo anche nell'app.
