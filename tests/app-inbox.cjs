const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const html=fs.readFileSync(path.join(__dirname,'../index.html'),'utf8');
const script=html.split('<script>')[1].split('</script>')[0];
function app({seen=true,broken=false,savedLanguage=null,languageFails=false,savedState=null,accountApi=null}={}){
 const nodes=new Map(),listeners={},documentListeners={};function node(s){if(!nodes.has(s)){const classes=new Set();nodes.set(s,{innerHTML:'',textContent:'',style:{},classList:{add:x=>classes.add(x),remove:x=>classes.delete(x),contains:x=>classes.has(x),toggle(){}},attrs:{},setAttribute(k,v){this.attrs[k]=v},listeners:{},addEventListener(name,fn){this.listeners[name]=fn},contains(target){return target===this||target?.inSheet===true},dataset:{}})}return nodes.get(s)}
 const data=new Map(seen?[['qs_ob','1'],['qs_p2','1']]:[]);if(savedLanguage)data.set('qs_language',savedLanguage);if(savedState)data.set('qs1',savedState);let completed=[],acks=[];
 const inbox={items:[{sourceKey:'payment',from:'HDFC',body:'Rs.125 debited at Shop on today',amount:125,ts:Date.now()}],entries:[]};
 const c={Date,Number,console:{warn(){}},Set,Map,JSON,document:{documentElement:node('html'),querySelector:node,querySelectorAll:()=>[],addEventListener:(name,fn)=>documentListeners[name]=fn,hidden:false},localStorage:{getItem:k=>data.get(k)||null,setItem:(k,v)=>data.set(k,v),removeItem:k=>data.delete(k)},window:{Capacitor:{isNativePlatform:()=>true,nativePromise:async(p,m,o)=>{
  if(p==='AccountBridge'){if(accountApi&&accountApi[m])return accountApi[m](o);if(m==='status')return {configured:false};return{}}
  if(m==='setLanguage'){if(languageFails)throw Error('Language unavailable');return {language:o.language}}
  if(m==='inboxSnapshot'){if(broken)throw Error('Inbox unavailable');return{payload:JSON.stringify(inbox)}}
  if(m==='drain'){if(broken)throw Error('Inbox unavailable');return inbox}
  if(m==='completeSms'){completed.push(o.sourceKey);inbox.items=inbox.items.filter(x=>x.sourceKey!==o.sourceKey);return{}}
  if(m==='acknowledgeSaved'){acks.push(...o.keys);inbox.entries=inbox.entries.filter(x=>!o.keys.includes(x.nativeKey));return{}}
  if(m==='overlayStatus')return{allowed:true,enabled:true,running:true};return{}
 }},addEventListener:(event,fn)=>listeners[event]=fn},setInterval(){},setTimeout(){},clearTimeout(){},confirm:()=>true,alert(){}};
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
 a=app({broken:true});await settle();assert.match(a.nodes.get('#review-sync').textContent,/Inbox unavailable/,'Sync failure must be visible');
 console.log('PASS: full app boot, onboarding payment retention, JSON native inbox, one-tap uncategorised Save, native overlay saves, edit without duplicates, Android resume refresh, visible sync errors');
})().catch(e=>{console.error(e);process.exit(1)});
