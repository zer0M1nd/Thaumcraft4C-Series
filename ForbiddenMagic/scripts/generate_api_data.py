"""Addon-only input tables for the public TC4 0.4.11 aspect APIs."""
import json,zipfile
from pathlib import Path
MOD='forbidden_magic'
def generate(out):
    root=out.ROOT
    def write(path,data):out.write(path,data)
    def entity(id,values,**kw):return dict(entity=id,operation='add',aspects={MOD+':'+a:n for a,n in values.items()},**kw)
    values={
      'thaumcraft:brainy_zombie':{'ira':3},'thaumcraft:giant_brainy_zombie':{'ira':4},
      'thaumcraft:fire_bat':{'ira':1,'infernus':2},'minecraft:wither_skeleton':{'infernus':3},
      'minecraft:creeper':{'ira':2},'minecraft:wither':{'infernus':7,'ira':7},
      'minecraft:ocelot':{'desidia':3},'minecraft:enderman':{'superbia':4,'invidia':4},
      'minecraft:ghast':{'infernus':3,'ira':3},'minecraft:zombified_piglin':{'ira':6},
      'minecraft:ender_dragon':{'superbia':10},'minecraft:pig':{'gula':3},'minecraft:magma_cube':{'infernus':1}}
    rows=[entity(k,v) for k,v in values.items()]
    rows.append(entity('minecraft:creeper',{'ira':2},variant='powered'))
    write(f'data/{MOD}/entity_aspect_contributions/core.json',{'format':1,'entries':rows})
    write(f'optional_packs/lust/data/{MOD}/entity_aspect_contributions/lust.json',{'format':1,'entries':[entity('thaumcraft:taintacle',{'luxuria':3})]})
    # Optional entities only gain additions if their owning addon supplies a base scan rule.
    rows=[entity('twilightforest:'+id,aspects,optional=True) for id,aspects in {
       'carminite_ghastling':{'infernus':1,'ira':1},'ur_ghast':{'infernus':10,'ira':10},
       'carminite_ghastguard':{'infernus':3,'ira':3},'lich':{'superbia':10},'king_spider':{'superbia':4}}.items()]
    write('optional_packs/twilightforest/pack.mcmeta',{'pack':{'pack_format':15,'description':'Forbidden Magic Twilight entity additions'}})
    write(f'optional_packs/twilightforest/data/{MOD}/entity_aspect_contributions/twilight.json',{'format':1,'entries':rows})
    write('optional_packs/twilight_lust/pack.mcmeta',{'pack':{'pack_format':15,'description':'Forbidden Magic Twilight lust additions'}})
    write(f'optional_packs/twilight_lust/data/{MOD}/entity_aspect_contributions/twilight_lust.json',{'format':1,'entries':[entity('twilightforest:dwarf_rabbit',{'luxuria':5},optional=True),entity('twilightforest:pinch_beetle',{'luxuria':3},optional=True)]})
    # Resolve vanilla/TC4 split metadata using the pinned dependency, without modifying its data.
    mappings={};base_items={}
    with zipfile.ZipFile('libs/thaumcraft-0.4.11.jar') as z:
        for name in ['vanilla','thaumcraft']:
            for row in json.loads(z.read('data/thaumcraft/legacy_item_mappings/'+name+'.json'))['items']:
                mappings[row['item']]={'item':row['legacy_item'],'metadata':row['metadata']}
        base_items={r['item']:r for r in json.loads(z.read('data/thaumcraft/recipe_aspect_inputs/legacy.json'))['items']}
    choices={'forge:dyes/black':['minecraft:black_dye',MOD+':black_dye'],
      'forge:nuggets/emerald':[MOD+':emerald_nugget'],MOD+':taint_materials':[MOD+':taint_shard','thaumcraft:tainted_goo'],
      **{'botania:'+m+'_nuggets':['botania:'+m+'_nugget'] for m in ['manasteel','elementium','terrasteel']}}
    items={}
    def key(id):
        if id in mappings:return mappings[id].copy()
        if id not in base_items:items[id]={'item':id,'damageable':False,'subtypes':False}
        return {'item':id,'metadata':0}
    def ingredient(v):
        if 'ingredient' in v:return ingredient(v['ingredient'])
        if isinstance(v,list):return {'alternatives':[a for x in v for a in ingredient(x)['alternatives']],'choose_nonempty':True}
        if 'item' in v:return {'alternatives':[key(v['item'])],'choose_nonempty':False}
        if 'tag' in v:return {'alternatives':[key(i) for i in choices[v['tag']]],'choose_nonempty':True}
        raise ValueError('Unknown aspect ingredient '+str(v))
    recipes=[]
    def recipe(d,conditions):
        conditions=conditions+d.get('conditions',[])
        if d['type']=='forge:conditional':
            for branch in d['recipes']:recipe(branch['recipe'],conditions+branch['conditions'])
            return
        t=d['type']
        if t in ['botania:mana_infusion','bloodmagic:altar'] and d['output']['item'].startswith(MOD+':wand_'):
            recipes.append({'kind':'CRUCIBLE','output':key(d['output']['item']),'count':1,'cost':[],
              'ingredients':[ingredient(d['input'])],'conditions':conditions})
            return
        if t not in ['minecraft:crafting_shaped','minecraft:crafting_shapeless','thaumcraft:arcane','thaumcraft:infusion','thaumcraft:crucible']:return
        if 'enchantment' in d:return
        result=d['result'];result={'item':result} if isinstance(result,str) else result
        if not result['item'].startswith(MOD+':'):return
        if 'pattern' in d:inputs=[d['key'][c] for line in d['pattern'] for c in line if c!=' ']
        elif 'ingredients' in d:inputs=d['ingredients']
        elif t=='thaumcraft:infusion':inputs=[d['center']]+d['components']
        elif t=='thaumcraft:crucible':inputs=[d['catalyst']]
        else:raise ValueError('Missing ingredients '+str(d))
        recipes.append({'kind':{'thaumcraft:arcane':'ARCANE','thaumcraft:infusion':'INFUSION','thaumcraft:crucible':'CRUCIBLE'}.get(t,'CRAFTING'),
          'output':key(result['item']),'count':result.get('count',1),'cost':[{'aspect':a,'amount':n} for a,n in d.get('aspects',d.get('vis',{})).items()],
          'ingredients':[ingredient(i) for i in inputs],'conditions':conditions})
    for path in sorted((root/f'data/{MOD}/recipes').rglob('*.json')):recipe(json.loads(path.read_text()),[])
    # Explicit definitions must also have semantic identities when used as recipe ingredients.
    for path in list((root/f'data/{MOD}/item_aspects').glob('*.json'))+list((root/'optional_packs').glob(f'*/data/{MOD}/item_aspects/*.json')):
        for row in json.loads(path.read_text())['entries']:
            for id in row.get('items',[]):key(id)
    write('optional_packs/aspect_inputs/pack.mcmeta',{'pack':{'pack_format':15,'description':'Forbidden Magic recipe aspect inputs'}})
    write(f'optional_packs/aspect_inputs/data/{MOD}/recipe_aspect_inputs/forbidden.json',{'format':1,'items':list(items.values()),'registrations':[],'recipes':recipes})
    print('Wrote',len(recipes),'semantic calculation recipes;',len(items),'additional item identities.')
