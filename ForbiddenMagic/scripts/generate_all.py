"""Final generation entry point: core, Botania, Blood Magic, then legacy vanilla additions."""
import json
from pathlib import Path
import port_blood_data as blood
out=blood.out
MOD=out.MOD

def rule(item,values):
    return {'items':['minecraft:'+item], 'aspects':[{'aspect':a,'amount':n} for a,n in values.items()]}

# Equipment rules are last-match, not additive. Preserve the fixed base's exact existing
# bonuses for these five items while adding a new aspect; never replace whole base files.
base=json.loads(Path('references/base-mappings/vanilla_equipment.json').read_text())
equipment={item.removeprefix('minecraft:'):{x['aspect']:x['amount'] for x in row['aspects']} for row in base['entries'] for item in row['items']}
additions={
 'netherrack':{'infernus':1},'nether_star':{'infernus':8,'superbia':8},'nether_wart':{'infernus':1},
 'nether_brick':{'infernus':1},'nether_bricks':{'infernus':2},'ghast_tear':{'ira':4},
 'wither_skeleton_skull':{'infernus':4},'creeper_head':{'ira':2},'tnt':{'ira':2},
 'golden_helmet':{'superbia':2},'golden_chestplate':{'superbia':1},'golden_leggings':{'superbia':1},
 'golden_boots':{'superbia':1},'golden_sword':{'superbia':2},'cake':{'gula':7},'cookie':{'gula':1},'fire_charge':{'ira':1},'saddle':{'luxuria':2}}
rows=[]
for item,extra in additions.items():
    values=equipment.get(item,{}).copy();values.update({MOD+':'+a:n for a,n in extra.items()});rows.append(rule(item,values))
out.write(f'data/{MOD}/item_bonus_aspects/zz_legacy_additions.json',{'format':1,'entries':rows})
values={
 'nether_quartz_ore':{'vitreus':3,MOD+':infernus':2},
 'ender_eye':{'sensus':4,'alienis':4,'praecantatio':3,MOD+':invidia':4},
 'comparator':{'machina':2,'ordo':2,MOD+':invidia':2},
 'lead':{'bestia':2,'pannus':2,'limus':1,MOD+':luxuria':2}}
# The old single bed item became sixteen colors, all sharing the same construction.
for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:
    values[color+'_bed']={'fabrico':3,'pannus':6,MOD+':desidia':4}
out.write(f'data/{MOD}/item_aspects/zz_legacy_vanilla.json',{'format':1,'entries':[rule(item,a) for item,a in values.items()]})
print('Wrote legacy additions for',len(rows),'items and',len(values),'explicit vanilla definitions.')

import port_other_integrations
port_other_integrations.generate(out)

# Keep one stable recipe ID/page reference while selecting the world research mode.
from copy import deepcopy
recipe_path=out.ROOT/f'data/{MOD}/recipes/crystalwell.json'
normal=json.loads(recipe_path.read_text())
easy=deepcopy(normal);easy['ingredients']=[{'item':'thaumcraft:scribing_tools'},{'tag':'forge:dyes/black'},{'item':'thaumcraft:balanced_shard'},{'item':'thaumcraft:balanced_shard'}]
def config(option,expected=True):return {'type':MOD+':config','option':option,'expected':expected}
out.write(f'data/{MOD}/recipes/crystalwell.json',{'type':'forge:conditional','recipes':[{'conditions':[config('easy_research')],'recipe':easy},{'conditions':[config('easy_research',False)],'recipe':normal}]})
for integration in ['botania','bloodmagic']:
    for path in (out.ROOT/f'data/{MOD}/recipes'/integration).glob('*.json'):
        recipe=json.loads(path.read_text())
        recipe.setdefault('conditions',[]).append(config(integration))
        result=recipe.get('result',recipe.get('output',{}))
        item=result.get('item','') if isinstance(result,dict) else ''
        if 'wand_rod_' in item or 'wand_cap_' in item:recipe['conditions'].append(config('conversion_wands'))
        out.write(str(path.relative_to(out.ROOT)),recipe)

# Cosmetic fork configuration uses the external original texture; no duplicated item ID.
texture=out.texture('item','diabolist_spork','items/spork')
out.write(f'assets/{MOD}/models/item/diabolist_spork.json',{'parent':'minecraft:item/handheld','textures':{'layer0':texture}})
path=out.ROOT/f'assets/{MOD}/models/item/diabolist_fork.json'
model=json.loads(path.read_text());model['overrides']=[{'predicate':{MOD+':spork':1},'model':MOD+':item/diabolist_spork'}]
out.write(str(path.relative_to(out.ROOT)),model)
out.translate('item.forbidden_magic.diabolist_spork','item.DiabolistSpork.name')
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)

out.translate('tooltip.creativeonly','tooltip.creativeonly')
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)

# Research tooltips use a separate translation key from the visible title.
for path in (out.ROOT/f'data/{MOD}/research_catalog').glob('*.json'):
    for category in json.loads(path.read_text())['categories']:
        for node in category.get('research',[]):
            old=node['key'].split(':',1)[-1]
            out.translate('tc.research_text.'+node['key'],'forbidden.research_text.'+('ROD_dreamwood_staff' if old=='ROD_dreamwood' else old))
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)


