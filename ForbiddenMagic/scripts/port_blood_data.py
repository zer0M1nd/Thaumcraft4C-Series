"""All data generation entry point: core, Botania, then modern Blood Magic integration."""
import port_botany_data as botan
from port_botany_data import core,out,ROOT,LEGACY,MOD,rid,split,body,args,array,quoted,aspects,strip_comments,shaped
import json,re

core.aliases['itemWandCap']='WandCap'
core.own.update(bloodwell='bloodwell',boundwell='boundwell',bloodRapier='blood_rapier',bloodOrb='eldritch_orb')
mapping={'masterOrb':'masterbloodorb','imbuedSlate':'infusedslate','magicales':'arcaneashes','aether':'reagentair','terrae':'reagentgrowth','aquasalus':'reagentwater','incendium':'reagentlava','sanctus':'reagentbloodlight','tennebrae':'reagentvoid','crapOrb':'weakbloodorb','demonShard':'dungeon_metal','bloodBucket':'life_essence_bucket','kamiOrb':'archmagebloodorb'}
def stack(s):
    if s.startswith('new ItemStack'):
        a=args(s)
        if a[0] in mapping:return {'item':'bloodmagic:'+mapping[a[0]],'count':int(a[1]) if len(a)>1 else 1}
    return botan.oldstack(s)
core.stack=stack
def ingredient(s):return core.ingredient(s)
source=strip_comments((LEGACY/'compat/BloodMagic.java').read_text())
recipes={};condition={'type':'forge:mod_loaded','modid':'bloodmagic'}
for m in re.finditer(r'(?:InfusionRecipe|IArcaneRecipe)\s+(\w+)\s*=\s*([^;]+);',source):
    name,expression=m.groups();a=args(expression)
    if 'addInfusionCraftingRecipe' in expression:
        research,result,instability,cost,center,components=a
        data={'type':'thaumcraft:infusion','research':rid(quoted(research)),'result':stack(result),'instability':int(instability),'aspects':aspects(cost),'center':{'ingredient':ingredient(center)},'components':[{'ingredient':ingredient(x)} for x in array(components)]}
    else:
        research,result,cost,*parts=a;data={'type':'thaumcraft:arcane','research':rid(quoted(research)),'result':stack(result),'vis':aspects(cost)}
        if 'Shapeless' in expression:data.update(mode='shapeless',ingredients=[ingredient(x) for x in parts])
        else:data.update(shaped(parts))
    data['conditions']=[condition];recipes[name]=data;out.write(f'data/{MOD}/recipes/bloodmagic/{name}.json',data)
keys=set(re.findall(r'new DarkResearchItem\(\s*"([^"]+)"',source))|core.research_keys
def research_id(key):return rid(key) if key in keys else key
warps={key:int(n) for key,n in re.findall(r'addWarpToResearch\("([^"]+)",\s*(\d+)\)',source)}
nodes=[];pages=[]
for m in re.finditer(r'new DarkResearchItem\(',source):
    raw,end=body(source,source.index('(',m.start()));a=split(raw);key=quoted(a[0]);chain=source[end:source.index(';',end)]
    _,category,label,cost,column,row,complexity,icon=a
    node={'key':rid(key),'category':'FORBIDDEN','displayColumn':int(column),'displayRow':int(row),'complexity':min(3,int(complexity)),'aspects':aspects(cost),'icon_item':{'item':'bloodmagic:sacrificialdagger','count':1} if icon.startswith('new ResourceLocation') else stack(icon),'warp':warps.get(key,0)}
    for old,new in [('setParents','parents'),('setParentsHidden','parentsHidden'),('setSiblings','siblings')]:
        found=re.search(r'\.'+old+r'\(',chain)
        if found:values,_=body(chain,chain.index('(',found.start()));node[new]=[research_id(quoted(x)) for x in split(values)]
    for flag in ['Secondary','Concealed','Round','Stub','AutoUnlock','Virtual','Special']:
        if f'.set{flag}(' in chain:node['is'+flag]=True
    found=re.search(r'\.setPages\(',chain);values,_=body(chain,chain.index('(',found.start()));parsed=[]
    for expression in split(values):
        first=args(expression)[0]
        if first.startswith('"'):
            text=quoted(first);out.translate(text,text);parsed.append({'type':'TEXT','text':text})
        else:
            kind='ARCANE_CRAFTING' if recipes[first]['type']=='thaumcraft:arcane' else 'INFUSION_CRAFTING'
            parsed.append({'type':kind,'recipe':{'kind':'reference','keys':[rid('bloodmagic/'+first)],'class':'modern'}})
    node['pageCount']=len(parsed);nodes.append(node);pages.append({'key':rid(key),'pages':parsed});out.translate('tc.research_name.'+rid(key),'forbidden.research_name.'+key)
out.write(f'data/{MOD}/research_catalog/bloodmagic.json',{'format':1,'categories':[{'key':'FORBIDDEN','icon':f'{MOD}:textures/misc/forbidden.png','background':f'{MOD}:textures/misc/runecircle.png','research':nodes}]})
out.write(f'data/{MOD}/research_pages/bloodmagic.json',{'format':1,'entries':pages})
for name,source_item,output,tier,cost,consume,drain in [('blood_rod',rid('wand_rod_blood_inert'),rid('wand_rod_blood'),3,20000,15,20),('bloodthorn','botania:red_mystical_flower',rid('bloodthorn'),3,15000,25,50)]:
    out.write(f'data/{MOD}/recipes/bloodmagic/altar_{name}.json',{'type':'bloodmagic:altar','input':{'item':source_item},'output':{'item':output},'upgradeLevel':tier,'altarSyphon':cost,'consumptionRate':consume,'drainRate':drain,'conditions':[condition]+([botan.condition] if name=='bloodthorn' else [])})
