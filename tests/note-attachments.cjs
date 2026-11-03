const assert=require('assert');const {app,settle}=require('./app-inbox.cjs');
const photo='data:image/jpeg;base64,SU1H',voice='data:audio/webm;base64,Vk9JQ0U=';
class Reader{readAsDataURL(blob){blob.arrayBuffer().then(b=>{this.result='data:'+blob.type+';base64,'+Buffer.from(b).toString('base64');this.onload?.()})}}
class Recorder{
 static isTypeSupported(type){return type==='audio/webm;codecs=opus'}
 constructor(stream,options){this.stream=stream;this.mimeType=options.mimeType;this.state='inactive'}
 start(){this.state='recording'}
 stop(){if(this.state==='inactive')return;this.state='inactive';this.ondataavailable?.({data:new Blob(['voice'],{type:this.mimeType})});this.onstop?.()}
}
(async()=>{
 const a=app({savedLanguage:'en'});await settle();a.c.closeSheet(false);a.eval('tx=[];pending=[];recur=[]');
 a.c.openSheet({manual:true,m:'Receipt',a:25,photo,voice,note:'Lunch'});let html=a.nodes.get('#sheet').innerHTML;assert.match(html,/Voice note|Stop recording/);assert.match(html,/<audio controls/);assert.match(html,/<img src="data:image\/jpeg/);a.c.save();
 assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('tx[0].photo'),photo);assert.equal(a.eval('safeCloudData().tx[0].voice'),voice);assert.match(a.c.historyItem(a.eval('tx[0]')),/transaction-attachments/);
 const restored=app({savedLanguage:'en',savedState:a.eval("localStorage.getItem('qs1')")});await settle();assert.equal(restored.eval('tx[0].photo'),photo);assert.equal(restored.eval('tx[0].voice'),voice);
 a.c.openTransactionEdit(a.eval('tx[0].id'));a.c.removeNoteAttachment('photo');a.c.removeNoteAttachment('voice');a.c.save();assert.equal(a.eval('tx.length'),1);assert.equal(a.eval('tx[0].photo'),null);assert.equal(a.eval('tx[0].voice'),null);
 assert.equal(a.c.noteAttachment('photo','data:image/svg+xml;base64,PHN2Zz4='),'');assert.equal(a.c.noteAttachment('voice','javascript:alert(1)'),'');assert.equal(a.c.noteAttachment('photo',photo+'" onerror="bad'),'');assert.equal(a.c.noteAttachment('voice','data:audio/webm;base64,'+'A'.repeat(180001)),'');
 a.c.openSheet({manual:true,m:'Audio',a:12});let stops=0;const stream=()=>({getTracks:()=>[{stop(){stops++}}]});a.c.navigator={mediaDevices:{getUserMedia:async()=>stream()}};a.c.MediaRecorder=Recorder;a.c.Blob=Blob;a.c.FileReader=Reader;
 await a.c.toggleVoiceNote();assert.ok(a.eval('noteRecorder'));const before=a.eval('tx.length');a.c.save();assert.equal(a.eval('tx.length'),before,'Save must wait for recording to finish');await a.c.toggleVoiceNote();await settle();assert.ok(a.eval('cur.voice').startsWith('data:audio/webm;codecs=opus;base64,'));assert.ok(stops>0,'Stopped recording releases microphone');assert.equal(a.eval('noteBusy'),false);a.c.save();assert.equal(a.eval('tx.length'),before+1);
 // Permission may resolve after the user closes or changes the entry.
 a.c.openSheet({manual:true,m:'Old entry',a:10});let grant;a.c.navigator.mediaDevices.getUserMedia=()=>new Promise(resolve=>{grant=resolve});const waiting=a.c.toggleVoiceNote();a.c.closeSheet(false);a.c.openSheet({manual:true,m:'New entry',a:20});const oldStops=stops;grant(stream());await waiting;assert.ok(stops>oldStops);assert.equal(a.eval('cur.voice'),undefined);assert.equal(a.eval('noteRecorder'),null);
 a.c.navigator.mediaDevices.getUserMedia=async()=>{throw Error('Denied')};await a.c.toggleVoiceNote();assert.match(a.eval('noteStatus'),/Microphone access/);assert.equal(a.eval('noteStream'),null);
 // Automatic duration limit stops and finalizes, rather than keeping the mic live.
 let limit;a.c.setTimeout=(fn,ms)=>{if(ms===30000)limit=fn;return 1};a.c.navigator.mediaDevices.getUserMedia=async()=>stream();await a.c.toggleVoiceNote();assert.ok(limit);limit();await settle();assert.equal(a.eval('noteRecorder'),null);assert.ok(a.eval('cur.voice'));a.c.closeSheet(false);
 // Storage failures leave the draft and ledger unchanged.
 a.c.openSheet({manual:true,m:'Retain draft',a:4,photo});const originalPersist=a.c.persist;a.c.persist=()=>false;const count=a.eval('tx.length');a.c.save();assert.equal(a.eval('tx.length'),count);assert.equal(a.eval('cur.photo'),photo);assert.ok(a.nodes.get('#sheet').classList.contains('on'));a.c.persist=originalPersist;a.c.closeSheet(false);
 // Preflight oversized cloud backups without dropping local media or making an upload.
 let uploads=0;const b=app({savedLanguage:'en',accountApi:{status:async()=>({configured:true}),upload:async()=>{uploads++;return{revision:1}}}});await settle();b.eval("cloud.account={uid:'u'};cloud.ready=true;cloud.lastPayload='previous';tx=Array.from({length:5},(_,id)=>({id,t:'Expense',m:'Receipt',a:1,voice:'data:audio/webm;base64,'+'A'.repeat(170000)}))");await assert.rejects(b.c.performCloudSave(),/Cloud backup is full/);assert.equal(uploads,0);assert.equal(b.eval('tx.length'),5);assert.equal(b.eval('cloud.lastPayload'),'previous');assert.equal(b.c.utf8Bytes('₹'),3);
 console.log('PASS: attachment save/edit/remove/restart/backup, safe previews, recording lifecycle/permission races/duration, storage rollback and cloud size feedback');
})().catch(e=>{console.error(e);process.exit(1)});
