"""Deterministic maximum-union importer. No fuzzy names or namespace-only guesses."""
import collections,csv,json,pathlib,re,zipfile
import family_names
ROOT=pathlib.Path(__file__).resolve().parents[1];BASE=ROOT.parents[1];COL=BASE/'Collaboration'
NS='thaumicomniverse';RES=ROOT/'src/main/resources'
LOCAL={'Automagy':'Automagy','EMT':'ElectroMagicTools','ForbiddenMagic':'ForbiddenMagic','gadomancy':'Gadomancy','TaintedMagic':'TaintedMagic','ThaumicHorizons':'ThaumicHorizons','ThaumicTinkerer':'ThaumicTinkerer','WitchingGadgets':'WitchingGadgets','thaumicbases':'ThaumicBases','thaumicenergistics':'ThaumicEnergistics'}
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def norm(s):return re.sub('[^a-z0-9]','',re.sub(r'§.','',s).lower())
def merge(a,b):
 for k,v in b.items():a[k]=max(a.get(k,0),v)
 return a
def metadata(r):
 m=re.search(r'metadata\s*=\s*([^；; ]+)',r['特殊要求']);return m.group(1) if m else '0'
def collect():
 result=collections.defaultdict(dict)
 for f in read(COL/'list-index.json'):
  if f['target_modid'] in ('minecraft','Thaumcraft'):continue
  with (COL/f['file']).open(encoding='utf-8-sig',newline='') as stream:
   for r in csv.DictReader(stream):
    key=(r['物品 id'],metadata(r));v=result[f['target_modid']].setdefault(key,{'names':set(),'aspects':{},'sources':set()})
    if r['物品英文本地化名称']:v['names'].add(r['物品英文本地化名称'])
    quantities=dict((a,int(n)) for a,n in re.findall(r'([A-Za-z_:./0-9-]+)\s+([0-9]+)',r['要素列表']))
    merge(v['aspects'],quantities);v['sources'].add(f['file'])
 return result
def inventories():
 result={read(p)['legacy']:read(p) for p in sorted((ROOT/'evidence').glob('inventory-*.json'))}
 if 'harvestcraft' in result:
  parts=[result[k] for k in ['harvestcraft','harvestcraft_foodextended','harvestcraft_crops','harvestcraft_trees'] if k in result]
  combined=dict(result['harvestcraft']);combined['items']=sorted({id for p in parts for id in p['items']});combined['blocks']=sorted({id for p in parts for id in p['blocks']});combined['modids']=sorted({id for p in parts for id in p['modids']});combined['lang']={key:value for p in parts for key,value in p['lang'].items()};combined['slug']='Pam HarvestCraft 2 Food Core + Food Extended + Crops + Trees';result['harvestcraft']=combined
 if 'ThermalFoundation' in result and 'ThermalCore' in result:
  a=result['ThermalFoundation'];b=result['ThermalCore'];a['items']=sorted(set(a['items'])|set(b['items']));a['blocks']=sorted(set(a['blocks'])|set(b['blocks']));a['lang'].update(b['lang']);a['modids']=sorted(set(a['modids'])|set(b['modids']))
 for legacy,folder in LOCAL.items():
  project=BASE/'Addons'/folder;lang={};items=set();blocks=set();ids=set()
  toml=project/'src/main/resources/META-INF/mods.toml'
  if toml.exists():
   text=toml.read_text(encoding='utf-8');part=text.split('[[mods]]',1)[-1].split('[[',1)[0];ids.update(re.findall(r'modId\s*=\s*"([^"]+)"',part))
  declared_modids=set(ids)
  for source in ['main','generated']:
   resources=project/'src'/source/'resources'
   for p in resources.glob('assets/*/lang/en_us.json'):lang.update(read(p));ids.add(p.parts[-3])
   for p in resources.glob('assets/*/models/item/*.json'):items.add(p.parts[-4]+':'+p.stem)
   for p in resources.glob('assets/*/blockstates/*.json'):blocks.add(p.parts[-3]+':'+p.stem)
   for p in resources.glob('data/*/thaumcraft/original_resources.json'):
    info=read(p)
    for target,table in info.get('languages',{}).items():
     if not target.endswith('en_us.json'):continue
     # Read only the pinned legacy binary's language; original assets remain external.
     legacy_modid=info.get('legacy_modid',legacy)
     old=legacy_languages_cached().get(legacy_modid,{})
     for modern_key,legacy_key in table.items():
      if legacy_key in old:lang[modern_key]=old[legacy_key]
  # Literal native registrations also cover technical blocks without a blockstate/model file.
  if len(declared_modids)==1:
   namespace=next(iter(declared_modids))
   for p in (project/'src/main/java').rglob('*.java'):
    if 'test' in p.parts:continue
    source=p.read_text(encoding='utf-8-sig')
    items.update(namespace+':'+name for name in re.findall(r'(?:ITEMS\.register|registerItem)\("([a-z0-9_]+)",',source))
    blocks.update(namespace+':'+name for name in re.findall(r'(?:BLOCKS\.register|registerBlock)\("([a-z0-9_]+)",',source))
    if p.name=='PlanarBlocks.java' and 'Horizons.ITEMS.register(name,' in source and 'Horizons.BLOCKS.register(name,' in source:
     paired={namespace+':'+name for name in re.findall(r'=register\("([a-z0-9_]+)",',source)};items.update(paired);blocks.update(paired)
    if namespace=='witchinggadgets' and p.name=='WGAccessories.java':
     content=(p.parent/'Content.java').read_text(encoding='utf-8-sig')
     if 'WGAccessories.TYPES.forEach' in content and 'ITEMS.register(id,' in content:items.update(namespace+':'+name for name in re.findall(r'TYPES\.put\("([a-z0-9_]+)",',source))
  result[legacy]={'legacy':legacy,'modids':sorted(ids),'load_modids':sorted(declared_modids),'items':sorted(items),'blocks':sorted(blocks),'lang':lang,'project':str(project.relative_to(BASE))}
 # GTCEu material items use runtime model generation. IDs and names come from
 # the pinned TagPrefix factory and material language table; missing/config-disabled
 # combinations are safely ignored by TC4, never substituted with a different form.
 if 'gregtech' in result:
  inv=result['gregtech'];items=set(inv['items']);blocks=set(inv['blocks'])
  for pattern,template,kind in gt_templates():
   if not kind:continue
   for key,english in list(inv['lang'].items()):
    if not key.startswith('material.gtceu.'):continue
    material=key[len('material.gtceu.'):];id='gtceu:'+pattern.replace('%s',material)
    items.add(id)
    if kind=='block':blocks.add(id)
    inv['lang'].setdefault(('block.' if kind=='block' else 'item.')+'gtceu.'+id.split(':')[1],template.replace('%s',english))
  inv['items']=sorted(items);inv['blocks']=sorted(blocks)
 return result
