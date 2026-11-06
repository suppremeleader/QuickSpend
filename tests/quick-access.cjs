const assert=require('assert');const{app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app({savedLanguage:'en'});await settle();
 let stored={enabled:false,Expense:['Food','Travel','Other','Shopping'],Income:['Salary','Gift','Other','Refund'],expenseCategories:['Food','Travel','Other','Shopping','Groceries','Bills','Health','Fun','Family','Refund'],incomeCategories:['Salary','Gift','Other','Refund']},writes=[],fail=false;
 a.c.SB=()=>({syncCaptureCatalog:async()=>({}),quickAccessSettings:async()=>({payload:JSON.stringify(stored)}),setQuickAccessSettings:async x=>{if(fail)throw Error('Storage unavailable');writes.push(JSON.parse(x.payload));stored={...stored,...JSON.parse(x.payload)}}});
 await a.c.openQuickAccessSettings();assert.equal(a.eval('quickDraft.enabled'),false);assert.match(a.nodes.get('#sheet').innerHTML,/Turn on Quick Access/);assert.match(a.nodes.get('#sheet').innerHTML,/Your 10-tag rail/);assert.match(a.nodes.get('#sheet').innerHTML,/>10<\/span>/,'All ten tags are previewed in settings');assert.match(a.nodes.get('#sheet').innerHTML,/Groceries/,'Custom categories can be assigned');
 a.eval("quickDraft.Expense[0]='Groceries'");await a.c.saveQuickAccessSettings(true);assert.equal(stored.enabled,true);assert.equal(writes[0].Expense[0],'Groceries');assert.deepEqual(writes[0].Income,['Salary','Gift','Other','Refund']);assert.match(a.nodes.get('#quick-result').textContent,/Quick Access is on/);
 a.eval("quickType='Income';paintQuickAccessSettings()");assert.match(a.nodes.get('#sheet').innerHTML,/Salary/);assert.doesNotMatch(a.nodes.get('#sheet').innerHTML,/option value="Food"/,'Income has separate valid targets');
 fail=true;await a.c.saveQuickAccessSettings(false);assert.equal(a.eval('quickDraft.enabled'),true,'Failed preference write keeps displayed state');assert.match(a.nodes.get('#quick-result').textContent,/Storage unavailable/);
 fail=false;await a.c.saveQuickAccessSettings(false);assert.equal(stored.enabled,false);await a.c.openQuickAccessSettings();assert.equal(a.eval('quickDraft.enabled'),false,'Reload uses device preference');
 a.eval("language='hi'");await a.c.openQuickAccessSettings();assert.match(a.nodes.get('#sheet').innerHTML,/दबाएँ। सरकाएँ। छोड़ें।/);assert.match(a.nodes.get('#sheet').innerHTML,/>10<\/span>/);
 a.eval("language='en'");a.c.SB=()=>null;await a.c.openQuickAccessSettings();assert.match(a.nodes.get('#quick-result').textContent,/Android app/);
 console.log('PASS: Quick Access off by default, native settings persistence, custom categories, separate income slots, failure feedback and unavailable bridge');
})().catch(e=>{console.error(e);process.exit(1)});

