"""Translate audited declarative legacy recipes/research into native JSON.

This is a development-time generator, never a runtime Java-source interpreter.
Unsupported expressions fail loudly instead of creating recipes with missing ingredients.
"""
import json,re
from pathlib import Path
import generate_resources as out

ROOT=Path(__file__).resolve().parents[1]
LEGACY=ROOT/'references/ForbiddenMagic-GTNH/src/main/java/fox/spiteful/forbidden'
MOD='forbidden_magic'
def strip_comments(s):return re.sub(r'/\*.*?\*/|//[^\n]*','',s,flags=re.S)
def split(s):
    result=[];start=0;depth=0;quote=None;escape=False
    for i,c in enumerate(s):
        if quote:
            if escape:escape=False
            elif c=='\\':escape=True
            elif c==quote:quote=None
        elif c in '\"\'':quote=c
        elif c in '([{':depth+=1
        elif c in ')]}':depth-=1
        elif c==',' and depth==0:result.append(s[start:i].strip());start=i+1
    if s[start:].strip():result.append(s[start:].strip())
    return result
def body(s,start):
    depth=0;quote=None;escape=False
    for i in range(start,len(s)):
        c=s[i]
        if quote:
            if escape:escape=False
            elif c=='\\':escape=True
            elif c==quote:quote=None
        elif c in '\"\'':quote=c
        elif c=='(':depth+=1
        elif c==')':
            depth-=1
            if depth==0:return s[start+1:i],i+1
    raise ValueError('Unbalanced Java argument list')
def args(s):return split(body(s,s.index('('))[0])
def quoted(s):return json.loads(s.strip())
def array(s):return split(s[s.index('{')+1:s.rindex('}')])
def snake(s):return re.sub(r'(?<!^)(?=[A-Z])','_',s).lower()
def rid(s):return MOD+':'+s

aspectmap={name:tag for name,tag in re.findall(r'public static final Aspect (\w+)=of\("([^"]+)"\)',(ROOT/'references/base-mappings/Aspect.java').read_text())}
aspectmap.update({'NETHER':rid('infernus'),'PRIDE':rid('superbia'),'LUST':rid('luxuria'),'GLUTTONY':rid('gula'),'ENVY':rid('invidia'),'SLOTH':rid('desidia'),'WRATH':rid('ira')})
def aspects(s):return {aspectmap[name]:int(n) for name,n in re.findall(r'\.(?:add|merge)\((?:(?:DarkAspects|Aspect)\.)?(\w+),\s*(\d+)\)',s)}
base={}
for filename in ['thaumcraft','vanilla']:
    data=json.loads((ROOT/f'references/base-mappings/{filename}.json').read_text())
    for row in data['items']:base[(row.get('legacy_item',row['item']).lower(),row.get('metadata',0))]=row['item']
own={'gluttonyShard':'gluttony_shard','skullAxe':'skull_axe','taintShovel':'taint_shovel','taintPickaxe':'taint_pickaxe','arcaneCakeItem':'arcane_cake','arcaneCake':'arcane_cake','morphPickaxe':'morph_pickaxe','morphSword':'morph_sword','morphShovel':'morph_shovel','morphAxe':'morph_axe','mobCrystal':'mob_crystal','fork':'diabolist_fork','crystalwell':'crystalwell','primewell':'primewell','ridingCrop':'riding_crop','subCollar':'pain_collar','ringFood':'ring_nutrition','taintFruit':'taint_fruit','taintCoal':'taint_coal','blinkFocus':'focus_blink','blackFlower':'ink_flower','roseBush':'umbral_bush','wrathCage':'wrath_cage','starBlock':'nether_star_block','taintLog':'taint_log','taintPlanks':'taint_planks','taintLeaves':'taint_leaves','taintSapling':'taint_sapling'}
ownmeta={'deadlyShards':['wrath_shard','envy_shard','taint_shard','pride_shard','lust_shard','sloth_shard','greed_shard'],'resource':['emerald_nugget','black_dye','manasteel_nugget','tainted_blood_shard','elementium_nugget'],'wandCore':['wand_rod_tainted','wand_rod_infernal','wand_rod_soul','wand_rod_blood','wand_rod_witchwood','wand_rod_profane'],'taintStone':['taint_stone','taint_bricks']}
tags={'nuggetEmerald':'forge:nuggets/emerald','dyeBlack':'forge:dyes/black','resourceTaint':rid('taint_materials')}
aliases={'itemPickElemental':'ItemPickaxeElemental'}
def stack(s):
    s=s.strip();count=1;meta=0
    if s.startswith('new ItemStack'):
        a=args(s);token=a[0];count=int(a[1]) if len(a)>1 else 1
        meta=int(a[2]) if len(a)>2 and a[2]!='OreDictionary.WILDCARD_VALUE' else 0
    elif s.startswith('ItemApi.get'):
        a=args(s);token='ConfigItems.'+quoted(a[0]);meta=int(a[1])
    else:token=s
    if token.startswith(('ForbiddenItems.','ForbiddenBlocks.')):
        field=token.split('.')[1];item=rid(ownmeta[field][meta] if field in ownmeta else own[field])
    elif token.startswith(('ConfigItems.','ConfigBlocks.')):
        field=token.split('.')[1];field=aliases.get(field,field);key=('thaumcraft:'+field.lower(),0 if meta==32767 else meta)
        if key not in base:raise ValueError('Missing TC item '+str(key))
        item=base[key]
    elif token.startswith(('Items.','Blocks.')):
        field=token.split('.')[1];key=('minecraft:'+field.lower(),meta)
        if key in base:item=base[key]
        elif meta==0:item='minecraft:'+{'netherbrick':'nether_brick','nether_brick':'nether_bricks','double_plant':'sunflower','quartz_ore':'nether_quartz_ore','fire':'fire_charge'}.get(field,field)
        else:raise ValueError('Missing vanilla metadata '+str(key))
    else:raise ValueError('Unsupported item expression '+s)
    return {'item':item,'count':count}
