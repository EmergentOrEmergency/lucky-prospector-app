# Adapter del modello on-device

`Detector` e `DetectionFrame` separano CameraX dal runtime ML. `FakeDetector` resta soltanto come fallback esplicitamente visibile quando il bundle reale manca o non supera le verifiche.

## Modello attivo

L'app usa `flc-yolox-tiny-v0.1.0-rc1`, scelto e congelato nel repository di laboratorio (privato). Il candidato primario e YOLOX-Tiny 640 dynamic-range INT8: framework Apache-2.0, 5,747,592 byte, SHA-256 `75d954cea0b77be875cacedc5928277ba028e5fa5fabd93c67f77dd9e3cf12cd`.

Il contratto validato e:

- input `float32 NHWC [1,640,640,3]`;
- ordine canali BGR, valori 0..255 senza normalizzazione;
- resize aspect-preserving in alto a sinistra, padding 114;
- output `[1,8400,8]`: `cx, cy, width, height, objectness` e tre score classe;
- classi dirette nell'ordine `clover_3`, `clover_4`, `clover_5_plus`;
- score `objectness * bestClassScore`, soglia 0,25;
- NMS class-agnostic, IoU 0,65;
- rimozione del letterbox prima di restituire box normalizzati.

`YoloxTfliteDetector` replica questo contratto; `LiteRtYoloxModelRunner` rifiuta forma o dtype diversi. `ModelBundleLoader` verifica prima dell'interprete sia l'hash del modello sia l'hash di `dataset-provenance.json`.

## Perche stretch e class_id + 1 non si applicano qui

Il reference `src/flc_lab/evaluation/tflite_inference.py` con stretch e `labels[class_id + 1]` appartiene al vecchio modello SSD `detect4clover`, che usa `TFLite_Detection_PostProcess`. Non e il reference della release YOLOX.

Il release gate YOLOX usa invece il contratto letterbox/BGR sopra, verificato su 316 immagini e sul Galaxy S23 Ultra. Applicargli stretch oppure lo shift della label produrrebbe risultati sbagliati. I due adapter restano quindi separati e i metadata impediscono di caricare un contratto con l'altro decoder.

## Il file del modello

Il modello `model.tflite` e incluso nel repository, in `app/src/main/assets/models/lucky-prospector/`, insieme a metadata, provenienza e attribuzioni. L'app ne verifica l'hash SHA-256 prima di caricarlo.

Una build senza il binario continua a funzionare in modalita demo e lo dichiara nella UI. Una build col binario ma con hash, provenienza o tensori errati fallisce il caricamento e non puo presentarsi come detector reale.

## Verifiche

I test unitari coprono metadata, mapping diretto delle classi, rimozione del letterbox e NMS. `YoloxModelInstrumentedTest` carica il bundle ed esegue una vera inferenza LiteRT sul dispositivo. Il benchmark completo e la parita numerica restano nel release gate del laboratorio.
