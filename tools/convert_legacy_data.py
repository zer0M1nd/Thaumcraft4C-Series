"""Deterministic literal translator for the audited 8.1.1 recipe/research declarations.
Does not import or execute reference Java. Unknown expressions fail closed.
"""
from pathlib import Path
import re,json,zipfile,hashlib
P=Path(__file__).resolve().parents[1];ROOT=P.parent.parent
REF=ROOT/'Addons/references/phase2/taintedmagic/original-source/src/main/java/taintedmagic/common'
def emit(name,data):
 p=P/'src/generated/resources'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def split(s):
 out=[];start=0;depth=0;quote=None;escape=False
 for n,c in enumerate(s):
  if quote:
   if escape:escape=False
   elif c=='\\':escape=True
   elif c==quote:quote=None
  elif c in '\"\'':quote=c
  elif c in '({[':depth+=1
  elif c in ')}]':depth-=1
  elif c==',' and depth==0:out.append(s[start:n].strip());start=n+1
 out.append(s[start:].strip());return out
def balanced(s,start):
 depth=0;quote=None
 for n in range(start,len(s)):
  c=s[n]
  if quote:
   if c==quote and s[n-1]!='\\':quote=None
  elif c in '\"\'':quote=c
  elif c=='(':depth+=1
  elif c==')':
   depth-=1
   if depth==0:return s[start+1:n],n+1
 raise ValueError('Unbalanced expression')
def snake(s):return re.sub(r'(?<!^)(?=[A-Z])','_',s).lower()
MATERIALS=['shadowmetal_ingot','shadow_imbued_cloth','crimson_stained_cloth','warped_unbalanced_shard','tainted_unbalanced_shard','shard_of_creation','thaumic_alloy_plate','crimson_alloy_plate','shadowmetal_nugget','primordial_nodule','primordial_mote','fragment_of_creation']
META={'ItemMaterial':MATERIALS,'ItemWandRod':['wand_rod_warpwood','wand_rod_warpwood_staff'],'ItemWandCap':['wand_cap_shadowmetal','wand_cap_cloth','wand_cap_crimsoncloth','wand_cap_shadowcloth'],'ItemSalis':['salis_tempestas','salis_aevus'],'ItemKatana':['thaumium_katana','voidmetal_katana','shadowmetal_katana']}
SINGLE=re.findall(r'public static (?:Item\w*|Block) (\w+);',(REF/'registry/ItemRegistry.java').read_text(encoding='utf-8'))
SINGLE+=[i for i in re.findall(r'public static Block (\w+);',(REF/'registry/BlockRegistry.java').read_text(encoding='utf-8'))]
OWN={name:snake(re.sub(r'^(Item|Block)','',name)) for name in SINGLE if name not in META}
ASPECTS=dict(re.findall(r'Aspect (\w+)=of\("([^"]+)"\)',(ROOT/'TC4/src/main/java/thaumcraft/api/aspects/Aspect.java').read_text(encoding='utf-8')))
LEGACY={}
TC_NAMES=dict(re.findall(r'GameRegistry\.registerItem\(\(Item\)(\w+), \(String\)"([^"]+)"',(ROOT/'TC4/references/deobfuscated_src/thaumcraft/common/config/ConfigItems.java').read_text(encoding='utf-8')))
for group in ['thaumcraft','vanilla']:
 for row in json.loads((ROOT/f'TC4/src/generated/resources/data/thaumcraft/legacy_item_mappings/{group}.json').read_text(encoding='utf-8'))['items']:
  LEGACY[(row['legacy_item'].lower(),row.get('metadata',0))]=row['item']
def item(expr):
 expr=expr.strip();count=1;meta=0
 if expr.startswith('new ItemStack('):
  args=split(balanced(expr,expr.index('('))[0]);expr=args[0];count=int(args[1]) if len(args)>1 else 1;meta=32767 if len(args)>2 and 'WILDCARD' in args[2] else int(args[2]) if len(args)>2 else 0
 if expr.startswith('ItemApi.getItem('):
  args=split(balanced(expr,expr.index('('))[0]);expr='ConfigItems.'+json.loads(args[0]);meta=int(args[1])
 owner,name=expr.split('.',1)
 if owner in ['ItemRegistry','BlockRegistry']:
  if name in META:
   if meta==32767:return {'alternatives':[{'item':'taintedmagic:'+v} for v in META[name]]}
   id='taintedmagic:'+META[name][meta]
  else:id='taintedmagic:'+OWN[name]
 elif owner in ['ConfigItems','ConfigBlocks']:
  name=TC_NAMES.get(name,name)
  key=('thaumcraft:'+name).lower()
  if meta==32767:
   options=[{'item':v} for (k,m),v in LEGACY.items() if k==key]
   if not options:raise ValueError(expr)
   return options[0] if len(options)==1 else {'alternatives':options}
  id=LEGACY.get((key,meta))
  if key=='thaumcraft:itemeldritchobject' and meta==3:id='thaumcraft:primordial_pearl'
  if not id:raise ValueError(f'Unmapped TC item {expr}:{meta}')
 elif owner in ['Items','Blocks']:
  key='minecraft:'+name;id=LEGACY.get((key,meta),key)
 else:raise ValueError('Unsupported item '+expr)
 return {'item':id,'count':count}
def ingredient(value):
 if 'alternatives' in value:return [ingredient(v) for v in value['alternatives']]
 if value['item']=='minecraft:ink_sac':return {'tag':'forge:dyes/black'}
 return {'item':value['item']}
