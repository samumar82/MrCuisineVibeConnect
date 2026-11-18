# Toque IT — Monsieur Cuisine Connect in italiano

> **Fork indipendente** di [Wuki60/MrCuisineVibeConnect](https://github.com/Wuki60/MrCuisineVibeConnect), dedicato alla localizzazione italiana e, in futuro, all'integrazione **Home Assistant tramite MQTT** del **Monsieur Cuisine Connect SKMC 1200**. Non è il progetto originale, né è affiliato a Lidl, SilverCrest o al produttore. Licenza MIT e attribuzioni originali mantenute.

## Stato delle funzioni

| Funzione | Stato del fork |
|---|---|
| Menu e comandi in italiano | **Implementati**: `app/src/main/assets/lang/it.json`, 417 stringhe |
| Avvisi e messaggi di sicurezza in italiano | **Tradotti**; verifica della resa su Android ancora necessaria |
| Ricette ufficiali in italiano | **Non ancora garantite**: origine e lingua del catalogo devono essere verificate |
| Entità Home Assistant effettivamente esposte | **Non ancora disponibili**; lo schema ne descrive 9 |
| Schema MQTT Discovery e payload telemetrico | **Implementati nel core**; non ancora connessi alla macchina o a un broker |
| Client MQTT e pubblicazione reale | **Non implementati** |
| APK italiano verificato | **Non ancora compilato o collaudato** |
| Root/installazione sul robot | **Non eseguiti** |

Questa distinzione è importante: il file italiano non implica automaticamente che titoli, ingredienti e istruzioni delle ricette siano italiani, e **Toque non espone attualmente entità Home Assistant** in questo fork.

## Differenze rispetto al progetto originale

**Progetto originale Toque (Wuki60):** interfaccia sostitutiva per il Connect, gestisce motore, riscaldamento, bilancia, programmi, ricerca e cottura guidata; supporta francese e inglese e sincronizza ricette. Il suo obiettivo principale è migliorare l'esperienza sul display del robot.

**Questo fork italiano:** aggiunge una localizzazione completa dei testi dell'interfaccia e ha come obiettivo successivo esportare **soltanto telemetria** a Home Assistant, mantenendo **controllo fisico e protezioni di sicurezza esclusivamente sul robot**. Nessun comando remoto di avvio, arresto, temperatura, velocità o Turbo è previsto.

Non è una sostituzione ufficiale o un fork approvato dall'autore. Le modifiche upstream di sicurezza andranno mantenute allineate.

## Italiano dell'interfaccia

Il file `app/src/main/assets/lang/it.json` traduce tutte le 417 chiavi dell'inglese presente al momento della creazione del fork. In **Impostazioni › Lingua** selezionare *Italiano*; con lingua di sistema italiana può essere selezionato automaticamente.

I segnaposto `%s`, `%d`, `%%` sono preservati. Non sono stati alterati limiti, soglie o logiche che azionano motore e riscaldamento. Prima di utilizzare il robot è comunque necessaria una revisione a schermo dei messaggi critici e dei testi che potrebbero risultare troppo lunghi.

## Ricette: distinzione fra contenuto e comandi

Le **ricette ufficiali non sono incluse** in questo repository. Toque le ottiene separatamente, oppure le importa dal database dell'app originale. Il codice di importazione locale attualmente usa anche un'indicazione di lingua francese (`OriginalDbMapper.map(tables, "fr")`): non va sostituita indiscriminatamente con `it` finché non è verificata la semantica del campo e il catalogo effettivamente disponibile.

La roadmap per le ricette prevede:
1. verificare se la fonte ufficiale offre già contenuti in italiano e con quali parametri;
2. selezionare il catalogo italiano **solo quando supportato** dalla fonte;
3. eventualmente tradurre per uso locale titoli, ingredienti e passaggi testuali, con controllo delle quantità, allergeni e terminologia;
4. lasciare **immutati** dati strutturati di tempo, temperatura, direzione, velocità e configurazione dei passaggi; nessuna traduzione automatica deve modificare i comandi della macchina.

L'eventuale riuso e redistribuzione dei testi delle ricette deve rispettare i diritti dei rispettivi titolari.

## Obiettivo Home Assistant: MQTT read-only

Architettura prevista, **non ancora operativa**. Sono stati aggiunti lo schema JSON, nove definizioni MQTT Discovery e test automatici, il collegamento ai dati di Toque è stato preparato con un adattatore che valida l'età del campione; mancano client MQTT reale e pubblicazione:

```text
Controller motore / bilancia
          │ seriale 4800 baud
          ▼
Toque: un solo proprietario della seriale
          │ stati con timestamp e controllo validità
          ▼
Pubblicatore MQTT read-only ───► Broker MQTT ───► Home Assistant
                                  MQTT Discovery
```

Entità desiderate: temperatura reale e impostata, RPM, velocità, peso, stato motore e riscaldamento, coperchio, errori, disponibilità. Ricetta attiva e tempo residuo richiederanno la lettura dello stato applicativo, non del solo protocollo a 27 byte.

**Vincoli di sicurezza prima della pubblicazione reale:**
- pubblicare solo stati, senza topic di comando o esecuzione di messaggi MQTT in ingresso;
- pubblicatore non bloccante e isolato dal ciclo di controllo; perdita Wi-Fi/broker non deve alterare il funzionamento del robot;
- disponibilità e scadenza dei campioni, non presentare misure vecchie come attuali;
- protezione delle credenziali MQTT; nessuna password nel repository;
- test con il simulatore e review del codice prima delle prove sul robot;
- niente lettori seriali concorrenti.

## Build e test

Il progetto usa Gradle/Android e richiede un ambiente Android SDK compatibile:

```bash
./gradlew :core:test
./gradlew assembleDebug
```

L'APK deve essere testato **in modalità Demo prima del root o del flashing**. La presenza della traduzione nel repository non certifica ancora il successo dei test e della build.

La procedura di root può modificare le partizioni di avvio del tablet Android integrato. Prima di intervenire sul dispositivo è necessario un backup verificato e una procedura di ripristino. Per dettagli e prerequisiti vedere le [istruzioni upstream](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/docs/INSTALLATION.md). Questo fork **non** autorizza a saltare alcun controllo di sicurezza.

## Riferimenti e attribuzione

- [Toque upstream](https://github.com/Wuki60/MrCuisineVibeConnect) — Wuki60 e contributori, **MIT**
- [Monsieur Cuisine Developer SDK](https://github.com/antrea/monsieur-cuisine-developer-sdk) — protocollo documentato, **MIT**
- [Licenza del presente repository](LICENSE)

**Nota:** le modifiche locali sono sperimentali, senza garanzia di sicurezza, accuratezza o compatibilità. Nessun APK è stato testato sul robot come parte delle modifiche qui descritte.

## Stato sviluppo MQTT (fase iniziale)

- `core/src/main/kotlin/app/toque/core/mqtt/HaMqttSchema.kt`: conversione di campioni validi in JSON, nove entità e configurazioni Discovery; **non apre connessioni e non controlla la macchina**.
- `core/src/test/kotlin/app/toque/core/mqtt/HaMqttSchemaTest.kt`: test per JSON, assenza di `command_topic` e freschezza dei campioni.
- `core/src/main/kotlin/app/toque/core/mqtt/HaSnapshotAdapter.kt`: mappa gli snapshot reali/simulati di Toque sui campi telemetrici, scartando campioni assenti o vecchi. Test in `HaSnapshotAdapterTest.kt`.
- **Da sviluppare**: coda non bloccante; client MQTT autenticato con LWT; impostazioni di rete; pubblicazione effettiva e prove con un broker.

La disponibilità pubblicata dovrà essere `offline` in caso di disconnessione o perdita di dati. Il modulo definito nel core non pubblica autonomamente lo stato `online`: lo farà il futuro servizio di integrazione solo quando avrà una misura valida e recente.

## Manopola fisica del Connect

Il codice originale Toque gestisce gli eventi `KeyEvent` e le rotazioni `MotionEvent.ACTION_SCROLL` della manopola, con mappatura configurabile. Nella cottura manuale: toccare la scheda **Durata**, **Temperatura** o **Velocità** per selezionare il parametro; ruotare la manopola per cambiarlo; premere per avviare e, durante il funzionamento, mettere in pausa. L'uso sul dispositivo reale richiede una verifica dei codici tasto e dei comportamenti. La pressione non passa automaticamente da un parametro all'altro nell'implementazione attuale.
