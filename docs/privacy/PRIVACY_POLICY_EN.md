# Privacy Policy – Lucky Prospector

**Last updated:** 29 September 2026

This notice is provided under Art. 13 of Regulation (EU) 2016/679 ("GDPR"). It explains what data the Lucky Prospector Android app (`com.luckyprospector.app`) processes, why, and for how long. The Italian version is the reference text and prevails in case of doubt.

## 1. Controller

Daniele Bulgarelli, Reggio Emilia, Italy. Privacy contact: intervistaconleai@gmail.com.

## 2. In short

- You can search for and save clovers without an account and without a connection.
- Photos, wishes and your precise location stay on your phone.
- Only what you choose to publish on the shared map goes online: the type of finding, an area rounded to at least 100 m and, if you want, a cropped photo without GPS data.
- The app contains no ads and no analytics or tracking tools.

## 3. Data that stays on your device

The controller does not receive this data and cannot read it.

- **Camera images.** While you search, the recognition model (TensorFlow Lite/LiteRT) analyses camera frames on the phone. Frames are never stored or sent anywhere. Your photos are not used to train the model.
- **Findings.** For each saved finding the app keeps these in a local database: date and time, type (four-leaf clover or clover patch), your optional wish, the precise position and its accuracy (only if you granted location access), the recognition result (class, confidence, model version), any class correction, and the sharing status.
- **Photos.** Each photo is saved in the app's private storage and **copied to the phone's gallery** ("Pictures/Lucky Prospector"). The gallery copy is visible to other apps with photo access and to any backup service you have turned on. It may remain in the gallery after you uninstall the app.
- **Session and cache.** If you sign in, the Firebase SDK keeps your session and a cache of shared-map data on the phone.

Android automatic backup is disabled for the app, so this data is not copied to the system cloud backup. You can delete a finding in the app: this removes the record, the private photo and the gallery copy. If the finding was shared, the public pin is withdrawn as well.

## 4. Data processed online

### 4.1 Google sign-in (optional)

Signing in is needed only to publish on the shared map, to view it, and to send feedback. It works through Google (Credential Manager) and Firebase Authentication. Google receives the sign-in request and passes us an identifier for your account. Firebase stores a user ID (UID), your email address, your Google profile name and photo, technical sign-in data (creation and last sign-in dates) and session tokens. The app shows your name and photo only to you, on the Profile screen.

### 4.2 Shared map (only if you choose to share)

A finding is published only when you tap "Share" and confirm after seeing a preview. The **public document** can be seen by every signed-in user and contains only:

- the type, decided by the recognition model on the phone;
- the position **rounded to the centre of a grid cell of at least 100 m** (150 m by default), and a geohash computed from that rounded point;
- the publication date and the date of the last confirmation;
- anonymous counters: confirmations, reports, "not there any more";
- whether a photo is attached.

The public document contains no identity, no wish and no precise position.

**Optional photo:** a square crop around the recognised clovers, 384 px at most, re-encoded from the pixels. It carries no EXIF metadata: no GPS, date or phone model. The crop reduces the background but does not remove it, so the app asks you to check the image before you send it.

**Private documents linked to your account.** These are stored under your UID:

- a "claim" for each pin you published, with its creation date, which lets you withdraw the pin;
- receipts for your confirmations, your reports (with the reason) and your "not there any more" signals;
- daily counters that limit publishing to 20 pins a day and feedback to 10 messages a day.

Other users cannot see these documents. **The controller can link your account to the pins you published and to your reports through the Firebase console.** Under the GDPR, sharing is therefore anonymous to other users but pseudonymous to the controller.

**Removal.** You can withdraw a pin you published at any time: the pin, its photo and your claim are deleted. Pins do not expire on their own. After 3 abuse reports, or once 2 different people say "not there any more", a pin and its photo are hidden from everyone. The document itself stays stored (see section 7).

### 4.3 Feedback

From the Profile screen you can send a message with this information: category, text (up to 2000 characters), an optional contact, app version, language and date. The message does not contain your UID or your name. However, a daily counter in your private area is updated with the time you last sent a message. **By comparing that time with the message date, the controller could link the message to your account.** Please do not put unnecessary personal data in the text.

### 4.4 Security (Firebase App Check)

To check that requests come from the genuine app on a genuine device, the app uses Firebase App Check with Google Play Integrity. Google assesses the integrity of the device and the app and issues a token. The controller receives no device data.

