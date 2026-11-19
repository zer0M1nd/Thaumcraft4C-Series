"""Idempotent original-author additions to the GTNH-derived data (1.3.1710.4).

Source: TBThaumonomicon lines 225, 318, 343, 379, 429 and 437 in the preserved
author JAR decompilation. Keep these additions when regenerating the GTNH base.
"""
import json
from pathlib import Path
from book_recipes import definition, CLASS


def apply(out):
    def read(path):
        return json.loads((out/path).read_text(encoding='utf-8'))
    def write(path, data):
        target=out/path
        target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    def replace_items(value):
        if isinstance(value,dict):
            return {key:('minecraft:crying_obsidian' if key in ('item','name') and v=='thaumicbases:crying_obelisk' else replace_items(v)) for key,v in value.items()}
        if isinstance(value,list):return [replace_items(v) for v in value]
        return value
    data=replace_items(read('tbmodern/configured-data.json'))
    entries=data['research_catalog/modern.json']['categories'][0]['research']
    page_data=read('data/thaumicbases/research_pages/modern.json')
    pages=page_data['entries']
    research=[
        {'key':'TB.Enchantments','aspects':{},'displayColumn':-3,'displayRow':8,'complexity':1,'warp':0,
         'icon_resource':'thaumicbases:textures/thaumonomicon/enchantments_icon.png','isAutoUnlock':True,'isRound':True,'isStub':True,'pageCount':6},
        {'key':'TB.TaintMinor','aspects':{'vitium':32,'cognitio':16,'alienis':16},'displayColumn':14,'displayRow':3,'complexity':3,'warp':4,
         'icon_resource':'thaumicbases:textures/thaumonomicon/taint_minor.png','isLost':True,'pageCount':1},
        {'key':'TB.HerobrinesScythe','aspects':{'alienis':16,'mortuus':16,'telum':16,'auram':8,'potentia':6},'displayColumn':15,'displayRow':1,'complexity':3,'warp':4,
         'icon_item':{'item':'thaumicbases:herobrines_scythe'},'parents':['VOIDMETAL'],'isConcealed':True,'isSpecial':True,'pageCount':3}
    ]
    new_pages=[
        {'key':'TB.Enchantments','pages':[{'type':'TEXT','text':f'tb.rec.enchantments.page.{i}'} for i in range(6)]},
        {'key':'TB.TaintMinor','pages':[{'type':'TEXT','text':'tb.rec.minorTaint.page.0'}]},
        {'key':'TB.HerobrinesScythe','pages':[{'type':'TEXT','text':f'tb.rec.hScythe.page.{i}'} for i in range(2)]+[
            {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':['thaumicbases:equipment/herobrinesscytherec'],'class':CLASS}}]}
    ]
    for row in research:
        row['category']='THAUMICBASES'
        entries[:]=[v for v in entries if v['key']!=row['key']];entries.append(row)
    for row in new_pages:
        pages[:]=[v for v in pages if v['key']!=row['key']];pages.append(row)
    for row in entries:
        if row['key'] in ('TB.ElderKnowledge','TB.EldritchBane','TB.MagicTouch','TB.Vaporising','TB.Tainted'):
            if 'TB.Enchantments' not in row.setdefault('parents',[]):row['parents'].append('TB.Enchantments')
    pedestal=lambda item:{'ingredient':{'item':item},'damage':-1}
    recipe={'type':'thaumcraft:infusion','research':'TB.HerobrinesScythe','center':pedestal('thaumcraft:void_hoe'),
            'instability':7,'aspects':{'telum':128,'potentia':64,'auram':12,'alienis':32,'mortuus':64},
            'components':[pedestal(i) for i in ('minecraft:nether_star','thaumcraft:liquid_death_bucket','thaumcraft:staff_rod_primal','thaumcraft:liquid_death_bucket')],
            'result':{'item':'thaumicbases:herobrines_scythe'}}
    write('data/thaumicbases/recipes/equipment/herobrinesscytherec.json',recipe)
    displays=data['research_page_recipes/modern.json']['entries'];key='thaumicbases:equipment/herobrinesscytherec'
    displays[:]=[v for v in displays if v['key']!=key]
    displays.append({'key':key,'class':CLASS,'data':definition(recipe)[1]})
    index=read('tbmodern/equipment_recipe_index.json');index['herobrinesScytheRec']=key;write('tbmodern/equipment_recipe_index.json',index)
    nether_path='recipes/equipment/nethersaplingrec.json'
    if (out/'data/thaumicbases'/nether_path).exists():
        data[nether_path]=read('data/thaumicbases/'+nether_path)
        (out/'data/thaumicbases'/nether_path).unlink()
    for element in ['air','fire','water','earth','order','entropy']:
        tag='forge:ores/infused_'+element
        data[f'recipes/alchemy/shards_{element}.json']['catalyst']={'tag':tag}
        write(f'data/forge/tags/items/ores/infused_{element}.json',{'replace':False,'values':['thaumcraft:'+element+'_infused_stone']})
        for row in displays:
            if row['key']==f'thaumicbases:alchemy/shards_{element}':
                row['data']['catalyst']={'kind':'ore','names':[tag],'elements':[{'kind':'stack','stack':{'item':f'thaumcraft:{element}_infused_stone'}}]}
    for row in displays:
        if row['key']=='thaumicbases:alchemy/chiseledbricks':
            row['data']['catalyst']={'kind':'ore','names':['minecraft:stone_bricks'],'elements':[{'kind':'stack','stack':{'item':'minecraft:'+kind}} for kind in ['stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks']]}
    write('tbmodern/configured-data.json',data);write('data/thaumicbases/research_pages/modern.json',page_data)
    # The author's JAR registers TaintMinor but contains no text for it in any language.
    # Supply a factual description of the preserved ritual instead of exposing raw keys.
    for locale,title,subtitle,text in [
        ('en_us','First Steps into Taint','A dangerous experiment',
         'This knowledge allows you to drink Concentrated Taint. Attempt the ritual only at full health: drinking while injured is fatal. Even at full health, it leaves you with half a heart and severe temporary effects. The whispers call you deeper. Drinking at Y 6 or below in the Nether advances the ritual and adds warp.'),
        ('zh_cn','初探污染','危险的实验',
         '这项知识让你能够饮用凝聚腐化。只有生命值全满时才能承受这一仪式；受伤时饮用会致命。即使满血，也会只剩半颗心，并承受一段时间的严重负面效果。低语呼唤你继续深入。在下界 Y 不高于 6 的深处饮用，可以推进仪式，同时增加扭曲。')]:
        path=f'assets/thaumicbases/lang/{locale}.json'
        language=read(path) if (out/path).exists() else {}
        language.update({'tc.research_name.TB.TaintMinor':title,'tc.research_text.TB.TaintMinor':subtitle,'tb.rec.minorTaint.page.0':text})
        write(path,language)
    for name in ['data/thaumicbases/recipes/machines/cryingobsidianrec.json','data/thaumicbases/loot_tables/blocks/crying_obelisk.json','data/thaumicbases/block_scan_targets/modern.json','data/thaumicbases/item_aspects/explicit.json']:
        value=replace_items(read(name))
        if '/block_scan_targets/' in name:
            for row in value['rules']:
                if row['block']=='thaumicbases:crying_obelisk':row['item']='minecraft:crying_obsidian'
        if '/item_aspects/' in name:
            # These three base definitions must have only one owner; merges use maxima.
            value['entries']=[row for row in value['entries'] if not any(i in ('thaumicbases:nether_leaves','thaumicbases:nether_sapling','thaumicbases:nether_log','thaumicbases:crying_obelisk') for i in row.get('items',[]))]
        write(name,value)
    # Original infusion inference: floor(0.75 * ingredient sums) + floor(sqrt(cost)),
    # followed by TC4's six-type weighted cull. Inputs use the port's explicit baseline.
    path='data/thaumicbases/item_aspects/explicit.json';value=read(path)
    value['entries']=[row for row in value['entries'] if 'thaumicbases:herobrines_scythe' not in row.get('items',[])]
    value['entries'].append({'items':['thaumicbases:herobrines_scythe'],'aspects':[{'aspect':a,'amount':n} for a,n in
                            {'aer':21,'metallum':14,'mortuus':15,'ordo':23,'perditio':24,'praecantatio':27}.items()]})
    write(path,value)
    path='data/thaumicbases/recipes/alchemy/chiseledbricks.json';value=read(path);value['catalyst']={'tag':'minecraft:stone_bricks'};write(path,value)
    for path in ['mineable/pickaxe','needs_diamond_tool']:
        name='data/minecraft/tags/blocks/'+path+'.json';value=read(name)
        if 'thaumicbases:crying_obelisk' not in value['values']:value['values'].append('thaumicbases:crying_obelisk')
        write(name,value)
    # No standalone addon item model remains; the structure's block models are retained.
    model=out/'assets/thaumicbases/models/item/crying_obelisk.json'
    if model.exists():model.unlink()
    # Blockbench's checked-in export also survives a clean resource regeneration.
    scythe=json.loads((Path(__file__).resolve().parents[1]/'model-source/herobrines_scythe.json').read_text('utf-8'))
    scythe['textures']['0']='thaumicbases:items/herobrinesscythe'
    scythe['gui_light']='front'
    write('assets/thaumicbases/models/item/herobrines_scythe.json',scythe)
    atlas=read('assets/minecraft/atlases/blocks.json')
    source={'type':'single','resource':'thaumicbases:items/herobrinesscythe'}
    if source not in atlas['sources']:atlas['sources'].append(source)
    write('assets/minecraft/atlases/blocks.json',atlas)
    mapping={'assets/thaumicbases/textures/items/herobrinesscythe.png':'assets/thaumicbases/textures/items/herobrinesScythe.png'}
    for name in ['enchantments_icon','taint_minor']:
        path=f'assets/thaumicbases/textures/thaumonomicon/{name}.png';mapping[path]=path
    path='tbmodern/resource-map.json';value=read(path);value['mapping'].update(mapping);value['language_keys']['item.thaumicbases.herobrines_scythe']='item.herobrinesScythe.name';write(path,value)
    path='data/thaumicbases/thaumcraft/original_resources.json'
    if (out/path).exists():
        value=read(path);value['mapping'].update(mapping)
        for language in value['languages'].values():language['item.thaumicbases.herobrines_scythe']='item.herobrinesScythe.name'
        write(path,value)


if __name__=='__main__':apply(Path(__file__).resolve().parents[1]/'src/main/resources')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
