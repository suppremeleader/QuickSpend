const assert=require('node:assert/strict');
const fs=require('node:fs');
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

 // The receiver must queue completed NEFT deposits before either alert path can run.
 const workflow=fs.readFileSync(require('node:path').join(__dirname,'../.github/workflows/build.yml'),'utf8');
 const receiver=workflow.split('public class SmsReceiver extends BroadcastReceiver {')[1].split('static void push')[0];
 const receiverPattern=name=>new RegExp(JSON.parse(receiver.match(new RegExp('Pattern '+name+' = Pattern.compile\\(("(?:[^"\\\\]|\\\\.)*")\\)'))[1]).replace(/^\(\?i\)/,''),'i');
 const keyword=receiverPattern('KW'),blocked=receiverPattern('BAD'),nonPosted=receiverPattern('NON_POSTED'),amount=receiverPattern('AMT');
 const accepts=body=>keyword.test(body)&&!blocked.test(body)&&!nonPosted.test(body.replace(/\s+/g,' '))&&amount.test(body);
 const deposit='Update! INR 12,345.67 deposited in HDFC Bank A/c XX0173 on 27-NOV-26 for NEFT Cr-TEST0000001-EXAMPLE COMPANY-EXAMPLE CUSTOMER-TESTREF.Avl bal INR 98,765.43. Cheque deposits in A/C are subject to clearing';
 assert.equal(accepts(deposit),true);
 assert.equal(accepts('INR 500.00 debited from HDFC Bank A/c XX0173'),true);
 for(const rejected of ['INR 500.00 will be deposited in HDFC Bank A/c XX0173','INR 500.00 was not deposited in HDFC Bank A/c XX0173','Avl bal INR 98,765.43. Cheque deposits in A/C are subject to clearing']){
  assert.equal(accepts(rejected),false);
  assert.equal(routing.c.parseSms('VK-HDFCBK-S',rejected),null)
 }
 const emiDebit='INR 3,200 was deducted from HDFC Bank A/c XX0173 for your EMI on 02-DEC-26';
 const nonPayments=[
  'Your INR 1,500 transaction status is pending',
  'INR 1,500 credited to A/c XX0173. Transaction status: pending',
  'Reminder: Your loan EMI of INR 3,200 is payable on 02-DEC-26. Please maintain sufficient balance.',
  'Your EMI of INR 1,500 is scheduled for auto-debit on 02-DEC-26',
  'Your NACH transaction for INR 1,500 shall be\ndeducted on 02-DEC-26',
  'Your eligible refund of INR 1,500 will be credited within 5 days'
 ];
 for(const body of nonPayments){
  assert.equal(accepts(body),false,body);
  assert.equal(routing.c.parseSms('VK-HDFCBK-S',body),null,body)
 }
 assert.equal(accepts(emiDebit),true);
 assert.equal(routing.c.parseSms('VK-HDFCBK-S',emiDebit).kind,'Expense');
 assert.equal(routing.c.parseSms('VK-HDFCBK-S',emiDebit).a,3200);
 routing.eval("pending.push({sourceKey:'old-reminder',a:1500,kind:'Income'})");
 routing.inbox.items=[
  {...incoming('old-reminder','VK-HDFCBK-S',nonPayments[2]),amount:1500,kind:'Income'},
  incoming('old-pending','VK-HDFCBK-S',nonPayments[1]),
  incoming('emi-debit','VK-HDFCBK-S',emiDebit)
 ];
 const transactionCount=routing.eval('tx.length');
 await routing.c.pull(true);
 await routing.c.pull(true);
 assert.equal(routing.eval('tx.length'),transactionCount,'Review sync cannot post ignored alerts');
 assert.equal(routing.eval("pending.filter(p=>p.sourceKey.startsWith('old-')).length"),0);
 assert.equal(routing.inbox.items.some(p=>p.sourceKey.startsWith('old-')),false);
 assert.equal(routing.eval("pending.filter(p=>p.sourceKey==='emi-debit'&&p.kind==='Expense').length"),1);
 routing.eval("openSheet(pending.find(p=>p.sourceKey==='emi-debit'));save()");
 await settle();
 await routing.c.pull(true);
 assert.equal(routing.eval("tx.filter(t=>t.sourceKey==='emi-debit'&&t.t==='Expense'&&t.a===3200).length"),1);
 const parsed=JSON.parse(JSON.stringify(routing.c.parseSms('VK-HDFCBK-S',deposit)));
 assert.equal(parsed.kind,'Income');
 assert.equal(parsed.a,12345.67);
 assert.equal(parsed.bankKey,'hdfc:0173');
 assert.equal(routing.c.parseSms('',deposit).bankKey,'hdfc:0173','Copy and paste works without a sender header');
 routing.eval("accts.push({n:'Salary bank',smsBank:'hdfc:0173'})");
 routing.inbox.items=[incoming('neft-salary','VK-HDFCBK-S',deposit)];
 await routing.c.pull(true);
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='neft-salary').acc"),'Salary bank');
 assert.equal(routing.eval("pending.find(p=>p.sourceKey==='neft-salary').kind"),'Income');

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
