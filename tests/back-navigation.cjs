const assert=require('assert');const {app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app({savedLanguage:'en',tabs:true});await settle();a.c.closeSheet(false);a.eval('pending=[];recur=[];historyOpen=false;sheetOwnEntry=false;homeOwnEntry=false;tabOwnEntry=false;ignoredBacks=0');
 let depth=0,pushes=0,pops=[];a.c.window.history={pushState(){depth++;pushes++},replaceState(){},back(){assert.ok(depth>0,'Never Back beyond the app base');depth--;pops.push(()=>a.listeners.popstate())}};
 const flush=()=>{while(pops.length)pops.shift()()};const back=()=>{a.c.window.history.back();flush()};
 assert.equal(a.eval('window.quickSpendSystemBack()'),false,'Home remains the only system-Back exit point');
 a.c.openHistory();a.c.openSheet({manual:true,m:'Test',a:10});assert.equal(depth,2);a.c.closeSheet();flush();assert.equal(a.eval('historyOpen'),true,'UI close consumes only editor history, preserving history screen');assert.equal(depth,1);a.c.closeHistory();flush();assert.equal(depth,0);
 a.c.openHistory();a.c.openSheet({manual:true,m:'Test',a:10});assert.equal(a.eval('window.quickSpendSystemBack()'),true);flush();assert.equal(a.eval('historyOpen'),true);assert.equal(a.nodes.get('#sheet').classList.contains('on'),false);assert.equal(a.eval('window.quickSpendSystemBack()'),true);flush();assert.equal(depth,0);
 a.c.openHistory();a.c.openSheet({manual:true,m:'Test',a:10});back();assert.equal(a.nodes.get('#sheet').classList.contains('on'),false);assert.equal(a.eval('historyOpen'),true);back();assert.equal(a.eval('historyOpen'),false);assert.equal(depth,0);
 a.c.navigateTab('insights');a.c.openData();assert.equal(depth,2);back();assert.equal(a.eval('currentTab'),'insights');back();assert.equal(a.eval('currentTab'),'home');assert.equal(depth,0);
 a.c.navigateTab('review');a.c.navigateTab('friends');assert.equal(depth,1,'Switching secondary tabs does not accumulate exit steps');a.c.navigateTab('home');flush();assert.equal(depth,0);assert.equal(a.eval('currentTab'),'home');
 a.c.selectHomePage('savings');a.c.selectHomePage('emi');assert.equal(depth,1);a.c.selectHomePage('overview');flush();assert.equal(depth,0);
 a.c.selectHomePage('savings');a.c.navigateTab('insights');assert.equal(depth,1,'Tab replaces Home subsection step');back();assert.equal(a.eval('currentTab'),'home');assert.equal(a.eval('homePage'),'overview');assert.equal(depth,0);
 a.c.openSheet({m:'Bank EMI',a:455.5,sourceKey:'pending-emi',kind:'Expense',note:'Keep this comment',acc:'Cash'});assert.equal(depth,1);a.c.openEmiFromEntry();assert.equal(depth,2);assert.equal(a.eval('window.quickSpendSystemBack()'),true);flush();assert.equal(a.eval('cur.a'),455.5);assert.equal(a.eval('cur.note'),'Keep this comment');assert.equal(depth,1);a.c.closeSheet();flush();assert.equal(depth,0);
 console.log('PASS: asynchronous UI Back and system Back unwind editor/history/settings/tabs/subsections without ghost exit steps');
})().catch(e=>{console.error(e);process.exit(1)});
