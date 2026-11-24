"""Generate conditional ATM compression chains and original pixel artwork."""
import json
from pathlib import Path
from PIL import Image, ImageDraw
from generate_item_textures import generate as recolor_items

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
NS = 'thaumicomniverse'
DATA = ROOT / 'data' / NS
ASSETS = ROOT / 'assets' / NS
CONDITIONS = [{'type':'forge:mod_loaded','modid':'allthemodium'}]

def write(path, data):
    if 'loot_tables' in path.parts: data['omniverse_requires'] = ['allthemodium']
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

names = {}
blocks = []
for material, maximum, color, chinese, english in [
    ('thaumium',4,(91,71,143),'神秘','Thaumium'),
    ('void',3,(42,26,62),'虚空','Void Metal'),
    ('alchemical_brass',4,(177,132,56),'炼金黄铜','Alchemical Brass')]:
    previous = 'thaumcraft:thaumium_block' if material == 'thaumium' else ('thaumcraft:void_ingot' if material=='void' else 'thaumicfutures:alchemical_brass_block')
    for tier in range(0 if material=='void' else 1, maximum+1):
        name = material+'_block'+('_'+str(tier)+'x' if tier else '')
        item = NS+':'+name
        blocks.append(item)
        form = 'storage_blocks/'+material+('_'+str(tier)+'x' if tier else '')
        previous_form = 'ingots/'+material if tier==0 else 'storage_blocks/'+material+('_'+str(tier-1)+'x' if tier>1 else '')
        for registry in ('items','blocks'):
            write(ROOT/'data'/'forge'/'tags'/registry/(form+'.json'), {'replace':False,'values':[{'id':item,'required':False}]})
        required = list(CONDITIONS)
        if material == 'alchemical_brass' and tier == 1:
            required.append({'type':'forge:item_exists','item':previous})
        write(DATA/'recipes/atm'/(name+'.json'), {'type':'minecraft:crafting_shaped','pattern':['III']*3,
            'key':{'I':{'tag':'forge:'+previous_form}},'result':{'item':item},'conditions':required})
        if material != 'alchemical_brass' or tier != 1:
            write(DATA/'recipes/atm'/(name+'_unpack.json'), {'type':'minecraft:crafting_shapeless',
            'ingredients':[{'tag':'forge:'+form}],'result':{'item':previous,'count':9},'conditions':CONDITIONS})
        write(ROOT/'assets'/NS/'blockstates'/(name+'.json'),{'variants':{'':{'model':NS+':block/'+name}}})
        write(ASSETS/'models/block'/(name+'.json'),{'parent':'minecraft:block/cube_all','textures':{'all':NS+':block/'+name}})
        write(ASSETS/'models/item'/(name+'.json'),{'parent':NS+':block/'+name})
        write(DATA/'loot_tables/blocks'/(name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,
            'entries':[{'type':'minecraft:item','name':item}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        image=Image.new('RGB',(16,16),color);draw=ImageDraw.Draw(image)
        for inset in range(tier+1):
            shade=tuple(min(255,c+35-inset*8) for c in color)
            draw.rectangle((inset,inset,15-inset,15-inset),outline=shade)
        for n in range(tier): draw.rectangle((3+n*3,11,4+n*3,13),fill=(225,207,139))
        save(image,ASSETS/'textures/block'/(name+'.png'))
        names['block.'+NS+'.'+name]=(f'{tier}x 压缩{chinese}块' if tier else chinese+'块',
            f'{tier}x Compressed {english} Block' if tier else english+' Block')
        previous=item

for tag in ['mineable/pickaxe','needs_iron_tool']:
    write(ROOT/'data/minecraft/tags/blocks'/(tag+'.json'),{'replace':False,'values':[{'id':x,'required':False} for x in blocks]})

# Preserve the GTM source animation and recolor its paper and stamp.
recolor_items('certificate')
write(ASSETS/'models/item/thaumaturge_certificate.json',{'parent':'minecraft:item/generated',
    'textures':{'layer0':NS+':item/thaumaturge_certificate'}})
names['item.'+NS+'.thaumaturge_certificate']=('神秘使之证','Thaumaturge Certificate')
names['tooltip.'+NS+'.atm_recipe']=('配方受神秘时代影响','Recipe influenced by Thaumcraft')
key = NS+':THAUMATURGE_CERTIFICATE'
certificate_metals = {f'{NS}:{metal}_block_{tier}x': f'forge:storage_blocks/{metal}_{tier}x'
                      for metal, tier in [('void', 3), ('thaumium', 4), ('alchemical_brass', 4)]}
def ingredient(item):
    value = {'tag': certificate_metals[item]} if item in certificate_metals else {'item': item}
    return {'ingredient': value}
def stack(item): return {'kind':'stack','stack':{'item':item,'count':1}}
components = [NS+':void_block_3x', NS+':thaumium_block_4x', NS+':alchemical_brass_block_4x',
    'thaumcraft:focus_hellbat','thaumcraft:liquid_death_bucket','thaumcraft:arcane_bore',
    'thaumcraft:golem_core_essentia','thaumcraft:staff_rod_primal']
optional = [('thaumic_tinkerer','placement_mirror'),('thaumicenergistics','storage_component_256k'),
    ('thaumicbases','node_focus_brightness'),('forbidden_magic','wand_rod_infernal'),
    ('forbidden_magic','wrath_cage'),('automagy','inventarium'),('thaumichorizons','void_putty'),
    ('thaumichorizons','ink_egg')]
recipe = {'type':NS+':certificate','research':key,'instability':30,
    'center':ingredient('thaumcraft:primordial_pearl'),'components':[ingredient(x) for x in components],
    'optional_components':[{'mod':mod,'component':ingredient(mod+':'+item)} for mod,item in optional],
    'aspects':{},'result':{'item':NS+':thaumaturge_certificate'},'conditions':CONDITIONS}
write(DATA/'recipes/atm/thaumaturge_certificate.json',recipe)
write(DATA/'research_catalog/atm.json',{'format':1,'categories':[{'key':'THAUMICOMNIVERSE',
    'icon':NS+':textures/gui/research_category.png','background':'thaumcraft:textures/gui/gui_researchback.png',
    'research':[{'key':key,'category':'THAUMICOMNIVERSE','displayColumn':0,'displayRow':12,'complexity':3,
        'isConcealed':True,'isSecondary':True,'parents':['PRIMPEARL'],'pageCount':2,
        'aspects':{'cognitio':16,'praecantatio':16},'icon_item':{'item':NS+':thaumaturge_certificate'},
        'omniverse_requires':['allthemodium']}]}]})
write(DATA/'research_pages/atm.json',{'format':1,'entries':[{'key':key,'omniverse_requires':['allthemodium'],
    'pages':[{'type':'TEXT','text':'omniverse.page.thaumaturge_certificate'},
        {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'modern','keys':[NS+':atm/thaumaturge_certificate']}}]}]})
# Keep the old book reference available to addon pages, with a representative for each tagged input.
book_recipe = dict(recipe, components=[dict(ingredient(item), display_item=item) if item in certificate_metals
                                       else ingredient(item) for item in components])
write(DATA/'research_page_recipes/atm.json',{'format':1,'entries':[{'key':key,
    'class':'thaumcraft.api.crafting.InfusionRecipe','omniverse_requires':['allthemodium'],
    'omniverse_certificate':book_recipe,'data':{'kind':'infusion','research':key,'instability':30,
        'input':stack('thaumcraft:primordial_pearl'),'output':stack(NS+':thaumaturge_certificate'),
        'ingredients':{'kind':'sequence','elements':[]},'aspects':[]}}]})
names['tc.research_name.'+key]=('神秘使之证','Thaumaturge Certificate')
names['tc.research_text.'+key]=('万象的见证','A testament to every aspect')
names['omniverse.page.thaumaturge_certificate']=(
    '以元始珍珠为中心，汇集神秘学的成果。注魔消耗每种已注册要素 64 点，元始要素各 640 点，风险为 30。周围材料随已安装的附属模组增加。压缩块可用九个低一级方块合成，也可拆回。',
    'Gather the accomplishments of thaumaturgy around a primordial pearl. Infusion costs 64 of every registered aspect, or 640 for primal aspects, with instability 30. Installed addons contribute further components. Combine nine blocks to compress them; compression is reversible.')
for index, language in enumerate(['zh_cn','en_us']):
    path=ASSETS/'lang'/(language+'.json');entries=json.loads(path.read_text(encoding='utf-8'))
    entries.update({key:value[index] for key,value in names.items()});write(path,entries)
print('Generated ATM compressed blocks, reversible recipes and certificate artwork')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
