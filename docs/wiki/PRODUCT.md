# Visione di prodotto

## Promessa

Lucky Prospector aiuta una persona a esplorare un prato, individuare un probabile quadrifoglio e conservarne il ricordo con foto, posizione e un desiderio privato.

## Flusso principale

1. L'utente apre la ricerca e concede fotocamera e, facoltativamente, posizione.
2. La camera analizza dal vivo senza caricare frame in rete.
3. L'interfaccia guida l'utente: avvicinarsi, allontanarsi, rallentare, aumentare luce o tenere fermo il telefono.
4. Una detection stabile evidenzia il candidato.
5. L'utente scatta e conferma oppure corregge il risultato.
6. L'app salva la foto sia nel proprio archivio sia nella galleria di sistema, insieme a punto GPS e desiderio facoltativo.
7. Il ritrovamento appare nella galleria interna e sulla mappa.

## Tipi di ritrovamento

- `four_leaf_clover`: quadrifoglio confermato o probabile.
- `clover_patch`: presenza di un prato/gruppo di trifogli, rappresentata da un singolo punto.
- `rejected_detection`: previsione rifiutata dall'utente, conservabile localmente per un futuro invio volontario.

## MVP Android

Include camera live, guida contestuale, detection, conferma/correzione, foto, desiderio, GPS, archivio interno, copia nella galleria e mappa dei marker. Non richiede account né backend.

## Evoluzione cloud

Login Google facoltativo abilita backup, ripristino dopo reinstallazione o cambio telefono e, successivamente, condivisione pubblica. Desideri e coordinate precise non diventano pubblici automaticamente.

L'accesso non e mai richiesto per cercare un quadrifoglio o per salvarlo in locale: serve solo per pubblicare sulla mappa condivisa. E cio che rende strutturale, e non dichiarativa, la promessa offline-first.

## Mappa condivisa

Ogni ritrovamento e privato per impostazione predefinita; la condivisione si sceglie **per singolo ritrovamento**, mai con un interruttore globale. Cio che viene pubblicato e una posizione arrotondata a una cella di griglia e il tipo di ritrovamento: nessun nome, nessun desiderio, nessuna coordinata precisa.

I pin condivisi sono anonimi verso gli altri utenti, ma restano risalibili dall'amministratore del progetto, perche un abuso deve poter essere perseguito.

Perimetro, motivazioni e funzioni collaborative — decadimento dei pin, conferme, segnalazioni, condivisione esterna — sono in [SOCIAL.md](SOCIAL.md).

## Marketing

Nome di lavoro: **Lucky Prospector**. Posizionamento: esplorazione naturalistica e scoperta, evitando codici visivi da gioco d'azzardo. Descrizione breve candidata: **Find four-leaf clovers**. Prima della pubblicazione vanno verificati store, domini, social e marchi.