_LANGS=None
def legacy_languages_cached():
 global _LANGS
 if _LANGS is None:_LANGS=legacy_languages()
 return _LANGS
def legacy_languages():
 result=collections.defaultdict(dict)
 known_modids={entry['target_modid'] for entry in read(COL/'list-index.json')}
 for jar in read(COL/'jar-audit.json'):
  info=jar.get('modinfo') or []
  if isinstance(info,dict):info=info.get('modList',[info])
  modids=[m.get('modid') for m in info if isinstance(m,dict) and m.get('modid')]
  path=COL/'jars'/jar['jar']
  if not path.exists():continue
  with zipfile.ZipFile(path) as z:
   for name in z.namelist():
    if name.lower().endswith(('/lang/en_us.lang','/lang/en_us.properties','/language/en_us.lang')):
     owners=modids or [mod for mod in known_modids if name.startswith('assets/') and norm(mod)==norm(name.split('/')[1])]
     for line in z.read(name).decode('utf-8-sig',errors='replace').splitlines():
      if '=' not in line or line.startswith('#'):continue
      k,v=line.split('=',1);k=k.strip();v=v.strip()
      for mod in owners:result[mod][k]=v
 return result
def candidate_index(inv):
 own=set(inv['modids'])|{k.split('.')[1] for k in inv['lang'] if k.startswith(('item.','block.')) and len(k.split('.'))>=3};own.discard('minecraft')
 ids={id for id in set(inv['items'])|set(inv['blocks']) if id.split(':')[0] in own};names=collections.defaultdict(set);paths=collections.defaultdict(set)
 for id in ids:
  ns,path=id.split(':',1);paths[norm(path)].add(id)
  for prefix in ['item','block']:
   key=f'{prefix}.{ns}.{path}'
   if key in inv['lang']:names[norm(inv['lang'][key])].add(id)
 return ids,names,paths
def gt_templates():
 source=ROOT/'evidence/modern-source/com/gregtechceu/gtceu/api/data/tag/TagPrefix.java'
 result=[]
 if not source.exists():return result
 for line in source.read_text(encoding='utf-8').splitlines():
  m=re.search(r'new TagPrefix\("([^"]+)"\)',line)
  if not m:continue
  name=re.sub(r'([a-z0-9])([A-Z])',r'\1_\2',m.group(1)).lower()
  pattern=re.search(r'\.idPattern\("([^"]+)"\)',line)
  template=re.search(r'\.langValue\("([^"]+)"\)',line)
  kind='block' if '.generateBlock(true)' in line else 'item' if '.generateItem(true)' in line else None
  result.append((pattern.group(1) if pattern else '%s_'+name,template.group(1) if template else '%s '+name.replace('_',' ').title(),kind))
 return result