def ingredient(s):
    if s.startswith('"'):return {'tag':tags[quoted(s)]}
    value=stack(s);return {'item':value['item']}
recipes={}
source=strip_comments((LEGACY/'ForbiddenRecipes.java').read_text())
for match in re.finditer(r'ForbiddenResearch\.recipes\.put\s*\(',source):
    raw,_=body(source,source.index('(',match.start()));key,expression=split(raw)
    key=quoted(key)
    if key in recipes:continue
    a=args(expression);kind=expression[:expression.index('(')].strip()
    output={}
    if 'addInfusionCraftingRecipe' in kind:
        research,result,instability,cost,center,components=a
        output={'type':'thaumcraft:infusion','research':rid(quoted(research)),'instability':int(instability),'aspects':aspects(cost),'center':{'ingredient':ingredient(center)},'components':[{'ingredient':ingredient(i)} for i in array(components)],'result':stack(result)}
    elif 'addInfusionEnchantmentRecipe' in kind:
        research,enchantment,instability,cost,components=a
        output={'type':'thaumcraft:infusion','research':rid(quoted(research)),'enchantment':rid(enchantment.split('.')[-1]),'instability':int(instability),'aspects':aspects(cost),'components':[{'ingredient':ingredient(i)} for i in array(components)]}
    elif 'addCrucibleRecipe' in kind:
        research,result,catalyst,cost=a
        output={'type':'thaumcraft:crucible','research':rid(quoted(research)),'legacy_order':20000+len(recipes),'result':stack(result),'catalyst':ingredient(catalyst),'aspects':aspects(cost)}
    elif 'ArcaneCraftingRecipe' in kind:
        research,result,cost,parts=a;output={'type':'thaumcraft:arcane','research':rid(quoted(research)),'result':stack(result),'vis':aspects(cost)}
        elements=array(parts)
        if 'Shapeless' in kind:output.update(mode='shapeless',ingredients=[ingredient(i) for i in elements])
        else:
            pattern=[]
            while elements and elements[0].startswith('"'):pattern.append(quoted(elements.pop(0)))
            output.update(pattern=pattern,key={args(elements[i])[0].strip("'"):ingredient(elements[i+1]) for i in range(0,len(elements),2)})
    elif 'addRecipe' in expression:
        # Get the outer addRecipe argument list, skipping getInstance().
        raw,_=body(expression,expression.index('(',expression.index('addRecipe')));a=split(raw)
        result,parts=a;elements=array(parts);pattern=[]
        while elements and elements[0].startswith('"'):pattern.append(quoted(elements.pop(0)))
        output={'type':'minecraft:crafting_shaped','result':stack(result),'pattern':pattern,'key':{args(elements[i])[0].strip("'"):ingredient(elements[i+1]) for i in range(0,len(elements),2)}}
    else:raise ValueError('Unsupported recipe '+key+': '+kind)
    recipes[key]=output
    out.write(f'data/{MOD}/recipes/{snake(key)}.json',output)
