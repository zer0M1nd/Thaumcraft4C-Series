"""Own modern JSON definitions. Legacy images/text remain in the player's original JAR."""
import json
import hashlib
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources'
MAPPING = {}
LANG = {}
NAMESPACE = 'thaumicbases'
with zipfile.ZipFile(ROOT/'references/ThaumicBases-1.3.1710.4.jar') as original:
    LEGACY_FILES=set(original.namelist())

def write(path, value):
    # Complete aspect definitions are maintained directly since TC4 0.4.56.
    if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (OUT / 'data/thaumicbases/item_aspects/explicit.json').exists(): return
    path = OUT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def texture(path):
    original = f'assets/{NAMESPACE}/textures/{path}.png'
    modern = original.lower()
    MAPPING[modern] = original
    if original+'.mcmeta' in LEGACY_FILES: MAPPING[modern+'.mcmeta']=original+'.mcmeta'
    return f'{NAMESPACE}:{path.lower()}'

def language(kind, name, original):
    LANG[f'{kind}.{NAMESPACE}.{name}'] = original

def item(name, path, legacy=None, handheld=False):
    write(f'assets/{NAMESPACE}/models/item/{name}.json', {'parent':'minecraft:item/handheld' if handheld else 'minecraft:item/generated', 'textures':{'layer0':texture('items/' + path)}})
    if legacy: language('item', name, legacy)

def drop(name, slab=False):
    entry = {'type':'minecraft:item','name':f'{NAMESPACE}:{name}'}
    if slab: entry['functions'] = [{'function':'minecraft:set_count','count':2,'conditions':[{'condition':'minecraft:block_state_property','block':f'{NAMESPACE}:{name}','properties':{'type':'double'}}]}]
    write(f'data/{NAMESPACE}/loot_tables/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})

def cube(name, path, legacy, sided=False, slab=False):
    textures = {'all':texture('blocks/' + path)}
    if sided: textures = {'end':texture('blocks/' + path), 'side':texture('blocks/' + path + '_side')}
    write(f'assets/{NAMESPACE}/models/block/{name}.json', {'parent':'minecraft:block/cube_column' if sided else 'minecraft:block/cube_all','textures':textures})
    write(f'assets/{NAMESPACE}/blockstates/{name}.json', {'variants':{'':{'model':f'{NAMESPACE}:block/{name}'}}})
    write(f'assets/{NAMESPACE}/models/item/{name}.json', {'parent':f'{NAMESPACE}:block/{name}'})
    language('block', name, legacy)
    drop(name)
    if slab:
        slabname = name + '_slab'
        for suffix, parent in [('', 'slab'),('_top','slab_top')]:
            write(f'assets/{NAMESPACE}/models/block/{slabname}{suffix}.json', {'parent':f'minecraft:block/{parent}', 'textures':{'bottom':texture('blocks/'+path), 'top':texture('blocks/'+path),'side':texture('blocks/'+path+('_side' if sided else ''))}})
        write(f'assets/{NAMESPACE}/blockstates/{slabname}.json', {'variants':{f'type={kind}':{'model':f'{NAMESPACE}:block/{model}'} for kind,model in [('bottom',slabname),('top',slabname+'_top'),('double',name)]}})
        write(f'assets/{NAMESPACE}/models/item/{slabname}.json', {'parent':f'{NAMESPACE}:block/{slabname}'})
        drop(slabname, True)
        write(f'data/{NAMESPACE}/recipes/{slabname}.json', {'type':'minecraft:crafting_shaped','pattern':['###'],'key':{'#':{'item':f'{NAMESPACE}:{name}'}},'result':{'item':f'{NAMESPACE}:{slabname}','count':6}})
        key = f'tile.tb.crystalslab.{name[8:]}.name' if name.startswith('crystal_') else 'tile.tb.slab.' + legacy
        language('block', slabname, key)

def shapeless(name, ingredients, output, count=1):
    write(f'data/{NAMESPACE}/recipes/{name}.json', {'type':'minecraft:crafting_shapeless','ingredients':[{'item':x} for x in ingredients], 'result':{'item':output,'count':count}})

def box(start, end, uv=None):
    return {'from':start,'to':end,'faces':{face:{'texture':'#texture','uv':uv or [0,0,16,16]} for face in ['up','down','north','south','east','west']}}

def node_models():
    display={'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}, 'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]}, 'fixed': {'rotation': [0, 180, 0], 'scale': [0.5, 0.5, 0.5]}, 'ground': {'scale': [0.25, 0.25, 0.25], 'translation': [0, 3, 0]}, 'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]}, 'thirdperson_lefthand': {'rotation': [75, 45, 0], 'scale': [0.375, 0.375, 0.375], 'translation': [0, 2.5, 0]}, 'thirdperson_righthand': {'rotation': [75, 45, 0], 'scale': [0.375, 0.375, 0.375], 'translation': [0, 2.5, 0]}}
    for name, old, tex in [('node_linker','nodeLinker','nodeLinkerUV'),('node_manipulator','nodeManipulator','baseUVMap')]:
        particle=texture('blocks/'+old+'/'+tex)
        source=f'assets/{NAMESPACE}/models/{old}/{old}.obj'
        MAPPING[source.lower()]=source
        # Actual geometry and original UVs are rendered from the external OBJ.
        write(f'assets/{NAMESPACE}/models/block/{name}.json',{'textures':{'particle':particle}})
        write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':{'':{'model':f'{NAMESPACE}:block/{name}'}}})
        write(f'assets/{NAMESPACE}/models/item/{name}.json',{'parent':'builtin/entity','textures':{'particle':particle},'display':display})
        drop(name);language('block',name,'tile.'+old+'.name')

