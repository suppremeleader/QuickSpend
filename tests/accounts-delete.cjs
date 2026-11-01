const assert=require('assert');const{app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app({savedLanguage:'en'});await settle();
 a.eval("tx=[];pending=[];recur=[];accts=[{n:'Main account',i:'🏦'},{n:'Cash',i:'👛'}];view='All';render()");
 assert.equal(a.eval('JSON.stringify(accountNames())'),JSON.stringify(['All','Cash']),'Unused placeholder bank is not a fresh-app card');
 a.c.openAccountPicker();assert.match(a.nodes.get('#sheet').innerHTML,/Add account/);a.eval('closeSheet();addA=true;render()');assert.match(a.nodes.get('#s-home').innerHTML,/id="an"/,'Add account opens its input');a.eval("$('#an').value='HDFC'");a.c.newAcc();assert.equal(a.eval('view'),'HDFC');
 const track=a.eval("$('#account-track')");track.offsetLeft=10;track.children=[{offsetLeft:10},{offsetLeft:322},{offsetLeft:634}];track.scrollLeft=0;a.c.syncAccountCarousel();assert.equal(track.scrollLeft,624,'Selected bank card stays in view after render');
 a.c.setTimeout=fn=>{fn();return 1};track.scrollLeft=312;a.c.accountCarouselScroll(track);assert.equal(a.eval('view'),'Cash','Swipe selects Cash and updates the transaction filter');
 a.eval("tx=[{id:900,t:'Expense',a:100,m:'Shop',c:'Food',ts:Date.now(),acc:'Cash',sourceKey:'payment',nativeKey:'payment'}];pending=[];view='All';render()");
 a.c.confirm=()=>false;a.c.deleteTransaction(900);assert.equal(a.eval('tx.length'),1,'Cancel keeps the transaction');
 a.c.confirm=()=>true;const persist=a.c.persist;a.c.persist=()=>false;a.c.deleteTransaction(900);assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('deletedSource("payment")'),false,'Failed deletion rolls back suppression metadata');a.c.persist=persist;
 a.c.deleteTransaction(900);assert.equal(a.eval('tx.length'),0);assert.equal(a.eval('deletedSource("payment")'),true);assert.equal(a.eval('accountTotals("All").bal'),0);
 a.inbox.entries.push({nativeKey:'payment',sourceKey:'payment',t:'Expense',a:100,c:'Food',m:'Shop',ts:Date.now(),acc:'Cash'});await a.c.pull(true);assert.equal(a.eval('tx.length'),0);assert.equal(a.eval('pending.length'),0,'Deleted payment is not resurrected from either native path');
 assert.equal(a.eval('safeCloudData().deletedSources.includes("payment")'),true,'Backup carries deletion markers');
 const saved=a.eval("localStorage.getItem('qs1')");const b=app({savedLanguage:'en',savedState:saved});await settle();assert.equal(b.eval('tx.length'),0);assert.equal(b.eval('pending.length'),0,'Deletion survives reload and inbox recovery');
 b.inbox.items.push({sourceKey:'different-payment',from:'HDFC',body:'Rs.25 debited at Cafe',amount:25,ts:Date.now()});await b.c.pull(true);assert.equal(b.eval('pending.length'),1,'New legitimate payments still import');
 console.log('PASS: account swipe/selection positioning, Add account, cancellation/rollback, durable delete and native reimport suppression');
})().catch(e=>{console.error(e);process.exit(1)});