# Normal research uses the normal crystalwell recipe, which follows the easy-mode branch in source.
recipes['Crystalwell']['ingredients']=[{'item':'thaumcraft:scribing_tools'},{'tag':'forge:dyes/black'},{'item':'thaumcraft:mixed_crystal_cluster'}]+[{'item':'thaumcraft:balanced_shard'}]*3
out.write(f'data/{MOD}/recipes/crystalwell.json',recipes['Crystalwell'])
out.tag(MOD,'taint_materials',[rid('taint_shard'),'thaumcraft:tainted_goo'])
out.write(f'data/{MOD}/recipes/taint_coal.json',{'type':'minecraft:smelting','ingredient':{'item':rid('taint_log')},'result':rid('taint_coal'),'experience':.15,'cookingtime':200})
(ROOT/'docs/CORE-RECIPE-INDEX.json').write_text(json.dumps({key:snake(key) for key in recipes},indent=2)+'\n',encoding='utf-8')
research_source=strip_comments((LEGACY/'ForbiddenResearch.java').read_text())
research_keys=set(re.findall(r'new DarkResearchItem\(\s*"([^"]+)"',research_source))
def research_id(key):return rid(key) if key in research_keys else key
nodes={};pages={}
warp={key:int(amount) for key,amount in re.findall(r'addWarpToResearch\("([^"]+)",\s*(\d+)\)',research_source)}
for match in re.finditer(r'new DarkResearchItem\s*\(',research_source):
    raw,end=body(research_source,research_source.index('(',match.start()))
    key,category,cost,column,row,complexity,icon=split(raw);key=quoted(key)
    if key in nodes:continue
    chain=research_source[end:research_source.index(';',end)]
    node={'key':rid(key),'category':'FORBIDDEN','displayColumn':int(column),'displayRow':int(row),'complexity':min(3,int(complexity)),'aspects':aspects(cost),'warp':warp.get(key,0)}
    if icon.startswith('new ResourceLocation'):
        namespace,path=map(quoted,args(icon));out.mapping[f'assets/{MOD}/{path}']=f'assets/{namespace}/{path}';node['icon_resource']=f'{MOD}:{path}'
    else:node['icon_item']=stack(icon)
    for old,new in [('setParents','parents'),('setParentsHidden','parentsHidden'),('setSiblings','siblings')]:
        m=re.search(r'\.'+old+r'\(',chain)
        if m:values,_=body(chain,chain.index('(',m.start()));node[new]=[research_id(quoted(v)) for v in split(values)]
    m=re.search(r'\.setAspectTriggers\(',chain)
    if m:values,_=body(chain,chain.index('(',m.start()));node['aspectTriggers']=[aspectmap[v.split('.')[-1]] for v in split(values)]
    for flag in ['Secondary','Concealed','Round','Stub','AutoUnlock','Hidden','Special','Virtual','Lost']:
        if f'.set{flag}(' in chain:node['is'+flag]=True
    parsed=[]
    pm=re.search(r'\.setPages\(',chain)
    if pm:
        raw,_=body(chain,chain.index('(',pm.start()))
        for expression in split(raw):
            page_args=args(expression);first=page_args[0]
            if first.startswith('"'):
                text=quoted(first);page={'type':'TEXT','text':text}
                if len(page_args)>1 and page_args[1].startswith('"'):page={'type':'TEXT_CONCEALED','research':research_id(text),'text':quoted(page_args[1])}
                out.translate(page['text'],page['text']);parsed.append(page)
            elif 'recipes.get(' in first:
                recipe_key=re.search(r'recipes.get\("([^"]+)"\)',first)[1];recipe=recipes[recipe_key]
                kind={'minecraft:crafting_shaped':'NORMAL_CRAFTING','thaumcraft:arcane':'ARCANE_CRAFTING','thaumcraft:crucible':'CRUCIBLE_CRAFTING','thaumcraft:infusion':'INFUSION_ENCHANTMENT' if 'enchantment' in recipe else 'INFUSION_CRAFTING'}[recipe['type']]
                parsed.append({'type':kind,'recipe':{'kind':'reference','keys':[rid(snake(recipe_key))],'class':'modern'}})
            elif first.startswith('new ItemStack'):
                parsed.append({'type':'SMELTING','recipe':{'kind':'stack','stack':stack(first)}})
            else:raise ValueError('Unsupported page '+key+': '+expression)
    node['pageCount']=len(parsed);nodes[key]=node;pages[key]={'key':rid(key),'pages':parsed}
    out.translate('tc.research_name.'+rid(key),'forbidden.research_name.'+key)
# Category translation uses the shared category key, while files remain in this addon's namespace.
out.translate('tc.research_category.FORBIDDEN','tc.research_category.FORBIDDEN')
out.texture('misc','forbidden','misc/forbidden');out.texture('misc','runecircle','misc/runecircle')
out.write(f'data/{MOD}/research_catalog/core.json',{'format':1,'categories':[{'key':'FORBIDDEN','icon':f'{MOD}:textures/misc/forbidden.png','background':f'{MOD}:textures/misc/runecircle.png','research':list(nodes.values())}]})
out.write(f'data/{MOD}/research_pages/core.json',{'format':1,'entries':list(pages.values())})
out.write(f'data/{MOD}/thaumcraft/original_resources.json',out.manifest)
aspect_source=strip_comments((LEGACY/'DarkAspects.java').read_text())
explicit={}
for match in re.finditer(r'ThaumcraftApi\.registerObjectTag\s*\(',aspect_source):
    raw,_=body(aspect_source,aspect_source.index('(',match.start()));target,values=split(raw)
    if 'ForbiddenItems.' not in target and 'ForbiddenBlocks.' not in target:continue
    item=stack(target)['item']
    if item not in explicit:explicit[item]=aspects(values)
out.write(f'data/{MOD}/item_aspects/core.json',{'format':1,'entries':[{'items':[item],'aspects':[{'aspect':aspect,'amount':amount} for aspect,amount in values.items()]} for item,values in explicit.items()]})
print('Wrote',len(recipes),'core recipes,',len(nodes),'research entries, and',len(explicit),'explicit item aspect rules.')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