def device_models():
    def model(name,elements,textures):
        write(f'assets/{NAMESPACE}/models/block/{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':textures,'elements':elements})
    def basic(name,variants,legacy,inventory=None):
        write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':variants})
        write(f'assets/{NAMESPACE}/models/item/{name}.json',{'parent':f'{NAMESPACE}:block/{inventory or name}'})
        drop(name);language('block',name,legacy)
    def faces_box(start,end,sprite):
        element=box(start,end)
        for face in element['faces'].values():face['texture']='#'+sprite
        return element
    for age in range(9):
        tex=texture('blocks/blazingFluid/'+('leftovers' if age==8 else 'block'))
        model('pyrofluid_'+str(age),[box([0,0,0],[16,1 if age==8 else 14-age*1.5,16])],{'texture':tex,'particle':tex})
    write(f'assets/{NAMESPACE}/blockstates/pyrofluid.json',{'variants':{f'age={age}':{'model':f'{NAMESPACE}:block/pyrofluid_{age}'} for age in range(9)}})
    language('block','pyrofluid','tile.pyrofluid.name')
    item('pyrofluid_bucket','bucket_blazing_fluid','item.pyrofluidBucket.name')
    item('concentrated_taint','concentratedTaint','item.concentratedTaint.name')
    for part in range(3):
        write(f'assets/{NAMESPACE}/models/block/crying_obelisk_{part}.json',{'parent':'minecraft:block/cube_column','textures':{'end':texture('blocks/cryingObelisk/'+('cryingObsidian' if part==0 else 'cryingObelisk_TB')),'side':texture('blocks/cryingObelisk/'+(['cryingObsidian','cryingObelisk_sideB','cryingObelisk_sideT'][part]))}})
    basic('crying_obelisk',{f'part={part}':{'model':f'{NAMESPACE}:block/crying_obelisk_{part}'} for part in range(3)},'tile.cryingObsidian.name','crying_obelisk_0')
    for name in ['relocator','attracting_relocator']:
        element=box([0,0,0],[16,16,16]);element['faces']['up']['texture']='#top';element['faces']['down']['texture']='#bottom'
        model(name,[element],{'texture':texture('blocks/relocator/rotatedside'),'particle':'thaumcraft:block/lifterside','top':'thaumcraft:block/liftertop','bottom':'thaumcraft:block/arcaneearbottom'})
        basic(name,{f'facing={face}':{'model':f'{NAMESPACE}:block/{name}','x':x,'y':y} for face,x,y in [('up',0,0),('down',180,0),('north',90,0),('south',90,180),('west',90,270),('east',90,90)]},'tile.thaumicRelocator.name')
    for state in range(3):
        elements=[faces_box([1,0,1],[15,1,15],'gravel'),faces_box([4,1,4],[12,2,12],'coal')]
        if state>0:
            for layer in range(2):
                y=layer*4
                elements.extend([faces_box([3,y,2],[5,y+2,14],'log'),faces_box([11,y,2],[13,y+2,14],'log'),faces_box([2,y+2,3],[14,y+4,5],'log'),faces_box([2,y+2,11],[14,y+4,13],'log')])
        model('campfire_'+str(state),elements,{'particle':'minecraft:block/gravel','gravel':'minecraft:block/gravel','coal':'minecraft:block/coal_block','log':'thaumcraft:block/greatwoodside'})
    basic('campfire',{f'state={state}':{'model':f'{NAMESPACE}:block/campfire_{state}'} for state in range(3)},'tile.tb.campfire.name','campfire_1')
    shapes=[([1,0,1],[15,2,15]),([2,2,2],[14,4,14]),([4,4,4],[12,12,12]),([2,12,2],[14,13,14]),([2,13,13],[14,16,14]),([2,13,2],[14,16,3]),([2,13,3],[3,16,13]),([13,13,3],[14,16,13])]
    model('brazier',[faces_box(start,end,'stone') for start,end in shapes]+[faces_box([3,13,3],[13,14,13],'coal')],{'stone':'thaumcraft:block/pedestal_top','coal':'minecraft:block/coal_block','particle':'thaumcraft:block/pedestal_top'})
    basic('brazier',{f'state={state}':{'model':f'{NAMESPACE}:block/brazier'} for state in range(3)},'tile.tb.brazier.name')
    model('entity_deconstructor',[box([0,0,0],[16,1,16]),box([0,1,0],[1,2,16]),box([15,1,0],[16,2,16]),box([1,1,0],[15,2,1]),box([1,1,15],[15,2,16]),faces_box([1,1,1],[15,1.2,15],'glass')],{'texture':texture('blocks/entityDeconstructor/goldbase'),'glass':texture('blocks/entityDeconstructor/glass'),'particle':texture('blocks/entityDeconstructor/block')})
    basic('entity_deconstructor',{'':{'model':f'{NAMESPACE}:block/entity_deconstructor'}},'tile.entityDeconstructor.name')
    variants={}
    for lit in [False,True]:
        for contents in [False,True]:
            suffix=f'{int(lit)}_{int(contents)}';name='advanced_alchemical_furnace_'+suffix
            element=box([0,0,0],[16,16,16]);element['faces']['up']['texture']='#top';element['faces']['down']['texture']='#bottom';element['faces']['north']['texture']='#front'
            model(name,[element],{'texture':texture('blocks/tFurnace/side'),'particle':texture('blocks/tFurnace/side'),'top':texture('blocks/tFurnace/'+('top_filled' if contents else 'top')),'bottom':texture('blocks/tFurnace/top'),'front':texture('blocks/tFurnace/'+('front_on' if lit else 'front_off'))})
            for face,y in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={face},lit={str(lit).lower()},contents={str(contents).lower()}']={'model':f'{NAMESPACE}:block/{name}','y':y}
    basic('advanced_alchemical_furnace',variants,'tile.advAlchFurnace.name','advanced_alchemical_furnace_0_0')

