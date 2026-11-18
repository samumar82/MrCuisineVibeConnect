# Piano prudente di installazione — Monsieur Cuisine Connect SKMC 1200

**Stato: SOLO PREPARAZIONE. Nessuna operazione sul robot è stata eseguita.**

Questo piano riguarda il fork italiano [samumar82/MrCuisineVibeConnect](https://github.com/samumar82/MrCuisineVibeConnect) con telemetria Home Assistant **solo in lettura**. Il controllo remoto è escluso.

## Compatibilità verificata e non verificata

| Elemento | Evidenza |
|---|---|
| Modello | SKMC 1200 Connect: indicato dalla documentazione upstream |
| Tablet Android | Upstream: MediaTek MT6580 / Android 6; firmware utente: Android 3.8d / build 4922 |
| Demo italiana + ricette | Verificate su telefono Android |
| MQTT 9 sensori | Verificati in Home Assistant sulla Demo |
| Root/boot sul firmware 4922 | **Non verificato** |
| Ripristino completo del firmware 4922 | **Non verificato** |
| Rotella fisica con questo firmware | **Non verificata**, anche se Toque la supporta nel codice |

L'autore riferisce un'installazione completa su **un solo esemplare** nel 2026: non è una certificazione di compatibilità per tutti gli SKMC 1200.

## Materiale necessario (non procedere ancora)

- PC Windows con USB-A, cavo **USB-A maschio ↔ USB-A maschio** dati, piccolo cacciavite Torx;
- backup locale e seconda copia offline per immagini originali e checksum;
- ADB, driver MediaTek PreLoader USB VCOM, SP Flash Tool, TWRP/scatter per il modello corretto e Magisk, scaricati e confrontati con gli SHA-256 descritti in [AGENTS.md upstream](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/AGENTS.md);
- APK **del fork italiano** da una build GitHub Actions verde, non la release upstream. Conservare nome, versione, checksum e artefatto originale.

La documentazione upstream prevede di aprire uno sportello di manutenzione e usare **soltanto la porta di destra etichettata Android USB**. L'altra porta non va usata.

## Fase A — controllo documentale e preparazione PC (nessuna scrittura)

1. Verificare etichetta SKMC 1200, firmware 4922, variante hardware e fotografie dello sportello/porta, prima di qualsiasi connessione.
2. Esaminare insieme la lista di tool, checksum e versioni supportate. Lo script **non deve** essere lanciato in modalità modifica senza revisione.
3. Preparare il percorso `C:\Toque-IT\` con file dell'upstream, file del fork, e cartella `backup\`.
4. Verificare che le impostazioni di sicurezza Windows siano conservate. Eventuale modifica della *Integrità della memoria* soltanto con consenso esplicito e piano per riattivarla.
5. Acquisire uno stato iniziale e uno screenshot del firmware/menu stock.

## Fase B — backup in sola lettura (richiede conferma separata)

La documentazione upstream usa SP Flash Tool Readback su **EMMC_USER** per:

| Partizione | Inizio | Lunghezza | File |
|---|---|---|---|
| boot | `0x1d20000` | `0x1000000` (16 MiB) | `boot-original.img` |
| recovery | `0x2d20000` | `0x1000000` (16 MiB) | `recovery-original.img` |

**Prima di procedere**, verificare che scatter/partition table corrispondano effettivamente al dispositivo, non semplicemente copiare gli offset. Dopo la lettura: confrontare dimensioni, header immagine Android, calcolare SHA-256, fare una seconda copia offline e assicurarsi che entrambi i file siano recuperabili.

Nota: il solo backup `boot`/`recovery` **non è un'immagine completa** del tablet. Lo script root upstream modifica anche `/system/build.prop`, rimuove `/system/recovery-from-boot.p` e può scrivere chiavi ADB. Prima del root è quindi necessario decidere come conservare/ripristinare anche queste modifiche (preferibilmente snapshot della partizione `system` e dei file coinvolti, se tecnicamente possibile), evitando di promettere un ripristino integrale sulla sola base di due immagini.

## Fase C — gate obbligatorio prima del root

Non eseguire **flash-twrp**, **root**, **flash-root**, `adb shell su` o installazione sul robot senza:
- verifica hardware/firmware e corrispondenza partizioni;
- backup leggibile e duplicato;
- una procedura di recupero praticabile e documentata;
- approvazione dell'utente per ogni scrittura;
- corretto APK italiano, non l'APK del progetto originale.

Il file upstream `tools/setup/toque-setup.ps1` **non va eseguito invariato**: scarica la release da `Wuki60/MrCuisineVibeConnect`. Inoltre, secondo l'AGENTS upstream, il percorso console di SP Flash Tool è generato da una configurazione precedente ma **non verificato dal vivo**: l'utente non deve trattarlo come garanzia di riuscita.

Non usare mai **Format**, **Firmware Upgrade** o flash di partizioni non previste. Sono operazioni a rischio di inutilizzabilità.

## Fase D — test sul robot, soltanto dopo autorizzazione

1. Verificare che stock app e servizi originali siano documentati; Toque disabilita temporaneamente il servizio hardware della stock app per ottenere accesso esclusivo alla seriale.
2. Test della manopola (rotazione sx/dx, pressione e tasti) prima di attivare motore/riscaldamento.
3. Diagnostica seriale **senza eseguire cottura**: aggiornamento delle misure, coperchio, presenza boccale, bilancia.
4. Se e solo se tutti i controlli precedenti passano: prove funzionali minime previste dall'upstream, con boccale vuoto, coperchio chiuso e interruttore principale a portata di mano; arresto immediato in caso di comportamento anomalo.
5. MQTT: prima **disattivare l'esportazione Demo sul telefono** per evitare conflitto del client ID `toque-it-mcc`. Attivare sul robot soltanto la pubblicazione in lettura, poi verificare i sensori esistenti in HA. Nessun comando di avvio da HA.
6. Verificare ricette in italiano e conservazione dei passaggi macchina originali.

## Modalità di ritorno

- **Ritorno all'app stock (mantenendo root):** `Toque → Impostazioni → Strumenti → Torna all'app originale` o script `tools/restore-robot.ps1` upstream, che ripristina il servizio originale e può disinstallare Toque. **Non annulla root e modifiche /system**.
- **Rimozione del root/TWRP:** upstream prescrive il ripristino delle immagini originali `boot`/`recovery`; non garantisce da solo la reversibilità delle modifiche `/system`. Da verificare e pianificare in anticipo.

## Fonti primarie

- [Upstream AGENTS.md](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/AGENTS.md)
- [Upstream INSTALLATION.md](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/docs/INSTALLATION.md)
- [Upstream twrp-root.sh](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/tools/robot/twrp-root.sh)
- [Upstream restore-robot.ps1](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/tools/restore-robot.ps1)

Questa è una revisione preventiva della procedura descritta dagli autori, non una verifica sul dispositivo fisico.
