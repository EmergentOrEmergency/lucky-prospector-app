# Datenschutzerklärung – Lucky Prospector

**Stand:** 29. September 2026

Diese Information erfolgt nach Art. 13 der Verordnung (EU) 2016/679 („DSGVO“). Sie erklärt, welche Daten die Android-App Lucky Prospector (`com.luckyprospector.app`) verarbeitet, warum und wie lange. Maßgeblich ist die italienische Fassung; im Zweifel gilt sie.

## 1. Verantwortlicher

Daniele Bulgarelli, Reggio Emilia, Italien. Datenschutzkontakt: intervistaconleai@gmail.com.

## 2. Kurz gesagt

- Du kannst Klee ohne Konto und ohne Verbindung suchen und speichern.
- Fotos, Wünsche und dein genauer Standort bleiben auf deinem Handy.
- Online geht nur, was du auf der gemeinsamen Karte veröffentlichst: die Art des Fundes, ein auf mindestens 100 m gerundeter Bereich und, wenn du willst, ein zugeschnittenes Foto ohne GPS-Daten.
- Die App enthält keine Werbung und keine Analyse- oder Tracking-Werkzeuge.

## 3. Daten, die auf deinem Gerät bleiben

Der Verantwortliche erhält diese Daten nicht und kann sie nicht lesen.

- **Kamerabilder.** Während der Suche analysiert das Erkennungsmodell (TensorFlow Lite/LiteRT) die Kamerabilder auf dem Handy. Die Bilder werden weder gespeichert noch versendet. Deine Fotos werden nicht zum Training des Modells verwendet.
- **Funde.** Für jeden gespeicherten Fund speichert die App in einer lokalen Datenbank: Datum und Uhrzeit, Art (Glücksklee oder Kleefeld), deinen optionalen Wunsch, den genauen Standort mit seiner Genauigkeit (nur wenn du den Standortzugriff erlaubt hast), das Erkennungsergebnis (Klasse, Konfidenz, Modellversion), eine eventuelle Korrektur der Klasse und den Freigabestatus.
- **Fotos.** Jedes Foto wird im privaten Speicher der App gespeichert und **in die Galerie des Handys kopiert** („Pictures/Lucky Prospector“). Die Kopie in der Galerie ist für andere Apps mit Fotozugriff und für von dir aktivierte Backup-Dienste sichtbar. Sie kann nach der Deinstallation der App in der Galerie bleiben.
- **Sitzung und Cache.** Wenn du dich anmeldest, speichert das Firebase-SDK auf dem Handy die Sitzung und einen Cache der Daten der gemeinsamen Karte.

Die automatische Android-Sicherung ist für die App deaktiviert; diese Daten werden also nicht in die Cloud-Sicherung des Systems kopiert. Du kannst einen Fund in der App löschen: Dabei werden der Eintrag, das private Foto und die Kopie in der Galerie entfernt. War der Fund geteilt, wird auch der öffentliche Pin zurückgezogen.

## 4. Online verarbeitete Daten

### 4.1 Anmeldung mit Google (optional)

Die Anmeldung wird nur benötigt, um auf der gemeinsamen Karte zu veröffentlichen, sie anzusehen und Vorschläge zu senden. Sie erfolgt über Google (Credential Manager) und Firebase Authentication. Google erhält die Anmeldeanfrage und übermittelt uns eine Kennung deines Kontos. Firebase speichert eine Nutzerkennung (UID), deine E-Mail-Adresse, Namen und Foto deines Google-Profils, technische Anmeldedaten (Datum der Erstellung und der letzten Anmeldung) und Sitzungstokens. Die App zeigt deinen Namen und dein Foto nur dir, auf dem Profil-Bildschirm.

### 4.2 Gemeinsame Karte (nur wenn du teilen möchtest)

Ein Fund wird nur veröffentlicht, wenn du auf „Teilen“ tippst und nach einer Vorschau bestätigst. Das **öffentliche Dokument** ist für alle angemeldeten Nutzer sichtbar und enthält nur:

- die Art, die das Erkennungsmodell auf dem Handy bestimmt;
- den Standort, **gerundet auf die Mitte einer Rasterzelle von mindestens 100 m** (standardmäßig 150 m), und einen aus diesem gerundeten Punkt berechneten Geohash;
- das Datum der Veröffentlichung und der letzten Bestätigung;
- anonyme Zähler: Bestätigungen, Meldungen, „nicht mehr da“;
- ob ein Foto angehängt ist.

Das öffentliche Dokument enthält keine Identität, keinen Wunsch und keinen genauen Standort.

**Optionales Foto:** ein quadratischer Ausschnitt um den erkannten Klee, höchstens 384 px, aus den Pixeln neu kodiert. Es enthält keine EXIF-Metadaten: kein GPS, kein Datum, kein Handymodell. Der Ausschnitt verkleinert den Hintergrund, entfernt ihn aber nicht; deshalb bittet dich die App, das Bild vor dem Senden zu prüfen.

