"""Exercise the actual manifest rewrite in isolation, including Android prefix use."""
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest


class ManifestConfigTest(unittest.TestCase):
    def test_preserves_android_prefix_for_manifest_merger(self):
        workflow = (Path(__file__).resolve().parents[1] / '.github/workflows/build.yml').read_text()
        block = textwrap.dedent(workflow.split("python3 - <<'PY_CONFIG'\n", 1)[1].split('\n          PY_CONFIG', 1)[0])
        xml = block[block.index("manifest = Path('android/app/src/main/AndroidManifest.xml')"):block.index("config=Path('config/google-services.json')")]
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / 'android/app/src/main/AndroidManifest.xml'
            manifest.parent.mkdir(parents=True)
            manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.quickspend.app"><uses-permission android:name="android.permission.INTERNET"/><application><activity android:name=".MainActivity"/></application></manifest>')
            # Separate process avoids inheriting namespace registration from tests
            # or the earlier manifest-writing step in the build.
            script = 'from pathlib import Path\n' + xml + '\n' + textwrap.dedent('''\
                import xml.etree.ElementTree as ET
                source = manifest.read_text()
                assert 'xmlns:android=' in source, 'Android tooling requires the android namespace prefix'
                # Emulate later tooling adding a namespaced permission.
                source = source.replace('</manifest>', '<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/></manifest>')
                root = ET.fromstring(source)
                app = root.find('application')
                ns = '{http://schemas.android.com/apk/res/android}'
                assert any(x.get(ns+'name') == '.UpdateMessagingService' for x in app.findall('service'))
                assert any(x.get(ns+'name') == 'com.android.vending' for x in root.findall('queries/package'))
                assert len(app.findall('meta-data')) == 2
                ''')
            subprocess.run(['python3', '-c', script], cwd=directory, check=True, capture_output=True, text=True)


if __name__ == '__main__':
    unittest.main()