def special_flora_models():
    def cross(name,path):
        write(f'assets/{NAMESPACE}/models/block/{name}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':texture('blocks/'+path)}})
    def block(name,variants,legacy,model=None):
        write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':variants})
        source=json.loads((OUT/f'assets/{NAMESPACE}/models/block/{model or name}.json').read_text('utf8'))
        # Vanilla plant items use a front-lit sprite, not their shaded world cross geometry.
        icon={'parent':'minecraft:item/generated','textures':{'layer0':source['textures']['cross']}} if source.get('parent')=='minecraft:block/cross' else {'parent':f'{NAMESPACE}:block/{model or name}'}
        write(f'assets/{NAMESPACE}/models/item/{name}.json',icon)
        language('block',name,legacy)
    for opened in [False,True]: cross('aurelia_'+str(opened).lower(),'aurelia/'+('open' if opened else 'closed'))
    block('aurelia',{f'open={str(v).lower()}':{'model':f'{NAMESPACE}:block/aurelia_{str(v).lower()}'} for v in [False,True]},'tile.aurelia.name','aurelia_false');drop('aurelia')
    for name,path,old in [('aurelia_petal_block','aurelia/petal','aureliaPetal'),('flaxium','flaxium','flaxium')]:
        cross(name,path);block(name,{'':{'model':f'{NAMESPACE}:block/{name}'}},'tile.'+old+'.name')
    drop('flaxium')
    variants={}
    for half,side in [('lower','bot'),('upper','top')]:
        for age in range(8):
            name=f'briar_{half}_{age}';cross(name,f'briar/stage_{age}_{side}')
            variants[f'half={half},age={age}']={'model':f'{NAMESPACE}:block/{name}'}
    block('briar',variants,'tile.briar.name','briar_lower_7')
    for age in range(8):
        tex=texture('blocks/redlon/redlon_disconnected')
        write(f'assets/{NAMESPACE}/models/block/redlon_{age}.json',{'parent':'minecraft:block/stem_growth'+str(age),'render_type':'minecraft:cutout','textures':{'stem':tex}})
    write(f'assets/{NAMESPACE}/models/block/redlon_attached.json',{'parent':'minecraft:block/stem_fruit','render_type':'minecraft:cutout','textures':{'stem':texture('blocks/redlon/redlon_disconnected'),'upperstem':texture('blocks/redlon/redlon_connected')}})
    variants={}
    for age in range(8):
        for connection in range(5):
            variants[f'age={age},connection={connection}']={'model':f'{NAMESPACE}:block/redlon_attached','y':[0,180,0,270,90][connection]} if age==7 and connection else {'model':f'{NAMESPACE}:block/redlon_{age}'}
    write(f'assets/{NAMESPACE}/blockstates/redlon_stem.json',{'variants':variants})
    language('block','redlon_stem','tile.redlonStem.name');item('redlon_seeds','redlon_seed','item.redlonSeeds.name')
    write(f'assets/{NAMESPACE}/models/block/rainbow_cactus.json',{'parent':'minecraft:block/cactus','textures':{side:texture('blocks/rainbowCacti/'+side) for side in ['side','top','bottom']}})
    block('rainbow_cactus',{'':{'model':f'{NAMESPACE}:block/rainbow_cactus'}},'tile.rainbowCactus.name')

