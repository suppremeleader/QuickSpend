"""Install planning receivers/widgets into the generated Android manifest."""
from pathlib import Path
import xml.etree.ElementTree as ET
ns='http://schemas.android.com/apk/res/android'; ET.register_namespace('android',ns); a='{'+ns+'}'
p=Path('android/app/src/main/AndroidManifest.xml'); root=ET.fromstring(p.read_text()); app=root.find('application')
if not any(x.get(a+'name')=='android.permission.RECEIVE_BOOT_COMPLETED' for x in root.findall('uses-permission')):
 root.insert(0,ET.Element('uses-permission',{a+'name':'android.permission.RECEIVE_BOOT_COMPLETED'}))
for old in list(app.findall('receiver')):
 if old.get(a+'name','').startswith(('.QsWidgets$', '.QsReminderReceiver')):app.remove(old)
r=ET.SubElement(app,'receiver',{a+'name':'.QsReminderReceiver',a+'exported':'false'})
f=ET.SubElement(r,'intent-filter')
for action in ['android.intent.action.BOOT_COMPLETED','android.intent.action.MY_PACKAGE_REPLACED','android.intent.action.TIME_SET','android.intent.action.TIMEZONE_CHANGED']:
 ET.SubElement(f,'action',{a+'name':action})
for cls,kind in [('Review','review'),('Summary','summary'),('Quick','quick')]:
 r=ET.SubElement(app,'receiver',{a+'name':'.QsWidgets$'+cls,a+'exported':'true',a+'label':'@string/qs_widget_'+kind})
 f=ET.SubElement(r,'intent-filter'); ET.SubElement(f,'action',{a+'name':'android.appwidget.action.APPWIDGET_UPDATE'})
 ET.SubElement(r,'meta-data',{a+'name':'android.appwidget.provider',a+'resource':'@xml/qs_widget_'+kind})
ET.indent(root); p.write_text(ET.tostring(root,encoding='unicode'))
