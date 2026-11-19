# Toque IT — Monsieur Cuisine Connect in italiano + MQTT Home Assistant

**Fork indipendente** di [Wuki60/MrCuisineVibeConnect](https://github.com/Wuki60/MrCuisineVibeConnect), destinato al SilverCrest Monsieur Cuisine Connect SKMC 1200 (Android 6). Non ufficiale, licenza MIT, nessuna affiliazione con Lidl, SilverCrest o Hoyer.

**[Installazione rapida](#installazione-rapida-con-backup-sicuro)** · **[Differenze](#differenze-dalloriginale)** · **[9 entità Home Assistant](#home-assistant-mqtt-9-entità)** · **[Verifiche e limiti](#verifiche-e-limiti)**

> **Sicurezza**: Toque controlla motore e riscaldamento. Non usare durante modifiche o backup, non aggirare protezioni, non aprire il coperchio né estrarre il boccale con lame in movimento. Il software non è certificato dal costruttore.

## Differenze dall'originale

Confronto con [Wuki60](https://github.com/Wuki60/MrCuisineVibeConnect), novembre 2026:

| Funzione | Progetto originale Wuki60 | Questo fork Toque IT |
|---|---|---|
| Lingue interfaccia | Francese e inglese | **Italiano** aggiunto (417 messaggi), oltre a francese/inglese |
| Ricette | Database Lidl e sincronizzazione ufficiale | Stesso sistema; interfaccia tradotta, **testi delle ricette nella lingua offerta dalla fonte** |
| Home Assistant | Nessun pubblicatore MQTT | **9 entità MQTT Discovery in sola lettura**, realmente presenti in HA |
| Firmware MCU 4922 | Polarità standard del bit presenza boccale | **Polarità invertita solo per 4922**, testata sul boccale reale |
| Avvio e passaggio a Lidl | Avvio Toque e ritorno a Lidl | Boot rispetta la scelta utente; Toque configurabile come HOME Android |
| Magisk 31 | Autorizzazione iniziale | Root **permanente e silenzioso dopo cold boot**, logging mantenuto |
| Aggiornamenti | Release upstream | **Release del fork**, senza reindirizzamenti ad APK upstream |
| Backup e installazione root già presente | Procedure ADB manuali | Installazione rapida con vecchia APK e dati cifrati prima dell'upgrade |

L'interfaccia italiana è nel file [it.json](app/src/main/assets/lang/it.json). I segnaposto delle traduzioni vengono verificati dalla CI. **La lingua dell'interfaccia non implica la traduzione automatica di tutte le ricette**: dati di tempo, temperatura, velocità, senso di rotazione e sequenze non vengono tradotti né modificati.

## Home Assistant: MQTT, 9 entità

**Verificato sul robot reale e in Home Assistant**: le nove entità vengono registrate tramite MQTT Discovery e pubblicano telemetria letta dalla base motore.

| Nome | Tipo | Proprietà MQTT |
|---|---|---|
| Temperatura reale | sensore | actual_temperature_c |
| Temperatura impostata | sensore | target_temperature_c |
| Velocità reale RPM | sensore | actual_rpm |
| Livello velocità | sensore | speed_level |
| Peso in grammi | sensore | weight_g |
| Motore attivo | sensore binario | motor_running |
| Riscaldamento attivo | sensore binario | heater_running |
| Coperchio aperto | sensore binario | lid_open |
| Codice errore | sensore | board_error_code |

La pubblicazione usa i topic **toque_it/mcc/state** e **toque_it/mcc/availability** e i discovery **homeassistant/sensor** e **homeassistant/binary_sensor**. I campioni non validi o troppo vecchi vengono scartati; la disponibilità viene marcata offline. Non esistono command_topic, sottoscrizioni o comandi MQTT in ingresso: **nessuno può avviare motore o riscaldamento tramite Home Assistant**. Il controllo rimane sul robot.

Configurare broker, utente, password e TLS da *Impostazioni → Home Assistant · MQTT*. Non condividere password nei log; su porta 1883 senza TLS le credenziali transitano in chiaro sulla LAN. Una sola istanza con client ID **toque-it-mcc**.

## Verifiche e limiti

Evidenze raccolte su **un solo SKMC 1200 Android 6 / firmware MCU 4922**:

- Installazione Toque e avvio HOME diretto al cold boot.
- Magisk 31: permesso root persistente e senza popup dopo spegnimento e riaccensione con interruttore principale.
- Collegamento fisico della base motore via **/dev/ttyMT0**.
- Presenza e rimozione boccale rilevate correttamente (con correzione bit 0x40 specifica per firmware 4922).
- Lettura della temperatura: circa 37 °C con acqua tiepida.
- MQTT funzionante, nove entità Home Assistant visibili, riavvio del servizio dopo cold boot.
- Orologio sincronizzato automaticamente, talvolta con ritardo iniziale.

**Cottura**: Toque mantiene le funzioni originali dell'upstream di cottura manuale/guidata, vapore, programmi, bilancia e controlli software del motore. I test automatici coprono protocollo, logica di controllo e condizioni di sicurezza. **Non è documentato un collaudo completo di riscaldamento reale e interblocchi dinamici sotto carico per la 0.5.2**. La build non è una certificazione di sicurezza; la rimozione del nome storico UNTESTED non cambia questi limiti.

## Installazione rapida con backup sicuro

Su **Windows PowerShell 5.1** con ADB e Magisk già funzionante, robot completamente fermo (motore e resistenza spenti), eseguire dalla cartella del fork:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\installazione-rapida-it.ps1 -ConfermoRobotFermo
~~~

Lo script accetta un solo dispositivo ADB, scarica **esclusivamente la release del fork**, ne confronta il checksum SHA-256 con il file SHA256SUMS.txt della release, esegue il backup e poi installa senza cancellare dati. Non effettua rooting, non flasha il firmware, non tocca l'app Lidl.

Prima dell'aggiornamento salva **l'APK precedente** e una copia **cifrata** di impostazioni (incluse credenziali MQTT), database ricette e preferiti e file locali. Destinazione standard: cartella privata di Windows **%LOCALAPPDATA%\ToqueIT\Backup**. La crittografia è **Windows DPAPI CurrentUser**, con **HMAC-SHA256 autenticato** e chiave protetta da DPAPI per rilevare alterazioni: il ripristino è possibile solo dallo **stesso utente Windows sullo stesso computer**. Non è un backup portatile né un backup delle partizioni Android.

Per usare un'APK locale:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\installazione-rapida-it.ps1 -Apk "C:\Percorso\Toque-IT-0.5.2.apk" -ConfermoRobotFermo
~~~

Per eseguire solo il backup, senza installare:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\installazione-rapida-it.ps1 -SoloBackup -ConfermoRobotFermo
~~~

Il flag **-ConfermoRobotFermo** conferma che l'utente ha verificato visivamente il completo arresto. Non eseguire se ci sono programmi di cucina attivi. Dopo l'upgrade verificare il collegamento base motore, i 9 stati MQTT, l'italiano e la HOME predefinita.

### Primo impianto (robot non rootato)

La scorciatoia **non** sostituisce il processo di root. Prima dell'installazione iniziale seguire [AGENTS.md](AGENTS.md) e la [guida prudente](docs/INSTALLAZIONE-IT-SICURA.md) con backup verificato di boot, recovery e delle modifiche eventuali alla partizione system. L'originale Lidl deve restare recuperabile.

## Ripristino

Per ripristinare impostazioni e dati da un backup locale protetto, prima installare la **stessa versione APK** indicata in manifest.json, poi:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\installazione-rapida-it.ps1 -Ripristina "C:\Percorso\ToqueIT-AAAAMMGG-HHMMSS" -ConfermoRobotFermo
~~~

Il restore crea un **nuovo backup dello stato corrente prima di sovrascriverlo**. Per tornare a Lidl usare le impostazioni di Toque o la procedura upstream: il ritorno all'app stock non rimuove Magisk e non ripristina le partizioni.

## Compilazione e APK

~~~bash
./gradlew --no-daemon :core:test :app:assembleDebug
~~~

La CI valida traduzioni, unit test Kotlin, sintassi degli script e build Android. L'artefatto debug ora si chiama **toque-it-debug**, senza il vecchio suffisso UNTESTED. Il workflow di release produce **Toque-IT-X.Y.Z.apk** e **SHA256SUMS.txt** in [GitHub Releases del fork](https://github.com/samumar82/MrCuisineVibeConnect/releases); anche il controllo aggiornamenti dentro Toque consulta questo fork.

La chiave di firma APK è condivisa pubblicamente per mantenere compatibilità con l'originale: **non costituisce garanzia di provenienza**. Usare solo APK delle release del repository, verificando hash e sorgente GitHub.

## Crediti

Originale e autori: [Wuki60/MrCuisineVibeConnect](https://github.com/Wuki60/MrCuisineVibeConnect). Protocollo: [Monsieur Cuisine Developer SDK](https://github.com/antrea/monsieur-cuisine-developer-sdk). Licenza [MIT](LICENSE). Nessuna ricetta Lidl né immagine ufficiale è distribuita nella repo.