def tree_models():
    kinds=[('golden_oak','goldenOak'),('peaceful','peacefullTree'),('nether','netherTree'),('ender','enderTree')]
    for kind,old in kinds:
        sapling=kind+'_sapling';leaves=kind+'_leaves';log='minecraft:oak_log' if kind=='golden_oak' else NAMESPACE+':'+kind+'_log'
        write(f'assets/{NAMESPACE}/models/block/{sapling}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':texture('blocks/'+old+'/sapling')}})
        write(f'assets/{NAMESPACE}/models/block/{leaves}.json',{'parent':'minecraft:block/cube_all','render_type':'minecraft:cutout_mipped','textures':{'all':texture('blocks/'+old+'/leaves')}})
        for name,suffix in [(sapling,'Sapling'),(leaves,'Leaves')]:
            write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':{'':{'model':f'{NAMESPACE}:block/{name}'}}})
            write(f'assets/{NAMESPACE}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture('blocks/'+old+'/sapling')}} if name==sapling else {'parent':f'{NAMESPACE}:block/{name}'})
            language('block',name,'tile.'+old+suffix+'.name')
        drop(sapling)
        if kind!='golden_oak':
            name=kind+'_log'
            write(f'assets/{NAMESPACE}/models/block/{name}.json',{'parent':'minecraft:block/cube_column','textures':{'side':texture('blocks/'+old+'/log'),'end':texture('blocks/'+old+'/log_top')}})
            write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':{f'axis={axis}':{'model':f'{NAMESPACE}:block/{name}',**rotation} for axis,rotation in [('x',{'x':90,'y':90}),('y',{}),('z',{'x':90})]}})
            write(f'assets/{NAMESPACE}/models/item/{name}.json',{'parent':f'{NAMESPACE}:block/{name}'})
            language('block',name,'tile.'+old+'Log.name');drop(name)
            plank=f'{NAMESPACE}:ender_planks' if kind=='ender' else 'minecraft:birch_planks' if kind=='peaceful' else 'minecraft:spruce_planks'
            shapeless(kind+'_planks',[log],plank,4)
            write(f'data/{NAMESPACE}/recipes/{kind}_charcoal.json',{'type':'minecraft:smelting','ingredient':{'item':log},'result':'minecraft:charcoal','experience':0.15,'cookingtime':200})
        def provider(name,properties=None): return {'type':'minecraft:simple_state_provider','state':{'Name':name,**({'Properties':properties} if properties else {})}}
        fancy=kind in ['nether','ender']
        config={'type':'minecraft:tree','config':{
            'trunk_provider':provider(log,{'axis':'y'}),'foliage_provider':provider(f'{NAMESPACE}:{leaves}',{'distance':'7','persistent':'false','waterlogged':'false'}),
            'dirt_provider':provider('minecraft:'+('netherrack' if kind=='nether' else 'end_stone' if kind=='ender' else 'dirt')),
            'trunk_placer':{'type':'minecraft:fancy_trunk_placer' if fancy else 'minecraft:straight_trunk_placer','base_height':5 if kind=='golden_oak' or fancy else 6,'height_rand_a':11 if fancy else 2,'height_rand_b':0},
            'foliage_placer':{'type':'minecraft:fancy_foliage_placer' if fancy else 'minecraft:blob_foliage_placer','radius':2,'offset':4 if fancy else 0,'height':4 if fancy else 3},
            'minimum_size':{'type':'minecraft:two_layers_feature_size','limit':1,'lower_size':0,'upper_size':2},'decorators':[],'ignore_vines':True,'force_dirt':False}}
        write(f'data/{NAMESPACE}/worldgen/configured_feature/{kind}_tree.json',config)
    cube('ender_planks','enderTree/planks','tile.enderPlanks.name')
    logs=[f'{NAMESPACE}:{kind}_log' for kind,_ in kinds if kind!='golden_oak']
    for registry,tag,values in [('blocks','enderman_holdable',logs),('blocks','logs',logs),('items','logs',logs),('blocks','mineable/axe',logs+[f'{NAMESPACE}:ender_planks']),('blocks','leaves',[f'{NAMESPACE}:{kind}_leaves' for kind,_ in kinds]),('items','leaves',[f'{NAMESPACE}:{kind}_leaves' for kind,_ in kinds]),('blocks','saplings',[f'{NAMESPACE}:{kind}_sapling' for kind,_ in kinds]),('items','saplings',[f'{NAMESPACE}:{kind}_sapling' for kind,_ in kinds]),('blocks','planks',[f'{NAMESPACE}:ender_planks']),('items','planks',[f'{NAMESPACE}:ender_planks'])]:
        write(f'data/minecraft/tags/{registry}/{tag}.json',{'replace':False,'values':values})

def overchanter_models():
    element=box([0,0,0],[16,12,16]);element['faces']['up']['texture']='#top';element['faces']['down']['texture']='#bottom'
    # Legacy RenderBlocks cropped the unused top quarter of this 16x16 sprite.
    for side in ['north','south','east','west']: element['faces'][side]['uv']=[0,4,16,16]
    write(f'assets/{NAMESPACE}/models/block/overchanter.json',{'parent':'minecraft:block/block','textures':{'texture':texture('blocks/overchanter/side'),'particle':texture('blocks/overchanter/side'),'top':texture('blocks/overchanter/top'),'bottom':texture('blocks/overchanter/bottom')},'elements':[element]})
    write(f'assets/{NAMESPACE}/blockstates/overchanter.json',{'variants':{'':{'model':f'{NAMESPACE}:block/overchanter'}}})
    write(f'assets/{NAMESPACE}/models/item/overchanter.json',{'parent':f'{NAMESPACE}:block/overchanter'})
    language('block','overchanter','tile.overchanter.name');drop('overchanter')

def anvil_models():
    for name,path,old in [('thaumic_anvil','thaumiumAnvil','thaumicAnvil'),('void_anvil','voidAnvil','voidAnvil')]:
        variants={}
        for damage in range(3):
            write(f'assets/{NAMESPACE}/models/block/{name}_{damage}.json',{'parent':'minecraft:block/template_anvil','textures':{'body':texture('blocks/'+path+'/base'),'top':texture('blocks/'+path+'/top_damaged_'+str(damage))}})
            for direction,y in [('north',0),('east',90),('south',180),('west',270)]: variants[f'facing={direction},damage={damage}']={'model':f'{NAMESPACE}:block/{name}_{damage}','y':y}
        write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':variants})
        write(f'assets/{NAMESPACE}/models/item/{name}.json',{'parent':f'{NAMESPACE}:block/{name}_0','overrides':[{'predicate':{f'{NAMESPACE}:anvil_damage':d},'model':f'{NAMESPACE}:item/{name}_{d}'} for d in [1,2]]})
        for damage,old_damage in [(0,'intact'),(1,'slightlyDamaged'),(2,'veryDamaged')]:
            language('block',name+'.'+str(damage),'tile.'+old+'.'+old_damage+'.name')
            if damage: write(f'assets/{NAMESPACE}/models/item/{name}_{damage}.json',{'parent':f'{NAMESPACE}:block/{name}_{damage}'})
        language('block',name,'tile.'+old+'.intact.name')
        write(f'data/{NAMESPACE}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{NAMESPACE}:{name}','functions':[{'function':'minecraft:copy_state','block':f'{NAMESPACE}:{name}','properties':['damage']}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})

def wand_parts():
    rods=[]
    for name,capacity,cost,old in [('thaumium',80,6,'tbthaumium'),('void',160,16,'tbvoid')]:
        tex=texture('items/wand_rod_'+name+'_uv')
        rods.append({'tag':f'{NAMESPACE}:{old}','capacity':capacity,'craftCost':cost,'texture':tex.replace(':',':textures/')+'.png','item':f'{NAMESPACE}:{name}_wand_core','research':'ROD_'+old,'regenerateAll':True,'regenerationInterval':50})
        LANG[f'item.Wand.{NAMESPACE}:{old}.rod']='item.Wand.'+old+'.rod'
    tex=texture('items/thauminite/wand_cap_thauminite_uv')
    write(f'data/{NAMESPACE}/thaumcraft/wand_rods.json',rods)
    write(f'data/{NAMESPACE}/thaumcraft/wand_caps.json',[{'tag':f'{NAMESPACE}:thauminite','cost':0.85,'craftCost':6,'texture':tex.replace(':',':textures/')+'.png','item':f'{NAMESPACE}:thauminite_wand_cap','research':'CAP_thauminite'}])
    LANG[f'item.Wand.{NAMESPACE}:thauminite.cap']='item.Wand.thauminite.cap'

def focus_definitions():
    for kind in ['activation','drain','experience','flux']:item('focus_'+kind,'foci/'+kind+'/foci','item.'+kind+'Foci.name')
    upgrades=[]
    for key,kind,aspects in [('aquatic','drain',['aqua','ordo']),('netheric','drain',['ignis','perditio']),('decomposing','experience',['perditio','terra']),('vaporizing','experience',['perditio','ordo','aer']),('warping','flux',['perditio','ignis','aer']),('crystalization','flux',['ordo','terra','aqua']),('calming','flux',['aqua','ordo','terra'])]:
        tex=texture('items/foci/'+kind+'/'+key)
        # Rank/mutual-exclusion choices are supplied by Foci.upgradesForRank, not global unconditional offers.
        upgrades.append({'key':f'{NAMESPACE}:{key}','aspects':aspects,'texture':tex.replace(':',':textures/')+'.png','name':'tb.foci.'+key+'.name','description':'tb.foci.'+key+'.desc'})
    write(f'data/{NAMESPACE}/thaumcraft/focus_upgrades.json',upgrades)
    write(f'data/{NAMESPACE}/tags/entity_types/decomposition_blacklist.json',{'replace':False,'values':[{'id':'thaumcraft:eldritch_warden','required':False},{'id':'thaumcraft:eldritch_golem','required':False}]})

def revolver_definitions():
    texture('gui/revolver')
    sounds={}
    for event,file in [('shot','revolver_shot'),('reload','revolver_reload'),('click','revolver_no_ammo')]:
        path=f'assets/{NAMESPACE}/sounds/{file}.ogg';MAPPING[path]=path
        sounds['revolver.'+event]={'sounds':[f'{NAMESPACE}:{file}']}
    write(f'assets/{NAMESPACE}/sounds.json',sounds)
    language('item','revolver','item.tb.revolver.name')
    model=f'assets/{NAMESPACE}/models/revolver/revolver.obj'
    MAPPING[model]=model
    for path in ['revolverHandleUV','revolverBarrelUV','revolverDarkMetal','revolverGunUV','revolverPressUV','revolverDarkMetalEldritch','revolverGunPrimalUV','revolverGunTaintedUV','revolverHandleVoidUV']:
        texture('items/revolver/'+path)
    write(f'assets/{NAMESPACE}/models/item/revolver.json',{'parent':'minecraft:builtin/entity','gui_light':'side','display':{'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'firstperson_righthand':{'rotation':[0,0,0],'translation':[1,0,0],'scale':[1,1,1]},'firstperson_lefthand':{'rotation':[0,0,0],'translation':[-1,0,0],'scale':[1,1,1]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[.7,.7,.7]},'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[.7,.7,.7]},'ground':{'scale':[.6,.6,.6]},'fixed':{'rotation':[0,90,0],'scale':[.8,.8,.8]}}})
    write(f'data/{NAMESPACE}/tags/entity_types/silver_vulnerable.json',{'replace':False,'values':[]})
    write(f'data/{NAMESPACE}/damage_type/revolver.json',{'message_id':'thaumicbases.revolver','exhaustion':0.1,'scaling':'never'})
    names=[('power','POWER',5,2),('accuracy','ACCURACY',3,1),('silver','SILVER',5,3),('poisoned','POISONED',3,1),('bane_of_arthropods','BANE_OF_ATROPODS',5,3),('bane_of_eldritch','BANE_OF_ELDRITCH',5,3),('piercing','PIERCING',1,12),('dueling','DUELING',5,3),('tainted','TAINTED',3,5),('primal','PRIMAL',1,12),('speed','SPEED',5,2),('efficiency','EFFICIENCY',5,4),('eldritch','ELDRITCH',2,7),('void','VOID',3,4),('knowledge','KNOWLEDGE',5,1),('heavy','HEAVY',5,1)]
    groups=[{'power','silver','bane_of_arthropods','bane_of_eldritch','dueling','eldritch'},{'accuracy','speed','efficiency'},{'poisoned','primal','tainted','knowledge'}]
    values=[]
    for name,old,maximum,instability in names:
        conflicts=sorted({other for group in groups if name in group for other in group if other!=name})
        values.append({'key':f'{NAMESPACE}:{name}','name':'revolverUpgrade.'+old+'.name','maximum':maximum,'instability':instability,'conflicts':[f'{NAMESPACE}:{c}' for c in conflicts]})
    write('tbmodern/revolver_upgrades.json',values)
    from revolver_recipes import generate as revolver_recipes
    revolver_recipes(write,values)

