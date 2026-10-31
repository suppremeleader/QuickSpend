const fs=require('fs'),vm=require('vm'),assert=require('assert');
const html=fs.readFileSync(require('path').join(__dirname,'../index.html'),'utf8');
const importer=html.slice(html.indexOf('let pullingNative=false;'),html.indexOf('function openFloatingSettings()'));
function setup(snapshot,{storageWorks=true}={}){
 const seen=new Set(),nativePending=JSON.parse(JSON.stringify(snapshot.items||[])),nativeSaved=JSON.parse(JSON.stringify(snapshot.entries||[]));let acknowledged=[],completed=[],writes=0;
 const nodes={};const node=()=>({classList:{add(){},contains:()=>false}});
 const P={drain:async()=>({items:nativePending,entries:nativeSaved}),acknowledgeSaved:async({keys})=>{acknowledged.push(...keys);for(let i=nativeSaved.length-1;i>=0;i--)if(keys.includes(nativeSaved[i].nativeKey))nativeSaved.splice(i,1)},completeSms:async({sourceKey})=>{completed.push(sourceKey);for(let i=nativePending.length-1;i>=0;i--)if(nativePending[i].sourceKey===sourceKey)nativePending.splice(i,1)}};
 const context={SB:()=>P,tx:[],pending:[],speed:[],smsSeen:[],cur:{},sid:200,accts:[{n:'Main account',i:'🏦'}],CATS:{Expense:[['','Food'],['','Travel']],Income:[['','Salary']],Lent:[['','Friend']],Borrowed:[['','Family']]},parseSms:(from,body)=>/invalid/.test(body)?null:{m:'Shop',a:100,kind:'Expense',acc:'HDFC 1234'},fd:()=> '31 Oct',closeSheet(){},render(){},persist:()=>{writes++;return storageWorks},$:s=>nodes[s]||(nodes[s]=node()),openSheet(){},console:{warn(){}}};
 vm.createContext(context);vm.runInContext(importer,context);
 return {c:context,nativeSaved,nativePending,acknowledged,completed,get writes(){return writes}};
}
(async()=>{
 const real={sourceKey:'sms-1',from:'HDFC',body:'Rs.100 debited',ts:100};
 let h=setup({items:[real,{...real,sourceKey:'demo-1',demo:true}]});
 await h.c.pull(true);assert.equal(h.c.pending.length,1);assert.equal(h.c.pending[0].sourceKey,'sms-1');assert.equal(h.nativePending.length,2);await h.c.pull(true);assert.equal(h.c.pending.length,1);
 h=setup({entries:[{nativeKey:'saved-1',sourceKey:'saved-1',t:'Expense',a:125,c:'Food',m:'Shop',ts:100}]});
 await h.c.pull(true);assert.equal(h.c.tx.length,1);assert.equal(h.c.tx[0].a,125);assert.deepEqual(h.acknowledged,['saved-1']);assert.equal(h.nativeSaved.length,0);await h.c.pull(true);assert.equal(h.c.tx.length,1);
 h=setup({entries:[{nativeKey:'debt-1',t:'Lent',a:250,c:'Friend',who:'Asha',ts:100}]});
 h.c.pending.push({sourceKey:'debt-1'});await h.c.pull(true);assert.equal(h.c.tx[0].who,'Asha');assert.equal(h.c.tx[0].t,'Lent');assert.equal(h.c.pending.length,0);assert.equal(h.c.speed[0],'Asha');
 h=setup({entries:[{nativeKey:'duplicate-1',t:'Expense',a:125,c:'Food',ts:100}]});h.c.tx.push({sourceKey:'duplicate-1',a:125});await h.c.pull(true);assert.equal(h.c.tx.length,1);assert.deepEqual(h.acknowledged,['duplicate-1']);
 h=setup({entries:[{nativeKey:'retain-1',t:'Income',a:800,c:'Salary',ts:100}]},{storageWorks:false});await h.c.pull(true);assert.equal(h.acknowledged.length,0);assert.equal(h.nativeSaved.length,1);
 h=setup({items:[real]});h.c.tx.push({sourceKey:'sms-1'});await h.c.pull(true);assert.deepEqual(h.completed,['sms-1']);assert.equal(h.nativePending.length,0);
 h=setup({items:[{...real,sourceKey:'bad',body:'invalid'}]});await h.c.pull(true);assert.deepEqual(h.completed,[]);assert.equal(h.c.pending.length,1);assert.equal(h.nativePending.length,1);assert.equal(h.c.pending[0].a,'');
 h=setup({entries:[{nativeKey:'borrowed-1',t:'Borrowed',a:25,c:'Family',who:'<b>Sam</b>',ts:100}]});await h.c.pull(true);assert.equal(h.c.tx[0].t,'Borrowed');assert.ok(!/[<>]/.test(h.c.tx[0].who));
 h=setup({items:[real]});h.c.smsSeen.push('sms-1');await h.c.pull(true);assert.equal(h.c.pending.length,1,'Previously seen SMS missing from Review must recover');
 h.c.pending=[];await h.c.pull(true);assert.equal(h.c.pending.length,1,'Native inbox is authoritative even if local pending is lost');
 h=setup({items:[{...real,sourceKey:'native-parser',body:'invalid',amount:250,kind:'Income',merchant:'Asha'}]});await h.c.pull(true);assert.equal(h.c.pending[0].a,250);assert.equal(h.c.pending[0].kind,'Income');
 // Exercise the actual Review Save function, including local-storage failure.
 const saveCode=html.slice(html.indexOf('function save(){'),html.indexOf('const MN='));
 function saving(works=true){
  const c={cur:{id:1,m:'Shop',a:125,type:'Expense',cat:'Food',sourceKey:'review-save',acc:'Main'},tx:[],pending:[{id:1,sourceKey:'review-save'}],recur:[],memory:{},sid:200,CATS:{Expense:[['','Food']]},SB:()=>({completeSms:async()=>{c.completed++}}),persist:()=>works,closeSheet:()=>{c.closed++},render(){},paint(){},fd:()=> '31 Oct',alert:()=>{c.alerted++},completed:0,closed:0,alerted:0};
  vm.createContext(c);vm.runInContext(saveCode,c);return c;
 }
 let s=saving();s.save();assert.equal(s.tx.length,1);assert.equal(s.pending.length,0);assert.equal(s.completed,1);s.save();assert.equal(s.tx.length,1,'Repeated Save must not duplicate');
 s=saving(false);s.save();assert.equal(s.tx.length,0);assert.equal(s.pending.length,1);assert.equal(s.completed,0);assert.equal(s.closed,0);assert.equal(s.alerted,1);
 for(const a of [-2,Infinity,'']){s=saving();s.cur.a=a;s.save();assert.equal(s.tx.length,0);assert.equal(s.pending.length,1)}
 s=saving();s.cur.type='Income';s.cur.cat='Salary';s.save();assert.equal(s.tx[0].t,'Income');
 s=saving();s.cur.type='Lent';s.cur.cat='Friend';s.cur.who='Asha';s.save();assert.equal(s.tx[0].who,'Asha');
 console.log('PASS: incoming SMS stays recoverable; native saves import once; debt type/person import; existing saved entries deduplicate; storage failure leaves native saves unacknowledged; saved payments acknowledge; unknown SMS remains for review; seen-but-missing SMS recovers; native parser fallback; Review Save and duplicate protection; failed Save rolls back; demo SMS never enters the ledger.');
})().catch(e=>{console.error(e);process.exit(1)});