# Preserve normal and costly food recipes under the same runtime IDs.
for name in ['arcane_cake','ring_food']:
    path=out.ROOT/f'data/{MOD}/recipes/{name}.json';normal=json.loads(path.read_text());hard=deepcopy(normal)
    if name=='arcane_cake':
        hard['components'][1]={'ingredient':{'item':MOD+':nether_star_block'}}
        hard['components'][2]={'ingredient':{'item':MOD+':nether_star_block'}}
    else:hard['key']['D']={'item':MOD+':nether_star_block'}
    out.write(str(path.relative_to(out.ROOT)),{'type':'forge:conditional','recipes':[
      {'conditions':[config('food_enabled'),config('food_hard')],'recipe':hard},
      {'conditions':[config('food_enabled'),config('food_hard',False)],'recipe':normal}]})
for language,text in [('en_us','The shown recipe is for normal food settings. With gluttony=2, the Arcane Cake replaces its first egg and milk bucket with two Nether Star Blocks; the Ring of Nutrition replaces its diamond with one Nether Star Block. With gluttony=1, both recipes are disabled.'),('zh_cn','展示配方对应普通暴食设置。gluttony=2 时，奥术蛋糕的第一枚鸡蛋和牛奶桶替换为两块下界之星块，营养指环的钻石替换为一块下界之星块。gluttony=1 时，两种配方均禁用。')]:
    path=out.ROOT/f'assets/{MOD}/lang/{language}.json';data=json.loads(path.read_text(encoding='utf8'));data['forbidden_magic.book.food_modes']=text;out.write(str(path.relative_to(out.ROOT)),data)
path=out.ROOT/f'data/{MOD}/research_pages/core.json';pages=json.loads(path.read_text())
for entry in pages['entries']:
    if entry['key'] in [MOD+':ARCANECAKE',MOD+':RINGFOOD']:entry['pages'].append({'type':'TEXT','text':'forbidden_magic.book.food_modes'})
out.write(str(path.relative_to(out.ROOT)),pages)
path=out.ROOT/f'data/{MOD}/research_catalog/core.json';catalog=json.loads(path.read_text())
counts={entry['key']:len(entry['pages']) for entry in pages['entries']}
for category in catalog['categories']:
    for entry in category['research']:
        if entry['key'] in counts:entry['pageCount']=counts[entry['key']]
out.write(str(path.relative_to(out.ROOT)),catalog)
import generate_book_recipes
generate_book_recipes.generate(out)

# Recipe visibility must match research gates, including already-unlocked worlds.
options={'lust':['riding_crop','sub_collar'], 'cage':['fork','wrath_cage','mob_crystal'],
         'emerald':['trans_emerald'], 'greedy':['greedy'],
         'enchantments':['cluster','wrath','consuming','educational','corrupting','impact','voidtouched']}
for option,names in options.items():
    for name in names:
        path=out.ROOT/f'data/{MOD}/recipes/{name}.json'
        if not path.exists():raise ValueError('Missing gated recipe '+name)
        recipe=json.loads(path.read_text());recipe.setdefault('conditions',[]).append(config(option));out.write(str(path.relative_to(out.ROOT)),recipe)

# Luxuria's registry ID remains stable, but its item contributions follow noLust.
out.write('optional_packs/lust/pack.mcmeta',{'pack':{'pack_format':15,'description':'Forbidden Magic enabled lust content'}})
for folder in ['item_aspects','item_bonus_aspects']:
    extra=[]
    for path in (out.ROOT/f'data/{MOD}/{folder}').glob('*.json'):
        data=json.loads(path.read_text())
        for row in data['entries']:
            if any(a['aspect']==MOD+':luxuria' for a in row['aspects']):
                extra.append(deepcopy(row));row['aspects']=[a for a in row['aspects'] if a['aspect']!=MOD+':luxuria']
        data['entries']=[r for r in data['entries'] if r['aspects']]
        out.write(str(path.relative_to(out.ROOT)),data)
    out.write(f'optional_packs/lust/data/{MOD}/{folder}/zzzz_lust.json',{'format':1,'entries':extra})

# The base converter rejects missing translation aliases. Omit absent localized
# keys so Minecraft can use en_us; never copy legacy text into our own language JSON.
import zipfile
missing=[]
with zipfile.ZipFile('references/Forbidden Magic-1.7.10-0.575.jar') as archive:
    for target,aliases in out.manifest['languages'].items():
        lines=archive.read(out.manifest['mapping'][target]).decode('utf-8-sig').splitlines()
        available={line.split('=',1)[0].strip() for line in lines if '=' in line and not line.startswith('#')}
        for key,legacy in list(aliases.items()):
            if legacy not in available:
                if target.endswith('/en_us.json'):raise ValueError('Missing English fallback '+legacy)
                missing.append({'target':target,'key':key,'missing_legacy_key':legacy});del aliases[key]
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)
Path('references/translation-fallbacks.json').write_text(json.dumps(missing,indent=2)+'\n',encoding='utf8')

import generate_api_data
generate_api_data.generate(out)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
