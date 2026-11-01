const assert=require('assert');
const{app,settle}=require('./app-inbox.cjs');

(async()=>{
 const a=app({savedLanguage:'en'});await settle();
 let home=a.nodes.get('#s-home').innerHTML;
 assert.match(home,/class="home-page on" aria-current="page"[^>]*>Home/);
 assert.match(home,/balance-hero/);
 assert.match(home,/Recent/);
 assert.ok(!home.includes('budget-card-strong'),'Overview keeps planning cards out of the transaction feed');
 assert.ok(!home.includes('recurring-feature'),'Overview keeps EMI cards out of the transaction feed');
 a.eval("homePage='savings';render()");home=a.nodes.get('#s-home').innerHTML;
 assert.match(home,/aria-current="page"[^>]*>Savings/);
 assert.match(home,/budget-card-strong/);
 assert.ok(!home.includes('recurring-feature'));
 a.eval("homePage='emi';render()");home=a.nodes.get('#s-home').innerHTML;
 assert.match(home,/aria-current="page"[^>]*>EMI/);
 assert.match(home,/recurring-feature/);
 assert.ok(!home.includes('budget-card-strong'));
 console.log('PASS: Home, Savings and EMI pages are distinct; balance remains prominent and active page is accessible.');
})().catch(error=>{console.error(error);process.exit(1)});