**Private Dokumente, die mit deinem Konto verknüpft sind.** Unter deiner UID werden gespeichert:

- eine „Inanspruchnahme“ für jeden veröffentlichten Pin, mit Erstellungsdatum, mit der du den Pin zurückziehen kannst;
- Belege deiner Bestätigungen, deiner Meldungen (mit Grund) und deiner „nicht mehr da“-Angaben;
- tägliche Zähler, die das Veröffentlichen auf 20 Pins pro Tag und Vorschläge auf 10 Nachrichten pro Tag begrenzen.

Andere Nutzer können diese Dokumente nicht sehen. **Der Verantwortliche kann über die Firebase-Konsole dein Konto mit den von dir veröffentlichten Pins und deinen Meldungen verknüpfen.** Im Sinne der DSGVO ist das Teilen daher für andere Nutzer anonym, für den Verantwortlichen aber pseudonym.

**Entfernen.** Du kannst einen veröffentlichten Pin jederzeit zurückziehen: Pin, Foto und deine Inanspruchnahme werden gelöscht. Pins laufen nicht von selbst ab. Nach 3 Missbrauchsmeldungen oder wenn 2 verschiedene Personen „nicht mehr da“ angeben, werden Pin und Foto für alle ausgeblendet. Das Dokument bleibt für die in Abschnitt 7 genannte Dauer gespeichert.

### 4.3 Vorschläge

Auf dem Profil-Bildschirm kannst du eine Nachricht mit folgenden Angaben senden: Kategorie, Text (höchstens 2000 Zeichen), ein optionaler Kontakt, App-Version, Sprache und Datum. Die Nachricht enthält weder deine UID noch deinen Namen. Allerdings wird in deinem privaten Bereich ein Tageszähler mit der Uhrzeit des letzten Versands aktualisiert. **Durch den Vergleich dieser Uhrzeit mit dem Datum der Nachricht könnte der Verantwortliche die Nachricht deinem Konto zuordnen.** Schreibe keine unnötigen personenbezogenen Daten in den Text.

### 4.4 Sicherheit (Firebase App Check)

Um zu prüfen, dass Anfragen von der echten App auf einem echten Gerät kommen, nutzt die App Firebase App Check mit Google Play Integrity. Google bewertet die Integrität von Gerät und App und stellt ein Token aus. Der Verantwortliche erhält keine Gerätedaten.

## 5. Karten (OpenFreeMap und MapLibre)

Die Karten zeichnet MapLibre auf dem Handy, mit Stil und Kacheln von OpenFreeMap (`tiles.openfreemap.org`). Der Anbieter erhält deine IP-Adresse und die angeforderten Kacheln und kann daher erkennen, **welchen Bereich du ansiehst**, auch auf deiner privaten Karte. Deine Funde und Markierungen erhält er nicht. Nach seinen eigenen Angaben speichert OpenFreeMap IP-Adressen nicht in den gewöhnlichen Protokollen, kann sie bei technischen Störungen bis zu 30 Tage aufbewahren und kann Cloudflare nutzen. Für Einzelheiten gilt seine Datenschutzerklärung. Ohne Verbindung bleibt eine vereinfachte lokale Karte verfügbar.

## 6. Zwecke und Rechtsgrundlagen

| Verarbeitung | Rechtsgrundlage |
|---|---|
| Anmeldung und Kontoverwaltung | Erfüllung des von dir angeforderten Dienstes (Art. 6 Abs. 1 lit. b) |
| Veröffentlichung auf der gemeinsamen Karte, mit Foto | Einwilligung, erteilt durch Bestätigen des Teilens und widerrufen durch Zurückziehen des Pins (Art. 6 Abs. 1 lit. a) |
| Inanspruchnahmen, Belege, Tageslimits, Schwellen zum Entfernen, App Check | Berechtigtes Interesse, Missbrauch und Spam zu verhindern und den Dienst zu schützen (Art. 6 Abs. 1 lit. f) |
| Vorschläge und etwaige Antwort | Berechtigtes Interesse, die App zu verbessern; der Kontakt ist optional (Art. 6 Abs. 1 lit. f) |
| Karten | Berechtigtes Interesse, die geöffnete Karte anzuzeigen (Art. 6 Abs. 1 lit. f) |
| Standort des Handys | Optionale Android-Berechtigung; die Angabe bleibt auf dem Gerät |

Es gibt keine automatisierte Entscheidung mit rechtlicher Wirkung. Das Erkennungsmodell entscheidet nur, wie ein Pin angezeigt wird (Glücksklee oder Kleefeld) und ob er geteilt werden kann.

## 7. Speicherdauer

