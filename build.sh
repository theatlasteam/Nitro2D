#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/classes build/preverified
mkdir -p build/classes dist
java -jar ../lib/ecj.jar -source 1.3 -target 1.3 -bootclasspath ../lib/cldcapi11.jar -classpath ../lib/midpapi20.jar -d build/classes src/n2d/*.java
java -jar ../lib/proguard.jar -injars build/classes -outjars build/preverified -libraryjars ../lib/cldcapi11.jar -libraryjars ../lib/midpapi20.jar -dontshrink -dontoptimize -dontobfuscate -microedition
python3 - <<'PY'
from pathlib import Path
import zipfile
fields=['MIDlet-Name: Nitro2D','MIDlet-Version: 0.1.0','MIDlet-Vendor: Atlas','MIDlet-1: Nitro2D,/icon.png,n2d.Demo','MicroEdition-Configuration: CLDC-1.1','MicroEdition-Profile: MIDP-2.0']
jar=Path('dist/Nitro2D.jar')
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('META-INF/MANIFEST.MF','\r\n'.join(['Manifest-Version: 1.0']+fields+['','']))
 for f in ['icon.png','atlas.png','atlas.tbl','bg_far.png','bg_near.png','tiles.png']:
  z.write('assets/'+f,f)
 for f in sorted(Path('build/preverified').rglob('*.class')):z.write(f,f.relative_to('build/preverified').as_posix())
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None
 for name in z.namelist():
  if name.endswith('.class'):assert int.from_bytes(z.read(name)[6:8],'big')==47
Path('dist/Nitro2D.jad').write_bytes(('\r\n'.join(fields+['MIDlet-Jar-URL: Nitro2D.jar','MIDlet-Jar-Size: '+str(jar.stat().st_size),''])).encode('ascii'))
print('Built',jar,jar.stat().st_size,'bytes; class version 47 and ZIP verified')
PY
