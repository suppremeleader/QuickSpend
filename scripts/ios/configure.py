"""Configure a generated Capacitor project; signing is intentionally not configured."""
from pathlib import Path
import json, os, plistlib
root=Path('ios/App/App')
with (root/'Info.plist').open('rb') as f: info=plistlib.load(f)
version=json.loads(Path('package.json').read_text())['version']
number=os.environ.get('IOS_BUILD_NUMBER','1')
assert number.isdigit() and int(number)>0
info.update(CFBundleDisplayName='QuickSpend',CFBundleShortVersionString=version,CFBundleVersion=number)
with (root/'Info.plist').open('wb') as f: plistlib.dump(info,f)
print('Configured iOS development version',version,number)
