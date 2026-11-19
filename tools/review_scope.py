"""Review every Collaboration target against its published project family."""
import concurrent.futures,json,re
import acquire_modern as a
import generate as g
old=g.collect();invs=g.inventories();audit=g.read(g.COL/'jar-audit.json');downloads=g.read(g.COL/'downloads.json')
jar_mods={}
for record in audit:
 info=record.get('modinfo') or []
 if isinstance(info,dict):info=info.get('modList',[info])
 jar_mods[record['jar']]={m['modid'] for m in info if isinstance(m,dict) and m.get('modid')}
projects={mod:set() for mod in old}
for record in downloads:
 for mod in jar_mods.get(record.get('jar'),set()):
  if mod not in projects:continue
  for link in record.get('links',[]):
   m=re.search('/minecraft/mc-mods/([^/?#]+)',link['url'])
   if m:projects[mod].add(m.group(1))
def check(pair):
 mod,slugs=pair
 if mod in invs:return {'legacy_mod':mod,'decision':'counterpart verified','counterpart':invs[mod].get('slug',invs[mod].get('project')),'slugs':sorted(slugs)}
 results=[]
 for slug in sorted(slugs):
  url='https://api.cfwidget.com/minecraft/mc-mods/'+slug
  try:
   d=json.loads(a.get(url));g.write(g.ROOT/'evidence/scope-projects'/(slug+'.json'),d)
   fs=[f for f in d.get('files',[]) if '1.20.1' in f.get('versions',[]) and 'Fabric' not in f.get('versions',[]) and 'NeoForge' not in f.get('versions',[])]
   results.append({'url':url,'forge_1201_files':len(fs),'versions':sorted(d.get('versions',{}))})
   if fs:
    a.CF_SLUGS[mod]=slug
    result=a.acquire((mod,[]))
    if result.get('jar'):return {'legacy_mod':mod,'decision':'counterpart acquired','counterpart':slug,'checks':results}
  except Exception as error:results.append({'url':url,'error':str(error)})
 print(mod,'reviewed',len(results),flush=True)
 return {'legacy_mod':mod,'decision':'no Forge 1.20.1 release in reviewed project family' if results and all('forge_1201_files' in r and not r['forge_1201_files'] for r in results) else 'no verified counterpart; retained for review','checks':results}
with concurrent.futures.ThreadPoolExecutor(6) as pool:results=list(pool.map(check,projects.items()))
g.write(g.ROOT/'evidence/scope-review.json',results)
