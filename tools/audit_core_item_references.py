"""Static item-name check against the exact core production JAR's generated model catalog."""
from pathlib import Path
import zipfile,json
base=Path(__file__).resolve().parents[1];core=base/'libs/thaumcraft-0.4.48.jar'
with zipfile.ZipFile(core) as z:items={p.removeprefix('assets/thaumcraft/models/item/').removesuffix('.json') for p in z.namelist() if p.startswith('assets/thaumcraft/models/item/') and p.endswith('.json')}
errors={}
def visit(value,path):
 if isinstance(value,dict):
  item=value.get('item')
  if isinstance(item,str) and item.startswith('thaumcraft:') and item.split(':',1)[1] not in items:errors.setdefault(item,[]).append(str(path))
  for child in value.values():visit(child,path)
 elif isinstance(value,list):
  for child in value:visit(child,path)
for path in (base/'src/main/resources/data/gadomancy').rglob('*.json'):visit(json.loads(path.read_text()),path.relative_to(base))
report='\n'.join(key+'\n  '+'\n  '.join(sorted(set(value))) for key,value in sorted(errors.items())) if errors else 'PASS: every referenced Thaumcraft item resolves to a model in the core JAR.'
(base/'reports/core-item-reference-audit.txt').write_text(report+'\n',encoding='utf8');print(report)
