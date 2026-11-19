#!/system/bin/sh
# Restore ONLY a previously authenticated, local Toque IT app-data backup.
# Requires the Toque app to be stopped and the robot physically idle.
set -e
[ "$(id -u)" = "0" ] || { echo "Root required" >&2; exit 11; }
BB=/data/adb/magisk/busybox
[ -x "$BB" ] || { echo "Magisk BusyBox missing" >&2; exit 12; }
P=/data/data/app.toque
[ -d "$P" ] || { echo "Install Toque APK before restoring data" >&2; exit 13; }
U=$(stat -c %u "$P")
case "$U" in ''|*[!0-9]*) exit 14 ;; esac
[ "$U" -ge 10000 ] || exit 15
cd "$P" || exit 16
"$BB" tar -xzf - || { echo "Archive extraction failed" >&2; exit 17; }
for dir in shared_prefs databases files; do
    if [ -e "$dir" ]; then
        chown -R "$U:$U" "$dir"
        restorecon -R "$P/$dir" 2>/dev/null || true
    fi
done
echo "Toque app data restored" >&2
