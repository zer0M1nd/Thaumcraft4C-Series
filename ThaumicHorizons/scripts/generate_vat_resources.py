"""Authored vat block models and initial healing/cloning progression. No legacy images are bundled."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'; MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
# Only exterior glass faces are emitted, with a second inset water surface.
# Coordinates follow VatStructure.offset; old renderer used boss - part coordinates.
def inner_texture(x,z):
    if x==z==1:return 'vatinnercenter'
    if x==1:return 'vatinnernegz' if z==0 else 'vatinnerposz'
    if z==1:return 'vatinnernegx' if x==0 else 'vatinnerposx'
    return 'vatinnercorner'+{(0,0):'b',(0,2):'a',(2,0):'c',(2,2):'d'}[x,z]

def element(faces,lo=None,hi=None):
    return {'from':lo or [0,0,0],'to':hi or [16,16,16],
            'faces':{side:{'texture':'#'+tex,'uv':[0,0,16,16]} for side,tex in faces.items()}}

variants={};textures=set()
for part in range(36):
    x=part%3;z=part//3%3;y=part//9
    variants[f'part={part}']={'model':f'{MOD}:block/vat_{part}'}
    face={};elements=[];extra={}
    if y in (1,2):
        stem='vattop' if y==2 else 'vatbottom'
        # L/R is viewed from the exterior, not world-space X.
        if x==0:face['west']=stem+('left' if z==0 else 'right' if z==2 else '')
        if x==2:face['east']=stem+('right' if z==0 else 'left' if z==2 else '')
        if z==0:face['north']=stem+('right' if x==0 else 'left' if x==2 else '')
        if z==2:face['south']=stem+('left' if x==0 else 'right' if x==2 else '')
        if face:
            elements.append(element(face))
            elements.append(element({side:'water' for side in face},
                                    [4 if x==0 else 0,0,4 if z==0 else 0],
                                    [12 if x==2 else 16,16,12 if z==2 else 16]))
        extra['water']='minecraft:block/water_still'
    else:
        corner=x!=1 and z!=1
        side=('vatlidleftright' if corner else 'vatlidcenter') if y==3 else ('vatbaseleftright' if corner else 'vatbasesidecenter')
        face={s:side for s in ['north','east','south','west']}
        if y==3:
            face.update(up='vatlidtopcenter' if x==z==1 else 'greatwood',down=inner_texture(x,z))
            extra['greatwood']='thaumcraft:block/planks_greatwood'
        else:face.update(up=inner_texture(x,z),down='vatbasesidebottom' if (x==1)!=(z==1) else 'vatbasecenter')
        elements.append(element(face))
    textures.update(t for t in face.values() if t not in extra)
    local={t:f'{MOD}:block/{t}' for t in face.values() if t not in extra}
    local.update(extra);local['particle']=f'{MOD}:block/vattop'
    model={'render_type':'minecraft:translucent' if y in (1,2) else 'minecraft:solid',
           'textures':local,'elements':elements}
    write(f'assets/{MOD}/models/block/vat_{part}.json',model)
write(f'assets/{MOD}/blockstates/vat.json',{'variants':variants})
# Formed parts drop only their original component; the remaining structure restores itself.
entries=[]
for parts,item in [([4,31],'thaumcraft:alchemical_construct'),([i for i in range(36) if (i<9 or i>=27) and i not in (4,31)],'thaumcraft:greatwood_planks')]:
    for part in parts:entries.append({'type':'minecraft:item','name':item,'conditions':[{'condition':'minecraft:block_state_property','block':f'{MOD}:vat','properties':{'part':str(part)}}]})
write(f'data/{MOD}/loot_tables/blocks/vat.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':entries,'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'assets/{MOD}/models/item/nutrients.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/nutrients'}})
write(f'data/{MOD}/recipes/crucible/nutrients.json',{'type':'thaumcraft:crucible','research':'incarnationVat','aspects':{'fames':2},'catalyst':{'item':'minecraft:sugar'},'result':{'item':f'{MOD}:nutrients'}})
research=[];pages=[]
for key,x,y,complexity,aspects,parents,content,icon in [
    ('healingVat',8,0,3,{'victus':2,'sano':4,'machina':2,'aqua':2},['essentiaInjection','fleshLeather'],[{'type':'TEXT','text':'healingVat1'},{'type':'TEXT','text':f'{MOD}.vat.structure'},{'type':f'{MOD}:vat_structure','data':{}},{'type':'TEXT','text':'healingVat2'}],'thaumcraft:alchemical_construct'),
    ('incarnationVat',10,1,2,{'victus':3,'fabrico':3,'bestia':3,'machina':2},['healingVat'],[{'type':'TEXT','text':'incarnationVat1'},{'type':'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.CrucibleRecipe','keys':[f'{MOD}:nutrients']}},{'type':'TEXT','text':'incarnationVat2'}],f'{MOD}:nutrients')]:
    research.append({'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':k,'amount':v} for k,v in aspects.items()],'parents':parents,'isSpecial':key=='healingVat','isSecondary':key=='incarnationVat','isConcealed':key=='incarnationVat','icon_item':{'item':icon},'pageCount':len(content)})
    pages.append({'key':key,'pages':content})
write(f'data/{MOD}/research_catalog/vat.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]})
write(f'data/{MOD}/research_pages/vat.json',{'format':1,'entries':pages})
write(f'data/{MOD}/research_page_recipes/vat.json',{'format':1,'entries':[{'key':f'{MOD}:nutrients','class':'thaumcraft.api.crafting.CrucibleRecipe','data':{'kind':'crucible','research':'incarnationVat','aspects':{'fames':2},'catalyst':stack('minecraft:sugar'),'output':stack(f'{MOD}:nutrients')}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for texture in textures:manifest['mapping'][f'assets/{MOD}/textures/block/{texture}.png']=f'assets/thaumichorizons/textures/blocks/{texture}.png'
for asset in ['textures/items/nutrients.png','textures/gui/guivat.png']:manifest['mapping'][f'assets/{MOD}/{asset}']=f'assets/thaumichorizons/{asset}'
for translations in manifest['languages'].values():
    translations[f'item.{MOD}.nutrients']='item.nutrients.name'
    translations[f'block.{MOD}.vat']='tile.ThaumicHorizons_vat.name'
    for key in ['healingVat','incarnationVat']:
        for prefix in ['tc.research_name.','tc.research_text.']:translations[prefix+key]=prefix+key
        for suffix in ['1','2']:translations[key+suffix]=key+suffix
write(path,manifest)
for lang,name,progress,structure in [
    ('zh_cn','治疗缸','克隆剩余：%s tick','建造 3×4×3 的结构。顶层和底层：中央为炼金构件，周围八块宏伟之木木板。中间两层：中央为水源，周围八块玻璃。持法杖右击，消耗水、地、秩序各 50 vis 组装。底层四边中点接源质管。潜行右击顶盖进入；再次右击离开。'),
    ('en_us','Curative Vat','Cloning: %s ticks','Build a 3×4×3 structure. Top and bottom: an alchemical construct surrounded by eight greatwood planks. Both middle layers: a water source surrounded by eight glass blocks. Right-click with a wand to assemble, spending 50 Aqua, Terra and Ordo vis. Attach essentia pipes to the four bottom edge centres. Sneak-right-click the lid to enter; right-click again to leave.')]:
    path=f'assets/{MOD}/lang/{lang}.json';data=read(path);data.update({f'container.{MOD}.vat':name,f'{MOD}.vat.cloning':progress,f'{MOD}.vat.structure':structure});write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
