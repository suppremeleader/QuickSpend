const assert=require('assert');const{app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app();await settle();let html=a.nodes.get('#language-choice').innerHTML;
 assert.match(html,/QUICKSPEND/);assert.match(html,/chooseLanguage\('hi'\)/);assert.match(html,/chooseLanguage\('en'\)/);assert.doesNotMatch(html,/<img|ftue-art/);
 for(let i=0;i<7;i++){a.eval(`ob=${i};showOb()`);html=a.nodes.get('#onb').innerHTML;assert.match(html,/id="onb-title"/);assert.match(html,/id="onb-actions"/);if(i>0)assert.match(html,/obBack/);if(i>=4)assert.match(html,/ob-defer/);assert.doesNotMatch(html,/<img|ftue-art|class="oe"/);assert.match(html,/onclick="(?:permStep|obNext)/)}
 a.c.openAccount(true);html=a.nodes.get('#account-setup').innerHTML;assert.doesNotMatch(html,/<img|ftue-art/);assert.match(html,/save obtn/);assert.match(html,/finishAccountSetup\(\)/);
 console.log('PASS: text-led language, all intro/permission steps and account setup retain headings and functional actions without decorative artwork');
})().catch(e=>{console.error(e);process.exit(1)});