## 5. Maps (OpenFreeMap and MapLibre)

Maps are drawn on your phone by MapLibre, using a style and tiles downloaded from OpenFreeMap (`tiles.openfreemap.org`). The provider receives your IP address and the tiles you request, so it can tell **which area you are looking at**, including on your private map. It does not receive your findings or markers. According to the information it publishes, OpenFreeMap does not log IP addresses in ordinary logs, may keep them for up to 30 days during technical incidents, and may use Cloudflare. Its own policy governs the details. A simplified local map remains available offline.

## 6. Purposes and legal bases

| Processing | Legal basis |
|---|---|
| Sign-in and account management | Performance of the service you request (Art. 6(1)(b)) |
| Publishing on the shared map, photo included | Consent, given when you confirm sharing and withdrawn by withdrawing the pin (Art. 6(1)(a)) |
| Claims, receipts, daily limits, removal thresholds, App Check | Legitimate interest in preventing abuse and spam and in protecting the service (Art. 6(1)(f)) |
| Feedback and any reply | Legitimate interest in improving the app; the contact field is optional (Art. 6(1)(f)) |
| Maps | Legitimate interest in showing the map you open (Art. 6(1)(f)) |
| Device location | Optional Android permission; the data stays on the device |

There is no automated decision-making with legal effects. The recognition model only decides how a pin is shown (four-leaf clover or patch) and whether it can be shared.

## 7. Retention

- **Local data:** until you delete the finding or uninstall the app. The gallery copy stays until you delete it.
- **Account and private documents:** for as long as the account exists. They are deleted when you delete your account, except the technical counters described in section 8.
- **Public pins and photos:** until you withdraw them or delete your account. Pins and photos hidden after reports: they are deleted within 120 days of being hidden.
- **Feedback:** up to 24 months after they are sent, then deleted.
- **Google/Firebase technical logs:** according to Google's policies.

## 8. Deleting your account

On the Profile screen, under "Info and privacy", "Delete account and data" removes: the pins you published with their photos and claims; the confirmations, reports and "not there any more" signals you gave to other pins, which are withdrawn (the public count drops by one) together with their receipts; and finally your Firebase account. Only two technical daily counters remain (number of submissions and time of the last one). They hold no content and can no longer be linked to you once the account is deleted; if they could be deleted, anyone could reset the anti-spam limits. A pin already hidden by the community stays hidden even when your vote is withdrawn. If you no longer have the app, you can request deletion at https://emergentoremergency.github.io/lucky-prospector/delete-account.html or by writing to intervistaconleai@gmail.com. To erase local data, delete your findings or uninstall the app. Feedback messages are not linked to your account: we can delete them on request if you help us identify them.

## 9. Recipients and transfers

- **Google Ireland Ltd / Google LLC** (Firebase Authentication, Cloud Firestore, App Check, Play Integrity), acting as processor under the Firebase terms. The Firestore database is in the **`eur3` (European Union)** region. Authentication, App Check and some supporting services may also process data outside the EU, notably in the United States. In those cases Google relies on the European Commission's Standard Contractual Clauses and/or the EU-U.S. Data Privacy Framework.
- **OpenFreeMap** (and any CDN it uses), for map tiles (section 5).
- **Other users** see only the public documents described in section 4.2.

Your data is not sold or used for advertising.

## 10. Your rights

Within the limits of Arts. 15–22 GDPR you can ask to access, rectify, erase, restrict and port your data, and you can object to processing based on legitimate interest. You can withdraw your consent at any time by withdrawing your shared pins; this does not affect processing that already took place. Write to intervistaconleai@gmail.com. You also have the right to complain to the **Garante per la protezione dei dati personali** (www.garanteprivacy.it) or to the supervisory authority of the country where you live.

## 11. Children

The app is not directed at people under 14, the age from which people in Italy can consent to online services on their own (Art. 2-quinquies of the Italian Privacy Code). Below that age, users must not sign in or share without the consent of a parent or guardian.

## 12. Android permissions

- **Camera** (required): for searching and taking photos.
- **Precise/approximate location** (optional): to place a finding on your map.
- **Internet and network state:** for sign-in, the shared map, map tiles and feedback.
- **Write storage** (Android 9 and earlier only): to copy photos to the gallery.

## 13. Changes

We will publish any change here and update the date. If a change is significant, we will also tell you in the app.
