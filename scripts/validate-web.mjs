import fs from 'node:fs';

const html=fs.readFileSync('index.html','utf8');
const errors=[];

const ids=[...html.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);
const counts=new Map();
for(const id of ids)counts.set(id,(counts.get(id)||0)+1);
for(const [id,n] of counts)if(n>1)errors.push(`Duplicate id: ${id} x${n}`);

const refs=[...html.matchAll(/\$\('#([^']+)'\)/g)].map(m=>m[1]);
for(const id of new Set(refs))if(!counts.has(id))errors.push(`Missing DOM id referenced by JS: ${id}`);

for(const tag of ['div','section']){
  const open=(html.match(new RegExp('<'+tag+'\\b','g'))||[]).length;
  const close=(html.match(new RegExp('</'+tag+'>','g'))||[]).length;
  if(open!==close)errors.push(`Unbalanced <${tag}>: ${open} open / ${close} close`);
}

const required=[
  'authScreen','homeScreen','profileScreen','achievementsScreen','settingsScreen',
  'gameScreen','resultScreen','gameCanvas','playBtn','pauseBtn','homeBoard'
];
for(const id of required)if(!counts.has(id))errors.push(`Required id missing: ${id}`);

const scripts=[...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)]
  .map(m=>m[1]).filter(x=>x.trim());
try{
  new Function(scripts.at(-1)||'');
}catch(e){
  errors.push('Inline JavaScript syntax error: '+e.message);
}

if(errors.length){
  console.error('Web validation failed:\n- '+errors.join('\n- '));
  process.exit(1);
}
console.log(`Web validation passed: ${ids.length} ids, ${refs.length} JS DOM references.`);
