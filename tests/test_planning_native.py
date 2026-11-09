from pathlib import Path
import tempfile,subprocess,unittest,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
class PlanningManifestTest(unittest.TestCase):
 def test_generated_manifest_has_three_providers_and_private_reminders(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp)/'android/app/src/main/AndroidManifest.xml';p.parent.mkdir(parents=True);p.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application/></manifest>')
   subprocess.run(['python3',str(ROOT/'scripts/configure_planning.py')],cwd=tmp,check=True)
   subprocess.run(['python3',str(ROOT/'scripts/configure_planning.py')],cwd=tmp,check=True)
   root=ET.parse(p).getroot();a='{http://schemas.android.com/apk/res/android}';app=root.find('application');rs=app.findall('receiver')
   self.assertEqual(len(rs),4);self.assertEqual(len([r for r in rs if r.get(a+'exported')=='true']),3)
   reminder=next(r for r in rs if r.get(a+'name')=='.QsReminderReceiver');self.assertEqual(reminder.get(a+'exported'),'false')
   self.assertIn('android.intent.action.BOOT_COMPLETED',[x.get(a+'name') for x in reminder.findall('intent-filter/action')])
   self.assertEqual([p.get(a+'name') for p in root.findall('uses-permission')],['android.permission.RECEIVE_BOOT_COMPLETED'])
   for r in rs:
    meta=r.find('meta-data')
    if meta is not None:ET.parse(ROOT/'native/res/xml'/ (meta.get(a+'resource').split('/')[-1]+'.xml'))
 def test_native_category_vectors_are_packaged_and_valid(self):
  source=(ROOT/'native/CategorySymbols.java').read_text();a='{http://schemas.android.com/apk/res/android}'
  for name in set(__import__('re').findall(r'R.drawable.(qs_category_\w+)',source)):
   root=ET.parse(ROOT/'native/res/drawable'/f'{name}.xml').getroot()
   self.assertEqual(root.tag,'vector');self.assertEqual(root.get(a+'viewportWidth'),'28')
   self.assertTrue(root.findall('.//path'));self.assertTrue(all(p.get(a+'pathData') for p in root.findall('.//path')))
  build=(ROOT/'.github/workflows/build.yml').read_text();self.assertIn('CategorySymbols.draw', (ROOT/'native/QuickAccessOverlay.java').read_text());self.assertIn('new CaptureEntryView(this,captureState(item)',build);self.assertNotIn('categoryIcon(String',build)
 def test_no_payment_engine_in_native_reminder(self):
  source=(ROOT/'native/PlanningBridge.java').read_text()
  self.assertNotIn('SmsStore.complete',source);self.assertNotIn('setExact',source);self.assertIn('stamp',source);self.assertIn('setAndAllowWhileIdle',source)
if __name__=='__main__':unittest.main()
