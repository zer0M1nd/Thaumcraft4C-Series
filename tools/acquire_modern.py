"""Pin Forge 1.20.1 counterparts and retain binary/localization evidence."""
import concurrent.futures, hashlib, json, pathlib, urllib.parse, urllib.request, zipfile
ROOT = pathlib.Path(__file__).resolve().parents[1]
CANDIDATES = {
 'TConstruct':['tinkers-construct'], 'Railcraft':['railcraft-reborn'], 'Forestry':['forestry'],
 'appliedenergistics2':['ae2'], 'Mekanism':['mekanism'], 'Botania':['botania'],
 'EnderIO':['enderio'], 'BiomesOPlenty':['biomes-o-plenty'], 'AWWayofTime':['blood-magic'],
 'PneumaticCraft':['pneumaticcraft-repressurized'], 'ProjectE':['projecte'],
 'TwilightForest':['twilightforest'], 'evilcraft':['evilcraft'], 'SilentGems':['silents-gems'],
 'Avaritia':['re-avaritia'], 'gregtech':['gregtechceu-modern'],
 'TofuCraft':['tofucraftreload'], 'harvestcraft':['pams-harvestcraft-2-food-core'],
 'MineFactoryReloaded':['industrial-foregoing'], 'IC2':['ic2-classic'],
 'NuclearCraft':['nuclearcraft-neoteric'], 'EnderTanks':['endertanks'],
 'flammpfeil.slashblade':['slashblade-resharped'], 'totemic':['totemic'],
 'AgriCraft':['agricraft'], 'Mantle':['mantle'], 'ThermalFoundation':['thermal-foundation'],
 'arsmagica2':['mana-and-artifice'], 'witchery':['bewitchment','enchanted-witchcraft'],
 'CarpentersBlocks':['framedblocks'], 'ExtraUtilities':['extra-utilities-reborn'],
 'Natura':['natura'], 'MagicBees':['magic-bees'], 'Baubles':['curios'],
 'MoCreatures':['mo-creatures'], 'abyssalcraft':['abyssalcraft'], 'erebus':['the-erebus'],
 'HardcoreEnderExpansion':['hardcore-ender-expansion'], 'techreborn':['techreborn'],
 'SaltMod':['salt'], 'betterstorage':['betterstorage'], 'lotr':['the-lord-of-the-rings-mod-renewed'],
}
CF_SLUGS={'Forestry':'forestry-community-edition','TwilightForest':'the-twilight-forest','ProjectE':'projecte',
 'MineFactoryReloaded':'industrial-foregoing','harvestcraft':'pams-harvestcraft-2-food-core',
 'IC2':'industrial-craft-2', 'ExtraUtilities':'extra-utilities-reborn',
 'MoCreatures':'mo-creatures', 'MagicBees':'magic-bees', 'Natura':'natura',
 'lotr':'the-lord-of-the-rings-mod-renewed', 'erebus':'the-erebus','abyssalcraft':'abyssalcraft',
 'HardcoreEnderExpansion':'hardcore-ender-expansion', 'totemic':'totemic', 'betterstorage':'betterstorage',
 'TofuFactory':'tofucraftreload','randomplants':'pams-harvestcraft-2-crops',
 'temperateplants':'pams-harvestcraft-2-crops','weeeflowers':'pams-harvestcraft-2-crops'}
