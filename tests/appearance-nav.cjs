const assert=require('assert'),fs=require('fs'),{app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app({savedLanguage:'en',tabs:true});await settle();assert.equal(a.eval('colorMode'),'frog','New users start in Frog');assert.equal(a.nodes.get('html').attrs['data-theme'],'frog');
 const tabs=['home','review','friends','insights'].map(key=>a.nodes.get('#tab-'+key));assert.equal(tabs[0].attrs['aria-selected'],'true');assert.ok(tabs[0].classList.contains('on'));for(const tab of tabs){assert.match(tab.innerHTML,/<svg/);assert.doesNotMatch(tab.innerHTML,/>Home<|>Review<|>Insights<|>Lent/);assert.ok(tab.attrs['aria-label'])}
 tabs[1].click();assert.equal(a.eval('currentTab'),'review');assert.equal(tabs[1].attrs['aria-selected'],'true');assert.equal(tabs[0].attrs['aria-selected'],'false');assert.ok(tabs[1].classList.contains('on'));
 tabs[1].listeners.keydown({key:'ArrowRight',preventDefault(){}});assert.equal(a.eval('currentTab'),'friends','Keyboard arrows navigate icon-only tabs');assert.equal(tabs[2].attrs['aria-selected'],'true');
 tabs[2].listeners.keydown({key:'Home',preventDefault(){}});assert.equal(a.eval('currentTab'),'home');assert.equal(tabs[0].attrs['aria-selected'],'true');
 a.eval("homePage='emi';render()");assert.ok(a.nodes.get('#tabs').classList.contains('no-center-action'));assert.equal(a.nodes.get('#entry-action').className,'fab emi-add-plan');assert.equal(a.nodes.get('#entry-action').textContent,'Add a plan');assert.equal(tabs[0].attrs['aria-selected'],'true','Home remains highlighted for its EMI subpage');
 a.eval("homePage='overview';render()");assert.ok(!a.nodes.get('#tabs').classList.contains('no-center-action'));assert.match(a.nodes.get('#entry-action').innerHTML,/<span aria-hidden="true">\+<\/span>/);
 const snapshot=a.eval('JSON.stringify(safeCloudData())'),transport=a.c.window.Capacitor.nativePromise,modes=[];a.c.window.Capacitor.nativePromise=async(plugin,method,options)=>{if(method==='setColorMode'){modes.push(options.mode);return{}}return transport(plugin,method,options)};
 await a.c.setColorMode('light');assert.equal(a.eval("localStorage.getItem('qs_color_mode')"),'light');assert.equal(a.nodes.get('html').attrs['data-theme'],'light');assert.equal(a.nodes.get('html').style.colorScheme,'light');assert.equal(modes.at(-1),'light');assert.equal(a.eval('JSON.stringify(safeCloudData())'),snapshot,'Appearance does not change finance records');assert.notDeepEqual(a.c.chartColors(),a.eval('CHART_COLORS'));assert.ok(new Set(a.c.chartColors()).size>=10);assert.match(a.nodes.get('#sheet').innerHTML,/color-choice selected/);
 const b=app({savedLanguage:'en',savedColorMode:'light'});await settle();assert.equal(b.eval('colorMode'),'light','Mode survives restart');assert.equal(b.nodes.get('html').attrs['data-theme'],'light');

 await a.c.setColorMode('frog');assert.equal(a.nodes.get('html').attrs['data-theme'],'frog');assert.equal(modes.at(-1),'frog');
 await a.c.setColorMode('aurora');assert.equal(modes.at(-1),'aurora');assert.equal(a.nodes.get('html').attrs['data-theme'],'aurora');assert.equal(a.nodes.get('html').style.colorScheme,'light');
 assert.equal(a.eval('JSON.stringify(safeCloudData())'),snapshot);
 const aurora=app({savedLanguage:'en',savedColorMode:'aurora'});await settle();assert.equal(aurora.eval('colorMode'),'aurora','Aurora survives restart');
 await a.c.setColorMode('light');
 const categories=Array.from({length:14},(_,i)=>({name:'Rank '+(i+1),amount:14-i}));
 const chart=a.c.spendingBreakdown({categories,expense:105});
 assert.equal((chart.match(/class="category-dot"/g)||[]).length,10);assert.match(chart,/>Top 10</);
 assert.match(chart,/Rank 10</);assert.doesNotMatch(chart,/Rank 11/);assert.match(chart,/Remaining categories \(4\) · ₹10/);
 assert.match(chart,/Spent<.*₹105</);assert.doesNotMatch(a.c.spendingBreakdown({categories:[],expense:0}),/Top 10/);
 const storage=a.c.localStorage.setItem;a.c.localStorage.setItem=(k,v)=>{if(k==='qs_color_mode')throw Error('Full');storage(k,v)};await a.c.setColorMode('dark');assert.equal(a.eval('colorMode'),'light','Failed preference save keeps the previous mode');a.c.localStorage.setItem=storage;await a.c.setColorMode('dark');assert.equal(a.eval('colorMode'),'dark');
 const plan1={open:true},plan2={open:false};const query=a.c.document.querySelectorAll;a.c.document.querySelectorAll=selector=>selector==='#s-home .emi-plan'?[plan1,plan2]:query(selector);a.c.syncEmiAccordion(plan1,0);plan2.open=true;a.c.syncEmiAccordion(plan2,1);assert.equal(plan1.open,false);assert.equal(plan2.open,true);a.c.syncEmiAccordion(plan1,0);assert.equal(a.eval('expandedEmiIndex'),1,'Closing the older plan cannot clear the new one');plan2.open=false;a.c.syncEmiAccordion(plan2,1);assert.equal(a.eval('expandedEmiIndex'),null);
 await a.c.chooseLanguage('hi');a.c.openColorMode();assert.match(a.nodes.get('#sheet').innerHTML,/रंग मोड|डार्क|लाइट/);assert.equal(tabs[0].attrs['aria-label'],'होम');a.c.openData();assert.match(a.nodes.get('#sheet').innerHTML,/रंग मोड/);
 const source=fs.readFileSync('index.html','utf8');assert.match(source,/name="emi-schedules"/);assert.match(source,/role="tablist"/);assert.match(source,/prefers-reduced-motion:reduce/);
 console.log('PASS: default/persisted themes, native preference transport, finance isolation, icon-only active navigation and keyboard support, single EMI accordion and Hindi');
})().catch(e=>{console.error(e);process.exit(1)});
