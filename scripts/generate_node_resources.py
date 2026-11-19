"""Authored data definitions. Legacy text/pixels/meshes remain in the external reference jar."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
MOD = 'thaumichorizons'
def write(path, value):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
def asset(path, value): write(f'assets/{MOD}/{path}.json', value)
def data(path, value): write(f'data/{MOD}/{path}.json', value)
def stack(item): return {'kind': 'stack', 'stack': {'item': item, 'count': 1, 'metadata': 0, 'damageable': False}}
def sequence(items): return {'kind':'sequence','elements':[stack(i) for i in items]}
def box(start, end, uv=(0,0,16,16)):
    return {'from':start,'to':end,'faces':{face:{'texture':'#body','uv':list(uv)} for face in ['up','down','north','south','east','west']}}

ids = {'synthetic_node':'synthNode','vis_dynamo':'visDynamo','essentia_dynamo':'essentiaDynamo','node_monitor':'nodeMonitor','recombinator':'recombinator'}
for modern in ids:
    asset('blockstates/'+modern, {'variants':{'':{'model':f'{MOD}:block/{modern}'}}})
    asset('models/item/'+modern, {'parent':'builtin/entity','textures':{'particle':'minecraft:block/gold_block'}})
    data('loot_tables/blocks/'+modern, {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:{modern}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    texture='minecraft:block/gold_block'
    elements=[]
    if modern.endswith('dynamo'):
        texture=f'{MOD}:models/'+('dynamobase' if modern=='vis_dynamo' else 'dynamoessentiabase')
        pedestal=box([0,0,0],[16,1,16])
        for face,uv in {'up':[4,0,8,8],'down':[8,0,12,8],'north':[4,8,8,8.5],'south':[12,8,16,8.5],'west':[0,8,4,8.5],'east':[8,8,12,8.5]}.items():pedestal['faces'][face]['uv']=uv
        elements=[pedestal]
    elif modern=='recombinator':
        texture=f'{MOD}:models/recombinator'
    asset('models/block/'+modern, {'textures':{'body':texture,'particle':texture},'elements':elements,'render_type':'minecraft:cutout'})

recipes=[]; research=[]; pages=[]; displays=[]
def entry(key,item,column,row,complexity,costs,parents=(),hidden=(),secondary=False,concealed=False,texts=1):
    research.append({'key':key,'category':'ThaumicHorizons','displayColumn':column,'displayRow':row,'complexity':complexity,
                     'aspects':[{'aspect':a,'amount':n} for a,n in costs.items()],'parents':list(parents),'parentsHidden':list(hidden),
                     'isSecondary':secondary,'isConcealed':concealed,'icon_item':{'item':item},'pageCount':texts+1})
    page=[{'type':'TEXT','text':key+'1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:{key}']}}]
    page += [{'type':'TEXT','text':key+str(i)} for i in range(2,texts+1)]
    pages.append({'key':key,'pages':page})
def infusion(key,item,center,components,instability,aspects):
    data('recipes/infusion/'+item,{'type':'thaumcraft:infusion','research':key,'instability':instability,'aspects':aspects,
                                 'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':x}} for x in components],
                                 'result':{'item':f'{MOD}:{item}'}})
    displays.append({'key':f'{MOD}:{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':key,
                     'input':stack(center),'output':stack(f'{MOD}:{item}'),'ingredients':sequence(components),'instability':instability,'aspects':aspects}})
infusion('nodeMonitor','node_monitor','thaumcraft:thaumometer',['thaumcraft:zombie_brain','minecraft:comparator','thaumcraft:air_shard'],2,{'auram':2,'sensus':8,'machina':8})
entry('nodeMonitor',f'{MOD}:node_monitor',1,6,1,{'auram':3,'machina':3,'sensus':3},['INFUSION'])
infusion('visDynamo','vis_dynamo','minecraft:piston',['thaumcraft:primal_charm','minecraft:gold_ingot','thaumcraft:greatwood_planks','minecraft:gold_ingot'],5,{'auram':4,'potentia':16,'permutatio':16,'machina':16})
entry('visDynamo',f'{MOD}:vis_dynamo',3,2,2,{'auram':5,'machina':5,'potentia':5},hidden=['INFUSION','VISPOWER'])
infusion('essentiaDynamo','essentia_dynamo','minecraft:piston',['thaumcraft:thaumium_ingot','thaumcraft:alchemical_construct','thaumcraft:greatwood_planks','thaumcraft:thaumium_ingot','thaumcraft:essentia_valve'],5,{'auram':4,'potentia':16,'permutatio':16,'machina':16,'aqua':32})
entry('essentiaDynamo',f'{MOD}:essentia_dynamo',3,4,2,{'auram':5,'machina':5,'potentia':5,'aqua':5},['visDynamo'],['INFUSION','VISPOWER','THAUMIUM'],True,True,2)
infusion('etherealShard','synthetic_node','thaumcraft:balanced_shard',['thaumcraft:alumentum']*4,7,{'auram':8,'potentia':32,'vacuos':32})
entry('etherealShard',f'{MOD}:synthetic_node',1,4,3,{'auram':8,'potentia':16,'vacuos':16,'praecantatio':8},['shardTrans'],['INFUSION','VISPOWER'],concealed=True,texts=2)
asset('models/item/planar_conduit',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/planarconduit'}})
planar_cost={a:50 for a in ['aer','ignis','aqua','terra','ordo','perditio']}
data('recipes/arcane/planar_conduit',{'type':'thaumcraft:arcane','research':'planarTheory','pattern':['VEV','EPE','VEV'],'key':{'V':{'item':'thaumcraft:void_ingot'},'E':{'item':'thaumcraft:wisp_essence'},'P':{'item':'minecraft:ender_pearl'}},'vis':planar_cost,'result':{'item':f'{MOD}:planar_conduit'}})
entry('planarTheory',f'{MOD}:planar_conduit',10,8,3,{'vacuos':5,'praecantatio':5,'alienis':5},['VISPOWER','VOIDMETAL'],concealed=True,texts=2)
pages[-1]['pages'][1]['type']='ARCANE_CRAFTING'
pages[-1]['pages'][1]['recipe']['class']='thaumcraft.api.crafting.ShapedArcaneRecipe'
displays.append({'key':f'{MOD}:planarTheory','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','data':{'kind':'arcane_shaped','research':'planarTheory','aspects':planar_cost,'width':3,'height':3,'mirrored':True,'output':stack(f'{MOD}:planar_conduit'),'ingredients':sequence(['thaumcraft:void_ingot','thaumcraft:wisp_essence','thaumcraft:void_ingot','thaumcraft:wisp_essence','minecraft:ender_pearl','thaumcraft:wisp_essence','thaumcraft:void_ingot','thaumcraft:wisp_essence','thaumcraft:void_ingot'])}})
infusion('recombinator','recombinator','thaumcraft:primordial_pearl',['thaumcraft:vis_filter',f'{MOD}:planar_conduit','thaumcraft:vis_filter','thaumcraft:node_converter','thaumcraft:vis_filter',f'{MOD}:planar_conduit','thaumcraft:vis_filter','thaumcraft:node_stabilizer'],9,{'permutatio':32,'auram':20,'fabrico':32,'potentia':32})
entry('recombinator',f'{MOD}:recombinator',12,8,3,{'auram':4,'permutatio':4,'fabrico':4},['planarTheory','PRIMPEARL'],concealed=True,texts=3)

keys=[]
for element,aspect in [('fire','ignis'),('water','aqua'),('air','aer'),('earth','terra'),('order','ordo'),('entropy','perditio')]:
    key=f'{MOD}:shard_trans_{element}';keys.append(key)
    data('recipes/crucible/shard_trans_'+element,{'type':'thaumcraft:crucible','research':'shardTrans','aspects':{aspect:2,'permutatio':1},'catalyst':{'item':'thaumcraft:balanced_shard'},'result':{'item':f'thaumcraft:{element}_shard'}})
    displays.append({'key':key,'class':'thaumcraft.api.crafting.CrucibleRecipe','data':{'kind':'crucible','research':'shardTrans','aspects':{aspect:2,'permutatio':1},'catalyst':stack('thaumcraft:balanced_shard'),'output':stack(f'thaumcraft:{element}_shard')}})
entry('shardTrans','thaumcraft:balanced_shard',1,2,2,{'permutatio':4,'vitreus':4,'ignis':2,'aqua':2,'terra':2,'aer':2,'ordo':2,'perditio':2},secondary=True)
pages[-1]['pages'][1]={'type':'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.CrucibleRecipe','keys':keys}}
data('research_catalog/nodes',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]})
data('research_pages/nodes',{'format':1,'entries':pages})
data('research_page_recipes/nodes',{'format':1,'entries':displays})

mapping={};languages={}
for path in ['textures/gui/guidynamo.png','textures/models/nodemon.png','textures/models/nodemonscreen.png','textures/models/nodemonscreenactive.png','textures/models/goldring.png','textures/models/thaumiumring.png','textures/models/dynamobase.png','textures/models/dynamoessentiabase.png','textures/models/recombinator.png','textures/misc/vat.png','textures/items/planarconduit.png','textures/misc/recombinatortext.png']:
    mapping[f'assets/{MOD}/{path}']=f'assets/thaumichorizons/{path}'
mapping[f'assets/{MOD}/models/hexagon.obj']='assets/thaumichorizons/textures/models/hexagon.obj'
for target,source in [('en_us','en_US'),('zh_cn','zh_CN')]:
    path=f'assets/{MOD}/lang/{target}.json';mapping[path]=f'assets/thaumichorizons/lang/{source}.lang'
    translations={f'block.{MOD}.{k}':f'tile.ThaumicHorizons_{v}.name' for k,v in ids.items()}
    translations['tc.research_category.ThaumicHorizons']='tc.research_category.ThaumicHorizons'
    translations[f'item.{MOD}.planar_conduit']='item.conduit.name'
    for row in research:
        key=row['key']
        for prefix in ['tc.research_name.','tc.research_text.']:translations[prefix+key]=prefix+key
    for row in pages:
        for page in row['pages']:
            if 'text' in page:translations[page['text']]=page['text']
    languages[path]=translations
data('thaumcraft/original_resources',{'name':'Thaumic Horizons','legacy_modid':'ThaumicHorizons','sha256':'946cda75e5a17785507e2a5dcc6798d718900952545a812be32b7b34c11f60b8','download':'https://www.curseforge.com/minecraft/mc-mods/thaumic-horizons/files/8413552','mapping':mapping,'languages':languages})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
