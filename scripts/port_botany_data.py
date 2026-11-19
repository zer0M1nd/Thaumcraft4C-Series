"""Generate the audited Botania branch using modern item IDs and native recipe types."""
import port_core_data as core
from port_core_data import out,ROOT,LEGACY,MOD,rid,split,body,args,array,quoted,aspects,strip_comments
import json,re

core.ownmeta['wandCore']=['wand_rod_'+s for s in ['tainted','infernal','soul','blood','witchwood','profane','blood_inert','livingwood','livingwood_inert','blood_staff','witchwood_staff','dreamwood','dreamwood_inert','dreamwood_staff','equivalent']]
core.ownmeta['wandCap']=['wand_cap_'+s for s in ['alchemical','vinteum','terrasteel','manasteel','manasteel_inert','elementium','elementium_inert']]
core.tags.update(nuggetManasteel='botania:manasteel_nuggets',nuggetElvenElementium='botania:elementium_nuggets',nuggetTerrasteel='botania:terrasteel_nuggets')
oldstack=core.stack
resources=['manasteel_ingot','mana_pearl','mana_diamond','livingwood_twig','terrasteel_ingot','pixie_dust','dragonstone','elementium_ingot','quartz_mana','dreamwood_twig']
runes=['water','fire','earth','air','spring','summer','autumn','winter','mana','lust','gluttony','greed','sloth','wrath','envy','pride']
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
def stack(s):
    if s in ['euclidaisy','whisperweed','tainthistle','bloodthorn']:return {'item':rid(s),'count':1}
    if s.startswith('new ItemStack'):
        a=args(s);token=a[0];n=int(a[1]) if len(a)>1 else 1;m=int(a[2]) if len(a)>2 else 0
        item={'livingLog':'livingwood_log','dreamwood':'dreamwood_log','lexicon':'lexicon'}.get(token)
        if token=='resource':item=resources[m]
        elif token=='rune':item='rune_'+runes[m]
        elif token=='petal':item=colors[m]+'_petal'
        elif token=='flower':item=colors[m]+'_mystical_flower'
        if item:return {'item':'botania:'+item,'count':n}
    return oldstack(s)
core.stack=stack
def ingredient(s):return core.ingredient(s)
source=strip_comments((LEGACY/'compat/ForbiddenBotany.java').read_text())
recipes={}
condition={'type':'forge:mod_loaded','modid':'botania'}
def shaped(parts):
    pattern=[]
    while parts and parts[0].startswith('"'):pattern.append(quoted(parts.pop(0)))
    key={parts[i].strip("'"):ingredient(parts[i+1]) for i in range(0,len(parts),2)}
    # Old shaped recipes treated unspecified symbols as empty cells.
    pattern=[''.join(c if c in key else ' ' for c in row) for row in pattern]
    return {'pattern':pattern,'key':key}
for m in re.finditer(r'(?:InfusionRecipe|IArcaneRecipe|IRecipe)\s+(\w+)\s*=\s*([^;]+);',source):
    name,expression=m.groups();a=args(expression)
    if 'addInfusionCraftingRecipe' in expression:
        research,result,instability,cost,center,components=a
        data={'type':'thaumcraft:infusion','research':rid(quoted(research)),'result':stack(result),'instability':int(instability),'aspects':aspects(cost),'center':{'ingredient':ingredient(center)},'components':[{'ingredient':ingredient(x)} for x in array(components)]}
        if name in ('livingwood_rod','dreamwood_rod'):data['center']['exact']=True
    elif 'ArcaneCraftingRecipe' in expression:
        research,result,cost,*parts=a;data={'type':'thaumcraft:arcane','research':rid(quoted(research)),'result':stack(result),'vis':aspects(cost)}
        if 'Shapeless' in expression:data.update(mode='shapeless',ingredients=[ingredient(x) for x in parts])
        else:data.update(shaped(parts))
    elif 'ShapedOreRecipe' in expression:
        result,*parts=a;data={'type':'minecraft:crafting_shaped','result':stack(result),**shaped(parts)}
    else:raise ValueError(expression)
    data['conditions']=[condition];recipes[name]=data;out.write(f'data/{MOD}/recipes/botania/{name}.json',data)
for m in re.finditer(r'BotaniaAPI.registerManaInfusionRecipe\(',source):
    raw,_=body(source,source.index('(',m.start()));result,source_item,mana=split(raw);result=stack(result)
    out.write(f'data/{MOD}/recipes/botania/mana_{result["item"].split(":")[1]}.json',{'type':'botania:mana_infusion','input':ingredient(source_item),'output':result,'mana':int(mana),'conditions':[condition]})

