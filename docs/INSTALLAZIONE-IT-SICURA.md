# Installazione prudente e ripristino — Toque IT, SKMC 1200

**Aggiornamento novembre 2026.** Il fork è stato installato e provato su **un** Monsieur Cuisine Connect SKMC 1200 Android 6 con firmware MCU 4922, Magisk 31. Prima dell'aggiornamento della documentazione la versione 0.5.1 era già operativa su questo dispositivo per letture, avvio, rilevamento boccale e telemetria MQTT. Non rappresenta una certificazione di compatibilità o sicurezza su altri robot.

> Non sono distribuiti backup firmware o ricette ufficiali. Modifiche a boot/recovery/system possono rendere inutilizzabile il tablet e compromettere le condizioni di garanzia. Non avviare test elettronici mentre il robot cucina; prima di qualsiasi installazione o restore attendere il completo arresto fisico di motore e riscaldamento.

## Percorso rapido: Magisk e Toque già presenti

Non fare nuovamente root e non flashare partizioni. Collegare il robot **via USB ADB** al PC Windows, verificare fisicamente che sia fermo, quindi dalla cartella del fork:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\installazione-rapida-it.ps1 -ConfermoRobotFermo
~~~

Il comando automatizza: acquisizione release del **fork samumar82**, verifica SHA-256, **backup cifrato** di impostazioni, ricette, favoriti e file dell'app, copia dell'APK installata, installazione nuovo APK, applicazione delle policy Magisk persistenti e avvio Toque. Non modifica l'app Lidl originale né le partizioni.

Per backup senza upgrade:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\installazione-rapida-it.ps1 -SoloBackup -ConfermoRobotFermo
~~~

**Backup:** contiene un file crittografato Windows DPAPI, un tag HMAC-SHA256 e una chiave HMAC protetta da DPAPI per rilevare manomissioni, oltre all'APK precedente in chiaro, con manifest e hash. Funziona per restore dei dati Toque **solo con lo stesso profilo Windows e PC**. Non contiene backup integrale di Android, firmware, recovery o boot. Se il computer si guasta, una copia DPAPI trasferita su un altro computer non è sufficiente: conservarne anche una strategia di recupero offline e i backup partizione iniziali.

Restore dei dati: installare prima la stessa APK riportata dal manifest, poi lanciare l'opzione Ripristina descritta in [README.it.md](../README.it.md). Lo script crea un ulteriore backup prima di ripristinare i dati. Non eliminare l'APK vecchia prima della verifica.

## Primo impianto (robot non rootato)

Non usare il percorso rapido per saltare i prerequisiti. Consultare [AGENTS.md](../AGENTS.md) e il [manuale upstream](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/docs/INSTALLATION.md). Prima di scrivere alcunché:

1. Confermare modello SKMC 1200 **Connect**, Android 6, firmware e varianti hardware; non usare procedure Connect su **Smart** o altri modelli.
2. Preparare PC Windows, ADB e driver MediaTek, cavo dati USB-A maschio ↔ USB-A maschio, porta di servizio Android USB corretta, utensile Torx e alimentazione stabile.
3. Salvare e verificare lettura di **boot** e **recovery**, e valutare il backup della partizione **system**: la procedura upstream modifica anche file di system, non solo boot/recovery.
4. Verificare scatter e tabella partizioni corrispondenti alla **specifica macchina**; non usare offset altrui senza verifica.
5. Salvare checksum SHA-256 degli originali, mantenere una seconda copia offline e provare una via di ripristino prima di flashare.
6. Mai usare **Format All** o **Firmware Upgrade** in SP Flash Tool. Ottenere autorizzazione esplicita per ogni scrittura fisica.
7. Installare solo APK del fork e testare inizialmente con macchina ferma; scegliere HOME e modalità hardware sul robot soltanto dopo i controlli passivi.

Il backup dell'app Toque, cifrato e creato dal comando rapido, **non annulla root né ripristina /system**: è destinato agli aggiornamenti dopo il primo impianto.

## Sicurezza firmware 4922

La base motore del robot osservato usa una polarità invertita del flag 0x40 per la presenza del boccale. Questo fork applica la decodifica invertita **solo per MCU 4922** ed impone l'interblocco boccale per quella versione; sono stati osservati frame 0x84 con boccale inserito e 0xC4 con boccale estratto. Sul robot reale è stato confermato il passaggio inserito/assente a macchina ferma.

Per usare la cottura in sicurezza occorre comunque verificare con l'operatore presente che coperchio, boccale, arresto, eventuali allarmi e controllo della temperatura siano corretti. **Non collaudare interblocchi aprendo il coperchio o rimuovendo il boccale con motore in movimento.** Non fare prove a caldo non presidiate.

## Magisk persistente e ritorno all'originale

Sul dispositivo verificato la policy Magisk per Toque rimane permanente e senza notifiche dopo lo spegnimento completo. È installato uno script di boot in /data/adb/post-fs-data.d, riservato al solo UID dell'app Toque; registra ancora gli accessi root. L'autorizzazione è una scelta esplicita dell'utilizzatore, da valutare in termini di sicurezza del dispositivo.

Per aprire l'app Lidl è prevista l'azione *Torna all'app originale*. Per restituire permanentemente il controllo allo stock, è disponibile la procedura di ripristino descritta nelle istruzioni upstream. Non disinstallare Toque senza avere confermato l'accesso all'app originale. Disinstallazione e ritorno stock **non rimuovono Magisk e non ripristinano boot, recovery o system**.

## Home Assistant: telemetria senza comandi

Il fork pubblica **nove entità MQTT in sola lettura**, osservate in Home Assistant e con stato offline quando il robot è spento. Le credenziali MQTT restano sul robot; i backup rapidi le proteggono con DPAPI. Il servizio non si sottoscrive a topic di comando e non può avviare motore o riscaldamento da Home Assistant. Il Wi-Fi e il broker non devono costituire una dipendenza per le protezioni locali.

La presenza delle nove entità, del display italiano, del root persistente e della connessione seriale è **verificata**, ma non sostituisce un collaudo di cottura completo e certificato.