def old_name(legacy,id,meta,langs,aliases):
 path=id.split(':',1)[1];unloc=aliases.get(id,path)
 keys=[]
 roots=[path,unloc,id,id.split(':')[0].lower()+':'+path,id.split(':')[0].lower()+':'+unloc,id.replace(':','.'),id.lower().replace(':','.'),id.split(':')[0].lower()+'.'+path]
 if legacy=='MineFactoryReloaded':roots.extend(['mfr.'+path,'mfr.'+unloc])
 for root in roots:
  for prefix in ['', 'item.', 'tile.']:
   keys.extend([prefix+root+'.'+meta+'.name',prefix+root+meta+'.name'])
   if meta in ('0','32767','-1'):keys.extend([prefix+root+'.name'])
 return {langs[k] for k in keys if k in langs}|family_names.names(id,meta,langs)
def choose(legacy,id,meta,row,inv,index,langs,aliases,overrides):
 key=id+'@'+meta
 if key in overrides:
  entry=overrides[key];return entry.get('targets',[]),'reviewed: '+entry['reason']
 ids,names,paths=index
 if legacy=='gregtech' and 'metaitem' in id:
  if '_gt_patterns' not in inv:
   source=(ROOT/'evidence/modern-source/com/gregtechceu/gtceu/api/data/tag/TagPrefix.java').read_text(encoding='utf-8')
   patterns={}
   for line in source.splitlines():
    m=re.search(r'TagPrefix (\w+) = new TagPrefix\("([^"]+)"\)',line)
    if not m:continue
    p=re.search(r'\.idPattern\("([^"]+)"\)',line)
    name=re.sub(r'([a-z0-9])([A-Z])',r'\1_\2',m.group(2)).lower()
    if '.generateItem(true)' in line:patterns[m.group(1)]=p.group(1) if p else '%s_'+name
   mats=collections.defaultdict(set)
   for key,value in inv['lang'].items():
    if key.startswith('material.gtceu.'):mats[norm(value)].add(key[len('material.gtceu.'):])
   inv['_gt_patterns']=patterns;inv['_gt_materials']=mats
  patterns=inv['_gt_patterns'];mats=inv['_gt_materials']
  rename={'dustPure':'dustPure','crushedCentrifuged':'crushedRefined','gearGt':'gear','gearGtSmall':'gearSmall','stickLong':'rodLong','toolHeadBuzzSaw':'toolHeadBuzzSaw'}
  material_renames={'Chrome':'Chromium','Phosphor':'Phosphorus','Uranium 238':'Uranium','Plutonium 239':'Plutonium'}
  candidates=set()
  for name in row['names']:
   if ' ' not in name:continue
   prefix,material=name.split(' ',1);prefix=rename.get(prefix,prefix);material=material_renames.get(material,material)
   if prefix not in patterns:continue
   for mat in mats.get(norm(material),[]):candidates.add('gtceu:'+patterns[prefix].replace('%s',mat))
  if len(candidates)==1:return sorted(candidates),'legacy OrePrefixes material + modern TagPrefix factory'
 # Multi-host GT ore variants need distinct treatment, never collapse by display name.
 if legacy=='gregtech' and 'blockores' in id:
  if id!='gregtech:gt.blockores' or int(meta)>=7000:return [],'removed legacy host/small ore variant; no direct modern block'
  host=int(meta)//1000
  if host in (3,4,5):return [],'legacy black/red granite or marble host has no identical modern host'
  if host:
   prefix={1:'Netherrack ',2:'Endstone ',6:'Basalt '}[host]
   variants=set()
   for name in row['names']|old_name(legacy,id,meta,langs,aliases):
    variants.update(names.get(norm(prefix+name),set()))
   return (sorted(variants),'same ore and host via TagPrefix factory') if len(variants)==1 else ([], 'ore material absent for matching modern host')
 en=row['names']|old_name(legacy,id,meta,langs,aliases)
 candidates=set()
 for name in en:candidates.update(names.get(norm(name),set()))
 if len(candidates)==1:return sorted(candidates),'exact English localization'
 # Name collisions are kept for manual review rather than arbitrarily choosing.
 if len(candidates)>1:return [],'ambiguous localization: '+','.join(sorted(candidates))
 if (id,meta) in family_names.FAMILIES:
  raw=family_names.FAMILIES[(id,meta)]
  variants=[raw]
  m=re.fullmatch(r'(dust|ingot|nugget|gear|crystal)([A-Z].*)',raw)
  if m:variants.append(m.group(2)+'_'+m.group(1))
  matches=set()
  for name in variants:matches.update(paths.get(norm(name),set()))
  if len(matches)==1:return sorted(matches),'metadata name in legacy class + same modern registry form'
 if meta in ('0','32767','-1'):
  path=id.split(':',1)[1]
  for prefix in ['item.','tile.','Tool','Item','Block','item','block']:
   if path.startswith(prefix):path=path[len(prefix):]
  matches=paths.get(norm(path),set())
  if id.split(':')[1].startswith('block'):matches={x for x in matches if x in inv['blocks']}
  if len(matches)==1:return sorted(matches),'same normalized registry path (default/wildcard metadata)'
 return [],'no verified equivalent in pinned 1.20.1 inventory'
