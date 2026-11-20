#!/usr/bin/env bash
# Qo'lda yangi versiya chiqarish (avtomatik 4 kunlik reliz .github/workflows/avto-reliz.yml da).
# Avval: git pull (avtomatik relizlar versiyani oshirgan bo'lishi mumkin), keyin AndroidManifest.xml
# dagi versionCode (+1) va versionName ni oshiring.
set -euo pipefail
cd "$(dirname "$0")"
bash build.sh
NOM=$(grep -o 'versionName="[^"]*"' AndroidManifest.xml | cut -d'"' -f2)
KOD=$(grep -o 'versionCode="[^"]*"' AndroidManifest.xml | cut -d'"' -f2)
LV=$(curl -fsS --max-time 20 https://inok.uz/claude-pc/klaviatura/lugat | python -c "import sys,json;print(json.load(sys.stdin)['versiya'])" 2>/dev/null || echo 0)
IZOH="${1:-Yangi versiya}"
gh release create "v$NOM" SofOzbekKlaviatura.apk videolar/*.mp4 --title "Sof Özbek klaviatura $NOM ($KOD)" --notes "$IZOH lugat-v$LV"
