const assert=require('node:assert/strict');
const {app,settle}=require('./app-inbox.cjs');

(async()=>{
 const a=app({savedLanguage:'en'});await settle();
 a.inbox.items=[];a.eval('tx=[];pending=[];recur=[]');
 const bankSms=(sourceKey,suffix)=>({sourceKey,from:'VK-HDFCBK-S',body:'Rs.125 debited '+(suffix?'from A/c XX'+suffix+' ':'')+'at Shop on today',ts:Date.now()});
 a.inbox.items.push(bankSms('first','1234'));await a.c.pull(true);
 a.eval("openSheet(pending.find(p=>p.sourceKey==='first'));chooseEntryAccount(1);save()");await settle();
 assert.equal(a.eval("bankAccounts['hdfc:1234'].account"),'Cash');
 a.inbox.items.push(bankSms('same-card','1234'),bankSms('different-card','9876'),bankSms('no-digits',''));
 await a.c.pull(true);
 for(const [key,account] of [['same-card','Cash'],['different-card','Main account'],['no-digits','Cash']]){
  a.eval(`openSheet(pending.find(p=>p.sourceKey==='${key}'))`);
  assert.equal(a.eval('cur.acc'),account,key)
 }
 const cashIndex=a.eval("accts.findIndex(x=>x.n==='Cash')");
 assert.equal(a.c.changeAccount(cashIndex,'Everyday'),true);
 assert.equal(a.eval("bankAccounts['hdfc:1234'].account"),'Everyday');
 const restored=app({savedLanguage:'en',savedState:a.eval("localStorage.getItem('qs1')")});await settle();
 assert.equal(restored.eval("bankAccountFor('hdfc:1234','HDFC 1234')"),'Everyday');
 a.eval("openSheet(pending.find(p=>p.sourceKey==='same-card'));chooseEntryAccount(0)");
 const persist=a.c.persist;a.c.persist=()=>false;a.c.save();a.c.persist=persist;
 assert.equal(a.eval("bankAccounts['hdfc:1234'].account"),'Everyday','Failed save must not learn the account');
 a.c.initialiseCapture({
  payment:{id:1,m:'Fixture',a:125,kind:'Expense'},sender:'VK-HDFCBK-S',body:'Rs.125 debited from A/c XX1234 at Shop on today',
  accounts:[{n:'Main account'},{n:'Everyday'}],people:[],subscriptions:[],emiPlans:[],categories:{},customCategories:{},
  bankAccounts:{'hdfc:1234':{account:'Everyday',updatedAt:Date.now()+1000}},language:'en',colorMode:'dark',categoryPicker:false
 });
 assert.equal(a.eval('cur.acc'),'Everyday','Shared floater renderer uses the bank choice from native state');

 const routing=app({savedLanguage:'en'});await settle();
 routing.eval("accts.push({n:'SBI',i:'🏦'},{n:'HDFC 4567',i:'🏦'},{n:'Personal',i:'🏦'});pending=[];tx=[]");
 assert.equal(routing.c.changeAccount(2,'Adi account'),true);
 const incoming=(sourceKey,from,body)=>({sourceKey,from,body,ts:Date.now()});
 routing.inbox.items=[
  incoming('sbi-in','VK-SBIUPI-S','Your a/c ending in 7890 credited by Rs.95'),
  incoming('hdfc-out','VK-HDFCBK-S','Rs.125 debited from card **4567'),
  incoming('other-hdfc','VK-HDFCBK-S','Rs.25 debited from card **5555'),
  incoming('unmatched','VK-ICICIB-S','Rs.99 debited from Account XX2323')
 ];
 routing.inbox.entries=[{nativeKey:'old-auto',t:'Expense',a:440,m:'Shop',c:'Food',acc:'SBI 7890',smsFrom:'VK-SBIUPI-S',smsBody:'Rs.440 debited from A/c XX7890'}];
 await routing.c.pull(true);
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='sbi-in').bankKey"),'sbi:7890');
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='sbi-in').acc"),'Adi account');
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='hdfc-out').acc"),'HDFC 4567');
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='other-hdfc').acc"),'Main account');
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='unmatched').acc"),'Main account');
 assert.equal(routing.eval("tx.find(p=>p.nativeKey==='old-auto').acc"),'Adi account');
 assert.deepEqual(JSON.parse(routing.eval("JSON.stringify(accts.map(a=>a.n))")),['Main account','Cash','Adi account','HDFC 4567','Personal']);
 assert.equal(routing.eval("safeCloudData().accts.find(a=>a.n==='Adi account').smsBank"),'sbi');
 routing.eval("openSheet(pending.find(p=>p.sourceKey==='unmatched'));save()");await settle();
 assert.equal(routing.eval("bankAccounts['icici:2323']"),undefined,'An unrecognized fallback does not create a remembered bank link');
 const afterRename=app({savedLanguage:'en',savedState:routing.eval("localStorage.getItem('qs1')")});await settle();
 assert.equal(afterRename.c.bankAccountFor('sbi:7890','SBI 7890'),'Adi account');

 const b=app({savedLanguage:'en'});await settle();b.eval('tx=[];pending=[];recur=[]');
 const records=[{id:80,sourceKey:'sms-a',ts:1763000000000,a:100,m:'Cafe',t:'Expense',acc:'Cash'},
                {id:81,sourceKey:'sms-b',ts:1763000000000,a:100,m:'Cafe',t:'Expense',acc:'Main account'}];
 const older={tx:records};
 b.c.mergeJSON(older);b.c.mergeJSON(older);assert.equal(b.eval('tx.length'),2);
 b.c.deleteTransaction(b.eval('tx[0].id'));b.c.mergeJSON(older);assert.equal(b.eval('tx.length'),1);
 assert.equal(b.eval('tx[0].sourceKey'),'sms-b');
 const manual={tx:[{id:60,ts:1763000000000,a:125,m:'Meal',t:'Expense',acc:'Cash'}]};
 b.c.mergeJSON(manual);b.c.mergeJSON(manual);assert.equal(b.eval('tx.length'),2);
 b.c.deleteTransaction(b.eval('tx[1].id'));b.c.mergeJSON(manual);assert.equal(b.eval('tx.length'),1,'Manual deletion is retained on restore too');
 b.eval('tx=[]');
 b.c.importCSV('Date,Amount,Description,Account\n2026-11-01,99,Cafe,Cash\n2026-11-01,99,Cafe,Main account');
 assert.equal(b.eval('tx.length'),2);

 let offline=true,requests=0;
 const c=app({savedLanguage:'en',accountApi:{status:async()=>({configured:true}),upload:async()=>{
  requests++;if(offline)throw Object.assign(Error('Temporary outage'),{code:'CLOUD_UNAVAILABLE'});return{revision:1}
 }}});await settle();
 c.eval("cloud.account={uid:'qa'};cloud.enabled=true;cloud.ready=true");
 await c.c.saveCloudNow(false);assert.equal(c.eval('cloud.enabled'),true);assert.match(c.c.cloudStatusText(),/Waiting to sync/);
 offline=false;await c.c.saveCloudNow(false);assert.equal(requests,2);assert.equal(c.eval('cloud.revision'),1);
 c.eval(`tx=[];const first=new Date().getFullYear()*12+new Date().getMonth();
  recur=[{t:'Expense',emi:true,dated:true,m:'Laptop',a:1000,total:2000,months:2,left:2,startDate:monthDate(first,1),endDate:monthDate(first+1,1)}];
  openSheet({manual:true,m:'First part',a:1});selectEmiPlan(0);save()`);
 assert.equal(c.eval('emiProgress().paid'),1);assert.equal(c.eval('emiProgress().remaining'),1999);
 assert.match(c.eval('emiBreakdown(recur[0],0)'),/Partially paid/);
 c.eval("openSheet({manual:true,m:'Remainder',a:999});selectEmiPlan(0);save()");assert.equal(c.eval('emiProgress().paid'),1000);
 c.eval("openSheet({manual:true,m:'Invalid amount',a:0.001});save()");assert.equal(c.eval('tx.length'),2);
 assert.match(c.eval('noteStatus'),/two decimal places/);
 console.log('PASS: bank account memory across capture surfaces and restart, restore identity/deletions, offline sync recovery, and partial EMI');
})().catch(e=>{console.error(e);process.exit(1)});