def aspect_aliases(invs):
 aliases={}
 core={r['tag'] for r in read(BASE/'TC4/src/main/resources/data/thaumcraft/aspect_registry.json')}
 for project in LOCAL.values():
  for p in (BASE/'Addons'/project/'src/main/resources/data').glob('*/thaumcraft/aspects.json'):
   for r in read(p):aliases[r['tag'].split(':')[-1]]=r['tag']
 for a in ['terminus','strontio','nebrisum','electrum','magneto','radio','tofu','tempus']:aliases[a]=NS+':'+a
 return core,aliases
def run():
 old=collect();invs=inventories();langs=legacy_languages_cached();aliases=read(COL/'extraction.json')['names'];overrides=read(ROOT/'tools/mappings.json') if (ROOT/'tools/mappings.json').exists() else {}
 core,aspects=aspect_aliases(invs);audit=[];stats={};outputs=collections.defaultdict(lambda:collections.defaultdict(dict))
 for legacy,rows in sorted(old.items()):
  inv=invs.get(legacy);index=candidate_index(inv) if inv else None;used=0
  for (id,meta),row in sorted(rows.items()):
   targets,reason=choose(legacy,id,meta,row,inv,index,langs.get(legacy,{}),aliases,overrides) if inv else ([], 'no verified Forge 1.20.1 counterpart')
   if targets:used+=1
   quantities={a if a in core else aspects.get(a,'unavailable:'+a.lower()):n for a,n in row['aspects'].items()}
   selector=overrides.get(id+'@'+meta,{}).get('nbt',{})
   selector_key=json.dumps(selector,sort_keys=True,separators=(',',':'))
   for target in targets:
    kind=overrides.get(id+'@'+meta,{}).get('kind','item_aspects' if target in inv['items'] else 'block_aspects')
    if kind not in ('item_aspects','block_aspects'):raise ValueError('Invalid definition kind: '+kind)
    if selector and kind!='item_aspects':raise ValueError('NBT selector requires an item: '+target)
    merge(outputs[(legacy,kind)][(target,selector_key)],quantities)
   audit.append({'legacy_mod':legacy,'legacy_id':id,'metadata':meta,'english':sorted(row['names']|old_name(legacy,id,meta,langs.get(legacy,{}),aliases)), 'targets':targets,'nbt':selector,'reason':reason,'aspects':quantities,'sources':sorted(row['sources'])})
  item_rows=outputs[(legacy,'item_aspects')];block_rows=outputs[(legacy,'block_aspects')]
  stats[legacy]={'legacy_variants':len(rows),'mapped_variants':used,'counterpart':inv.get('slug',inv.get('project')) if inv else None,'items':len(item_rows),'blocks':len(block_rows),'item_ids':len({id for id,selector in item_rows}),'nbt_rules':sum(selector!='{}' for id,selector in item_rows)}
 for (legacy,kind),entries in outputs.items():
  rows=[]
  for (target,selector_key),v in sorted(entries.items()):
   entry={'items':[target],'aspects':[{'aspect':a,'amount':n} for a,n in sorted(v.items())]} if kind=='item_aspects' else {'block':target,'aspects':dict(sorted(v.items()))}
   selector=json.loads(selector_key)
   if selector:entry['nbt']=selector
   rows.append(entry)
  table={'format':1,'entries':rows}
  guard=[id for id in invs.get(legacy,{}).get('load_modids',invs.get(legacy,{}).get('modids',[])) if id not in ('minecraft','forge')]
  if guard:table['required_any_mods']=sorted(set(guard))
  write(RES/'data'/NS/kind/(norm(legacy)+'.json'),table)
 write(ROOT/'evidence/mapping-audit.json',audit);write(ROOT/'evidence/coverage.json',stats)
 for mod,s in stats.items():print(mod,s['legacy_variants'],s['mapped_variants'],s['items'],s['blocks'],flush=True)
if __name__=='__main__':run()
