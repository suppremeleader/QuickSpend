/* Real Chromium smoke check. Isolated CI dependency; no production packages. */
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright'),assert=require('assert'),path=require('path'),fs=require('fs');
(async()=>{
 const browser=await chromium.launch({headless:true});try{
 const page=await browser.newPage({viewport:{width:390,height:844},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
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
 await page.evaluate(async()=>{await setColorMode('light');closeSheet()});await page.screenshot({animations:'disabled',path:'ui-smoke-output/history-light.png'});
 await page.getByRole('button',{name:'Back',exact:true}).click();await page.locator('#s-home.on').waitFor();await page.evaluate(()=>{ob=0;showOb()});assert.equal(await page.locator('#onb img').count(),0);await page.screenshot({animations:'disabled',path:'ui-smoke-output/setup.png'});
 const overflow=await page.locator('#onb').evaluate(el=>el.scrollWidth>el.clientWidth+1);assert.equal(overflow,false,'Setup must fit phone width');assert.deepEqual(errors,[]);
 console.log('PASS: Chromium history search/filter/edit routes, navigation scroll reset, text-only setup, Insights cleanup, dark/light screenshots and no runtime errors');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
