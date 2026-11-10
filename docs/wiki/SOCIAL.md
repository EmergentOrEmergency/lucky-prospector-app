# Mappa condivisa e funzioni sociali

Perimetro e motivazioni delle funzioni collaborative. Le decisioni corrispondenti sono ADR-014..ADR-020 in [DECISIONS.md](DECISIONS.md); l'implementazione vive in `domain/GeoCoarsening.kt`, `domain/SharedSighting.kt` e `firestore.rules`.

## Principio guida

I dati condivisi non si richiamano indietro. Ogni funzione qui descritta e stata valutata su una domanda sola: **cosa rivela di una persona quando funziona come previsto**, non solo quando qualcosa va storto.

## Anonimato e tracce

I pin pubblici non portano identita (ADR-015). Il motivo non e il nome in se: e che **un identificativo stabile cuce insieme i pin di una persona in una traccia**, e una traccia rivela abitudini e residenza anche quando ogni singolo punto e arrotondato. L'arrotondamento a 150 m protegge il pin, non la sequenza.

Questo e anonimato **verso gli altri utenti**. La rivendicazione privata sotto `users/{uid}/shared_claims/{id}` collega comunque ogni pin al suo autore, e l'amministratore del progetto puo risalirci. Senza quel collegamento, "anonimo" avrebbe significato "impunito" e un recidivo non sarebbe bloccabile.

Un nome utente locale resta utile per l'esperienza personale. Se verra introdotto: parole casuali da una lista, mai derivato dall'email (ADR-018). Un nome buffo assegnato d'ufficio viene tenuto; un campo vuoto invita a scriverci il proprio nome vero.

## Freschezza e "non c'è più"

Un prato di quadrifogli puo durare anni, ma puo anche essere falciato, seccare o sparire sotto un cantiere. Un luogo sparito non e un abuso: e un fatto, e per questo resta separato dalle segnalazioni (ADR-020).

I pin **non scadono mai** (ADR-025). La freschezza e solo informazione visiva: "visto da 3 persone nell'ultimo mese" contro "nessuna conferma da 8 mesi", con i secondi disegnati piu tenui. Un pin esce dalla mappa solo quando **due persone diverse** dicono "non c'è più". Il documento resta in Firestore con `hidden: true` e non viene piu servito a nessuno. Chi l'ha detto resta in una ricevuta privata; sul pin pubblico compare solo il contatore `goneCount`.

E anche la funzione collaborativa migliore del gruppo, perche da agli altri qualcosa da **fare** oltre che da guardare, senza alcun incentivo a esporsi.

Le conferme non devono diventare un identificativo: chi conferma non compare, si vede solo il conteggio.

## Foto condivise

Attive dal 2026-09-26 (ADR-028): ritaglio attorno ai trifogli rilevati, ricodificato senza EXIF, mostrato all'utente prima dell'invio, con il tipo deciso dal classificatore. I due requisiti qui sotto sono rispettati così: il primo dalla ricodifica, verificata da un test strumentato; il secondo dal ritaglio e dall'anteprima obbligatoria.

Il rischio maggiore della condivisione non sono le coordinate, sono le foto. Due requisiti non negoziabili:

1. **Rimuovere l'EXIF prima della pubblicazione.** Una foto scattata col telefono contiene il GPS esatto. Pubblicarla annulla in un colpo solo tutto l'arrotondamento: posizione protetta nel documento e regalata al metro nell'allegato.
2. **Avvisare sullo sfondo.** Un quadrifoglio si fotografa da vicino, ma nell'inquadratura entrano cancelli, targhe, numeri civici, persone. Un avviso esplicito prima della prima condivisione, non un tooltip.

Il desiderio non accompagna mai una foto condivisa.

## Segnalazioni

Solo per contenuti non pertinenti, informazioni false o ingannevoli, spam e contenuti offensivi; non per i prati falciati o rimossi, per cui c'e "non c'è più" (ADR-025).

La data completa della fonte e visibile nel dettaglio del pin. Ogni account puo inviare una sola segnalazione per pin e l'autore non puo segnalare il proprio. Motivo, data e account restano nella ricevuta privata `users/{uid}/reports/{sightingId}`; sul documento pubblico compare soltanto il contatore anonimo `reportCount`.

A **tre segnalazioni indipendenti** il pin si nasconde da solo in attesa di revisione. La soglia evita che una singola contestazione oscuri un luogo, ma non lascia la moderazione interamente manuale. Il batch atomico e le regole `exists`/`existsAfter` impediscono il doppio voto dello stesso account anche a un client modificato.

Da sapere prima di attivarla: contenuti generati da utenti significa che qualcuno deve guardare la coda, e prima o poi si trova davanti qualcosa che non voleva vedere, con l'obbligo di agire. Finche l'app resta nella cerchia di amici il rischio e teorico; il giorno che si apre, non lo e piu.

## Condivisione esterna e inviti

Lo share sheet di sistema con un link profondo al pin copre WhatsApp, Instagram, Telegram e tutto il resto senza integrare SDK di terze parti.

Si condivide **sempre la posizione arrotondata**, mai quella precisa; da quel momento in poi non la si controlla piu.

Gli inviti agli amici Facebook **non sono realizzabili**: le API di App Invites sono state ritirate nel 2018 e `invitable_friends` e riservata ai giochi. Non e una questione di sforzo. Gli inviti tramite rubrica richiedono `READ_CONTACTS`, permesso costoso in fiducia e di resa modesta: da evitare.

## Coinvolgimento senza smontare la privacy

**Qualunque classifica che premi il numero di ritrovamenti spinge a condividere di piu e piu precisamente**, cioe esattamente contro il modello descritto qui. Gamification e privacy tirano in direzioni opposte e vince quella progettata meglio, quindi le classifiche competitive sono escluse.

Alternative che tirano dalla stessa parte:

- **Conferme** (sopra): collaborazione pura, nessun incentivo a esporsi.
- **Collezione personale**: stagioni, primo dell'anno, serie consecutive. Si compete con se stessi.
- **Bacheca dei desideri senza posizione**: il campo `wish` esiste gia e resta sul telefono. Un conteggio aggregato — "1.284 desideri espressi" — da il lato emotivo a rischio zero, perche non c'e nulla da geolocalizzare.
- **Zone invece di punti**: "il Parco Ducale ha 12 ritrovamenti" e socialmente soddisfacente e strutturalmente piu sicuro di dodici pin individuali.
