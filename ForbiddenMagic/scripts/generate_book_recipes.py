"""Build the book's display registry separately from actual crafting recipes."""
import json
from pathlib import Path

def generate(out):
    root=out.ROOT;mod=out.MOD
    def stack(value):return {'kind':'stack','stack':value}
    def sequence(values):return {'kind':'sequence','elements':values}
    choices={
      'forge:dyes/black':['minecraft:black_dye',mod+':black_dye'],
      'forge:nuggets/emerald':[mod+':emerald_nugget'],
      'forbidden_magic:taint_materials':[mod+':taint_shard','thaumcraft:tainted_goo'],
      'botania:manasteel_nuggets':['botania:manasteel_nugget'],
      'botania:elementium_nuggets':['botania:elementium_nugget'],
      'botania:terrasteel_nuggets':['botania:terrasteel_nugget']}
    def ingredient(value):
        if isinstance(value,list):return sequence([ingredient(v) for v in value])
        if 'ingredient' in value:return ingredient(value['ingredient'])
        if 'tag' in value:return {'kind':'ore','names':[value['tag']],'elements':[stack({'item':i,'count':1}) for i in choices[value['tag']]]}
        if 'item' in value:return stack(value)
        raise ValueError('Unknown display ingredient '+str(value))
    entries={};maxlevels={'cluster':4,'wrath':5,'educational':5}
    for path in (root/f'data/{mod}/research_pages').glob('*.json'):
        for research in json.loads(path.read_text())['entries']:
            for page in research['pages']:
                reference=page.get('recipe',{})
                if reference.get('kind')!='reference':continue
                for key in reference['keys']:
                    if key in entries:continue
                    namespace,name=key.split(':',1)
                    recipe=json.loads((root/f'data/{namespace}/recipes/{name}.json').read_text())
                    # Both branches have a stable reference; display normal-mode ingredients.
                    # The alternative is added below as a separate display page choice.
                    if recipe['type']=='forge:conditional':recipe=recipe['recipes'][-1]['recipe']
                    kind=recipe['type'];data={'research':recipe.get('research','')}
                    if kind in ['minecraft:crafting_shaped','minecraft:crafting_shapeless','thaumcraft:arcane']:
                        shaped='pattern' in recipe
                        data.update(kind=('arcane_' if kind=='thaumcraft:arcane' else 'crafting_')+('shaped' if shaped else 'shapeless'),output=stack(recipe['result']),aspects=recipe.get('vis',{}))
                        if shaped:
                            pattern=recipe['pattern'];data.update(width=len(pattern[0]),height=len(pattern),mirrored=True)
                            values=[ingredient(recipe['key'][c]) if c!=' ' else {'kind':'sequence','elements':[]} for line in pattern for c in line]
                        else:values=[ingredient(v) for v in recipe['ingredients']]
                        data['ingredients']=sequence(values)
                    elif kind=='thaumcraft:crucible':data.update(kind='crucible',output=stack(recipe['result']),catalyst=ingredient(recipe['catalyst']),aspects=recipe['aspects'])
                    elif kind=='thaumcraft:infusion':
                        data.update(aspects=recipe['aspects'],instability=recipe['instability'],ingredients=sequence([ingredient(v) for v in recipe['components']]))
                        if 'enchantment' in recipe:
                            name=recipe['enchantment'].split(':')[1]
                            minimum={'wrath':5,'greedy':20,'corrupting':20,'impact':30,'voidtouched':30}.get(name,1)
                            data.update(kind='infusion_enchantment',output=None,enchantment={'id':-1,'name':'enchantment.'+mod+'.'+name,'max_level':maxlevels.get(name,1)},recipeXP=recipe.get('base_xp',max(1,minimum//3)))
                        else:data.update(kind='infusion',input=ingredient(recipe['center']),output=stack(recipe['result']))
                    else:raise ValueError('Unsupported book recipe '+kind)
                    entries[key]={'key':key,'class':reference['class'],'data':data}

    # The display registry is not configuration-aware. Explicitly document both
    # branches instead of presenting the normal recipe as valid in easy mode.
    from copy import deepcopy
    normal_key=mod+':crystalwell';easy_key=mod+':crystalwell_easy_display'
    easy=deepcopy(entries[normal_key]);easy['key']=easy_key
    easy['data']['ingredients']=sequence([ingredient({'item':'thaumcraft:scribing_tools'}),ingredient({'tag':'forge:dyes/black'}),ingredient({'item':'thaumcraft:balanced_shard'}),ingredient({'item':'thaumcraft:balanced_shard'})])
    entries[easy_key]=easy
    pages_path=root/f'data/{mod}/research_pages/core.json'
    pages=json.loads(pages_path.read_text())
    for node in pages['entries']:
        if node['key']==mod+':CRYSTALWELL':
            node['pages'].insert(1,{'type':'TEXT','text':'forbidden_magic.book.crystalwell_modes'})
            node['pages'].append({'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[easy_key],'class':'modern'}})
            page_count=len(node['pages'])
    out.write(str(pages_path.relative_to(root)),pages)
    catalog_path=root/f'data/{mod}/research_catalog/core.json';catalog=json.loads(catalog_path.read_text())
    for category in catalog['categories']:
        for node in category['research']:
            if node['key']==mod+':CRYSTALWELL':node['pageCount']=page_count
    out.write(str(catalog_path.relative_to(root)),catalog)
    for language,text in [('en_us','The following recipes depend on the server research difficulty. The first uses a mixed crystal cluster and three balanced shards in normal mode. The second uses two balanced shards in easy mode. Only the recipe for the current world can be crafted.'),('zh_cn','接下来的两种配方由服务器研究难度决定。第一种用于普通模式，消耗混合水晶簇和三枚平衡碎片；第二种用于简单模式，消耗两枚平衡碎片。只有当前世界难度对应的配方可以合成。')]:
        path=root/f'assets/{mod}/lang/{language}.json';data=json.loads(path.read_text(encoding='utf8'));data['forbidden_magic.book.crystalwell_modes']=text;out.write(str(path.relative_to(root)),data)
    out.write(f'data/{mod}/research_page_recipes/forbidden.json',{'format':1,'entries':list(entries.values())})
    print('Wrote',len(entries),'named book recipe definitions.')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
