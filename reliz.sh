#!/usr/bin/env bash
# Yangi versiyani chiqarish: yig'adi va GitHub relizini yaratadi.
# Telefonlardagi ilova va @claude_asadbek_bot shu relizni ko'rib, yangilanishni taklif qiladi.
# Avval AndroidManifest.xml dagi versionCode (+1) va versionName ni oshiring.
set -euo pipefail
cd "$(dirname "$0")"
bash build.sh
NOM=$(grep -o 'versionName="[^"]*"' AndroidManifest.xml | cut -d'"' -f2)
KOD=$(grep -o 'versionCode="[^"]*"' AndroidManifest.xml | cut -d'"' -f2)
IZOH="${1:-Yangi versiya}"
gh release create "v$NOM" SofOzbekKlaviatura.apk videolar/*.mp4 --title "Sof Özbek klaviatura $NOM ($KOD)" --notes "$IZOH"
