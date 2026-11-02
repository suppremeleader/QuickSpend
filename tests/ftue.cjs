const assert=require('assert'),fs=require('fs'),path=require('path');
const {app,settle}=require('./app-inbox.cjs');
(async()=>{
 const a=app();await settle();let html=a.nodes.get('#language-choice').innerHTML;
 assert.match(html,/ftue-brand/);assert.match(html,/ftue-wallet-v1\.png/);assert.match(html,/chooseLanguage\('hi'\)/);assert.match(html,/chooseLanguage\('en'\)/);
 a.eval("ob=0;showOb()");html=a.nodes.get('#onb').innerHTML;assert.match(html,/ftue-brand/);assert.match(html,/ftue-art-wallet/);assert.doesNotMatch(html,/class="oe"/);assert.match(html,/id="onb-actions"/);assert.match(html,/obNext\(\)/);
 a.eval('ob=1;showOb()');assert.match(a.nodes.get('#onb').innerHTML,/ftue-art-capture/);
 a.c.openAccount(true);html=a.nodes.get('#account-setup').innerHTML;assert.match(html,/onb-header/);assert.match(html,/ftue-art-privacy/);assert.match(html,/save obtn/);assert.match(html,/finishAccountSetup\(\)/);
 const root=path.join(__dirname,'..'),css=fs.readFileSync(path.join(root,'assets/ftue.css'),'utf8'),workflow=fs.readFileSync(path.join(root,'.github/workflows/build.yml'),'utf8');
 assert.match(css,/prefers-reduced-motion:reduce/);assert.match(css,/@media\(max-height:480px\)/);assert.match(css,/\[data-theme="light"\].*--warm:/);assert.match(css,/\.onb-actions\{[^}]*flex:none/);
 assert.match(workflow,/cp assets\/quickspend-icon.png assets\/ftue-wallet-v1.png assets\/ftue.css www\/assets\//);
 const png=fs.readFileSync(path.join(root,'assets/ftue-wallet-v1.png'));assert.equal(png.subarray(1,4).toString(),'PNG');
 console.log('PASS: shared language/intro/account artwork and actions, distinct step icons, responsive/reduced-motion styles, light palette and packaged assets');
})().catch(e=>{console.error(e);process.exit(1)});
