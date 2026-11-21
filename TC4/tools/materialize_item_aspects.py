"""One-time explicit-definition migration from the pre-refactor live exports.
Run only against archived baseline data. All overwritten definitions are archived.
"""
from pathlib import Path
import json,shutil,zipfile,collections
CORE=Path(__file__).resolve().parents[1];ROOT=CORE.parent
OUT=CORE/'docs/aspect-baseline/2026-10-08';OUT.mkdir(parents=True,exist_ok=True)
TASK=CORE/'.tooling/aspect-refactor'
PROJECTS={'thaumcraft':CORE,'thaumic_tinkerer':ROOT/'Addons/ThaumicTinkerer','forbidden_magic':ROOT/'Addons/ForbiddenMagic','thaumicenergistics':ROOT/'Addons/ThaumicEnergistics','thaumichorizons':ROOT/'Addons/ThaumicHorizons','thaumicbases':ROOT/'Addons/ThaumicBases','automagy':ROOT/'Addons/Automagy'}
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def quantities(v):return [{'aspect':a,'amount':n} for a,n in sorted(v.items()) if n>0]
def row(id,v):return {'items':[id],'aspects':quantities(v)}
def merge(a,b):
 for k,v in b.items():a[k]=max(a.get(k,0),v)
 return a
def vals(r):return {x['aspect']:x['amount'] for x in r['aspects']}
sources={};owned={};cross=collections.defaultdict(list);complex_rows=collections.defaultdict(list)
for ns,project in PROJECTS.items():
 for source in ['main','generated']:
  for p in (project/'src'/source/'resources').rglob('*.json'):
   if p.parent.name in ('item_aspects','item_bonus_aspects'):
    sources[p]=read(p)
archive=OUT/'original-definitions.zip'
assert not archive.exists(),'Migration already applied; inspect archived result before rerunning'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sources:z.write(p,p.relative_to(ROOT).as_posix())
for name,profile in [('six-mods','sixretry-before'),('core','core-before')]:
 target=OUT/name;shutil.copytree(TASK/profile,target)
 for f in ['table.json','resources.json']:
  # Keep JSON source evidence compressed; CSV remains directly readable.
  with zipfile.ZipFile(target/(f+'.zip'),'w',zipfile.ZIP_DEFLATED) as z:z.write(target/f,f)
  (target/f).unlink()
six=read(TASK/'sixretry-before/table.json');core=read(TASK/'core-before/table.json')
allitems={r['id']:r for r in six['items']};coreitems={r['id']:r for r in core['items']}
for ns in PROJECTS:
 owned[ns]={}
 dataset=coreitems if ns=='thaumcraft' else allitems
 for id,r in dataset.items():
  if id=='minecraft:air' or r['complex']:continue
  if id.split(':')[0] not in (['minecraft','thaumcraft'] if ns=='thaumcraft' else [ns]):continue
  if id=='thaumicbases:node_focus_brightness':continue # Configuration selects one complete explicit variant.
  owned[ns][id]={a:n for a,n in r['aspects'].items() if ':' not in a or a.split(':')[0]==ns}
for ns,project in PROJECTS.items():
 for p,d in sources.items():
  if not p.is_relative_to(project):continue
  optional='optional_packs' in p.parts
  if optional:
   # Preserve optional mod/config conditions and move bonuses into base files.
   target=Path(str(p).replace('item_bonus_aspects','item_aspects'))
   if target!=p:
    prev=sources.get(target,{'format':1,'entries':[]})
    write(target,{'format':1,'entries':prev['entries']+d['entries']});write(p,{'format':1,'entries':[]})
   else:
    # A paired bonus file may already have appended to this path.
    if not any(q!=p and Path(str(q).replace('item_bonus_aspects','item_aspects'))==p for q in sources):
     write(p,{'format':1,'entries':[{k:v for k,v in r.items() if k!='generated'} for r in d['entries']]})
   continue
  for r in d['entries']:
   for id in r['items']:
    if id in allitems and allitems[id]['complex']:
     complex_rows[ns].append(row(id,vals(r)));continue
    native=id.split(':')[0] in (['minecraft','thaumcraft'] if ns=='thaumcraft' else [ns])
    if native:
     # Restore the owner's normal definition when a conditional addon replaced it.
     if id in owned[ns] and any(':' in a and a.split(':')[0]!=ns for a in allitems.get(id,{}).get('aspects',{})):
      merge(owned[ns][id],vals(r))
    else:cross[ns].append((id,vals(r)))
  write(p,{'format':1,'entries':[]})
# Ordinary base definitions are now complete, including an explicit empty list.
report={'counts':{},'redundant_cross_contributions':[]}
for ns,project in PROJECTS.items():
 entries=[row(id,v) for id,v in sorted(owned[ns].items())]+complex_rows[ns]
 baseline={id:v for rows in owned.values() for id,v in rows.items()}
 additions={}
 for id,v in cross[ns]:
  previous=baseline.get(id,coreitems.get(id,{}).get('aspects',{}))
  # Snapshot values already include generated-list pruning and all loaded rules.
  effective=allitems.get(id,{}).get('aspects',v)
  delta={a:n for a,n in effective.items() if n>previous.get(a,0)}
  if delta:merge(additions.setdefault(id,{}),delta)
  else:report['redundant_cross_contributions'].append({'owner':ns,'item':id,'aspects':v})
 entries += [row(id,v) for id,v in sorted(additions.items())]
 write(project/f'src/main/resources/data/{ns}/item_aspects/explicit.json',{'format':1,'entries':entries})
 report['counts'][ns]={'complete':len(owned[ns]),'complex_preserved':len(complex_rows[ns]),'cross_additions':len(additions)}
# Freeze technical block/legacy-only targets, retaining state selectors and scan identity.
for ns,project in PROJECTS.items():
 entries=[];dataset=core if ns=='thaumcraft' else six
 for b in dataset['blocks']:
  if b['id'].split(':')[0] not in (['minecraft','thaumcraft'] if ns=='thaumcraft' else [ns]):continue
  # Blocks with a normal item continue following the existing item/scan mappings.
  if b['item']!='minecraft:air':continue
  if b['id'] in ['minecraft:air','minecraft:cave_air','minecraft:void_air']:continue
  states=b['states'];unique={json.dumps(s['aspects'],sort_keys=True) for s in states}
  if len(unique)==1:entries.append({'block':b['id'],'aspects':states[0]['aspects']})
  else:entries += [{'block':b['id'],'properties':s['properties'],'aspects':s['aspects']} for s in states]
 write(project/f'src/main/resources/data/{ns}/block_aspects/explicit.json',{'format':1,'entries':entries})
 report['counts'][ns]['block_rules']=len(entries)
write(OUT/'migration.json',report);print(json.dumps(report,ensure_ascii=False,indent=2))
