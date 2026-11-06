const assert=require('assert');const{app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app({savedLanguage:'en'});await settle();
 const created=new Date(2026,9,3).getTime();
 a.eval(`tx=[{id:1,t:'Expense',a:250,c:'Food',m:'Before',ts:new Date(2026,9,2).getTime(),acc:'Main account'},{id:2,t:'Expense',a:300,c:'Food',m:'After',ts:new Date(2026,9,4).getTime(),acc:'Cash'},{id:3,t:'Expense',a:700,c:'Health',m:'Not selected',ts:new Date(2026,9,5).getTime(),acc:'Main account'},{id:4,t:'Income',a:10000,c:'Salary',m:'Work',ts:new Date(2026,9,6).getTime(),acc:'Main account'}];budgetMonth='2026-10';budgets={'2026-10':{balance:15000,createdAt:${created},allocations:{Food:4000,Travel:3000}}};`);
 let b=a.eval("budgetSummary('2026-10')");assert.equal(b.balance,15000);assert.equal(b.totalSpent,300,'only expenses after budget creation count');assert.equal(b.spent.Food,300);assert.equal(b.spent.Health,undefined,'unselected categories stay outside budget');assert.equal(b.remaining,14700);assert.deepEqual(b.selected,['Food','Travel']);
 const txCount=a.eval('tx.length');a.eval('editBudget();budgetDraft.balance=16000;budgetDraft.allocations={Food:5000,Travel:3000};saveBudget()');assert.equal(a.eval('tx.length'),txCount,'budget balance must not create an account transaction');assert.equal(a.eval("budgets['2026-10'].balance"),16000);assert.equal(a.eval("budgets['2026-10'].createdAt"),created,'editing retains budget start boundary');
 a.eval('editBudget();budgetDraft.allocations.Food=20000;saveBudget()');assert.equal(a.eval("budgets['2026-10'].allocations.Food"),5000);assert.match(a.nodes.get('#budget-error').textContent,/exceed/);
 a.eval("budgetMonth='2026-10';openBudget()");const html=a.nodes.get('#sheet').innerHTML;assert.match(html,/Separate budget balance/);assert.match(html,/Selected categories/);assert.doesNotMatch(html,/Health/);assert.doesNotMatch(html,/Recorded income/);
 a.eval('persist()');const saved=a.eval("localStorage.getItem('qs1')");const reloaded=app({savedLanguage:'en',savedState:saved});await settle();assert.equal(reloaded.eval("budgets['2026-10'].createdAt"),created);assert.equal(reloaded.eval("budgets['2026-10'].balance"),16000);
 const migrated=a.eval("normaliseBudgets({'2026-09':{income:9000,allocations:{Food:1000}}})['2026-09']");assert.equal(migrated.balance,9000);assert.ok(migrated.createdAt>0,'legacy budgets migrate with a safe month boundary');
 a.eval('openReset()');await a.c.resetAll();assert.equal(a.eval('Object.keys(budgets).length'),0);
 console.log('PASS: separate budget balance, creation boundary, selected categories, main-account isolation, edit persistence, migration and reset');
})().catch(e=>{console.error(e);process.exit(1)});
