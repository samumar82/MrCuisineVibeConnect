# Toque IT: upgrade/backup/restore on an ALREADY rooted Monsieur Cuisine Connect.
# Windows PowerShell 5.1. App data is streamed over ADB and encrypted using
# Windows DPAPI CurrentUser BEFORE being saved to disk on the PC.
# Backups cannot be decrypted from a different Windows profile or computer.
# NEVER run while cooking: stopping the app can interrupt a program.
param(
    [string]$Apk = "",
    [switch]$SoloBackup,
    [switch]$Autotest,
    [string]$Ripristina = "",
    [switch]$ConfermoRobotFermo,
    [string]$CartellaBackup = (Join-Path $env:LOCALAPPDATA "ToqueIT\Backup")
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Security
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
$adb = if ($adbCmd) { $adbCmd.Source } else { Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe" }

function Check([string]$message) {
    if ($LASTEXITCODE -ne 0) { throw "$message (exit $LASTEXITCODE)" }
}
function Adb([string[]]$command) {
    & $script:adb @command | Out-Host
    Check "ADB: $($command -join ' ')"
}
function Read-Adb([string[]]$command) {
    $output = & $script:adb @command
    Check "ADB: $($command -join ' ')"
    return ($output -join [Environment]::NewLine).Trim()
}
function Current-Version {
    $info = Read-Adb @("shell","dumpsys package app.toque")
    $m = [regex]::Match($info, 'versionName=([^\s]+)')
    if (!$m.Success) { throw "Impossibile leggere la versione Toque installata." }
    return $m.Groups[1].Value
}
function Push-RootScript([string]$name) {
    $local = Join-Path $repoRoot "tools\robot\$name"
    if (!(Test-Path $local)) { throw "Script mancante nel fork: $local" }
    Adb @("push",$local,"/data/local/tmp/$name")
}
function ADB-StreamBytes([string]$remoteScript) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $script:adb
    $psi.Arguments = 'exec-out su -c "sh /data/local/tmp/' + $remoteScript + '"'
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $psi
    [void]$p.Start()
    $mem = New-Object System.IO.MemoryStream
    $buf = New-Object byte[] 65536
    try {
        while (($n = $p.StandardOutput.BaseStream.Read($buf,0,$buf.Length)) -gt 0) {
            $mem.Write($buf,0,$n)
            if ($mem.Length -gt 536870912) {
                $p.Kill()
                throw "Backup superiore a 512 MiB; eseguire backup manuale completo."
            }
        }
        $p.WaitForExit()
        if ($p.ExitCode -ne 0) {
            throw "Backup dispositivo fallito: $($p.StandardError.ReadToEnd())"
        }
        return ,$mem.ToArray()
    } finally {
        $mem.Dispose()
        $p.Dispose()
    }
}
function Validate-Gzip([byte[]]$bytes) {
    $m = New-Object System.IO.MemoryStream (,$bytes)
    $gz = [System.IO.Compression.GzipStream]::new($m,[System.IO.Compression.CompressionMode]::Decompress)
    try {
        $buf = New-Object byte[] 65536
        $total = 0
        while (($n = $gz.Read($buf,0,$buf.Length)) -gt 0) { $total += $n }
        if ($total -lt 512) { throw "Archivio vuoto." }
    } finally { $gz.Dispose(); $m.Dispose() }
}
function Save-ProtectedData([byte[]]$archive,[string]$directory) {
    # Confidentiality: DPAPI CurrentUser. Authenticity: HMAC-SHA256 with a
    # random HMAC key also protected by DPAPI; do NOT trust a naked file hash.
    $key = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($key)
    $rng.Dispose()
    $h = $null
    try {
        $cipher = [System.Security.Cryptography.ProtectedData]::Protect(
            $archive,$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        $sealedKey = [System.Security.Cryptography.ProtectedData]::Protect(
            $key,$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        $h = [System.Security.Cryptography.HMACSHA256]::new($key)
        $tag = $h.ComputeHash($cipher)
        [IO.File]::WriteAllBytes((Join-Path $directory "dati-toque.dpapi"),$cipher)
        [IO.File]::WriteAllBytes((Join-Path $directory "chiave-hmac.dpapi"),$sealedKey)
        [IO.File]::WriteAllBytes((Join-Path $directory "dati-toque.hmac"),$tag)
    } finally {
        if ($h) { $h.Dispose() }
        [Array]::Clear($key,0,$key.Length)
        if ($cipher) { [Array]::Clear($cipher,0,$cipher.Length) }
        if ($sealedKey) { [Array]::Clear($sealedKey,0,$sealedKey.Length) }
    }
}
function Load-ProtectedData([string]$directory) {
    $cipher = [IO.File]::ReadAllBytes((Join-Path $directory "dati-toque.dpapi"))
    $tag = [IO.File]::ReadAllBytes((Join-Path $directory "dati-toque.hmac"))
    $key = [System.Security.Cryptography.ProtectedData]::Unprotect(
        [IO.File]::ReadAllBytes((Join-Path $directory "chiave-hmac.dpapi")),
        $null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    $h = $null
    try {
        $h = [System.Security.Cryptography.HMACSHA256]::new($key)
        $computed = $h.ComputeHash($cipher)
        if ($computed.Length -ne 32 -or $tag.Length -ne 32) {
            throw "Backup danneggiato: tag HMAC non valido."
        }
        $diff = 0
        for ($i=0; $i -lt 32; $i++) {
            $diff = $diff -bor ($tag[$i] -bxor $computed[$i])
        }
        if ($diff -ne 0) { throw "Backup manomesso o corrotto: verifica HMAC fallita." }
        return ,[System.Security.Cryptography.ProtectedData]::Unprotect(
            $cipher,$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    } finally {
        if ($h) { $h.Dispose() }
        [Array]::Clear($key,0,$key.Length)
        [Array]::Clear($cipher,0,$cipher.Length)
    }
}
function Backup-Toque {
    Write-Host "===== BACKUP PRIVATO + APK ORIGINALE ====="
    $version = Current-Version
    $date = Get-Date -Format "yyyyMMdd-HHmmss"
    $dir = Join-Path $CartellaBackup "ToqueIT-$date"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $apkPath = (Read-Adb @("shell","pm path app.toque")).Split([Environment]::NewLine) |
        Where-Object { $_ -match '^package:/data/app/.+\.apk$' } | Select-Object -First 1
    if (!$apkPath) { throw "APK installata non individuata. Aggiornamento annullato." }
    $installed = Join-Path $dir "app-precedente.apk"
    Adb @("pull",$apkPath.Substring(8),$installed)
    if (!(Test-Path $installed)) { throw "Backup APK non disponibile." }
    # Stop only after explicit consent and only with motor/heater already idle.
    Adb @("shell","su -c 'am force-stop app.toque'")
    Push-RootScript "backup-data.sh"
    $raw = ADB-StreamBytes "backup-data.sh"
    try {
        Validate-Gzip $raw
        Save-ProtectedData $raw $dir
        $protectedPath = Join-Path $dir "dati-toque.dpapi"
        $manifest = [ordered]@{
            schema = 1
            app = "app.toque"
            versionName = $version
            date = (Get-Date).ToString("o")
            encryption = "Windows DPAPI CurrentUser"
            encryptedSha256 = (Get-FileHash $protectedPath -Algorithm SHA256).Hash
            apkSha256 = (Get-FileHash $installed -Algorithm SHA256).Hash
            note = "App data only: shared_prefs, databases and files. Not firmware/boot/recovery/system."
        }
        [System.IO.File]::WriteAllText(
            (Join-Path $dir "manifest.json"),
            ($manifest | ConvertTo-Json -Depth 4),
            [System.Text.UTF8Encoding]::new($false))
    } finally {
        [Array]::Clear($raw,0,$raw.Length)
    }
    Write-Host "BACKUP VERIFICATO: $dir"
    Write-Host "Attenzione: per decrittare serve questo stesso utente Windows e computer."
    return $dir
}
function Install-Latest {
    $temp = Join-Path $env:TEMP ("ToqueIT-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $temp | Out-Null
    try {
        Write-Host "===== SCARICAMENTO RELEASE UFFICIALE DEL FORK ====="
        $release = Invoke-RestMethod -Uri "https://api.github.com/repos/samumar82/MrCuisineVibeConnect/releases/latest" -Headers @{ "User-Agent" = "Toque-IT-QuickInstall" }
        $asset = @($release.assets | Where-Object { $_.name -match '^Toque-IT-[0-9.]+\.apk$' }) | Select-Object -First 1
        $hashAsset = @($release.assets | Where-Object { $_.name -eq "SHA256SUMS.txt" }) | Select-Object -First 1
        if (!$asset -or !$hashAsset) { throw "Release priva di APK o checksum. Nessuna installazione." }
        $file = Join-Path $temp $asset.name
        $shaFile = Join-Path $temp "SHA256SUMS.txt"
        Invoke-WebRequest -UseBasicParsing -Uri $asset.browser_download_url -OutFile $file
        Invoke-WebRequest -UseBasicParsing -Uri $hashAsset.browser_download_url -OutFile $shaFile
        $line = Get-Content $shaFile | Where-Object { $_ -match ('^[a-fA-F0-9]{64}\s+\*?' + [regex]::Escape($asset.name) + '$') } | Select-Object -First 1
        if (!$line) { throw "Checksum SHA256 non pubblicato per l'APK." }
        $expected = $line.Substring(0,64)
        $actual = (Get-FileHash $file -Algorithm SHA256).Hash
        if ($actual -ine $expected) { throw "SHA256 NON CORRISPONDE: installazione annullata." }
        Write-Host "SHA256 verificato: $actual"
        return $file
    } catch {
        Remove-Item -Force -Recurse $temp -ErrorAction SilentlyContinue
        throw
    }
}
function Restore-Data([string]$directory) {
    $dir = (Resolve-Path $directory).Path
    $manifestFile = Join-Path $dir "manifest.json"
    $protectedPath = Join-Path $dir "dati-toque.dpapi"
    if (!(Test-Path $manifestFile) -or !(Test-Path $protectedPath)) { throw "Backup incompleto." }
    $manifest = Get-Content $manifestFile -Raw | ConvertFrom-Json
    if ($manifest.schema -ne 1 -or $manifest.app -ne "app.toque") { throw "Formato backup sconosciuto." }
    if ((Get-FileHash $protectedPath -Algorithm SHA256).Hash -ne $manifest.encryptedSha256) {
        throw "Backup alterato: SHA256 errato."
    }
    if ((Current-Version) -ne $manifest.versionName) {
        throw "Versione diversa: installare prima l'app-precedente.apk del backup, poi ripristinare i dati."
    }
    # Always keep a fresh rollback before restoring older app data.
    $safety = Backup-Toque
    Write-Host "Backup di sicurezza prima del restore: $safety"
    $raw = Load-ProtectedData $dir
    try {
        Validate-Gzip $raw
        Push-RootScript "restore-data.sh"
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $script:adb
        $psi.Arguments = 'exec-in su -c "sh /data/local/tmp/restore-data.sh"'
        $psi.UseShellExecute = $false
        $psi.CreateNoWindow = $true
        $psi.RedirectStandardInput = $true
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $proc = New-Object System.Diagnostics.Process
        $proc.StartInfo = $psi
        [void]$proc.Start()
        try {
            $proc.StandardInput.BaseStream.Write($raw,0,$raw.Length)
            $proc.StandardInput.Close()
            $proc.WaitForExit()
            if ($proc.ExitCode -ne 0) {
                throw "Ripristino fallito: $($proc.StandardError.ReadToEnd())"
            }
        } finally { $proc.Dispose() }
        Adb @("shell","am start -n app.toque/.ui.MainActivity")
        Write-Host "RIPRISTINO DATI COMPLETATO. Verifica MQTT, ricette e avvio in Toque."
    } finally { [Array]::Clear($raw,0,$raw.Length) }
}

if ($Autotest) {
    # No device required: validate gzip handling, DPAPI round trip and tamper detection.
    Write-Host "===== AUTOTEST BACKUP CIFRATO ====="
    $expected = [Text.Encoding]::UTF8.GetBytes(("TOQUE-IT-TEST " * 50))
    $mem = New-Object System.IO.MemoryStream
    $temp = Join-Path $env:TEMP ("ToqueIT-selftest-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $temp | Out-Null
    try {
        $gz = [System.IO.Compression.GzipStream]::new(
            $mem,[System.IO.Compression.CompressionMode]::Compress,$true)
        $gz.Write($expected,0,$expected.Length)
        $gz.Dispose()
        $plain = $mem.ToArray()
        Validate-Gzip $plain
        Save-ProtectedData $plain $temp
        $decrypted = Load-ProtectedData $temp
        if (([Convert]::ToBase64String($plain) -ne [Convert]::ToBase64String($decrypted))) {
            throw "Round-trip cifratura fallito"
        }
        $cipherFile = Join-Path $temp "dati-toque.dpapi"
        $tampered = [IO.File]::ReadAllBytes($cipherFile)
        $tampered[10] = $tampered[10] -bxor 7
        [IO.File]::WriteAllBytes($cipherFile,$tampered)
        $denied = $false
        try { [void](Load-ProtectedData $temp) } catch { $denied = $true }
        if (!$denied) { throw "HMAC non ha rilevato la manomissione." }
        Write-Host "AUTOTEST OK: gzip, DPAPI, HMAC-SHA256, tamper rejection"
    } finally {
        $mem.Dispose()
        Remove-Item -Force -Recurse $temp -ErrorAction SilentlyContinue
        if ($expected) { [Array]::Clear($expected,0,$expected.Length) }
        if ($plain) { [Array]::Clear($plain,0,$plain.Length) }
        if ($decrypted) { [Array]::Clear($decrypted,0,$decrypted.Length) }
    }
    exit 0
}

if (!(Test-Path $adb)) { throw "ADB mancante. Installa Android Platform Tools." }
if (!$ConfermoRobotFermo) {
    throw "Per sicurezza esegui solo a robot FERMO, senza motore o riscaldamento: aggiungi -ConfermoRobotFermo."
}
$devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' })
if ($devices.Count -ne 1) { throw "Collega via USB un solo dispositivo ADB (trovati: $($devices.Count))." }
if ((Read-Adb @("shell","su -c 'id -u'")) -ne "0") {
    throw "Root Magisk non disponibile: questa procedura NON installa root e NON flasha partizioni."
}
if (!(Read-Adb @("shell","pm path app.toque"))) {
    throw "Toque non installato: usare la guida di primo impianto, con backup firmware prima del root."
}
if ($Ripristina) {
    Restore-Data $Ripristina
    exit 0
}
if ($SoloBackup) {
    [void](Backup-Toque)
    Adb @("shell","am start -n app.toque/.ui.MainActivity")
    exit 0
}
$downloaded = $false
if (!$Apk) {
    $Apk = Install-Latest
    $downloaded = $true
}
try {
    if (!(Test-Path $Apk)) { throw "APK non trovata: $Apk" }
    Write-Host "APK SHA256: $((Get-FileHash $Apk -Algorithm SHA256).Hash)"
    $backup = Backup-Toque
    Write-Host "===== INSTALLAZIONE APK ====="
    Adb @("install","-r",(Resolve-Path $Apk).Path)
    Push-RootScript "20-toque-root-policy.sh"
    Push-RootScript "setup-root.sh"
    Adb @("shell","su -c 'sh /data/local/tmp/setup-root.sh'")
    Adb @("shell","am start -n app.toque/.ui.MainActivity")
    Write-Host "INSTALLAZIONE COMPLETATA. Backup: $backup"
    Write-Host "Controlla sul display la base, l'italiano, MQTT e i nove sensori HA."
} finally {
    if ($downloaded) {
        Remove-Item -Force -Recurse (Split-Path $Apk -Parent) -ErrorAction SilentlyContinue
    }
}
