#!/system/bin/sh
# Called after APK install on an already-rooted robot. Does not flash firmware.
set -e
U=$(stat -c %u /data/data/app.toque 2>/dev/null)
case "$U" in ''|*[!0-9]*) echo "Toque not installed"; exit 1 ;; esac
[ "$U" -ge 10000 ] || exit 1
magisk --sqlite "REPLACE INTO policies (uid,policy,until,logging,notification) VALUES($U,2,0,1,0);"
echo "Toque superuser authorization: permanent, silent, logging enabled"
if [ ! -x /data/adb/magisk/busybox ]; then
    for b in /data/app/com.topjohnwu.magisk*/lib/arm*/libbusybox.so; do
        [ -f "$b" ] || continue
        mkdir -p /data/adb/magisk
        cp "$b" /data/adb/magisk/busybox
        chmod 755 /data/adb/magisk/busybox
        chcon u:object_r:magisk_file:s0 /data/adb/magisk/busybox 2>/dev/null || true
        break
    done
fi
[ -x /data/adb/magisk/busybox ] || {
    echo "ERROR: Magisk BusyBox missing, serial driver unavailable."
    exit 1
}
mkdir -p /data/adb/post-fs-data.d
cp /data/local/tmp/20-toque-root-policy.sh /data/adb/post-fs-data.d/20-toque-root-policy.sh
chown root:root /data/adb/post-fs-data.d/20-toque-root-policy.sh
chmod 700 /data/adb/post-fs-data.d/20-toque-root-policy.sh
restorecon /data/adb/post-fs-data.d/20-toque-root-policy.sh 2>/dev/null || true
echo "Cold-boot root policy installed"
