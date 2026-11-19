"""Implemented passive body infusions. Active powers and soul transfer are separate work."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def item(key):return {'item':key}
def stack(key,nbt=None):
    value={'item':key,'count':1,'metadata':0,'damageable':False}
    if nbt:value['nbt']=json.dumps(nbt)
    return {'kind':'stack','stack':value}
def potion(name):return {'type':'forge:nbt','item':'minecraft:potion','nbt':{'Potion':f'minecraft:{name}'}}
def display(ingredient):
    if 'tag' in ingredient:
        options={'minecraft:leaves':['oak_leaves','spruce_leaves','birch_leaves','jungle_leaves','acacia_leaves','dark_oak_leaves'],'minecraft:saplings':['oak_sapling','spruce_sapling','birch_sapling','jungle_sapling','acacia_sapling','dark_oak_sapling'],'minecraft:small_flowers':['poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy'],'forge:raw_fishes':['cod','salmon','tropical_fish','pufferfish']}
        return {'kind':'sequence','elements':[stack('minecraft:'+i) for i in options[ingredient['tag']]]}
    return stack(ingredient['item'],ingredient.get('nbt'))
recipes=[
 ('quicksilver_limbs','selfInfusion',4,{'motus':64,'machina':32,'volatus':32},[potion('strong_swiftness'),item('thaumcraft:quicksilver'),item('thaumcraft:void_ingot')]*2,'quicksilverlimbs','quicksilver'),
 ('awake_blood','selfInfusion',5,{'sano':32,'victus':64,'cognitio':32},[item('minecraft:golden_apple'),item('minecraft:glistering_melon_slice'),item('minecraft:glistering_melon_slice'),item('thaumcraft:zombie_brain'),item('minecraft:glistering_melon_slice'),item('minecraft:glistering_melon_slice')],'awakenedblood','awakeBlood'),
 ('diamond_skin','selfInfusion',6,{'tutamen':32,'vitreus':32,'humanus':16},[item('minecraft:'+i) for i in ['leather_helmet','diamond','leather_chestplate','diamond_block','leather_leggings','diamond','leather_boots','diamond_block']],'diamondskin','diamondSkin'),
 ('silverwood_heart','silverHeart',7,{'ordo':32,'sano':32,'permutatio':16},[item('thaumcraft:'+i) for i in ['shimmerleaf','silverwood_log','cinderpearl','silverwood_log']],'silverwoodheart','silverHeart'),
 ('photosynthetic_skin','synthSkin',6,{'herba':32,'fames':32,'lux':32,'humanus':16},[{'tag':'minecraft:leaves'},{'tag':'minecraft:saplings'},{'tag':'minecraft:small_flowers'},item('minecraft:vine')],'synthskin','synthSkin'),
 ('amphibious','amphibious',7,{'aqua':64,'aer':64,'victus':32,'permutatio':16},[{'tag':'forge:raw_fishes'},potion('long_water_breathing')]*2,'humaninfusion','amphibious'),
 ('spider_climb','spiderClimb',8,{'bestia':32,'motus':48,'limus':32},[item('minecraft:cobweb'),item('thaumcraft:enchanted_fabric'),item('minecraft:cobweb'),item('minecraft:ladder')]*2,'spiderclimb','spiderClimb'),
 ('chameleon_skin','chameleonSkin',7,{'sensus':48,'permutatio':32,'vacuos':32},[item('minecraft:'+i) for i in ['spider_eye','red_dye','blue_dye','yellow_dye','black_dye','white_dye']],'chameleonskin','chameleonSkin'),
 ('warped_tumor','warpedTumor',12,{'vitium':64,'alienis':48,'vinculum':32,'corpus':24,'permutatio':16},[item('thaumcraft:flesh_block'),item('thaumcraft:bath_salts'),item('minecraft:nether_star'),item('thaumcraft:arcane_stone'),item('minecraft:nether_star'),item('thaumcraft:sanity_soap')],'warpedtumor','warpedTumor'),
 ('morphic_fingers','morphicFingers',6,{'instrumentum':32,'fabrico':32,'humanus':32,'permutatio':16},[item('thaumcraft:arcane_workbench'),item('thaumcraft:void_ingot'),item('thaumcraft:balanced_shard'),item('thaumcraft:void_ingot')],'morphicfingers','morphic')]
displays=[]
manifest=read(f'data/{MOD}/thaumcraft/original_resources.json')
for key,research,danger,aspects,ingredients,texture,translation in recipes:
    write(f'data/{MOD}/vat_infusions/self_{key}.json',{'research':research,'instability':danger,'target':{'kind':'self'},'ingredients':ingredients,'aspects':aspects,'output':{'kind':'self','id':key}})
    icon=f'{MOD}:self_infusion_{key}'
    displays.append({'key':f'{MOD}:self_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':research,'input':stack(f'{MOD}:corpse_effigy'),'output':stack(icon),'ingredients':{'kind':'sequence','elements':[display(i) for i in ingredients]},'aspects':aspects,'instability':danger}})
    write(f'assets/{MOD}/models/item/self_infusion_{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
    manifest['mapping'][f'assets/{MOD}/textures/items/{texture}.png']=f'assets/thaumichorizons/textures/items/{texture}.png'
    for language in manifest['languages'].values():language[f'item.{MOD}.self_infusion_{key}']=f'selfInfusions.{translation}'
def text(key):return {'type':'TEXT','text':key}
def recipe(key):return {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:self_{key}']}}
definitions=[
 ('selfInfusion',14,1,3,{'humanus':6,'praecantatio':6,'fabrico':6},['generalInfusion','cloneSelf'],[text('selfInfusion1'),text('selfInfusion2'),text('selfInfusion3')],'humaninfusion'),
 ('humanInfusion',16,1,1,{'humanus':1},['selfInfusion'],[text('humanInfusion1'),recipe('quicksilver_limbs'),text('humanInfusion3'),recipe('awake_blood'),text('humanInfusion4'),recipe('diamond_skin')],'quicksilverlimbs'),
 ('silverHeart',19,-1,2,{'humanus':3,'ordo':3,'sano':3},['humanInfusion'],[text('silverHeart1'),recipe('silverwood_heart'),text('silverHeart2')],'silverwoodheart'),
 ('synthSkin',19,0,2,{'humanus':3,'herba':3,'fames':3},['humanInfusion'],[text('synthSkin1'),recipe('photosynthetic_skin')],'synthskin'),
 ('amphibious',19,1,2,{'humanus':3,'aer':3,'aqua':3},['humanInfusion'],[text('amphibious1'),recipe('amphibious')],'humaninfusion'),
 ('spiderClimb',19,3,2,{'humanus':3,'bestia':3,'motus':3,'limus':3},['humanInfusion'],[text('spiderClimb1'),recipe('spider_climb')],'spiderclimb'),
 ('chameleonSkin',19,-2,2,{'sensus':4,'permutatio':4,'vacuos':4},['humanInfusion'],[text('chameleonSkin1'),recipe('chameleon_skin')],'chameleonskin'),
 ('warpedTumor',19,4,3,{'humanus':15,'vitium':12,'alienis':9,'corpus':6,'permutatio':3},['humanInfusion'],[text('thaumichorizons.tumor.description'),recipe('warped_tumor')],'warpedtumor'),
 ('morphicFingers',19,2,2,{'humanus':3,'fabrico':3,'instrumentum':3,'permutatio':2},['humanInfusion'],[text('morphicFingers1'),recipe('morphic_fingers')],'morphicfingers')]
catalog=[];pages=[]
for key,x,y,complexity,aspects,parents,content,texture in definitions:
    entry={'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()],'parents':parents,'isConcealed':True,'pageCount':len(content)}
    if key=='amphibious':entry['icon_item']=item('minecraft:cod')
    else:entry['icon_resource']=f'{MOD}:textures/items/{texture}.png'
    if key=='selfInfusion':entry['siblings']=['humanInfusion']
    catalog.append(entry);pages.append({'key':key,'pages':content})
    for language in manifest['languages'].values():
        for prefix in ['tc.research_name.','tc.research_text.']:language[prefix+key]=prefix+key
        for page in content:
            if page['type']=='TEXT' and page['text']!=f'{MOD}.tumor.description':language[page['text']]=page['text']
        language.pop(f'{MOD}.tumor.description',None)
write(f'data/{MOD}/research_catalog/self_infusions.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':catalog}]})
write(f'data/{MOD}/research_pages/self_infusions.json',{'format':1,'entries':pages})
write(f'data/{MOD}/research_page_recipes/self_infusions.json',{'format':1,'entries':displays})
manifest['mapping'][f'assets/{MOD}/textures/gui/guifingers.png']=f'assets/thaumichorizons/textures/gui/guifingers.png'
write(f'data/{MOD}/thaumcraft/original_resources.json',manifest)
for language,category,climb,invisible in [('zh_cn','神秘视界','切换蜘蛛攀爬','切换变色龙皮肤'),('en_us','Thaumic Horizons','Toggle Spider Climb','Toggle Chameleon Skin')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data.update({f'key.categories.{MOD}':category,f'key.{MOD}.climb':climb,f'key.{MOD}.invisible':invisible});write(path,data)
for language,description in [('zh_cn','扭曲肿瘤将扭曲暂时隔离在当前身体内，依次吸收永久、黏性和临时扭曲。容量由服务端 warpedTumorValue 配置决定，默认 50。身体死亡或被替换时，其中的扭曲会按原类型全部返还；这不是永久净化。'),('en_us','A warped tumor isolates warp inside your current body, absorbing permanent warp first, then sticky and temporary warp. The server setting warpedTumorValue controls its capacity, defaulting to 50. Death or replacement of the body returns the stored warp in its original categories. This is not permanent purification.')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data[f'{MOD}.tumor.description']=description;write(path,data)
for language,title,key in [('zh_cn','变形手指','使用变形手指'),('en_us','Morphic Fingers','Activate Morphic Fingers')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data.update({f'container.{MOD}.fingers':title,f'key.{MOD}.fingers':key});write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