def infusion_ingredient(value):
 result={'ingredient':ingredient(value)}
 if value.get('item')=='minecraft:ink_sac':result.update(exact=True,damage=-1)
 return result

def aspects(expr):
 result={}
 for value in re.findall(r'getPrimals\((\d+)\)',expr):
  for a in ['aer','terra','ignis','aqua','ordo','perditio']:result[a]=result.get(a,0)+int(value)
 for a,n in re.findall(r'\.(?:add|merge)\(Aspect\.(\w+),\s*(\d+)\)',expr):result[ASPECTS[a]]=result.get(ASPECTS[a],0)+int(n)
 return result
def descriptor(v):
 if 'alternatives' in v:return {'kind':'sequence','elements':[descriptor(i) for i in v['alternatives']]}
 if v.get('item')=='minecraft:ink_sac':return {'kind':'ore','names':['forge:dyes/black'],'elements':[{'kind':'stack','stack':{'item':'minecraft:black_dye','count':1}}]}
 return {'kind':'stack','stack':v}
def rid(key):return 'taintedmagic:'+snake(key.replace(':','_'))
source=(REF/'registry/RecipeRegistry.java').read_text(encoding='utf-8')
source=re.sub(r'/\*.*?\*/|//[^\n]*','',source,flags=re.S)
researchSource=(REF/'registry/ResearchRegistry.java').read_text(encoding='utf-8')
RESEARCH=set(re.findall(r'new TMResearchItem\(\s*"([^"]+)"',researchSource))|{'TMELDRITCHMAJOR'}
def research(key):return 'taintedmagic:'+key if key in RESEARCH else key
RECIPES={};DISPLAY=[]
for match in re.finditer(r'ResearchRegistry\.recipes\.put\(',source):
 args=split(balanced(source,match.end()-1)[0]);key=json.loads(args[0]);call=args[1];op=call[:call.index('(')].split('.')[-1];a=split(balanced(call,call.index('('))[0]);name=rid(key);out={};d={}
 if op=='addInfusionCraftingRecipe':
  researchKey=json.loads(a[0]);result=item(a[1]) if a[1].startswith('new ItemStack') else None
  center=item(a[4]);components=[item(x) for x in split(a[5][a[5].index('{')+1:a[5].rindex('}')])]
  if result is None:
   patchkey='inscription' if 'TAG_INSCRIPTION' in a[1] else re.search(r'"([^"]+)"',a[1])[1];num=int(re.search(r'NBTTag(?:Int|Byte)\((?:\(byte\)\s*)?(\d+)\)',a[1])[1]);result={'patch':'{'+patchkey+':'+str(num)+('b' if 'NBTTagByte' in a[1] else '')+'}'}
  out={'type':'thaumcraft:infusion','research':research(researchKey),'instability':int(a[2]),'aspects':aspects(a[3]),'center':{'ingredient':ingredient(center)},'components':[infusion_ingredient(i) for i in components],'result':result}
  output=descriptor(result) if 'item' in result else descriptor(center)
  d={'kind':'infusion','research':out['research'],'instability':out['instability'],'aspects':out['aspects'],'input':descriptor(center),'output':output,'ingredients':{'kind':'sequence','elements':[descriptor(i) for i in components]}}
 elif op=='addCrucibleRecipe':
  result=item(a[1]);catalyst=item(a[2]);cost=aspects(a[3]);out={'type':'thaumcraft:crucible','research':research(json.loads(a[0])),'catalyst':ingredient(catalyst),'aspects':cost,'result':result};d={'kind':'crucible','research':out['research'],'aspects':cost,'catalyst':descriptor(catalyst),'output':descriptor(result)}
 elif op in ['addArcaneCraftingRecipe','addShapelessArcaneCraftingRecipe','addShapedRecipe']:
  arcane=op!='addShapedRecipe';shapeless='Shapeless' in op;result=item(a[1] if arcane else a[0]);cost=aspects(a[2]) if arcane else {};tail=a[3:] if arcane else a[1:];out={'type':'thaumcraft:arcane' if arcane else 'minecraft:crafting_shaped','result':result};d={'kind':('arcane_' if arcane else 'crafting_')+('shapeless' if shapeless else 'shaped'),'output':descriptor(result),'aspects':cost}
  if arcane:out.update(research=research(json.loads(a[0])),vis=cost,mode='shapeless' if shapeless else 'shaped');d['research']=out['research']
  if shapeless:
   values=[item(x) for x in tail];out['ingredients']=[ingredient(x) for x in values];d.update(ingredients={'kind':'sequence','elements':[descriptor(x) for x in values]})
  else:
   pattern=[]
   while tail and tail[0].startswith('"'):pattern.append(json.loads(tail.pop(0)))
   keys={tail[i][1:-1]:item(tail[i+1]) for i in range(0,len(tail),2)};out.update(pattern=pattern,key={k:ingredient(v) for k,v in keys.items()});d.update(width=len(pattern[0]),height=len(pattern),mirrored=True,ingredients={'kind':'sequence','elements':[descriptor(keys[c]) if c!=' ' else None for row in pattern for c in row]})
 else:raise ValueError(op)
 RECIPES[key]=name;emit('data/taintedmagic/recipes/'+name.split(':')[1]+'.json',out);DISPLAY.append({'key':name,'class':'modern','data':d})
emit('data/taintedmagic/research_page_recipes/core.json',{'format':1,'entries':DISPLAY})
(P/'tools/content_inventory.json').write_text(json.dumps({'items':OWN,'meta':META,'recipes':RECIPES},indent=2),encoding='utf-8')
print('Converted',len(RECIPES),'recipe declarations')
