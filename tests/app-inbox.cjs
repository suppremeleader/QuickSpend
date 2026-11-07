const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const html=fs.readFileSync(path.join(__dirname,'../index.html'),'utf8');
const script=html.split('<script>')[1].split('</script>')[0];
function app({seen=true,broken=false,savedLanguage=null,languageFails=false,savedState=null,accountApi=null,savedColorMode=null,tabs=false}={}){
 const nodes=new Map(),listeners={},documentListeners={};function node(s){if(!nodes.has(s)){const classes=new Set();nodes.set(s,{innerHTML:'',textContent:'',style:{},classList:{add:x=>classes.add(x),remove:x=>classes.delete(x),contains:x=>classes.has(x),toggle(x,force){const on=force===undefined?!classes.has(x):force;if(on)classes.add(x);else classes.delete(x);return on}},attrs:{},setAttribute(k,v){this.attrs[k]=v},getAttribute(k){return this.attrs[k]??null},click(){this.onclick?.()},focus(){c.document.activeElement=this},listeners:{},addEventListener(name,fn){this.listeners[name]=fn},contains(target){return target===this||target?.inSheet===true},dataset:{}})}return nodes.get(s)}
 const data=new Map(seen?[['qs_ob','1'],['qs_p2','1']]:[]);if(savedLanguage)data.set('qs_language',savedLanguage);if(savedColorMode)data.set('qs_color_mode',savedColorMode);if(savedState)data.set('qs1',savedState);let completed=[],acks=[];
 const inbox={items:[{sourceKey:'payment',from:'HDFC',body:'Rs.125 debited at Shop on today',amount:125,ts:Date.now()}],entries:[]};
 const c={Date,Number,console:{warn(){}},Set,Map,JSON,document:{documentElement:node('html'),querySelector:node,querySelectorAll:selector=>!tabs?[]:selector==='.tab'?['home','review','friends','insights'].map(key=>{const el=node('#tab-'+key);el.dataset.t=key;return el}):selector==='.scr'?['home','review','friends','insights','history'].map(key=>node('#s-'+key)):[],addEventListener:(name,fn)=>documentListeners[name]=fn,hidden:false},localStorage:{getItem:k=>data.get(k)||null,setItem:(k,v)=>data.set(k,v),removeItem:k=>data.delete(k)},window:{Capacitor:{isNativePlatform:()=>true,nativePromise:async(p,m,o)=>{
  if(p==='AccountBridge'){if(accountApi&&accountApi[m])return accountApi[m](o);if(m==='status')return {configured:false};return{}}
  if(m==='setLanguage'){if(languageFails)throw Error('Language unavailable');return {language:o.language}}
  if(m==='inboxSnapshot'){if(broken)throw Error('Inbox unavailable');return{payload:JSON.stringify(inbox)}}
  if(m==='drain'){if(broken)throw Error('Inbox unavailable');return inbox}
  if(m==='completeSms'){completed.push(o.sourceKey);inbox.items=inbox.items.filter(x=>x.sourceKey!==o.sourceKey);return{}}
  if(m==='acknowledgeSaved'){acks.push(...o.keys);inbox.entries=inbox.entries.filter(x=>!o.keys.includes(x.nativeKey));return{}}
  if(m==='overlayStatus')return{allowed:true,enabled:true,running:true};return{}
 }},addEventListener:(event,fn)=>{const previous=listeners[event];listeners[event]=previous?(...args)=>{previous(...args);fn(...args)}:fn}},setInterval(){},setTimeout(){},clearTimeout(){},confirm:()=>true,alert(){}};
 vm.createContext(c);vm.runInContext(script,c);
 return{c,nodes,inbox,listeners,documentListeners,completed,acks,eval:s=>vm.runInContext(s,c)};
}
const settle=()=>new Promise(r=>setImmediate(r));
module.exports={app,settle};
if(require.main===module)(async()=>{
 let a=app({seen:false});await settle();assert.equal(a.eval('pending.length'),1);a.eval('endOb()');assert.equal(a.eval('pending.length'),1,'Completing onboarding must preserve incoming payment');
 a.eval('openSheet(pending[0])');assert.equal(a.eval('cur.cat'),'Uncategorised');a.eval('save()');await settle();assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('tx[0].c'),'Uncategorised');assert.equal(a.eval('pending.length'),0);assert.match(a.nodes.get('#s-review').innerHTML,/Saved · needs a category/);
 a.eval("openSavedReview(tx[0].id);cur.cat='Food';save()");assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('tx[0].c'),'Food');assert.equal(a.eval('tx[0].sourceKey'),'payment');
 a.inbox.items.push({sourceKey:'after-resume',from:'SBI',body:'Rs.50 debited at Cafe on today',ts:Date.now()});a.listeners.quickspendResume();await settle();assert.equal(a.eval('pending.length'),1,'Native Android resume must refresh Review');
 a.inbox.entries.push({nativeKey:'overlay-save',sourceKey:'overlay-save',t:'Expense',a:70,c:'Uncategorised',m:'Cafe',ts:Date.now()});await a.c.pull(true);assert.equal(a.eval('tx.length'),2);assert.equal(a.eval("tx.find(x=>x.nativeKey==='overlay-save').c"),'Uncategorised');assert.ok(a.acks.includes('overlay-save'));await a.c.pull(true);assert.equal(a.eval('tx.length'),2);
 a=app({savedLanguage:'en'});await settle();
 const now=Date.now(),phonepe={sourceKey:'phonepe:fixture',from:'PhonePe',amount:1,kind:'Income',merchant:'Example Sender',account:'Central Bank Of India 1234',ts:now};
 a.inbox.items.push(phonepe);await a.c.pull(true);await a.c.pull(true);
 assert.equal(a.eval("pending.filter(p=>isUpiPayment(p)).length"),1,'Repeated PhonePe inbox reads import once');
 assert.equal(a.eval("pending.find(p=>isUpiPayment(p)).acc"),'Central Bank Of India 1234');
 assert.equal(a.eval("pending.find(p=>isUpiPayment(p)).kind"),'Income');
 assert.equal(a.eval("pending.find(p=>isUpiPayment(p)).m"),'Example Sender');
 assert.equal(a.eval('tx.length'),0,'PhonePe stays pending until the user saves');
 assert.match(a.nodes.get('#s-review').innerHTML,/Income · PhonePe/);
 a.inbox.items.push({sourceKey:'sms-fixture',from:'CBI',body:'Rs.1 credited to your account XXXX1234',ts:now+1000});
 await a.c.pull(true);
 a.eval("openSheet(pending.find(p=>isUpiPayment(p)))");assert.equal(a.eval('hasPossiblePaymentDuplicate(cur)'),true);
 a.c.confirm=()=>false;a.c.save();assert.equal(a.eval('tx.length'),0,'Declining duplicate confirmation keeps both alerts in Review');
 a.c.confirm=()=>true;a.c.save();await settle();assert.equal(a.eval('tx.length'),1);
 assert.equal(a.eval('tx[0].acc'),'Central Bank Of India 1234');assert.ok(a.completed.includes(phonepe.sourceKey));
 a.eval("openSheet(pending.find(p=>p.sourceKey==='sms-fixture'))");assert.equal(a.eval('hasPossiblePaymentDuplicate(cur)'),true,'SMS also warns after saving PhonePe');
 const gpay={sourceKey:'upi:gpay',from:'Google Pay',amount:50,kind:'Expense',merchant:'Market',ts:now+3000};
 a.inbox.items.push(gpay);await a.c.pull(true);
 assert.equal(a.eval("pending.find(p=>p.sourceKey==='upi:gpay').kind"),'Expense');
 assert.match(a.nodes.get('#s-review').innerHTML,/Expense · Google Pay/);
 let access=false,enabled=false,settingsOpened=0,demos=0;
 const previousTransport=a.c.window.Capacitor.nativePromise;
 a.c.window.Capacitor.nativePromise=async (plugin,method,options)=>{
  if(plugin==='SmsBridge'){
   if(method==='upiCaptureStatus')return{enabled,allowed:access};
   if(method==='setUpiCapture')return{enabled:enabled=options.enabled,allowed:access};
   if(method==='openUpiNotificationAccess'){settingsOpened++;return{}}
   if(method==='testOverlay'){demos++;return{}}
  }
  return previousTransport(plugin,method,options)
 };
 a.c.openData();assert.doesNotMatch(a.nodes.get('#sheet').innerHTML,/PhonePe capture/);
 a.c.openFloatingSettings();await settle();assert.match(a.nodes.get('#upi-status').textContent,/off/);
 assert.match(a.nodes.get('#sheet').innerHTML,/Test floater/);
 await a.c.manageUpiAccess();assert.equal(settingsOpened,1);assert.match(a.nodes.get('#upi-status').textContent,/finish setup/);
 access=true;await a.c.refreshUpiStatus();assert.match(a.nodes.get('#upi-status').textContent,/is on/);
 const beforeDemo=a.eval('JSON.stringify({pending,tx})');
 await a.c.testFloating(true);assert.equal(demos,1);assert.match(a.nodes.get('#float-demo-status').textContent,/Demo ready/);
 assert.equal(a.eval('JSON.stringify({pending,tx})'),beforeDemo);
 a=app({broken:true});await settle();assert.match(a.nodes.get('#review-sync').textContent,/Inbox unavailable/,'Sync failure must be visible');
 console.log('PASS: full app boot, onboarding payment retention, JSON native inbox, one-tap uncategorised Save, native overlay saves, edit without duplicates, Android resume refresh, visible sync errors');
})().catch(e=>{console.error(e);process.exit(1)});
