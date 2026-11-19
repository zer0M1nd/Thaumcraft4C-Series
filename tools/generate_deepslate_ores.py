"""Generate the deepslate variant data without copying any original image pixels.

Existing providers also generate models, tags, loot and languages. This targeted
entry point avoids regenerating unrelated content during the initial addition.
"""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
GEN=ROOT/'src/generated/resources'
def read(path): return json.loads(path.read_text(encoding='utf-8'))
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def add(path,value):
    data=read(path)
    if value not in data['values']:data['values'].append(value)
    write(path,data)

names=['cinnabar_ore']+[x+'_infused_stone' for x in ['air','fire','water','earth','order','entropy']]+['amber_ore']
tags=['cinnabar']+['infused_'+x for x in ['air','fire','water','earth','order','entropy']]+['amber']
zh=['深层朱砂矿石','深层风之蕴魔石','深层火之蕴魔石','深层水之蕴魔石','深层地之蕴魔石','深层秩序蕴魔石','深层混沌蕴魔石','深层琥珀矿石']
en=['Deepslate Cinnabar Ore']+['Deepslate '+x+' Infused Stone' for x in ['Air','Fire','Water','Earth','Order','Entropy']]+['Deepslate Amber Ore']
legacy=read(GEN/'data/thaumcraft/recipe_aspect_inputs/legacy.json')
original={r['metadata']:r['aspects'] for r in legacy['registrations'] if r['item']=='Thaumcraft:blockCustomOre'}
aspects=[]
for i,name in enumerate(names):
    deep='deepslate_'+name;old_id='thaumcraft:'+name;new_id='thaumcraft:'+deep
    model=read(GEN/'assets/thaumcraft/models/block/air_infused_stone.json')
    model['textures']['base']='minecraft:block/deepslate'
    model['textures']['overlay']='thaumcraft:block/deepslate_'+('cinnabar' if i==0 else 'amber' if i==7 else 'infused')+'_overlay'
    if i in (0,7):
        for face in model['elements'][1]['faces'].values():face.pop('tintindex',None)
    write(GEN/f'assets/thaumcraft/models/block/{deep}.json',model)
    write(GEN/f'assets/thaumcraft/models/item/{deep}.json',{'parent':'thaumcraft:block/'+deep})
    write(GEN/f'assets/thaumcraft/blockstates/{deep}.json',{'variants':{'':{'model':'thaumcraft:block/'+deep}}})
    loot=json.loads(json.dumps(read(GEN/f'data/thaumcraft/loot_tables/blocks/{name}.json')).replace(old_id,new_id).replace('thaumcraft:blocks/'+name,'thaumcraft:blocks/'+deep))
    write(GEN/f'data/thaumcraft/loot_tables/blocks/{deep}.json',loot)
    for kind in ['items','blocks']:
        add(GEN/f'data/forge/tags/{kind}/ores/{tags[i]}.json',new_id)
        add(GEN/f'data/forge/tags/{kind}/ores.json',new_id)
    add(GEN/'data/minecraft/tags/blocks/mineable/pickaxe.json',new_id)
    if i in (0,7):add(GEN/'data/minecraft/tags/blocks/needs_iron_tool.json',new_id)
    aspects.append({'items':[new_id],'aspects':original[i]+[{'aspect':'tenebrae','amount':1}]})
    for locale,labels in [('zh_cn',zh),('en_us',en)]:
        p=GEN/f'assets/thaumcraft/lang/{locale}.json';data=read(p);data['block.thaumcraft.'+deep]=labels[i];write(p,data)
    if i in (0,7):
        p=GEN/f'data/thaumcraft/recipes/{name}_smelting.json';data=read(p);data['ingredient']={'tag':'forge:ores/'+tags[i]};write(p,data)
write(ROOT/'src/main/resources/data/thaumcraft/item_aspects/deepslate_ores.json',{'format':1,'entries':aspects})
print('Generated eight deepslate ores, tags, loot, languages, aspects and tag-based smelting.')
