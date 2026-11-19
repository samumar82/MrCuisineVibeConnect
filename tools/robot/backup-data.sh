#!/system/bin/sh
# Read-only, stdout-only gzip tar of Toque's PRIVATE app data.
# Run through "adb exec-out su -c" AFTER an explicit idle confirmation and force-stop.
# stdout MUST contain only binary tar.gz; send errors to stderr.
set -e
[ "$(id -u)" = "0" ] || { echo "Root required" >&2; exit 11; }
BB=/data/adb/magisk/busybox
[ -x "$BB" ] || { echo "Magisk BusyBox missing" >&2; exit 12; }
cd /data/data/app.toque || { echo "App data missing" >&2; exit 13; }
set --
for dir in shared_prefs databases files; do
    [ ! -d "$dir" ] || set -- "$@" "$dir"
done
[ "$#" -gt 0 ] || { echo "No data to back up" >&2; exit 14; }
exec "$BB" tar -czf - "$@"
