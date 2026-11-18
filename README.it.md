# Toque IT — Monsieur Cuisine Connect in italiano

> **Fork indipendente** di [Wuki60/MrCuisineVibeConnect](https://github.com/Wuki60/MrCuisineVibeConnect), dedicato alla localizzazione italiana e, in futuro, all'integrazione **Home Assistant tramite MQTT** del **Monsieur Cuisine Connect SKMC 1200**. Non è il progetto originale, né è affiliato a Lidl, SilverCrest o al produttore. Licenza MIT e attribuzioni originali mantenute.

## Stato delle funzioni

| Funzione | Stato del fork |
|---|---|
| Menu e comandi in italiano | **Implementati**: `app/src/main/assets/lang/it.json`, 417 stringhe |
| Avvisi e messaggi di sicurezza in italiano | **Tradotti**; verifica della resa su Android ancora necessaria |
| Ricette ufficiali in italiano | **Confermate nella Demo**: sincronizzazione e visualizzazione in italiano verificate dall'utente |
| Entità Home Assistant effettivamente esposte | **9 definite, pubblicazione sperimentale implementata**; comparsa in HA da verificare |
| Schema MQTT Discovery e payload telemetrico | **Implementati nel core**; non ancora connessi alla macchina o a un broker |
| Client MQTT e pubblicazione reale | **Implementati in versione sperimentale**, da validare con broker e APK |
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

## Installazione sul robot: piano preventivo

Prima di qualsiasi modifica leggere [Piano di installazione e ripristino per SKMC 1200](docs/INSTALLAZIONE-IT-SICURA.md). Il firmware 4922 del dispositivo di destinazione non è stato ancora validato con TWRP/root. Le procedure upstream modificano anche file di sistema Android, quindi un backup di sole partizioni boot/recovery potrebbe non ripristinare ogni modifica. Nessun root o flash è stato eseguito.

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
- `app/src/main/kotlin/app/toque/mqtt/MqttPublisher.kt`: client MQTT 3.1.1 QoS 0 in sola pubblicazione; TLS opzionale e LWT `offline` retained.
- `app/src/main/kotlin/app/toque/mqtt/HaTelemetryService.kt`: servizio indipendente che acquisisce gli stati via `MachineClient`, pubblica Discovery/telemetria e marca offline i campioni non più validi; lavora su un thread separato.
- **Da verificare**: build e prova con broker reale, disconnessioni, reconnect, persistenza, comportamento quando l’interfaccia viene chiusa.

La disponibilità pubblicata dovrà essere `offline` in caso di disconnessione o perdita di dati. Il modulo definito nel core non pubblica autonomamente lo stato `online`: lo farà il futuro servizio di integrazione solo quando avrà una misura valida e recente.

## Manopola fisica del Connect

Il codice originale Toque gestisce gli eventi `KeyEvent` e le rotazioni `MotionEvent.ACTION_SCROLL` della manopola, con mappatura configurabile. Nella cottura manuale: toccare la scheda **Durata**, **Temperatura** o **Velocità** per selezionare il parametro; ruotare la manopola per cambiarlo; premere per avviare e, durante il funzionamento, mettere in pausa. L'uso sul dispositivo reale richiede una verifica dei codici tasto e dei comportamenti. La pressione non passa automaticamente da un parametro all'altro nell'implementazione attuale.

## Come attivare MQTT in modalità Demo

1. Installa l'APK generato dal workflow **Validate Italian fork** soltanto dopo una build verde, su **telefono Android in modalità Demo**.
2. Vai su **Impostazioni → Home Assistant · MQTT** e inserisci IP del broker, porta, utente e password.
3. **TLS attivo (predefinito)** usa normalmente la porta **8883** e richiede un certificato TLS valido per l'host. La porta **1883 senza TLS** va usata soltanto su una LAN fidata: username e password viaggiano in chiaro.
4. Attiva *Pubblica sensori su Home Assistant* e premi *Salva configurazione MQTT*.
5. Verifica che Home Assistant disponga dell'integrazione MQTT, e che il broker consenta di pubblicare sui topic `homeassistant/#` e `toque_it/mcc/#`.
6. Attendi la comparsa del dispositivo *Monsieur Cuisine Connect*. Le nove entità rappresentano soltanto la **macchina simulata del telefono**. Non attivare Toque sul robot durante questa prova.

L'implementazione attuale usa il client ID `toque-it-mcc` e ID entità fissi: **eseguire una sola istanza alla volta**, altrimenti le due app si disconnetteranno a vicenda. Il publisher non si sottoscrive a topic MQTT e non accetta comandi di cottura. Le credenziali sono memorizzate nelle preferenze private Android, non nel repository; per ambienti non fidati è necessario TLS.

**Limiti sperimentali:** il protocollo QoS 0 non garantisce la consegna di ogni singolo messaggio; la connessione e l'eventuale LWT dipendono dal broker. Occorrono ancora test reali sul sistema di Home Assistant e test di affidabilità prima di uso in cucina.

## Diagnostica MQTT (novembre 2026)

Nelle impostazioni **Home Assistant · MQTT** compare il campo di stato e il pulsante **Aggiorna stato MQTT**. Serve a diagnosticare il problema riscontrato con la prima Demo, in cui nessun messaggio `toque_it/mcc/#` era visibile nel broker.

I messaggi distinguono:
- **connessione TCP rifiutata**: host, porta, firewall o servizio Mosquitto;
- **errore TLS**: certificato non attendibile, nome host errato o TLS usato sulla porta senza TLS;
- **autenticazione MQTT rifiutata**: credenziali o autorizzazioni sul broker;
- **Discovery inviata**: connessione stabilita, configurazioni inviate, ma telemetria non ancora valida;
- **MQTT OK: stato macchina pubblicato**: un messaggio di stato è stato consegnato allo stack di rete locale (non equivale a conferma di ricezione del broker con QoS 0).

Lo stato mostra soltanto categorie di errori; non registra la password né la stampa nella schermata. Il broker e l'integrazione MQTT di Home Assistant **non devono essere rimossi o riconfigurati per questo test**.

Per verificare, usare soltanto **MQTT → Configura → Ascolta un topic** e il filtro `toque_it/mcc/#`. La pagina Zigbee2MQTT non mostrerà Toque: è un client MQTT non Zigbee.

## Controllo remoto Home Assistant: rimandato

**Il ramo principale `main` è intenzionalmente in sola lettura**: pubblica nove sensori via MQTT Discovery e non accetta topic di comando. L'utente ha verificato il funzionamento della telemetria in Home Assistant.

Il lavoro sperimentale su Pausa/Arresta Demo è conservato separatamente nel ramo [`esperimenti-controllo-ha`](https://github.com/samumar82/MrCuisineVibeConnect/tree/esperimenti-controllo-ha), non è parte del ramo principale e non deve essere utilizzato sul robot reale.

## Verifica ricette italiane (18 novembre 2026)

Test nella Demo Android: l'utente ha scaricato il catalogo ufficiale e verificato che le ricette sono visualizzate in italiano. Il client usa `Accept-Language: it-IT` tramite `McOnline.languageFor("it")`. **Non occorre tradurre automaticamente il catalogo ufficiale**: il contenuto viene recuperato dalla fonte già localizzata. Il catalogo non è incluso in questo repository.

È stata aggiunta una protezione per riconoscere anche la dicitura italiana *farfalla* o *frusta* nella conversione degli accessori indicati dalle ricette. Non vengono modificati tempi, temperature o velocità. È comunque obbligatoria la revisione dei passaggi importati prima di azionare una macchina fisica.

## Baseline stabile

Il ramo principale è stato riportato ai file MQTT in sola lettura dell'ultima revisione con CI verde e Demo confermata dall'utente, mantenendo le successive modifiche locali per l'italiano delle ricette. La compilazione del nuovo commit di ripristino deve essere verificata nel workflow `Validate Italian fork`.