out.write(f'data/{MOD}/recipes/bloodmagic/boundwell.json',{'type':'bloodmagic:array','baseinput':{'item':rid('crystalwell')},'addedinput':{'item':'bloodmagic:reagentbinding'},'output':{'item':rid('boundwell')},'texture':'bloodmagic:textures/models/alchemyarrays/bindingarray.png','conditions':[condition]})
for name,source_item,effect,duration in [('blood_seal',rid('tainted_blood_shard'),rid('blood_seal'),1200),('taint','thaumcraft:tainted_goo','thaumcraft:flux_taint',450)]:
    out.write(f'data/{MOD}/recipes/bloodmagic/flask_{name}.json',{'type':'bloodmagic:flask_potioneffect','input':[{'item':'bloodmagic:simplecatalyst'},{'item':source_item}],'effect':effect,'baseDuration':duration,'syphon':500,'ticks':200,'upgradeLevel':1,'conditions':[condition]})

rods=json.loads((out.ROOT/f'data/{MOD}/thaumcraft/wand_rods.json').read_text())
for name,capacity,craft in [('blood',100,12),('blood_staff',50,24)]:
    out.texture('models','wand_rod_blood','models/wand_rod_blood');rods.append({'tag':rid(name),'item':rid('wand_rod_'+name),'capacity':capacity,'craftCost':craft,'texture':f'{MOD}:textures/models/wand_rod_blood.png','staff':name.endswith('_staff'),'research':rid('ROD_'+name)})
out.write(f'data/{MOD}/thaumcraft/wand_rods.json',rods)
caps=json.loads((out.ROOT/f'data/{MOD}/thaumcraft/wand_caps.json').read_text());out.texture('models','wand_cap_alchemical','models/wand_cap_alchemical')
caps.append({'tag':rid('alchemical'),'item':rid('wand_cap_alchemical'),'cost':.9,'aspectCosts':{'aqua':.8},'craftCost':7,'texture':f'{MOD}:textures/models/wand_cap_alchemical.png','research':rid('CAP_alchemical')});out.write(f'data/{MOD}/thaumcraft/wand_caps.json',caps)
for name in ['blood','blood_inert','blood_staff']:out.item('wand_rod_'+name,'wand_rod_'+name,'item.WandCores.'+name+'.name')
out.item('wand_cap_alchemical','wand_cap_alchemical','item.WandCaps.alchemical.name')
out.translate('item.Wand.'+rid('blood')+'.rod','item.Wand.blood.rod');out.translate('item.Wand.'+rid('alchemical')+'.cap','item.Wand.alchemical.cap')
for name,texture,key in [('bloodwell','bloodwell','Bloodwell'),('boundwell','boundwell','Boundwell'),('blood_rapier','blood_rapier','BloodRapier'),('eldritch_orb','divineorb','EldritchOrb'),('tainted_blood_shard','tainted_blood_shard','FMResource.3')]:out.item(name,texture,'item.'+key+'.name',name=='blood_rapier')
out.translate('effect.forbidden_magic.blood_seal','potion.blood_seal');out.texture('misc','potions','misc/potions')
tainted=['taint_spider','taint_swarm','taintacle','tainted_pig','tainted_sheep','tainted_cow','tainted_chicken','tainted_villager','tainted_creeper']
out.write(f'data/{MOD}/tags/entity_types/tainted_mobs.json',{'replace':False,'values':[{'id':'thaumcraft:'+n,'required':False} for n in tainted]})
for mod in ['botania','bloodmagic']:out.write(f'optional_packs/{mod}/pack.mcmeta',{'pack':{'pack_format':15,'description':'Forbidden Magic optional '+mod+' aspect definitions'}})

# Explicit legacy aspect lists for surviving modern Blood Magic objects. Removed spell/reagent items use the documented modern counterparts.
explicit={
 'magicianbloodorb':{'victus':16,'praecantatio':6,'vitreus':4},'masterbloodorb':{'victus':16,'praecantatio':8,'vitreus':4},
 'archmagebloodorb':{'victus':32,rid('infernus'):4,'praecantatio':8,'vitreus':4},'dungeon_metal':{'victus':16,rid('infernus'):8,'vitreus':4,'ignis':1,'aqua':1,'aer':1,'terra':1,'lux':1,'tenebrae':1},
 'life_essence_bucket':{'metallum':8,'vacuos':1,'victus':4},'arcaneashes':{'praecantatio':6,'potentia':3},
 'reagentair':{'aer':6,'potentia':3},'reagentgrowth':{'terra':6,'potentia':3},'reagentwater':{'aqua':6,'potentia':3},'reagentlava':{'ignis':6,'potentia':3},'reagentbloodlight':{'lux':6,'potentia':3},'reagentvoid':{'tenebrae':6,'potentia':3},'reagentfrost':{'gelum':6,'potentia':3}}
out.write(f'optional_packs/bloodmagic/data/{MOD}/item_aspects/bloodmagic.json',{'format':1,'entries':[{'items':['bloodmagic:'+item],'aspects':[{'aspect':aspect,'amount':amount} for aspect,amount in values.items()]} for item,values in explicit.items()]})
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)
print('Wrote',len(recipes),'Blood Magic crafting recipes and',len(nodes),'research entries.')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
