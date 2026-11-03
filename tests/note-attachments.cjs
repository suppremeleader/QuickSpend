const fs=require('fs'),assert=require('assert');
const html=fs.readFileSync(require('path').join(__dirname,'../index.html'),'utf8');
assert.match(html,/accept="image\/\*" capture="environment"/,'picture picker should offer the camera');
assert.match(html,/navigator\.mediaDevices\?\.getUserMedia/,'voice notes should request microphone input');
assert.match(html,/photo:cur\.photo\|\|null,voice:cur\.voice\|\|null/,'new entries should save both attachments');
assert.match(html,/photo:cur\.photo\|\|null,voice:cur\.voice\|\|null,t:cur\.type/,'edited entries should save both attachments');
assert.match(html,/\['id'.*'photo','voice'\]/,'cloud backups should retain note attachments');
assert.match(html,/function attachmentBadges\(x\)/,'saved entries should identify attachments');
console.log('PASS: note picture and voice controls, persistence, backup, and saved-entry indicators are present.');
