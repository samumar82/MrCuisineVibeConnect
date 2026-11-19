#!/system/bin/sh
# Magisk post-fs-data.d: grant silent, permanent superuser access only to installed Toque.
# Verified on one Android 6 / Magisk 31 / SKMC1200 firmware 4922.
# Never disable Magisk safety alerts globally; keep logging enabled.
APP_UID=$(stat -c %u /data/data/app.toque 2>/dev/null)
case "$APP_UID" in
    ''|*[!0-9]*) exit 0 ;;
esac
[ "$APP_UID" -ge 10000 ] 2>/dev/null || exit 0
magisk --sqlite "REPLACE INTO policies (uid,policy,until,logging,notification) VALUES($APP_UID,2,0,1,0);" >/dev/null 2>&1
