const assert=require('assert');const{app,settle}=require('./app-inbox.cjs');
(async()=>{
 let a=app({savedLanguage:'en'});await settle();a.eval("$('#onb').classList.remove('on');$('#account-setup').classList.remove('on');$('#language-choice').classList.remove('on');$('#sheet').classList.remove('on')");const original=a.c.document.querySelector;a.c.document.querySelector=s=>s==='.onb.on'?null:original(s);
 let starts=0,restarts=0,dismissed=[],alerts=true,downloaded=false,cancelled=false;
 a.c.updateBridge=()=>({check:async()=>({available:true,downloaded,versionCode:1037,promptAllowed:true,installStatus:downloaded?11:0}),start:async()=>{starts++;return{cancelled}},complete:async()=>{restarts++},dismiss:async x=>dismissed.push(x.versionCode),status:async()=>({enabled:alerts,allowed:true,configured:true}),setAlerts:async x=>{alerts=x.enabled},openStore:async()=>{}});
 await a.c.checkForAppUpdate(false);assert.equal(a.nodes.get('#app-update-banner').hidden,false);assert.match(a.nodes.get('#app-update-banner').innerHTML,/Update now/);
 await a.c.dismissAppUpdate();assert.deepEqual(dismissed,[1037]);assert.equal(a.nodes.get('#app-update-banner').hidden,true);
 a.eval("appUpdate.lastCheck=0;$('#sheet').classList.add('on')");await a.c.checkForAppUpdate(false);assert.equal(a.nodes.get('#app-update-banner').hidden,true,'No automatic prompt over an active form');
 await a.c.checkForAppUpdate(true);assert.equal(a.nodes.get('#updates-result').textContent,'QuickSpend update available');await a.c.startAppUpdate();assert.equal(starts,1);
 a.eval("$('#sheet').classList.remove('on');appUpdate.lastCheck=0");downloaded=true;await a.c.checkForAppUpdate(false);assert.match(a.nodes.get('#app-update-banner').innerHTML,/Restart and update/);await a.c.startAppUpdate();assert.equal(restarts,1);
 downloaded=false;cancelled=true;await a.c.checkForAppUpdate(true);await a.c.startAppUpdate();assert.equal(a.eval('appUpdate.downloading'),false,'Cancelled Play dialog must not start download polling');assert.equal(a.nodes.get('#app-update-banner').hidden,true);
 await a.c.toggleUpdateAlerts();assert.equal(alerts,false);assert.equal(a.nodes.get('#update-alert-toggle').textContent,'Turn on update notifications');
 a.c.updateBridge=()=>({check:async()=>{throw Error('Could not check updates. Open Google Play to check.')}});await a.c.checkForAppUpdate(true);assert.match(a.nodes.get('#updates-result').textContent,/Could not check/,'Offline error must not claim up-to-date');
 console.log('PASS: optional update banner, Later, no prompt over forms, manual check, download/restart, update-alert opt-out, offline feedback');
})().catch(e=>{console.error(e);process.exit(1)});
