const assert=require('assert');const {app,settle}=require('./app-inbox.cjs');
const photo='data:image/jpeg;base64,SU1H',voice='data:audio/webm;base64,Vk9JQ0U=';
(async()=>{
 const a=app({savedLanguage:'en'});await settle();a.c.closeSheet(false);a.eval('tx=[];pending=[];recur=[]');
 a.c.openSheet({manual:true,m:'Receipt',a:25,photo,voice,note:'Lunch'});let html=a.nodes.get('#sheet').innerHTML;assert.doesNotMatch(html,/toggleVoiceNote|Stop recording/);assert.equal(typeof a.c.toggleVoiceNote,'undefined');assert.match(html,/<audio controls/);assert.match(html,/<img src="data:image\/jpeg/);a.c.save();
 assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('tx[0].photo'),photo);assert.equal(a.eval('safeCloudData().tx[0].voice'),voice);assert.match(a.c.historyItem(a.eval('tx[0]')),/transaction-attachments/);
 // A photo saved from the floating popup survives a native inbox import and opens from Review.
 a.inbox.entries.push({nativeKey:'quick-photo',t:'Expense',a:42,c:'Uncategorised',m:'Quick payment',photo});
 await a.c.pull(true);
 const captured=a.eval("tx.find(entry=>entry.nativeKey==='quick-photo')");
 assert.equal(captured.photo,photo);
 assert.match(a.c.reviewPage(),/Quick payment[\s\S]*transaction-attachments/);
 a.c.openSavedReview(captured.id);
 assert.match(a.nodes.get('#sheet').innerHTML,/Enlarge attached picture/);
 const image={removeAttribute(name){delete this[name]}},closeButton={setAttribute(){}};
 const dialog={open:false,setAttribute(){},addEventListener(){},
  querySelector(selector){return selector==='button'?closeButton:image},
  showModal(){this.open=true},close(){this.open=false}};
 a.c.document.createElement=tag=>{assert.equal(tag,'dialog');return dialog};
 a.c.document.getElementById=id=>a.nodes.get('#'+id)?.showModal?dialog:null;
 a.c.document.body={appendChild(node){a.nodes.set('#'+node.id,node)}};
 a.c.viewEntryPicture();
 assert.equal(dialog.open,true);assert.equal(image.src,photo);
 assert.equal(a.c.window.quickSpendSystemBack(),true);
 assert.equal(dialog.open,false);assert.equal(image.src,undefined);
 assert.ok(a.nodes.get('#sheet').classList.contains('on'));
 // Enter keeps the draft and calls the native keyboard dismissal. IME composition may still commit text.
 const mainBridge=a.c.window.Capacitor.nativePromise;
 let nativeDismissals=0,blurCount=0,preventCount=0;
 a.c.window.Capacitor.nativePromise=(plugin,method,options)=>{
  if(plugin==='SmsBridge'&&method==='hideKeyboard'){nativeDismissals++;return Promise.resolve({})}
  return mainBridge(plugin,method,options)
 };
 a.c.openSheet({manual:true,m:'New entry',a:25,note:'milk'});
 assert.match(a.nodes.get('#sheet').innerHTML,/enterkeyhint="done"/);
 const input={value:'milk',blur(){blurCount++}};
 const enter={type:'keydown',key:'Enter',preventDefault(){preventCount++}};
 a.c.finishEntryComment({...enter,isComposing:true},input);
 assert.equal(blurCount,0);
 a.c.finishEntryComment(enter,input);
 assert.equal(blurCount,1);assert.equal(preventCount,1);assert.equal(nativeDismissals,1);
 assert.equal(a.eval('cur.note'),'milk');assert.ok(a.nodes.get('#sheet').classList.contains('on'));
 a.c.closeSheet(false);
 a.eval("tx=tx.filter(entry=>entry.nativeKey!=='quick-photo')");
 const restored=app({savedLanguage:'en',savedState:a.eval("localStorage.getItem('qs1')")});await settle();assert.equal(restored.eval('tx[0].photo'),photo);assert.equal(restored.eval('tx[0].voice'),voice);
 a.c.openTransactionEdit(a.eval('tx[0].id'));a.c.removeNoteAttachment('photo');a.c.removeNoteAttachment('voice');a.c.save();assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('tx[0].photo'),null);assert.equal(a.eval('tx[0].voice'),null);
 assert.equal(a.c.noteAttachment('photo','data:image/svg+xml;base64,PHN2Zz4='),'');assert.equal(a.c.noteAttachment('voice','javascript:alert(1)'),'');assert.equal(a.c.noteAttachment('photo',photo+'" onerror="bad'),'');assert.equal(a.c.noteAttachment('voice','data:audio/webm;base64,'+'A'.repeat(180001)),'');
 // A late picture-read callback cannot mutate a different entry.
 const readers=[];a.c.FileReader=class{readAsDataURL(){readers.push(this)}};
 a.c.openSheet({manual:true,m:'Old picture draft',a:10});a.c.addNotePicture({files:[{type:'image/png',size:50}]});assert.equal(a.eval('noteBusy'),true);
 const beforeBusy=a.eval('tx.length');a.c.save();assert.equal(a.eval('tx.length'),beforeBusy,'Save waits for picture processing');
 a.c.closeSheet(false);a.c.openSheet({manual:true,m:'Different entry',a:20});readers[0].result=photo;readers[0].onload();assert.equal(a.eval('cur.photo'),undefined);assert.equal(a.eval('noteBusy'),false);
 // Storage failures leave the draft and ledger unchanged.
 a.c.openSheet({manual:true,m:'Retain draft',a:4,photo});const originalPersist=a.c.persist;a.c.persist=()=>false;const count=a.eval('tx.length');a.c.save();assert.equal(a.eval('tx.length'),count);assert.equal(a.eval('cur.photo'),photo);assert.ok(a.nodes.get('#sheet').classList.contains('on'));a.c.persist=originalPersist;a.c.closeSheet(false);
 // Preflight oversized cloud backups without dropping local media or making an upload.
 let uploads=0;const b=app({savedLanguage:'en',accountApi:{status:async()=>({configured:true}),upload:async()=>{uploads++;return{revision:1}}}});await settle();b.eval("cloud.account={uid:'u'};cloud.ready=true;cloud.lastPayload='previous';tx=Array.from({length:5},(_,id)=>({id,t:'Expense',m:'Receipt',a:1,voice:'data:audio/webm;base64,'+'A'.repeat(170000)}))");await assert.rejects(b.c.performCloudSave(),/Cloud backup is full/);assert.equal(uploads,0);assert.equal(b.eval('tx.length'),5);assert.equal(b.eval('cloud.lastPayload'),'previous');assert.equal(b.c.utf8Bytes('₹'),3);
 console.log('PASS: attachment save/edit/remove/restart/backup, safe previews, deferred recording, existing voice retention and picture-read races, storage rollback and cloud size feedback');
})().catch(e=>{console.error(e);process.exit(1)});
