/* Real Chromium smoke check. Isolated CI dependency; no production packages. */
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright'),assert=require('assert'),path=require('path'),fs=require('fs');
(async()=>{
 const browser=await chromium.launch({headless:true});try{
 const page=await browser.newPage({viewport:{width:390,height:844},deviceScaleFactor:1});page.setDefaultTimeout(30000);page.setDefaultNavigationTimeout(30000);const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(()=>{
  for(const key of ['qs_ob','qs_p2','qs_account_setup'])localStorage.setItem(key,'1');localStorage.setItem('qs_language','en');
  const tx=Array.from({length:12},(_,i)=>({id:i+1,t:i%3?'Expense':'Income',a:(i+1)*125,c:i%3?'Food':'Salary',m:i%3?'Market '+i:'Salary',note:i===1?'Weekend lunch':'',ts:Date.now()-i*86400000,acc:'Main account',d:'Today',settled:0}));
  localStorage.setItem('qs1',JSON.stringify({tx,pending:[],memory:{},speed:[],accts:[{n:'Main account',i:'🏦'},{n:'Cash',i:'👛'}],recur:[],sid:100,budgets:{}}));
 });
 fs.mkdirSync('ui-smoke-output',{recursive:true});
 await page.goto('file://'+path.join(process.cwd(),'index.html'));await page.locator('#s-home .history-row').first().waitFor();assert.equal(await page.locator('#s-home .history-row').count(),5);
 await page.screenshot({animations:'disabled',path:'ui-smoke-output/home.png'});
 await page.evaluate(()=>document.querySelector('#s-home').scrollTop=200);assert.ok(await page.locator('#s-home').evaluate(el=>el.scrollTop)>0);
 await page.locator('[data-t="insights"]').click();assert.equal(await page.locator('#s-insights').evaluate(el=>el.scrollTop),0);assert.equal(await page.getByText('Daily average',{exact:true}).count(),0);assert.equal(await page.getByText('Money over time',{exact:true}).count(),0);await page.screenshot({animations:'disabled',path:'ui-smoke-output/insights.png'});
 await page.locator('[data-t="home"]').click();assert.equal(await page.locator('#s-home').evaluate(el=>el.scrollTop),0);
 await page.locator('#s-home button').filter({hasText:'See all'}).click();await page.locator('#history-search').fill('Weekend lunch');assert.equal(await page.locator('#history-results .history-row').count(),1);assert.equal(await page.locator('#history-search').inputValue(),'Weekend lunch');assert.equal(await page.locator('#tabs').isVisible(),false);
 await page.getByRole('button',{name:'Clear filters',exact:true}).click();assert.equal(await page.locator('#history-results .history-row').count(),12);await page.locator('#history-type').selectOption('Income');assert.equal(await page.locator('#history-results .history-row').count(),4);await page.getByRole('button',{name:'Clear filters',exact:true}).click();await page.screenshot({animations:'disabled',path:'ui-smoke-output/history-dark.png'});
 await page.setViewportSize({width:320,height:720});
 assert.equal(await page.locator('.history-filters').evaluate(el=>el.scrollWidth>el.clientWidth+1),false,'History filters fit without horizontal scrolling');
 const narrowFilters=await page.locator('.history-filters select').evaluateAll(nodes=>nodes.map(el=>el.getBoundingClientRect().top));
 assert.ok(narrowFilters.some(top=>top>narrowFilters[0]),'Filters wrap on small screens');
 await page.screenshot({animations:'disabled',path:'ui-smoke-output/history-narrow.png'});
 await page.setViewportSize({width:390,height:844});
 await page.evaluate(async()=>{await setColorMode('light');closeSheet()});await page.screenshot({animations:'disabled',path:'ui-smoke-output/history-light.png'});
 await page.getByRole('button',{name:'Back',exact:true}).click();await page.locator('#s-home.on').waitFor();
 await page.locator('#s-home button').filter({hasText:'See all'}).click();await page.locator('#history-results .history-row').first().click();await page.locator('#sheet.on').waitFor();
 await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on'));assert.equal(await page.locator('#s-history.on').count(),1,'System Back closes editor before history');
 await page.evaluate(()=>window.quickSpendSystemBack());await page.locator('#s-home.on').waitFor();
 await page.locator('[data-t="insights"]').click();await page.evaluate(()=>window.quickSpendSystemBack());await page.locator('#s-home.on').waitFor();
 await page.locator('.profile-settings').first().click();await page.locator('#sheet.on').waitFor();await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on'));
 await page.evaluate(()=>{const now=new Date(),month=now.getFullYear()*12+now.getMonth();recur=[{t:'Expense',m:'Car EMI test',a:100,total:1200,months:12,left:12,startMonth:month,last:month,day:1,dated:true,startDate:monthDate(month,1),endDate:monthDate(month+11,1),emi:true,acc:'Main account'}];openSheet({manual:true,m:'Receipt test',a:25})});
 assert.equal(await page.getByRole('button',{name:'+ Add new EMI',exact:true}).count(),0,'Collapsed plans are excluded from keyboard/accessibility navigation');
 assert.equal(await page.locator('#sheet .entry-emi:not(.entry-subscriptions)').evaluate(el=>el.open),false,'EMI starts collapsed');assert.equal(await page.getByRole('button',{name:'+ Add new EMI',exact:true}).isVisible(),false);await page.locator('#sheet .entry-emi:not(.entry-subscriptions) summary').click();await page.waitForFunction(()=>cur.emiExpanded);assert.equal(await page.locator('#sheet .entry-emi:not(.entry-subscriptions)').evaluate(el=>el.open),true);
 assert.equal(await page.getByText('Existing EMI',{exact:true}).count(),0);assert.equal(await page.locator('#sheet .entry-emi:not(.entry-subscriptions) .emi-entry-plans').getByRole('button',{name:/Car EMI test/}).count(),1);
 await page.getByRole('button',{name:'Add picture',exact:true}).click();
 assert.equal(await page.getByRole('button',{name:'Camera',exact:true}).count(),1);
 assert.equal(await page.getByRole('button',{name:'Browse',exact:true}).count(),1);
 await page.locator('#note-picture').setInputFiles({name:'receipt.png',mimeType:'image/png',buffer:fs.readFileSync('assets/quickspend-icon.png')});
 await page.waitForFunction(()=>!!cur.photo&&!noteBusy);
 await page.getByRole('button',{name:'+ Add new EMI',exact:true}).click();await page.waitForFunction(()=>!!emiEntryReturn);await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!emiEntryReturn&&!!document.querySelector('#note-picture'));assert.equal(await page.evaluate(()=>!!cur.photo&&cur.a===25),true);
 assert.equal(await page.getByRole('button',{name:'🎙️ Voice note',exact:true}).count(),0,'Recording remains deferred');
 assert.equal(await page.locator('#sheet .note-attachment img').count(),1);await page.screenshot({animations:'disabled',path:'ui-smoke-output/note-picture.png'});await page.locator('#sheet .entry-emi:not(.entry-subscriptions) .emi-entry-plans').scrollIntoViewIfNeeded();await page.screenshot({animations:'disabled',path:'ui-smoke-output/entry-emi.png'});
 await page.locator('#sheet .save').click();await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on'));
 assert.equal(await page.evaluate(()=>!!tx.find(x=>x.m==='Receipt test')?.photo),true);
 await page.evaluate(()=>openTransactionEdit(tx.find(x=>x.m==='Receipt test').id));
 assert.equal(await page.locator('#sheet .entry-emi:not(.entry-subscriptions)').evaluate(el=>el.open),false);await page.locator('#sheet .entry-emi:not(.entry-subscriptions) summary').click();await page.waitForFunction(()=>cur.emiExpanded);assert.equal(await page.getByRole('button',{name:'+ Add new EMI',exact:true}).isVisible(),true,'Existing expense exposes EMI actions after expanding');
 await page.locator('#sheet .entry-emi:not(.entry-subscriptions) .emi-entry-plans').getByRole('button',{name:/Car EMI test/}).click();await page.locator('#sheet .save').click();await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on'));
 assert.equal(await page.evaluate(()=>{const row=tx.find(x=>x.m==='Receipt test');return row.a===25&&row.acc==='Main account'&&row.emiPlanName==='Car EMI test'&&!!row.photo}),true,'Edited payment keeps amount/merchant/picture and saves the plan link');
 assert.equal(await page.evaluate(()=>emiOutstanding(recur[0])),1100,'Linked current instalment reduces outstanding once');assert.equal(await page.evaluate(()=>emiMonthPaid(recur[0],new Date().getFullYear()*12+new Date().getMonth())),true);
 await page.evaluate(()=>openTransactionEdit(tx.find(x=>x.m==='Receipt test').id));await page.locator('#sheet .entry-emi:not(.entry-subscriptions) summary').click();await page.waitForFunction(()=>cur.emiExpanded);await page.locator('#sheet .entry-emi:not(.entry-subscriptions) .emi-entry-plans').getByRole('button',{name:/Car EMI test/}).click();
 await page.getByRole('button',{name:'Remove picture',exact:true}).click();await page.locator('#sheet .save').click();await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on'));
 assert.equal(await page.evaluate(()=>tx.find(x=>x.m==='Receipt test').photo===null&&tx.find(x=>x.m==='Receipt test').emiPlanIndex===null),true);
 assert.equal(await page.evaluate(()=>emiOutstanding(recur[0])),1200,'Unlinking restores the instalment balance');
 await page.evaluate(()=>{colorMode='dark';applyColorMode();mergeCustomCategories({Expense:['Groceries','Subscriptions']});openSheet({manual:true,m:'Manual entry',a:450,note:'Weekend essentials'})});
 assert.equal(await page.locator('#sheet .category-chip svg[data-icon="bag"]').count(),1);assert.equal(await page.locator('#sheet .category-chip').filter({hasText:'Shopping'}).getAttribute('aria-pressed'),'false');
 assert.equal(await page.locator('#sheet').evaluate(el=>/[\u{1F000}-\u{1FAFF}]/u.test(el.innerText)),false,'Entry chrome has no emoji');
 await page.locator('#sheet .category-chip').filter({hasText:'Shopping'}).click();assert.equal(await page.evaluate(()=>cur.cat),'Shopping');
 await page.locator('#sheet .entry-emi:not(.entry-subscriptions) summary').scrollIntoViewIfNeeded();await page.screenshot({animations:'disabled',path:'ui-smoke-output/entry-icons-dark.png'});
 await page.locator('#sheet .entry-emi:not(.entry-subscriptions) summary').focus();await page.keyboard.press('Enter');await page.waitForFunction(()=>cur.emiExpanded);await page.screenshot({animations:'disabled',path:'ui-smoke-output/entry-icons-expanded.png'});await page.keyboard.press('Enter');await page.waitForFunction(()=>!cur.emiExpanded);
 assert.equal(await page.locator('#sheet .entry-emi:not(.entry-subscriptions) .emi-entry-plans button').first().isVisible(),false);
 await page.evaluate(()=>{colorMode='light';applyColorMode()});await page.screenshot({animations:'disabled',path:'ui-smoke-output/entry-icons-light.png'});
 await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on')&&ignoredBacks===0);await page.setViewportSize({width:320,height:720});await page.evaluate(async()=>{await chooseLanguage('hi');openSheet({manual:true,m:'Manual entry',a:450})});await page.locator('#sheet .entry-emi:not(.entry-subscriptions) summary').scrollIntoViewIfNeeded();
 assert.equal(await page.locator('#sheet').evaluate(el=>el.scrollWidth>el.clientWidth+1),false,'Hindi entry fits narrow phone width');await page.screenshot({animations:'disabled',path:'ui-smoke-output/entry-icons-hindi.png'});await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on')&&ignoredBacks===0);await page.setViewportSize({width:390,height:844});await page.evaluate(async()=>{await chooseLanguage('en')});
 await page.evaluate(()=>{closeSheet(false);homePage='budget';currentTab='home';render()});await page.locator('#s-home .subscription-shortcut').click();await page.getByRole('button',{name:'+ Add Subscription',exact:true}).click();await page.locator('#subscription-search').fill('net');assert.equal(await page.locator('.service-name').count(),1);await page.getByRole('button',{name:'Netflix',exact:true}).click();assert.equal(await page.locator('#planning-amount').inputValue(),'');await page.locator('#planning-amount').fill('199');await page.locator('#sheet .save').click();await page.waitForFunction(()=>subscriptions.length===1);assert.equal(await page.locator('#s-home .planning-record').count(),1);await page.screenshot({animations:'disabled',path:'ui-smoke-output/subscriptions.png'});
 await page.evaluate(()=>closeSheet(false));await page.evaluate(()=>selectHomePage('savings'));await page.getByRole('button',{name:'+ Add savings',exact:true}).click();await page.locator('#planning-name').fill('Emergency fund');await page.locator('#planning-amount').fill('25000');await page.locator('#sheet .save').click();await page.waitForFunction(()=>savingsRecords.length===1);assert.equal(await page.locator('#s-home .planning-summary .big').innerText(),'₹25,000');await page.screenshot({animations:'disabled',path:'ui-smoke-output/savings.png'});
 await page.evaluate(()=>openSheet({manual:true,m:'Bank debit',a:199,note:'Keep this draft'}));assert.equal(await page.locator('.entry-subscriptions').evaluate(e=>e.open),false);await page.locator('.entry-subscriptions summary').click();await page.locator('.entry-subscriptions button').filter({hasText:'Netflix'}).click();assert.equal(await page.evaluate(()=>cur.m==='Bank debit'&&cur.a===199&&cur.note==='Keep this draft'),true);await page.locator('#sheet .save').click();await page.waitForFunction(()=>tx.some(x=>x.m==='Bank debit'&&x.subscriptionId));
 await page.setViewportSize({width:320,height:720});await page.evaluate(async()=>{await chooseLanguage('hi');selectHomePage('savings')});assert.equal(await page.locator('#s-home').evaluate(el=>el.scrollWidth>el.clientWidth+1),false);await page.screenshot({animations:'disabled',path:'ui-smoke-output/savings-hindi.png'});await page.setViewportSize({width:390,height:844});await page.evaluate(async()=>chooseLanguage('en'));
 await page.evaluate(()=>{closeSheet(false);openSheet({manual:true,m:'Discard test',a:88,note:'Unsaved'})});const beforeDiscard=await page.evaluate(()=>JSON.stringify({tx,pending,recur}));await page.getByRole('button',{name:'Discard',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on'));assert.equal(await page.evaluate(()=>JSON.stringify({tx,pending,recur})),beforeDiscard);await page.evaluate(async()=>{await chooseLanguage('hi');openSheet({manual:true,m:'Manual entry',a:88})});assert.equal(await page.getByRole('button',{name:'छोड़ें',exact:true}).count(),1);await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on')&&ignoredBacks===0);await page.evaluate(async()=>chooseLanguage('en'));
 await page.evaluate(()=>{closeSheet(false);budgets={};homePage='budget';budgetView='budget';currentTab='home';render();colorMode='dark';applyColorMode()});assert.equal(await page.locator('#s-home .budget-card-empty .feature-number').innerText(),'₹0');assert.equal(await page.locator('#s-home .feature-stats').count(),0);assert.equal(await page.locator('#s-home .savings-category').count(),0);assert.equal(await page.locator('#s-home .subscription-shortcut').innerText(),'Subscriptions');
 const cardBounds=await page.locator('#s-home .budget-card-empty').boundingBox();
 const cardX=cardBounds.x+cardBounds.width/2,cardY=cardBounds.y+cardBounds.height/2;
 await page.mouse.move(cardX,cardY);await page.mouse.down();await page.mouse.move(cardX-80,cardY,{steps:4});await page.mouse.up();
 await page.locator('#s-home .subscription-shortcut[aria-pressed="true"]').waitFor();
 assert.equal(await page.locator('#s-home .planning-nav button').first().innerText(),'Subscriptions');
 const subBounds=await page.locator('#s-home .planning-hero').boundingBox();
 assert.ok(Math.abs(subBounds.height-cardBounds.height)<1,'Budget and Subscription summaries use the same height');
 await page.getByRole('button',{name:'Budget',exact:true}).and(page.locator('.planning-nav button')).click();
 await page.locator('#s-home .budget-card-empty').scrollIntoViewIfNeeded();await page.screenshot({animations:'disabled',path:'ui-smoke-output/budget-empty-dark.png'});const gear=await page.locator('#s-home .profile-settings').boundingBox(),gearSvg=await page.locator('#s-home .profile-settings svg').boundingBox();assert.ok(Math.abs(gear.width-40)<1&&Math.abs(gear.height-40)<1,'Gear button remains square');assert.ok(Math.abs(gearSvg.width-20)<1&&Math.abs(gearSvg.height-20)<1,'Gear SVG remains square');const gearPath=await page.locator('#s-home .profile-settings svg path').evaluate(el=>{const r=el.getBBox();return {x:r.x+r.width/2,y:r.y+r.height/2}});assert.ok(Math.abs(gearPath.x-12)<.01&&Math.abs(gearPath.y-12)<.01,'Gear outline is centred on its hub');assert.ok(Math.abs(gear.x+gear.width/2-gearSvg.x-gearSvg.width/2)<1&&Math.abs(gear.y+gear.height/2-gearSvg.y-gearSvg.height/2)<1,'Gear is centred');
 await page.evaluate(()=>{document.querySelector('#profile-name').textContent='A very long account profile name that should never shrink the settings button';colorMode='light';applyColorMode()});const longGear=await page.locator('#s-home .profile-settings').boundingBox();assert.ok(Math.abs(longGear.width-40)<1&&Math.abs(longGear.height-40)<1);await page.screenshot({animations:'disabled',path:'ui-smoke-output/budget-empty-light.png'});
 const ledgerBeforeBudget=await page.evaluate(()=>JSON.stringify(tx));await page.locator('#s-home .budget-card-empty').click();await page.getByRole('button',{name:'Set budget',exact:true}).click();await page.locator('#budget-income').fill('10000');await page.getByRole('spinbutton',{name:'Food',exact:true}).fill('2000');await page.getByRole('button',{name:'Save budget',exact:true}).click();await page.evaluate(()=>window.quickSpendSystemBack());await page.waitForFunction(()=>!document.querySelector('#sheet').classList.contains('on')&&ignoredBacks===0);assert.equal(await page.locator('#s-home .budget-card-empty').count(),0);assert.equal(await page.locator('#s-home .feature-stats').count(),1);assert.ok(await page.locator('#s-home .savings-category').count()>0);assert.equal(await page.evaluate(()=>JSON.stringify(tx)),ledgerBeforeBudget,'Budget setup does not create transactions');await page.screenshot({animations:'disabled',path:'ui-smoke-output/budget-configured.png'});
 const configuredBounds=await page.locator('#s-home .budget-teaser').boundingBox();
 await page.locator('#s-home .subscription-shortcut').click();
 const configuredSubBounds=await page.locator('#s-home .planning-hero').boundingBox();
 assert.ok(Math.abs(configuredBounds.height-configuredSubBounds.height)<1,'Configured budget and subscriptions are equal height');
 const titles=await page.locator('.planning-nav button').evaluateAll(buttons=>buttons.map(button=>{const r=button.getBoundingClientRect();return r.top+r.height/2}));
 assert.ok(Math.abs(titles[0]-titles[1])<1,'Planning headings share one vertical centre');
 await page.getByRole('button',{name:'Budget',exact:true}).and(page.locator('.planning-nav button')).click();
 console.log('CHECK: isolated budget edit');
 const budgetStart=await page.evaluate(()=>budgets[budgetKeyForTime(Date.now())].createdAt);assert.ok(budgetStart>0);assert.equal(await page.evaluate(()=>budgetSummary(budgetKeyForTime(Date.now())).totalSpent),0,'Existing account history is excluded');
 await page.evaluate(()=>{tx.push({id:++sid,t:'Expense',a:120,c:'Food',m:'New budget spend',ts:Date.now()+10,acc:'Cash'},{id:++sid,t:'Expense',a:90,c:'Health',m:'Unselected spend',ts:Date.now()+10,acc:'Main account'});openBudget()});assert.equal(await page.evaluate(()=>budgetSummary(budgetMonth).totalSpent),120);assert.equal(await page.locator('#sheet .budget-category').count(),1);assert.equal(await page.locator('#sheet .budget-hero .big').innerText(),'₹9,880');
 await page.locator('#sheet .budget-topline .text-btn').click();await page.locator('#budget-income').fill('12000');await page.getByRole('button',{name:'Save budget',exact:true}).click();assert.equal(await page.evaluate(()=>budgets[budgetMonth].createdAt),budgetStart,'Editing never resets the spending boundary');await page.evaluate(()=>closeSheet(false));
 console.log('CHECK: Quick Access rail, neutral Light mode, and sheet handles');
 await page.evaluate(()=>{quickDraft={enabled:false,Expense:['Food','Food','Travel','Other'],Income:['Salary','Gift','Other','Refund'],expenseCategories:['Food','Travel','Shopping','Bills','Health','Fun','Groceries','Other','Subscriptions','Uncategorised'],incomeCategories:['Salary','Gift','Other','Refund']};quickType='Expense';paintQuickAccessSettings();document.querySelector('#sheet').classList.add('on');document.querySelector('#dim').classList.add('on')});
 assert.equal(await page.locator('#sheet select').count(),0);assert.equal(await page.locator('.quick-slot-preview').count(),10);
 for(const mode of ['dark','light']){
  await page.evaluate(mode=>{colorMode=mode;applyColorMode()},mode);
  assert.equal(await page.locator('#sheet .grab').evaluate(el=>getComputedStyle(el).backgroundColor),'rgba(0, 0, 0, 0)');
  assert.equal(await page.locator('#sheet').evaluate(el=>el.scrollWidth>el.clientWidth+1),false);
  await page.locator('.quick-rail-preview').scrollIntoViewIfNeeded();
  await page.screenshot({animations:'disabled',path:'ui-smoke-output/quick-settings-'+mode+'.png'});
 }
 assert.equal(await page.locator('#ph').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(237, 237, 237)');
 await page.setViewportSize({width:320,height:720});await page.evaluate(()=>{language='hi';paintQuickAccessSettings()});assert.equal(await page.locator('#sheet').evaluate(el=>el.scrollWidth>el.clientWidth+1),false);assert.equal(await page.getByText('आपकी 10 श्रेणियाँ',{exact:true}).count(),1);await page.locator('.quick-rail-preview').scrollIntoViewIfNeeded();await page.screenshot({animations:'disabled',path:'ui-smoke-output/quick-settings-hindi.png'});await page.evaluate(()=>{language='en';document.querySelector('#sheet').scrollTop=9999});
 assert.ok(await page.locator('#sheet').evaluate(el=>el.scrollTop)>0,'Quick Access sheet is scrolled before returning');
 assert.ok(await page.locator('#sheet .grab').evaluate(el=>el.getBoundingClientRect().bottom<el.parentElement.getBoundingClientRect().top),'Grab handle scrolls away from content');
 await page.locator('#sheet .sheet-heading .icon-btn').evaluate(el=>el.click());
 assert.equal(await page.locator('#sheet').evaluate(el=>el.scrollTop),0,'Settings opens at its heading after a scrolled sheet');
 assert.equal(await page.locator('#sheet h2').innerText(),'Settings');await page.setViewportSize({width:390,height:844});
 assert.equal(await page.locator('#sheet .grab').evaluate(el=>getComputedStyle(el).backgroundColor),'rgba(0, 0, 0, 0)');await page.screenshot({animations:'disabled',path:'ui-smoke-output/settings-light.png'});
 await page.evaluate(()=>{closeSheet(false);recur=[{t:'Expense',m:'Example plan',a:200,months:5,total:1000,left:1,last:new Date().getFullYear()*12+new Date().getMonth(),day:1,emi:true}];selectHomePage('emi')});
 assert.equal(await page.locator('.emi-progress-total strong').innerText(),'₹800');assert.equal(await page.locator('.emi-progress-total span').innerText(),'/ ₹1,000');assert.equal(await page.getByRole('progressbar',{name:'EMI paid'}).getAttribute('aria-valuenow'),'80');await page.screenshot({animations:'disabled',path:'ui-smoke-output/emi-progress-light.png'});
 console.log('CHECK: Quick Access sideways selection and cancellation, Review swipe, and quick entry delete');
 await page.evaluate(()=>{
  homePage='overview';render();
  const payment={id:501,m:'Swipe test',a:123.67,kind:'Expense',ts:Date.now()};
  pending.push(payment);showPaymentBubble(payment,true)
 });
 const bubble=page.locator('#bub');
 const bubbleBox=await bubble.boundingBox(), phoneBox=await page.locator('#ph').boundingBox();
 const x=bubbleBox.x+bubbleBox.width/2,y=bubbleBox.y+bubbleBox.height/2;
 await page.mouse.move(x,y);await page.mouse.down();await page.locator('#quick-live').waitFor();
 await page.mouse.move(x-66,y,{steps:4});
 await page.locator('#quick-live .is-selected').waitFor({timeout:3000}).catch(async error=>{
  console.log('Quick Access gesture state',await page.evaluate(()=>({selected:inAppQuickGesture?.selected,origin:inAppQuickGesture?.origin,held:!!inAppQuickGesture?.categories,cancelled:inAppQuickGesture?.cancelled})));throw error
 });
 assert.equal(await page.locator('#quick-live .is-selected').count(),1,'A level sideways gesture highlights a category');
 await page.mouse.move(phoneBox.x+phoneBox.width/2,phoneBox.y+phoneBox.height-25,{steps:4});
 await page.locator('#quick-live .quick-live-hint.is-active').waitFor();await page.mouse.up();
 assert.equal(await page.locator('#sheet.on').count(),0,'Cancel does not open or save a payment');
 await page.mouse.move(x,y);await page.mouse.down();await page.locator('#quick-live').waitFor();
 await page.mouse.move(x-66,y,{steps:4});await page.mouse.up();
 await page.locator('#sheet.on .entry-delete').waitFor();
 await page.getByRole('button',{name:'Delete',exact:true}).click();
 await page.waitForFunction(()=>!pending.some(p=>p.id===501));
 await page.evaluate(()=>{pending.push({id:502,m:'Swipe review test',a:234.67,kind:'Expense',ts:Date.now()});render()});
 await page.locator('[data-t="review"]').click();
 const review=page.locator('.review-swipe').first();
 await review.scrollIntoViewIfNeeded();
 const reviewBox=await review.boundingBox(),cx=reviewBox.x+reviewBox.width/2,cy=reviewBox.y+reviewBox.height/2;
 for(const sign of [-1,1]){
  await page.mouse.move(cx,cy);await page.mouse.down();
  await page.mouse.move(cx+sign*85,cy,{steps:5});await page.mouse.up();
  await page.locator('.review-swipe[data-open][data-side='+ (sign>0?'left':'right') +']').waitFor();
  assert.equal(await page.locator('#sheet.on').count(),0,'Swiping does not open the payment')
 }
 await page.getByRole('button',{name:'Dismiss payment'}).click();
 await page.waitForFunction(()=>!pending.some(p=>p.id===502));
 assert.equal(await page.evaluate(()=>tx.some(t=>t.a===123.67||t.a===234.67)),false,'Deleted pending payments never enter the ledger');
 await page.locator('[data-t="home"]').click();
 await page.evaluate(()=>{categoryPickerEnabled=true;colorMode='matte';applyColorMode();openSheet({manual:true,m:'Picker draft',a:45,note:'Keep this note'})});
 await page.getByRole('button',{name:'Change category',exact:true}).click();
 await page.getByRole('button',{name:'My categories',exact:true}).click();
 await page.getByRole('button',{name:'+ Create a category',exact:true}).click();
 await page.locator('#nb').fill('School supplies');await page.locator('#nb').press('Enter');
 assert.equal(await page.evaluate(()=>cur.cat==='School supplies'&&cur.a===45&&cur.note==='Keep this note'),true);
 await page.getByRole('button',{name:'Change category',exact:true}).click();
 await page.evaluate(()=>window.quickSpendSystemBack());
 await page.locator('#note-picture').waitFor({state:'attached'});
 assert.equal(await page.locator('#sheet.on').count(),1,'Back from categories preserves the entry sheet');
 await page.evaluate(()=>{appUpdate.info={required:true,available:true,downloaded:false};renderUpdateBanner()});
 await page.getByRole('dialog',{name:'Update required'}).waitFor();
 await page.keyboard.press('Escape');
 assert.equal(await page.getByRole('dialog',{name:'Update required'}).isVisible(),true,'Required update stays after Escape');
 assert.equal(await page.evaluate(()=>window.quickSpendSystemBack()),true);
 await page.evaluate(()=>{appUpdate.info=null;renderUpdateBanner();closeSheet(false)});
 await page.evaluate(()=>{ob=0;showOb()});assert.equal(await page.locator('#onb img').count(),0);await page.screenshot({animations:'disabled',path:'ui-smoke-output/setup.png'});
 const overflow=await page.locator('#onb').evaluate(el=>el.scrollWidth>el.clientWidth+1);assert.equal(overflow,false,'Setup must fit phone width');assert.deepEqual(errors,[]);
 const overlay=await browser.newPage({viewport:{width:390,height:650}});
 const overlayErrors=[];overlay.on('pageerror',e=>overlayErrors.push(e.message));
 const stubPhoto='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1sAAAAASUVORK5CYII=';
 await overlay.addInitScript(photo=>{
  window.captureActions=[];
  window.QuickCapture={postMessage(raw){
   const request=JSON.parse(raw);captureActions.push(request);
   const reply={id:request.id,status:'ok'};
   if(request.action==='picture'&&request.source==='browse')reply.photo=photo;
   queueMicrotask(()=>QuickCapture.onmessage({data:JSON.stringify(reply)}))
  }}
 },stubPhoto);
 await overlay.goto('file://'+path.join(process.cwd(),'index.html'));
 await overlay.evaluate(()=>initialiseCapture({
  payment:{id:1,m:'Capture test',a:75,kind:'Expense',acc:'Main account',demo:true},language:'en',colorMode:'dark',
  categoryPicker:true,accounts:[{n:'Main account',i:'🏦'},{n:'Cash',i:'👛'}],people:[],
  categories:{},customCategories:{},emiPlans:[{index:3,name:'Car loan',left:5,a:400}],subscriptions:[{id:'sub1',name:'Example',status:'active',cents:19900}]
 }));
 await overlay.getByRole('button',{name:'Change category',exact:true}).click();
 await overlay.getByRole('button',{name:'Food',exact:true}).click();
 await overlay.getByRole('textbox',{name:'Comment'}).fill('Keep this draft');
 await overlay.getByRole('button',{name:'Add picture',exact:true}).click();
 await overlay.getByRole('button',{name:'Camera',exact:true}).click();
 await overlay.waitForFunction(()=>!noteBusy);
 assert.equal(await overlay.getByRole('textbox',{name:'Comment'}).inputValue(),'Keep this draft','Cancelled camera returns to the same draft');
 await overlay.getByRole('button',{name:'Browse',exact:true}).click();
 await overlay.locator('.note-attachment img').waitFor();
 await overlay.locator('.entry-subscriptions summary').click();
 await overlay.getByRole('button',{name:/Example/}).click();
 await overlay.screenshot({animations:'disabled',path:'ui-smoke-output/shared-floater-entry.png'});
 await overlay.getByRole('button',{name:'Finish test',exact:true}).click();
 await overlay.waitForFunction(()=>captureActions.some(x=>x.action==='save'));
 const sent=await overlay.evaluate(()=>captureActions.find(x=>x.action==='save').entry);
 assert.equal(sent.note,'Keep this draft');assert.equal(sent.subscriptionId,'sub1');assert.equal(sent.photo,stubPhoto);
 assert.deepEqual(overlayErrors,[]);await overlay.close();
 console.log('PASS: Chromium history search/filter/edit routes, navigation scroll reset, text-only setup, Insights cleanup, dark/light screenshots real photo compression/save/removal and recording-control absence and no runtime errors');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