def inventory(legacy,slug,version,version_id,url,dest,trials):
 data=dest.read_bytes()
 with zipfile.ZipFile(dest) as z:
  lang={}; items=set(); blocks=set(); modids=[]
  for n in z.namelist():
   if n.endswith('/lang/en_us.json'):
    try: lang.update(json.loads(z.read(n)))
    except Exception: pass
   p=n.split('/')
   if len(p)>=5 and p[0]=='assets' and p[2:4]==['models','item'] and n.endswith('.json'): items.add(p[1]+':'+ '/'.join(p[4:])[:-5])
   if len(p)>=4 and p[0]=='assets' and p[2]=='blockstates' and n.endswith('.json'): blocks.add(p[1]+':'+ '/'.join(p[3:])[:-5])
   if n.endswith('.json') and n.startswith('data/') and ('/tags/items/' in n or '/tags/blocks/' in n):
    try:
     values=json.loads(z.read(n)).get('values',[])
     for value in values:
      id=value.get('id') if isinstance(value,dict) else value
      if isinstance(id,str) and not id.startswith('#') and ':' in id:
       (items if '/tags/items/' in n else blocks).add(id)
    except Exception: pass
  if 'META-INF/mods.toml' in z.namelist():
   import re
   text=z.read('META-INF/mods.toml').decode()
   for part in re.split(r'\[\[mods\]\]',text)[1:]:
    m=re.search(r'modId\s*=\s*[\"\']([^\"\']+)',part.split('[[',1)[0])
    if m:modids.append(m[1])
 for key in lang:
  p=key.split('.')
  if len(p)==3 and p[0] in ('item','block') and re.fullmatch('[a-z0-9_/-]+',p[2]):
   (items if p[0]=='item' else blocks).add(p[1]+':'+p[2])
 result={'legacy':legacy,'slug':slug,'version':version,'version_id':version_id,'url':url,'jar':str(dest.relative_to(ROOT)), 'sha256':hashlib.sha256(data).hexdigest(),'modids':modids,'items':sorted(items),'blocks':sorted(blocks),'lang':lang,'trials':trials}
 (ROOT/'evidence'/('inventory-'+legacy.replace('.','_')+'.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
 print(legacy,slug,version,len(items),len(blocks),flush=True)
 return result
def get(url):
 import time
 for attempt in range(3):
  try:
   with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'ThaumicOmniverse-development/0.1'}),timeout=90) as response:
    chunks=[]
    while True:
     chunk=response.read(32768)
     if not chunk: break
     chunks.append(chunk)
    return b''.join(chunks)
  except Exception:
   if attempt==2: raise
   time.sleep(1)
def acquire(pair):
 legacy, slugs = pair
 trials=[]
 for slug in slugs:
  url='https://api.modrinth.com/v2/project/'+slug+'/version?'+urllib.parse.urlencode({'game_versions':'["1.20.1"]','loaders':'["forge"]'})
  try:
   versions=json.loads(get(url)); trials.append({'url':url,'versions':len(versions)})
   if not versions: continue
   v=next((v for v in versions if v['version_type']=='release'),versions[0]); f=next((f for f in v['files'] if f.get('primary')),v['files'][0])
   dest=ROOT/'evidence'/'modern-jars'/f['filename']; dest.parent.mkdir(parents=True,exist_ok=True)
   if not dest.exists() or hashlib.sha512(dest.read_bytes()).hexdigest()!=f['hashes']['sha512']:
    dest.write_bytes(get(f['url']))
   data=dest.read_bytes()
   if hashlib.sha512(data).hexdigest()!=f['hashes']['sha512']: raise ValueError('hash mismatch')
   return inventory(legacy,slug,v['version_number'],v['id'],f['url'],dest,trials)
  except Exception as e:
   import traceback
   trials.append({'slug':slug,'error':str(e),'traceback':traceback.format_exc()})
 if legacy in CF_SLUGS:
  slug=CF_SLUGS[legacy];url='https://api.cfwidget.com/minecraft/mc-mods/'+slug
  try:
   d=json.loads(get(url));(ROOT/'evidence'/('cf-'+legacy+'.json')).write_text(json.dumps(d,ensure_ascii=False),encoding='utf-8')
   fs=[f for f in d.get('files',[]) if '1.20.1' in f.get('versions',[]) and ('Forge' in f.get('versions',[]) or not any(x in f.get('versions',[]) for x in ['Fabric','NeoForge'])) and f['name'].endswith('.jar') and not any(x in f['name'] for x in ['sources','-dev','1.20.2','1.20.3','1.20.4','1.20.5','1.20.6','1.21'])]
   trials.append({'url':url,'forge_1201_files':len(fs)})
   if fs:
    f=sorted(fs,key=lambda f:f.get('uploaded_at',''),reverse=True)[0];fid=str(f['id']);u='https://mediafilez.forgecdn.net/files/'+fid[:-3]+'/'+str(int(fid[-3:]))+'/'+urllib.parse.quote(f['name'])
    dest=ROOT/'evidence'/'modern-jars'/f['name'];dest.parent.mkdir(parents=True,exist_ok=True)
    if not dest.exists() or dest.stat().st_size!=f['filesize']:dest.write_bytes(get(u))
    if dest.stat().st_size!=f['filesize']:raise ValueError('CF file length mismatch')
    return inventory(legacy,slug,f['display'],fid,u,dest,trials)
  except Exception as e:
   import traceback
   trials.append({'url':url,'error':str(e),'traceback':traceback.format_exc()})
 print(legacy,'NO VERIFIED FORGE VERSION',flush=True)
 return {'legacy':legacy,'trials':trials}
if __name__=='__main__':
 candidates=dict(CANDIDATES);candidates.update({k:[] for k in CF_SLUGS if k not in candidates})
 with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool: results=list(pool.map(acquire,candidates.items()))
 (ROOT/'evidence'/'acquisition.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