keys=set(re.findall(r'new DarkResearchItem\(\s*"([^"]+)"',source))|core.research_keys
def research_id(key):return rid(key) if key in keys else key
nodes=[];pages=[]
for m in re.finditer(r'new DarkResearchItem\(',source):
    raw,end=body(source,source.index('(',m.start()));a=split(raw);key=quoted(a[0]);chain=source[end:source.index(';',end)]
    if len(a)==2:node={'key':rid(key),'category':'FORBIDDEN','displayColumn':0,'displayRow':0,'complexity':0,'aspects':{},'isVirtual':True,'pageCount':0};nodes.append(node);pages.append({'key':rid(key),'pages':[]});continue
    keyarg,category,label,cost,column,row,complexity,icon=a
    node={'key':rid(key),'category':'FORBIDDEN','displayColumn':int(column),'displayRow':int(row),'complexity':min(3,int(complexity)),'aspects':aspects(cost),'icon_item':stack(icon),'warp':2 if key=='ROD_livingwood' else 0}
    for old,new in [('setParents','parents'),('setParentsHidden','parentsHidden'),('setSiblings','siblings')]:
        found=re.search(r'\.'+old+r'\(',chain)
        if found:values,_=body(chain,chain.index('(',found.start()));node[new]=[research_id(quoted(x)) for x in split(values)]
    for flag in ['Secondary','Concealed','Round','Stub','AutoUnlock','Virtual']:
        if f'.set{flag}(' in chain:node['is'+flag]=True
    found=re.search(r'\.setPages\(',chain);values,_=body(chain,chain.index('(',found.start()));parsed=[]
    for expression in split(values):
        first=args(expression)[0]
        if first.startswith('"'):
            text=quoted(first);out.translate(text,text);parsed.append({'type':'TEXT','text':text})
        else:
            recipe=recipes[first];kind={'thaumcraft:arcane':'ARCANE_CRAFTING','thaumcraft:infusion':'INFUSION_CRAFTING','minecraft:crafting_shaped':'NORMAL_CRAFTING'}[recipe['type']]
            parsed.append({'type':kind,'recipe':{'kind':'reference','keys':[rid('botania/'+first)],'class':'modern'}})
    node['pageCount']=len(parsed);nodes.append(node);pages.append({'key':rid(key),'pages':parsed});out.translate('tc.research_name.'+rid(key),'forbidden.research_name.'+key)
out.write(f'data/{MOD}/research_catalog/botania.json',{'format':1,'categories':[{'key':'FORBIDDEN','icon':f'{MOD}:textures/misc/forbidden.png','background':f'{MOD}:textures/misc/runecircle.png','research':nodes}]})
out.write(f'data/{MOD}/research_pages/botania.json',{'format':1,'entries':pages})
rods=json.loads((out.ROOT/f'data/{MOD}/thaumcraft/wand_rods.json').read_text())
for name,capacity,craft in [('livingwood',100,12),('dreamwood',100,12),('dreamwood_staff',250,25)]:
    image=name.replace('_staff','');out.texture('models','wand_rod_'+image,'models/wand_rod_'+image)
    rods.append({'tag':rid(name),'item':rid('wand_rod_'+name),'capacity':capacity,'craftCost':craft,'texture':f'{MOD}:textures/models/wand_rod_{image}.png','staff':name.endswith('_staff'),'runes':name.endswith('_staff'),'research':rid('ROD_'+name)})
out.write(f'data/{MOD}/thaumcraft/wand_rods.json',rods)
caps=[]
for name,cost,craft in [('manasteel',.9,6),('elementium',.8,9),('terrasteel',1.8,1),('orichalcum',0,1000)]:
    out.texture('models','wand_cap_'+name,'models/wand_cap_'+name)
    data={'tag':rid(name),'cost':cost,'craftCost':craft,'texture':f'{MOD}:textures/models/wand_cap_{name}.png','research':rid('CAP_'+name) if name!='orichalcum' else ''}
    if name!='orichalcum':data['item']=rid('wand_cap_'+name)
    caps.append(data);out.translate('item.Wand.'+rid(name)+'.cap','item.Wand.'+name+'.cap')
out.write(f'data/{MOD}/thaumcraft/wand_caps.json',caps)
for name in ['livingwood','livingwood_inert','dreamwood','dreamwood_inert','dreamwood_staff']:out.item('wand_rod_'+name,'wand_rod_'+name,'item.WandCores.'+name+'.name')
for name in ['manasteel','manasteel_inert','elementium','elementium_inert','terrasteel']:out.item('wand_cap_'+name,'wand_cap_'+name,'item.WandCaps.'+name+'.name')
for row in rods:
    name=row['tag'].split(':')[1].replace('_staff','');out.translate('item.Wand.'+rid(name)+'.rod','item.Wand.'+name+'.rod')
for name in ['luxuria','infernus','superbia','gula','invidia','desidia','ira']:out.translate('tc.aspect.'+rid(name),'tc.aspect.'+name)
for name in ['euclidaisy','whisperweed','tainthistle','bloodthorn']:
    texture=out.texture('block',name,'blocks/'+name)
    out.write(f'assets/{MOD}/blockstates/{name}.json',{'variants':{'':{'model':f'{MOD}:block/{name}'}}})
    out.write(f'assets/{MOD}/models/block/{name}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':texture}})
    out.write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':texture}})
    out.write(f'data/{MOD}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':rid(name)}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    out.translate('block.'+MOD+'.'+name,'darkflower.'+name+'.name')
    out.translate('forbidden.lexicon.'+name+'.0','forbidden.lexicon.'+name+'.0')
    out.write(f'assets/botania/patchouli_books/lexicon/en_us/entries/forbidden_magic/{name}.json',{'name':'block.'+MOD+'.'+name,'icon':rid(name),'category':'botania:'+('generating_flowers' if name=='tainthistle' else 'functional_flowers'),'pages':[{'type':'patchouli:text','text':'forbidden.lexicon.'+name+'.0'}]})
out.write(f'data/{MOD}/item_aspects/flowers.json',{'format':1,'entries':[{'items':[rid(n) for n in ['euclidaisy','whisperweed','tainthistle','bloodthorn']],'aspects':[{'aspect':'herba','amount':4},{'aspect':'praecantatio','amount':1}]}]})
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)
print('Wrote',len(recipes),'Botania crafting recipes and',len(nodes),'research entries.')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
