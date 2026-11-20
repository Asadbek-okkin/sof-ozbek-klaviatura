"""AndroidManifest.xml dagi versiyani oshiradi.

Ishlatish: python versiya_oshir.py AndroidManifest.xml <oxirgi_reliz_kodi>
versionCode = max(manifestdagi, oxirgi reliz) + 1; versionName = "<katta>.<kichik>.<kod>".
Chiqish (stdout): "<versionName> <versionCode>"
"""
import re
import sys

yol, oxirgi = sys.argv[1], int(sys.argv[2] or 0)
s = open(yol, encoding="utf-8").read()
kod = int(re.search(r'versionCode="(\d+)"', s).group(1))
nom = re.search(r'versionName="([^"]+)"', s).group(1)
yangi_kod = max(kod, oxirgi) + 1
asos = ".".join(nom.split(".")[:2])
yangi_nom = f"{asos}.{yangi_kod}"
s = re.sub(r'versionCode="\d+"', f'versionCode="{yangi_kod}"', s, count=1)
s = re.sub(r'versionName="[^"]+"', f'versionName="{yangi_nom}"', s, count=1)
open(yol, "w", encoding="utf-8", newline="\n").write(s)
print(yangi_nom, yangi_kod)
