"""Owned JSON definitions only. No original binary or language resources are copied."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
def write(name, value):
    target = ROOT / name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
def data(name, value): write('data/nodalmechanics/' + name + '.json', value)
def stack(item, nbt=None):
    value = {'item': item, 'count': 1}
    if nbt: value['nbt'] = nbt
    return {'kind':'stack', 'stack':value}
def sequence(items): return {'kind':'sequence', 'elements':items}
MATRIX='nodalmechanics:matrix'
SALT='thaumcraft:salis_mundus'
RESEARCH='NODECATALYZATION'
primals=['aer','terra','ignis','aqua','ordo','perditio']
def matrix_nbt(tags): return '{aspects:"'+','.join(tags)+'"}'
def amounts_nbt(tags, amount):
    return '{Aspects:['+','.join('{key:"'+tag+'",amount:'+str(tags.count(tag)*amount)+'}' for tag in dict.fromkeys(tags))+']}'
write('assets/nodalmechanics/models/item/matrix.json', {'parent':'minecraft:item/generated','textures':{'layer0':'nodalmechanics:item/matrix'}})
mapping={'assets/nodalmechanics/textures/item/matrix.png':'assets/nodalmechanics/textures/items/itemMatrix.png'}
languages={}
for old,new in [('en_US','en_us'),('zh_CN','zh_cn'),('ru_RU','ru_ru')]:
    path=f'assets/nodalmechanics/lang/{new}.json'
    mapping[path]=f'assets/nodalmechanics/lang/{old}.lang'
    languages[path]={f'item.nodalmechanics.matrix.{state}':f'item.nodalmechanics.matrix.{state}.name' for state in ['attuned','unattuned']}
    languages[path].update({key:key for key in ['tooltip.nodalmechanics.matrix.attuned','tooltip.nodalmechanics.matrix.unattuned',
        'tc.research_name.NODECATALYZATION','tc.research_text.NODECATALYZATION']})
    languages[path]['itemGroup.NodalMechanics']='itemGroup.nodalmechanics.tab'
    languages[path]['tc.research_page.NODECATALYZATION.0']='nodalmechanics.nodecatalyzation.research'
data('thaumcraft/original_resources', {'name':'Nodal Mechanics 1.7-1.0-7','legacy_modid':'NodalMechanics',
    'sha256':'538ca06dd04eabaab530bbdedabe435c981547add5c6b20bb688863444f34efb',
    'download':'https://www.curseforge.com/minecraft/mc-mods/nodal-mechanics/files/2213742',
    'mapping':mapping,'languages':languages})
data('recipes/matrix', {'type':'thaumcraft:arcane','mode':'shaped','research':RESEARCH,'pattern':['XYX','YZY','XYX'],
    'key':{'X':{'item':SALT},'Y':{'item':'thaumcraft:greatwood_log'},'Z':{'item':'thaumcraft:essentia_jar'}},
    'vis':{'ordo':20,'perditio':20},'result':{'item':MATRIX}})
for count in range(1,9):
    data(f'recipes/attune_{count}', {'type':'thaumcraft:arcane','mode':'shapeless','research':RESEARCH,
        'ingredients':[{'item':MATRIX}]+[{'item':'thaumcraft:essentia_phial'}]*count,'vis':{'ordo':10},
        'result':{'item':MATRIX},'result_transform':{'type':'nodalmechanics:attune'}})
data('recipes/node', {'type':'nodalmechanics:node','research':RESEARCH,'instability':10,
    'center':{'ingredient':{'item':MATRIX},'nbt':matrix_nbt(['ignis']*8)},
    'components':[{'ingredient':{'item':SALT}}]*2,'aspects':{'ignis':1200},
    'result':{'item':'thaumcraft:node_jar','nbt':amounts_nbt(['ignis']*8,15)}})
data('research_catalog/main', {'format':1,'categories':[{'key':'BASICS',
    'icon':'thaumcraft:textures/items/thaumonomiconcheat.png','background':'thaumcraft:textures/gui/gui_researchback.png',
    'research':[{'key':RESEARCH,'category':'BASICS','displayColumn':-5,'displayRow':4,'pageCount':6,
        'icon_item':{'item':MATRIX,'count':1},'complexity':3,'aspects':{'auram':6,'vacuos':3},
        'parents':['NODEJAR','NODETAPPER2'],'parentsHidden':['JARLABEL'],'isSpecial':True,'warp':2}]}]})
pages=[{'type':'TEXT','text':'tc.research_page.NODECATALYZATION.0'}]
entries=[]
def recipe(key, kind, value):
    pages.append({'type':kind,'recipe':{'kind':'reference','keys':['nodalmechanics:'+key],'class':'modern'}})
    entries.append({'key':'nodalmechanics:'+key,'class':'modern','data':value})
recipe('matrix','ARCANE_CRAFTING',{'kind':'arcane_shaped','research':RESEARCH,'width':3,'height':3,
    'aspects':{'ordo':20,'perditio':20},'output':stack(MATRIX),'ingredients':sequence([
        stack(SALT),stack('thaumcraft:greatwood_log'),stack(SALT),stack('thaumcraft:greatwood_log'),stack('thaumcraft:essentia_jar'),
        stack('thaumcraft:greatwood_log'),stack(SALT),stack('thaumcraft:greatwood_log'),stack(SALT)])})
for key,tags in [('mixed',primals),('single',['ignis']*8)]:
    recipe('attune_'+key,'ARCANE_CRAFTING',{'kind':'arcane_shapeless','research':RESEARCH,'aspects':{'ordo':10},
        'output':stack(MATRIX,matrix_nbt(tags)),
        'ingredients':sequence([stack(MATRIX)]+[stack('thaumcraft:essentia_phial',amounts_nbt([tag],8)) for tag in tags])})
for key,tags in [('mixed',primals),('single',['ignis']*8)]:
    recipe('node_'+key,'INFUSION_CRAFTING',{'kind':'infusion','research':RESEARCH,'instability':10,
        'input':stack(MATRIX,matrix_nbt(tags)),'output':stack('thaumcraft:node_jar',amounts_nbt(tags,15)),
        'aspects':{tag:tags.count(tag)*150 for tag in dict.fromkeys(tags)},'ingredients':sequence([stack(SALT)]*2)})
data('research_pages/main',{'format':1,'entries':[{'key':RESEARCH,'pages':pages}]})
data('research_page_recipes/main',{'format':1,'entries':entries})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
