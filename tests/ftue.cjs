const assert=require('assert');
const {app,settle}=require('./app-inbox.cjs');

(async()=>{
 const status={notif:false,sms:false},overlay={allowed:false,enabled:false},upi={allowed:false,enabled:false};
 let overlaySettings=0,upiSettings=0,smsSettings=0;
 const a=app({seen:false,permissionApi:{
  status:()=>status,requestNotif:()=>status,requestSms:()=>status,
  overlayStatus:()=>overlay,setOverlayEnabled:({enabled})=>{overlay.enabled=enabled;return overlay},
  openOverlaySettings:()=>{overlaySettings++},
  upiCaptureStatus:()=>upi,setUpiCapture:({enabled})=>{upi.enabled=enabled;return upi},
  openUpiNotificationAccess:()=>{upiSettings++},openAppPermissionSettings:()=>{smsSettings++}
 }});
 await settle();
 let html=a.nodes.get('#language-choice').innerHTML;
 assert.match(html,/QUICKSPEND/);assert.match(html,/chooseLanguage\('hi'\)/);assert.match(html,/chooseLanguage\('en'\)/);assert.doesNotMatch(html,/<img|ftue-art/);
 for(let i=0;i<8;i++){
  a.eval(`ob=${i};showOb()`);html=a.nodes.get('#onb').innerHTML;
  assert.match(html,/id="onb-title"/);assert.match(html,/id="onb-actions"/);
  if(i>0)assert.match(html,/obBack/);
  if(i>=4)assert.doesNotMatch(html,/ob-defer|Not now|skipObIntro/);
  assert.doesNotMatch(html,/<img|ftue-art|class="oe"/);
  assert.match(html,/onclick="(?:permStep|obNext)/);
 }
 a.eval('ob=0;showOb();skipObIntro()');
 assert.equal(a.eval('OB[ob][3]'),'notif');
 a.c.obNext();await a.c.endOb();
 assert.equal(a.eval('OB[ob][3]'),'notif');
 assert.ok(!a.eval('seen()'),'No shortcut through permission steps');
 const attempt=async name=>{a.eval('obPermissionReadyAt=0');await a.c.permStep(name)};
 await attempt('notif');assert.equal(a.eval('OB[ob][3]'),'notif');
 assert.match(a.nodes.get('#perm-result').textContent,/required/);
 status.notif=true;await attempt('notif');assert.equal(a.eval('OB[ob][3]'),'sms');
 await attempt('sms');assert.equal(a.eval('OB[ob][3]'),'sms');
 await a.nodes.get('#perm-settings').onclick();assert.equal(smsSettings,1);
 await a.c.resumeObPermission();assert.equal(a.eval('OB[ob][3]'),'sms');
 status.sms=true;await a.c.resumeObPermission();assert.equal(a.eval('OB[ob][3]'),'overlay');
 await attempt('overlay');assert.equal(overlaySettings,1);
 await a.c.resumeObPermission();assert.equal(a.eval('OB[ob][3]'),'overlay');
 overlay.allowed=true;a.listeners.quickspendResume();await settle();
 assert.equal(a.eval('OB[ob][3]'),'upi');assert.ok(overlay.enabled);
 await attempt('upi');assert.equal(upiSettings,1);assert.equal(upi.enabled,true);
 a.listeners.quickspendResume();await settle();
 assert.equal(a.eval('OB[ob][3]'),'upi');assert.ok(!a.eval('seen()'));
 upi.allowed=true;status.sms=false;
 a.listeners.quickspendResume();await settle();
 assert.equal(a.eval('OB[ob][3]'),'sms');assert.ok(!a.eval('seen()'),'Check every permission before finishing');
 status.sms=true;await attempt('sms');await attempt('overlay');await attempt('upi');
 assert.equal(a.eval('seen()'),'1');assert.ok(!a.nodes.get('#onb').classList.contains('on'));
 assert.equal(upiSettings,1,'An existing UPI grant does not reopen Settings');
 a.c.openAccount(true);html=a.nodes.get('#account-setup').innerHTML;
 assert.doesNotMatch(html,/<img|ftue-art/);assert.match(html,/account-secondary obtn/);assert.match(html,/finishAccountSetup\(\)/);
 console.log('PASS: setup requires all four Android permissions and handles denied access and Settings returns');
})().catch(e=>{console.error(e);process.exit(1)});