def generate():
    from item_aspects import generate as item_aspects
    item_aspects(write)
    for name in ['elder_knowledge','eldritch_bane','magic_touched','tainted','vaporising']:
        texture('enchantments/'+name)
    from alchemy_recipes import generate as alchemy_recipes
    alchemy_recipes(write)
    from equipment_recipes import generate as equipment_recipes
    equipment_recipes(write)
    from machine_recipes import generate as machine_recipes
    machine_recipes(write)
    revolver_definitions()
    focus_definitions()
    wand_parts()
    anvil_models()
    overchanter_models()
    tree_models()
    special_flora_models()
    node_models()
    device_models()
    for kind in ['iron','thaumic','void']:
        name='spike_'+kind
        for bloody in [False,True]:
            suffix='_bloody' if bloody else ''
            write(f'assets/{NAMESPACE}/models/block/{name}{suffix}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':texture('blocks/spike/'+kind+suffix)}})
        write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':{f'bloody={str(bloody).lower()}':{'model':f'{NAMESPACE}:block/{name}'+('_bloody' if bloody else '')} for bloody in [False,True]}})
        write(f'assets/{NAMESPACE}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture('blocks/spike/'+kind)}})
        language('block',name,'tile.spike'+kind+'.name');drop(name)
    for name,path,old in [('chestplate','bloodychest','bloodyChest'),('leggings','bloodylegs','bloodyLegs'),('boots','bloodyboots','bloodyBoots')]:
        item('bloody_'+name,'bloodyRobes/'+path,'item.'+old+'.name')
    for layer in [1,2]: MAPPING[f'assets/thaumicbases/textures/armor/bloody_{layer}.png']=f'assets/thaumicbases/textures/items/armor/bloody/bloody_{layer}.png'
    for modern,legacy in [('elder_knowledge','elderKnowledge'),('eldritch_bane','eldritchBane'),('magic_touch','magicTouch'),('tainted','tainted'),('vaporising','vaporising')]:
        LANG['enchantment.thaumicbases.'+modern]='enchantment.'+legacy
    for kind in ['pile','eldritch','fighting','hunger','knowledge','mining','sanity','tainted','wispy']:
        item('tobacco_'+kind,'tobacco_'+kind,'item.tobacco.tobacco_'+kind+'.name')
    item('greatwood_pipe','smokingPipe','item.greatwoodPipe.name',True)
    item('silverwood_pipe','smokingPipe_silverwood','item.silverwoodPipe.name',True)
    item('mortar','mortar','item.mortar.name')
    item('rosehip_syrup','rosehip_syrup','item.rosehipSyrup.name')
    write('data/forge/tags/items/tools/mortar.json',{'replace':False,'values':['thaumicbases:mortar']})
    resources = [('thauminite_nugget','nuggetthauminite'),('thauminite_ingot','thauminite/thauminite_ingot'),('thauminite_wand_cap','thauminite/wand_cap_thauminite'),('thaumium_wand_core','thaumium_wand_core'),('void_wand_core','void_wand_core'),('aurelia_petal','aurelia_petal'),('briar_seedbag','briar_seedbag'),('tobacco_leaves','tobacco_leaves'),('bloody_cloth','bloodycloth')]
    for name,path in resources: item(name,path,'item.resource'+path.replace('/','.')+'.name')
    for tool,sprite,old in [('axe','axe','Axe'),('pickaxe','pick','Pickaxe'),('shovel','shovel','Shovel'),('hoe','hoe','Hoe'),('sword','sword','Sword'),('shears','shears','Shears'),('helmet','helm','Helmet'),('chestplate','chest','Chest'),('leggings','legs','Legs'),('boots','boots','Boots')]:
        item('thauminite_'+tool,'thauminite/thauminite'+sprite,'item.thauminite'+old+'.name',tool in ['axe','pickaxe','shovel','hoe','sword','shears'])
    item('void_shears','shears','item.tb.voidShears.name',True)
    item('void_flint_and_steel','flint_and_steel','item.tb.voidFAS.name',True)
    for layer in [1,2]: MAPPING[f'assets/thaumicbases/textures/armor/thauminite_{layer}.png'] = f'assets/thaumicbases/textures/items/armor/thauminite/thauminite_{layer}.png'
    cubes = [('quicksilver_block','quicksilverBlock','quicksilverBlock'),('quicksilver_brick','quicksilverBrick','quicksilverBrick'),('salis_mundus_block','dust_block','salisMundusBlock'),('thauminite_block','thauminiteblock','thauminiteBlock'),('eldritch_ark','eldritchArk','eldritchArk'),('old_brick','brick','TBoldBricks'),('old_cobble','cobblestone','TBoldCobblestone'),('old_mossy_cobble','cobblestone_mossy','TBoldCobblestoneMossy'),('old_lapis','lapis_block','TBoldLapis'),('old_iron','iron_block','TBoldIron'),('old_gold','gold_block','TBoldGold'),('old_diamond','diamond_block','TBoldDiamond'),('old_gravel','gravel','TBoldGravel'),('iron_greatwood','ironGreatwood','ironGreatwood'),('void_block','voidblock','tb.voidblock')]
    for name,path,old in cubes: cube(name,path,'tile.'+old+'.name',name in ['old_iron','old_gold','old_diamond'],name.startswith('old_') and name!='old_gravel' or name=='eldritch_ark')
    for kind,old in [('air','air'),('fire','fire'),('water','aqua'),('earth','terra'),('order','order'),('entropy','entropy'),('mixed','mixed'),('tainted','tainted')]:
        cube('crystal_'+kind,'crystal/'+kind,'tile.crystalBlock'+kind+'.name',slab=True)
        item('knose_fragment_'+kind,'knose/'+old+'Fragment','item.knoseFragment.'+old+'.name')
    crops=[('plax',8,'plax','plax_seeds','plax_seeds','plaxSeeds'),('metalleat',8,'metalleat','metalleat_seeds','metalleat_seeds','metalleatSeeds'),('lucrite',8,'lucrite','lucrite_seeds','lucrite_seeds','lucriteSeeds'),('knose',4,'knoze','knose_seed','knose_seed','knoseSeed'),('sweed',4,'sweed','sweed_seeds','seeds_sweed','sweedSeeds'),('lazullia',8,'lazullia','lazullia_seeds','lazullia_seeds','lazulliaSeeds'),('glieonia',4,'glieonia','glieonia_seed','glieonia_seed','glieoniaSeed'),('tobacco',8,'tobacco','tobacco_seeds','tobacco_seeds','tobaccoSeeds'),('void_plant',4,'voidPlant','void_seed','void_seed','voidSeed'),('ashroom',4,'aspectshroom','ashroom',None,None)]
    for name,stages,path,seed,seedtex,old in crops:
        farmland = name in ['plax','metalleat','lucrite','lazullia','tobacco','void_plant']
        for age in range(stages):
            write(f'assets/{NAMESPACE}/models/block/{name}_{age}.json',{'parent':'minecraft:block/crop' if farmland else 'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'crop' if farmland else 'cross':texture(f'blocks/{path}/stage_{age}')}})
        write(f'assets/{NAMESPACE}/blockstates/{name}.json',{'variants':{f'age={age}':{'model':f'{NAMESPACE}:block/{name}_{min(age,stages-1)}'} for age in range(9)}})
        language('block',name,'tile.'+('voidPlant' if name=='void_plant' else name)+'.name')
        if seedtex: item(seed,seedtex,'item.'+old+'.name')
        else:
            write(f'assets/{NAMESPACE}/models/item/{seed}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture(f'blocks/{path}/stage_0')}})
            language('item',seed,'tile.'+name+'.name')
    for low,high in [('thaumicbases:thauminite_nugget','thaumicbases:thauminite_ingot'),('thaumicbases:thauminite_ingot','thaumicbases:thauminite_block'),('thaumcraft:quicksilver','thaumicbases:quicksilver_block')]:
        shapeless(high.split(':')[1], [low]*9, high)
        shapeless(low.split(':')[1]+'_from_'+high.split(':')[1], [high], low, 9)
    shapeless('quicksilver_brick',['thaumicbases:quicksilver_block']*4,'thaumicbases:quicksilver_brick',4)
    shapeless('salis_mundus_from_block',['thaumicbases:salis_mundus_block'],'thaumcraft:salis_mundus',9)
    for tag,name in [('ingots/thauminite','thauminite_ingot'),('nuggets/thauminite','thauminite_nugget'),('storage_blocks/thauminite','thauminite_block'),('storage_blocks/quicksilver','quicksilver_block'),('storage_blocks/void_metal','void_block')]:
        write(f'data/forge/tags/items/{tag}.json',{'replace':False,'values':[f'{NAMESPACE}:{name}']})
    stabilized=['thaumic_anvil','void_anvil','ender_planks']+[n for n,_,_ in cubes if n!='old_gravel']+['crystal_'+k for k in ['air','fire','water','earth','order','entropy','mixed','tainted']]
    stabilized += [n+'_slab' for n in stabilized if (OUT/f'assets/{NAMESPACE}/blockstates/{n}_slab.json').exists()]
    write('data/thaumcraft/tags/blocks/infusion_stabilizers.json',{'replace':False,'values':[f'{NAMESPACE}:{name}' for name in stabilized]})
    write('data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':[f'{NAMESPACE}:{name}' for name in stabilized+['overchanter'] if name not in ['iron_greatwood','ender_planks']]})
    write('data/minecraft/tags/blocks/needs_diamond_tool.json',{'replace':False,'values':['thaumicbases:void_block','thaumicbases:void_anvil','thaumicbases:overchanter']})
    for kind in 'iron gold greatwood thaumium silverwood reed bone obsidian blaze ice quartz void primal'.split():
        write(f'assets/thaumicbases/models/item/bracelet_{kind}.json', {'parent':'builtin/entity'})
        language('item','bracelet_'+kind,'item.tb.bracelet.'+kind)
    for path in ['sugarcaneblock','boneblock','blazeblock','voidblock']:texture('blocks/'+path)
    MAPPING['assets/thaumicbases/models/bracelet/bracelet.obj']='assets/thaumicbases/models/bracelet/bracelet.obj'
    from research_catalog import generate as research_catalog
    research_catalog(write,OUT,ROOT,texture)
    # Placed addon blocks otherwise fall through to the base's empty modern-block scan.
    picked = {name: seed for name, _, _, seed, _, _ in crops}
    picked['redlon_stem'] = 'redlon_seeds'
    scan_rules = []
    for path in sorted((OUT / 'assets/thaumicbases/blockstates').glob('*.json')):
        name = path.stem
        if name == 'pyrofluid': continue  # No inventory item: handled by the public scan callback.
        scan_rules.append({'block': 'thaumicbases:' + name, 'properties': {}, 'item': 'thaumicbases:' + picked.get(name, name)})
    write('data/thaumicbases/block_scan_targets/modern.json', {'format': 1, 'rules': scan_rules})
    configured_paths=['research_catalog/modern.json','research_page_recipes/modern.json','recipes/equipment/brightfocirec.json']
    configured_paths += ['recipes/alchemy/shards_'+kind+'.json' for kind in ['air','fire','water','earth','order','entropy']]
    configured={path:json.loads((OUT/'data/thaumicbases'/path).read_text(encoding='utf-8')) for path in configured_paths}
    write('tbmodern/configured-data.json',configured)
    # These generated definitions are supplied by the configurable built-in pack, not duplicated in mod resources.
    for relative in configured_paths:
        target=(OUT/'data/thaumicbases'/relative).resolve()
        if not target.is_relative_to(OUT.resolve()): raise ValueError('Generated path escapes addon resources')
        target.unlink()
    # This manifest is private to the addon: the base loader requires metadata absent from the original TB JAR.
    write('tbmodern/resource-map.json',{'sha256':hashlib.sha256((ROOT/'references/ThaumicBases-1.3.1710.4.jar').read_bytes()).hexdigest(),'mapping':MAPPING,'language_keys':LANG})
    from original_union import apply
    apply(OUT)

if __name__ == '__main__': generate()
