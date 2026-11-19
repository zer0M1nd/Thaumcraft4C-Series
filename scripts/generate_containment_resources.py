"""Containment, stored creatures and reachable vat research. Source artwork stays in the player's legacy jar."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
def ref(key):return {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:{key}']}}
def text(key):return {'type':'TEXT','text':key}
write(f'assets/{MOD}/models/item/focus_containment.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/focus_containment'}})
write('assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:single','resource':f'{MOD}:items/focus_containment_depth','sprite':f'{MOD}:focus_containment_depth'}]})
upgrades=read(f'data/{MOD}/thaumcraft/focus_upgrades.json');upgrades.append({'key':f'{MOD}:slow','aspects':['vinculum'],'texture':f'{MOD}:textures/foci/slow.png','name':'focus.upgrade.slow.name','description':'focus.upgrade.slow.text','foci':[f'{MOD}:focus_containment'],'ranks':[2,4]});write(f'data/{MOD}/thaumcraft/focus_upgrades.json',upgrades)
components=['thaumcraft:amber','minecraft:cobweb','thaumcraft:amber','minecraft:iron_bars','thaumcraft:amber','minecraft:cobweb','thaumcraft:amber','minecraft:iron_bars']
write(f'data/{MOD}/recipes/infusion/focus_containment.json',{'type':'thaumcraft:infusion','research':'focusContainment','instability':6,'aspects':{'vacuos':32,'vinculum':32,'iter':8},'center':{'ingredient':{'item':'thaumcraft:focus_portable_hole'}},'components':[{'ingredient':{'item':key}} for key in components],'result':{'item':f'{MOD}:focus_containment'}})
# Transparent jar shell; the enclosed creature is rendered separately in-world and as an item.
faces={side:{'texture':'#'+('top' if side=='up' else 'bottom' if side=='down' else 'side')} for side in ['up','down','north','south','east','west']}
write(f'assets/{MOD}/models/block/creature_jar.json',{'render_type':'minecraft:translucent','textures':{'side':'thaumcraft:block/jar_side','top':'thaumcraft:block/jar_top','bottom':'thaumcraft:block/jar_bottom','particle':'thaumcraft:block/jar_side'},'elements':[{'from':[3,0,3],'to':[13,12,13],'faces':faces}]})
write(f'assets/{MOD}/models/item/creature_jar.json',{'parent':'builtin/entity','textures':{'particle':'thaumcraft:block/jar_side'},'display':{'gui':{'rotation':[30,225,0],'scale':[.9,.9,.9]},'ground':{'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.375,.375,.375]}}})
write(f'assets/{MOD}/blockstates/creature_jar.json',{'variants':{'':{'model':f'{MOD}:block/creature_jar'}}})
displays=[]
for key in ['focus_containment','modified_matrix']:
    recipe=read(f'data/{MOD}/recipes/infusion/{key}.json')
    displays.append({'key':f'{MOD}:{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':recipe['research'],'input':stack(recipe['center']['ingredient']['item']),'output':stack(recipe['result']['item']),'ingredients':{'kind':'sequence','elements':[stack(value['ingredient']['item']) for value in recipe['components']]},'aspects':recipe['aspects'],'instability':recipe['instability']}})
research=[];pages=[]
def entry(key,item,x,y,complexity,aspects,content,parents=(),hidden=(),siblings=(),round=False):
    research.append({'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()],'parents':list(parents),'parentsHidden':list(hidden),'siblings':list(siblings),'isRound':round,'isConcealed':True,'icon_item':{'item':item},'pageCount':len(content)})
    pages.append({'key':key,'pages':content})
entry('focusContainment',f'{MOD}:focus_containment',8,-3,3,{'vinculum':6,'vacuos':6,'iter':2},[text('focusContainment1'),ref('focus_containment'),text('focusContainment2')],hidden=['DISTILESSENTIA','FOCUSPORTABLEHOLE'])
entry('infusionVat',f'{MOD}:modified_matrix',10,-1,3,{'praecantatio':5,'victus':5,'fabrico':5,'bestia':5},[text('infusionVat1'),ref('modified_matrix'),text('infusionVat2'),text('infusionVat3'),text('infusionVatClone')],['healingVat','INFUSION','focusContainment'],siblings=['generalInfusion'])
icons=[('quick_limbs','quicksilverlimbs','quicksilver'),('thaumic_claws','thaumclaws','thaumClaws'),('awake_blood','awakenedblood','awakeBlood'),('diamond_skin','diamondskin','diamondSkin'),('ender_heart','enderheart','enderHeart'),('shock_skin','shockskin','shockSkin'),('instilled_loyalty','instilledloyalty','instilledLoyalty'),('runic_hide','runichide','runicHide'),('eldritch_fangs','eldritchfangs','eldritchFangs'),('portability','portability','portability')]
content=[]
for index,(key,texture,translation) in enumerate(icons,1):
    write(f'assets/{MOD}/models/item/infusion_{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
    recipe=read(f'data/{MOD}/vat_infusions/{key}.json');ingredients=[]
    for ingredient in recipe['ingredients']:
        value=stack(ingredient['item'])
        if 'nbt' in ingredient:value['stack']['nbt']=json.dumps(ingredient['nbt'])
        ingredients.append(value)
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'generalInfusion','input':stack(f'{MOD}:creature_jar'),'output':stack(f'{MOD}:infusion_{key}'),'ingredients':{'kind':'sequence','elements':ingredients},'aspects':recipe['aspects'],'instability':recipe['instability']}})
    if index<=6:content.extend([text(f'generalInfusion{index}'),ref('creature_'+key)])
entry('generalInfusion',f'{MOD}:infusion_quick_limbs',12,-3,1,{'bestia':1},content,['infusionVat'],round=True)
for key,icon,x,y,aspects,parent,body in [
    ('instilledLoyalty','instilled_loyalty',15,-1,{'cognitio':4,'bestia':3,'permutatio':3},['generalInfusion'],[text('generalInfusion7'),ref('creature_instilled_loyalty'),text('generalInfusion7b')]),
    ('runicCreature','runic_hide',15,-3,{'potentia':4,'tutamen':4,'praecantatio':4},['generalInfusion','RUNICAUGMENTATION'],[text('generalInfusion8'),ref('creature_runic_hide')]),
    ('eldritchFangs','eldritch_fangs',15,-2,{'alienis':4,'telum':4,'venenum':4},['generalInfusion','VOIDMETAL'],[text('generalInfusion9'),ref('creature_eldritch_fangs')]),
    ('portability','portability',15,-4,{'vacuos':4,'vinculum':4},['generalInfusion'],[text('portability1'),ref('creature_portability')])]:
    entry(key,f'{MOD}:infusion_{icon}',x,y,2,aspects,body,parent)
    if key=='runicCreature':research[-1]['isSecondary']=True
category={'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}
write(f'data/{MOD}/research_catalog/containment.json',{'format':1,'categories':[category]});write(f'data/{MOD}/research_pages/containment.json',{'format':1,'entries':pages});write(f'data/{MOD}/research_page_recipes/containment.json',{'format':1,'entries':displays})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for asset in ['textures/items/focus_containment.png','textures/items/focus_containment_depth.png','textures/foci/slow.png']+[f'textures/items/{texture}.png' for _,texture,_ in icons]:manifest['mapping'][f'assets/{MOD}/{asset}']=f'assets/thaumichorizons/{asset}'
for translations in manifest['languages'].values():
    translations[f'item.{MOD}.focus_containment']='item.focusContainment.name';translations[f'block.{MOD}.creature_jar']='item.jarredSoul.0.name'
    for key in ['thaumichorizons.noJar','focus.upgrade.slow.name','focus.upgrade.slow.text']:translations[key]=key
    for key,_,translation in icons:translations[f'item.{MOD}.infusion_{key}']='critterInfusions.'+translation
    for researchKey in ['focusContainment','infusionVat','generalInfusion','instilledLoyalty','runicCreature','eldritchFangs','portability']:
        for prefix in ['tc.research_name.','tc.research_text.']:translations[prefix+researchKey]=prefix+researchKey
    for key in ['focusContainment1','focusContainment2','infusionVat1','infusionVat2','infusionVat3','infusionVatClone','generalInfusion7b','portability1']+[f'generalInfusion{i}' for i in range(1,10)]:translations[key]=key
write(path,manifest)
for language,label in [('en_us','Jarred %s'),('zh_cn','罐装%s')]:
    path=f'assets/{MOD}/lang/{language}.json';values=read(path);values[f'{MOD}.jar.creature']=label;write(path,values)
for language,wait,follow in [('en_us','%s is waiting.','%s will follow you.'),('zh_cn','%s 正在等待。','%s 将跟随你。')]:
    path=f'assets/{MOD}/lang/{language}.json';values=read(path);values.update({f'{MOD}.loyal.wait':wait,f'{MOD}.loyal.follow':follow});write(path,values)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
