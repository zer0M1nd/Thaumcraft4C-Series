"""Read-only audit; no legacy content is copied into source resources."""
from pathlib import Path
import json,zipfile
base=Path(__file__).resolve().parents[2]
for mod,namespace,jar in [('ElectroMagicTools','emt','ElectroMagicTools-1.2.6.2.jar'),('Gadomancy','gadomancy','gadomancy-1.7.10-1.0.7.3.jar')]:
 resources=base/mod/'src/main/resources';definition=json.loads((resources/f'data/{namespace}/thaumcraft/original_resources.json').read_text())
 bridges={}
 if mod=='ElectroMagicTools':
  bridges={'assets/emt/textures/blocks/amber_bricks.png':'assets/thaumcraft/textures/blocks/amberbrick.png','assets/emt/textures/items/legacy_tin_nugget.png':'assets/thaumcraft/textures/items/nuggettin.png'}
  provider=(base/mod/'src/main/java/emtmodern/client/AmberBridgeResources.java').read_text()
 if mod=='Gadomancy':
  bridges={'assets/gadomancy/textures/items/golem_core_bodyguard.png':'assets/thaumcraft/textures/items/golem_core_bodyguard.png'}
  provider=(base/mod/'src/main/java/gadomancymodern/client/LegacyGolemResources.java').read_text()
 if bridges:
  with zipfile.ZipFile(base.parent/'TC4/references/Thaumcraft-1.7.10-4.2.3.5.jar') as tc:
   for target,source in list(bridges.items()):
    if target not in provider or source not in provider or source not in tc.namelist():del bridges[target]
 with zipfile.ZipFile(base/'references/phase2'/mod/jar) as archive:
  available=set(archive.namelist());issues=[];fallbacks=[]
  english=definition['mapping'].get(f'assets/{namespace}/lang/en_us.lang')
  english_keys={line.split('=',1)[0] for line in archive.read(english).decode('utf8').splitlines() if '=' in line and not line.startswith('#')} if english else set()
  for target,source in definition['mapping'].items():
   if not target.startswith('assets/'+namespace+'/'):issues.append('Invalid target ownership '+target)
   if source not in available:issues.append('Missing mapped entry '+source)
  for language,aliases in definition.get('languages',{}).items():
   original=definition['mapping'].get(language.replace('.json','.lang'))
   if not original:continue
   lines=archive.read(original).decode('utf8').splitlines();keys={line.split('=',1)[0] for line in lines if '=' in line and not line.startswith('#')}
   for target,source in aliases.items():
    if source not in keys:
     message='Missing language '+language+' '+target+' -> '+source
     if source in english_keys:fallbacks.append('English fallback: '+language+' '+target+' -> '+source)
     else:issues.append(message)
  for model in (resources/f'assets/{namespace}/models').rglob('*.json'):
   for texture in json.loads(model.read_text()).get('textures',{}).values():
    if texture.startswith(namespace+':'):
     path=f'assets/{namespace}/textures/'+texture.split(':',1)[1]+'.png'
     if path not in definition['mapping'] and path not in bridges and not (resources/path).exists():issues.append('Missing texture '+str(model.relative_to(resources))+' -> '+path)
  report='\n'.join(issues) if issues else 'PASS: mappings, own texture references and all aliases resolve in the original author JAR (including normal English fallback).'
  if fallbacks:report+='\n\n'+'\n'.join(fallbacks)
  (base/mod/'reports').mkdir(exist_ok=True);(base/mod/'reports/resource-audit.txt').write_text(report+'\n',encoding='utf8');print(mod+': '+str(len(issues))+' issues');print(report[:7000])