- **Lokale Daten:** bis du den Fund löschst oder die App deinstallierst. Die Kopie in der Galerie bleibt, bis du sie löschst.
- **Konto und private Dokumente:** solange das Konto besteht. Sie werden mit dem Konto gelöscht, außer den technischen Zählern aus Abschnitt 8.
- **Öffentliche Pins und Fotos:** bis du sie zurückziehst oder dein Konto löschst. Nach Meldungen ausgeblendete Pins und Fotos: Sie werden spätestens 120 Tage nach dem Ausblenden gelöscht.
- **Vorschläge:** bis zu 24 Monate nach dem Versand, dann gelöscht.
- **Technische Protokolle von Google/Firebase:** nach den Richtlinien von Google.

## 8. Konto löschen

Auf dem Profil-Bildschirm entfernt „Konto und Daten löschen“ unter „Info und Datenschutz“: die von dir veröffentlichten Pins mit ihren Fotos und Inanspruchnahmen; die Bestätigungen, Meldungen und „nicht mehr da“-Angaben, die du bei anderen Pins abgegeben hast; sie werden zurückgenommen (der öffentliche Zähler sinkt um eins), zusammen mit ihren Belegen; zuletzt dein Firebase-Konto. Es bleiben nur zwei technische Tageszähler (Anzahl der Sendungen und Uhrzeit der letzten), ohne Inhalt und nach der Kontolöschung nicht mehr dir zuordenbar: Könnten sie gelöscht werden, könnte jeder die Spam-Limits zurücksetzen. Ein bereits von der Gemeinschaft ausgeblendeter Pin bleibt ausgeblendet, auch wenn deine Stimme zurückgenommen wird. Wenn du die App nicht mehr hast, kannst du die Löschung unter https://emergentoremergency.github.io/lucky-prospector/delete-account-de.html oder per E-Mail an intervistaconleai@gmail.com beantragen. Lokale Daten löschst du, indem du die Funde löschst oder die App deinstallierst. Vorschläge sind nicht mit dem Konto verknüpft: Wir können sie auf Anfrage löschen, wenn du uns hilfst, sie zu finden.

## 9. Empfänger und Übermittlungen

- **Google Ireland Ltd / Google LLC** (Firebase Authentication, Cloud Firestore, App Check, Play Integrity) als Auftragsverarbeiter nach den Firebase-Bedingungen. Die Firestore-Datenbank liegt in der Region **`eur3` (Europäische Union)**. Authentifizierung, App Check und einige unterstützende Dienste können Daten auch außerhalb der EU verarbeiten, insbesondere in den USA. In diesen Fällen stützt sich Google auf die Standardvertragsklauseln der Europäischen Kommission und/oder das EU-U.S. Data Privacy Framework.
- **OpenFreeMap** (und ein etwaiges CDN) für die Kartenkacheln (Abschnitt 5).
- **Andere Nutzer** sehen nur die in Abschnitt 4.2 beschriebenen öffentlichen Dokumente.

Deine Daten werden weder verkauft noch für Werbung verwendet.

## 10. Deine Rechte

Im Rahmen der Art. 15–22 DSGVO kannst du Auskunft, Berichtigung, Löschung, Einschränkung und Übertragbarkeit deiner Daten verlangen und Verarbeitungen auf Grundlage des berechtigten Interesses widersprechen. Du kannst deine Einwilligung jederzeit widerrufen, indem du deine geteilten Pins zurückziehst; frühere Verarbeitungen bleiben davon unberührt. Schreibe an intervistaconleai@gmail.com. Du hast außerdem das Recht, dich beim **Garante per la protezione dei dati personali** (www.garanteprivacy.it) oder bei der Aufsichtsbehörde deines Wohnsitzlandes zu beschweren (in Deutschland die Datenschutzbehörde deines Bundeslandes, in Österreich die Datenschutzbehörde, in der Schweiz der EDÖB).

## 11. Minderjährige

Die App richtet sich nicht an Personen unter 14 Jahren, dem Alter, ab dem man in Italien selbst in Online-Dienste einwilligen kann (Art. 2-quinquies des italienischen Datenschutzgesetzes). Sieht das Recht deines Landes ein höheres Alter vor (in Deutschland 16 Jahre), gilt dieses. Unter diesem Alter darf man sich nur mit Zustimmung eines Erziehungsberechtigten anmelden oder etwas teilen.

## 12. Android-Berechtigungen

- **Kamera** (erforderlich): für die Suche und die Fotos.
- **Genauer/ungefährer Standort** (optional): um einen Fund auf deiner Karte zu platzieren.
- **Internet und Netzwerkstatus:** für die Anmeldung, die gemeinsame Karte, die Kartenkacheln und Vorschläge.
- **Speicher schreiben** (nur Android 9 und älter): um Fotos in die Galerie zu kopieren.

## 13. Änderungen

Änderungen veröffentlichen wir hier mit aktualisiertem Datum. Bei wesentlichen Änderungen informieren wir dich auch in der App.
