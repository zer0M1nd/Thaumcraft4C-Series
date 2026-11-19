"""Build editable aspect JSON from the pinned dependency recipes, rather than generic machine values.

AE recipes use the legacy per-aspect ingredient maximum and six-aspect cap. Addon recipes
use the ordinary TC recipe formula. This is build-time data; resource packs can override it.
"""
import json, math, zipfile
from pathlib import Path
from collections import defaultdict

def apply(root, mod, write):
    quantities=lambda a:{v['aspect']:v['amount'] for v in a}
    key=lambda a:(a['item'],a.get('metadata',0))
    with zipfile.ZipFile('local-maven/local/te/thaumcraft/0.4.11/thaumcraft-0.4.11.jar') as jar:
        data=json.loads(jar.read('data/thaumcraft/recipe_aspect_inputs/legacy.json'))
        cache={key(r):quantities(r['aspects']) for r in data['registrations']}
        recipes=defaultdict(list)
        for r in data['recipes']:recipes[key(r['output'])].append(r)
        props={r['item']:r for r in data['items']}
        active=set()
        def legacy(k):
            p=props.get(k[0],{});normalized=(k[0],32767) if p.get('damageable') or not p.get('subtypes',True) else k
            for lookup in [normalized,(k[0],32767),*([(k[0],i) for i in range(16)] if normalized[1]==32767 else [])]:
                if lookup in cache:return cache[lookup]
            if normalized in active or len(active)>=99:return {}
            active.add(normalized);result={}
            for kind in ['CRUCIBLE','ARCANE','INFUSION','CRAFTING']:
                choices=[]
                for r in recipes.get((k[0],0 if normalized[1]==32767 else k[1]),[]):
                    if r['kind']!=kind:continue
                    total=defaultdict(int)
                    for ingredient in r['ingredients']:
                        value={}
                        for candidate in ingredient['alternatives']:
                            value=legacy(key(candidate))
                            if value or not ingredient['choose_nonempty']:break
                        for a,n in value.items():total[a]+=n
                    value={a:(n if kind=='CRUCIBLE' else int(n*.75/r['count'])) for a,n in total.items()}
                    for a,n in quantities(r['cost']).items():value[a]=value.get(a,0)+int(math.sqrt(n)/r['count'])
                    choices.append({a:min(64,n) for a,n in value.items() if n>0})
                    if kind in ['CRUCIBLE','INFUSION']:break
                if choices:
                    result=choices[-1] if kind=='ARCANE' else min((c for c in choices if c),key=lambda c:sum(c.values()),default={})
                    if result:break
            active.remove(normalized);cache[normalized]=result;return result
        base={}
        for name in ['vanilla','thaumcraft']:
            for r in json.loads(jar.read(f'data/thaumcraft/legacy_item_mappings/{name}.json'))['items']:
                base[r['item']]=legacy((r['legacy_item'],r['metadata']))
        tags={}
        for n in jar.namelist():
            if '/tags/items/' in n and n.endswith('.json'):
                namespace=n.split('/')[1];path=n.split('/tags/items/')[1][:-5]
                tags[namespace+':'+path]=json.loads(jar.read(n)).get('values',[])
    explicit=json.loads((root/f'data/{mod}/item_aspects/main.json').read_text(encoding='utf8'))
    for row in explicit['entries']:
        for item in row['items']:base[item]=quantities(row['aspects'])
    pinned=set(base)
    ae=Path('references/AE2/src/generated/resources/data/ae2')
    for p in (ae/'tags/items').rglob('*.json'):
        tags['ae2:'+p.relative_to(ae/'tags/items').as_posix()[:-5]]=json.loads(p.read_text())['values']
    for namespace in ['forge','ae2']:
        folder=ae.parent/namespace/'tags/items'
        if folder.exists():
            for p in folder.rglob('*.json'):tags[namespace+':'+p.relative_to(folder).as_posix()[:-5]]=json.loads(p.read_text())['values']
    defaults={'dusts/redstone':'redstone','dusts/glowstone':'glowstone_dust','gems/quartz':'quartz','gems/diamond':'diamond','gems/emerald':'emerald','gems/lapis':'lapis_lazuli','glass':'glass','glass/colorless':'glass','glass_panes':'glass_pane','glass_panes/colorless':'glass_pane','rods/wooden':'stick','ender_pearls':'ender_pearl','string':'string','chests/wooden':'chest'}
    for metal in ['iron','gold','copper']:
        defaults['ingots/'+metal]=metal+'_ingot';defaults['storage_blocks/'+metal]=metal+'_block'
    for name,item in defaults.items():tags.setdefault('forge:'+name,[]).insert(0,'minecraft:'+item)
    tags.setdefault('minecraft:wool',['minecraft:white_wool'])
    def ingredient(value,seen=frozenset()):
        if isinstance(value,list):
            for option in value:
                found=ingredient(option,seen)
                if found:return found
            return {}
        if not isinstance(value,dict):return {}
        if 'item' in value:return base.get(value['item']) if value['item'] in base else derive(value['item'])
        tag=value.get('tag')
        if not tag or tag in seen:return {}
        for candidate in tags.get(tag,[]):
            if isinstance(candidate,dict):candidate=candidate.get('id','')
            found=ingredient({'tag':candidate[1:]} if candidate.startswith('#') else {'item':candidate},seen|{tag})
            if found:return found
        return {}
    modern=[]
    for folder in [ae/'recipes',root/f'data/{mod}/recipes']:
        for p in sorted(folder.rglob('*.json')):
            r=json.loads(p.read_text(encoding='utf8'));out=r.get('result',r.get('output'))
            if isinstance(out,list) or not out:continue
            item=out if isinstance(out,str) else out.get('item')
            if not item or item in pinned:continue
            modern.append((item,r,out.get('count',1) if isinstance(out,dict) else 1))
    bonuses={'molecular_assembler':{'fabrico':4,'potentia':6,'machina':2,'vitreus':1},'sky_stone_chest':{'vacuos':4},'quartz_glass':{'vitreus':4},'controller':{'machina':3,'sensus':1,'cognitio':9},'drive':{'vacuos':4},'chest':{'vacuos':4},'io_port':{'permutatio':4},'condenser':{'vacuos':10},'redstone_card':{'sensus':2},'cell_workbench':{'pannus':2},'interface':{'permutatio':6},'logic_processor':{'cognitio':3,'metallum':1},'calculation_processor':{'cognitio':6,'metallum':1},'engineering_processor':{'cognitio':9}}
    resolved=set();active_modern=set();by_output=defaultdict(list)
    for item,r,count in modern:by_output[item].append((r,count))
    def derive(item):
        if item in base:return base[item]
        if item in active_modern or len(active_modern)>99:return {}
        active_modern.add(item)
        for r,count in by_output.get(item,[]):
            parts=[]
            if 'pattern' in r:parts=[r['key'][letter] for row in r['pattern'] for letter in row if letter!=' ']
            else:
                ingredients=r.get('ingredients',[]);parts=list(ingredients.values()) if isinstance(ingredients,dict) else list(ingredients)
            if r.get('type')=='minecraft:smithing_transform':parts.extend([r['base'],r['addition']])
            if 'ingredient' in r:parts.append(r['ingredient'])
            if 'center' in r:parts.append(r['center']['ingredient']);parts.extend(c['ingredient'] for c in r.get('components',[]))
            if not parts:continue
            values=[ingredient(p) for p in parts]
            if not any(values):continue
            value=defaultdict(int)
            for v in values:
                for a,n in v.items():value[a]=max(value[a],n) if item.startswith('ae2:') else value[a]+n
            if item.startswith('ae2:'):
                name=item[4:]
                remove=['ignis','ordo'] if name=='controller' else ['ignis','ordo','sensus','machina'] if name in ['drive','chest','io_port'] else ['metallum','ignis','ordo'] if name.endswith('_processor') else []
                for a in remove:value.pop(a,None)
                for a,n in bonuses.get(name,{}).items():value[a]+=n
                value=dict(sorted(value.items(),key=lambda e:-e[1])[:6])
            else:
                value={a:int(n*.75/count) for a,n in value.items()}
                for a,n in r.get('vis',r.get('aspects',{})).items():value[a]=value.get(a,0)+int(math.sqrt(n)/count)
            base[item]={a:min(64,n) for a,n in value.items() if n>0};resolved.add(item);active_modern.remove(item);return base[item]
        active_modern.remove(item);return {}
    for item,_,_ in modern:derive(item)
    # All essentia cell tiers deliberately share the legacy 1k-cell-derived aspect list.
    if base.get('ae2:item_storage_cell_1k') or base.get('ae2:item_cell_1k'):
        cell=dict(sorted(base.get('ae2:item_cell_1k',base.get('ae2:item_storage_cell_1k')).items(),key=lambda e:-e[1])[:4]);cell['praecantatio']=cell.get('praecantatio',0)+3;cell['auram']=cell.get('auram',0)+5
        for tier in ['1k','4k','16k','64k','256k','1024k','4096k','16384k','quantum','singularity']:
            name=f'{mod}:essentia_cell_{tier}';base[name]=cell;resolved.add(name)
    entries=[{'items':[item],'aspects':[{'aspect':a,'amount':n} for a,n in base[item].items()]} for item in sorted(resolved)]
    write(f'data/{mod}/item_aspects/recipes.json',{'format':1,'generated':True,'entries':entries})
    report=Path('docs/ASPECT_GENERATION.md')
    missing=sorted({i for i,_,_ in modern}-resolved)
    report.write_text('# Recipe aspect generation\n\nPinned TC 0.4.11 / AE2 15.4.8 inputs. Derived entries: '+str(len(entries))+'.\n\nUnresolved recipes (require explicit baseline review, not generic fallback):\n\n'+'\n'.join('- '+i for i in missing)+'\n',encoding='utf8')
