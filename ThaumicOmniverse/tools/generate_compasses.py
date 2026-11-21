"""Authored compass artwork, infusion recipes and matching research display definitions."""
import json
from pathlib import Path
from generate_item_textures import generate as recolor_items

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
NS = 'thaumicomniverse'
DATA = ROOT / 'data' / NS
ASSETS = ROOT / 'assets' / NS

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

catalog, pages, displays = [], [], []
texts = {}
for primal in [False, True]:
    name = 'primal_compass' if primal else 'sinister_compass'
    title = ('元始指南针','Primal Compass') if primal else ('凶险指南针','Sinister Compass')
    key = NS + ':' + name.upper()
    components = ['thaumcraft:primordial_pearl', 'thaumcraft:void_ingot', 'thaumcraft:primal_charm', 'thaumcraft:void_ingot'] if primal else [
        'thaumcraft:sinister_stone', 'thaumcraft:thaumium_ingot', 'thaumcraft:ender_pearl', 'thaumcraft:thaumium_ingot']
    # Ender pearls are vanilla; use the real modern ID.
    components = [x.replace('thaumcraft:ender_pearl','minecraft:ender_pearl') for x in components]
    center = NS + ':sinister_compass' if primal else 'minecraft:compass'
    aspects = {'alienis':32,'sensus':32,'cognitio':16,'iter':32} if primal else {'alienis':16,'sensus':16,'iter':16}
    recipe = {'type':'thaumcraft:infusion','center':{'ingredient':{'item':center}},
              'components':[{'ingredient':{'item':x}} for x in components], 'aspects':aspects,
              'instability':8 if primal else 4,'research':key,'result':{'item':NS + ':' + name}}
    write(DATA / 'recipes/collab' / (name + '.json'), recipe)
    def stack(item): return {'kind':'stack','stack':{'item':item,'count':1}}
    displays.append({'key':key,'class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion',
        'research':key,'instability':recipe['instability'],'input':stack(center),'output':stack(NS + ':' + name),
        'ingredients':{'kind':'sequence','elements':[stack(x) for x in components]},
        'aspects':[{'aspect':x,'amount':n} for x,n in aspects.items()]}})
    catalog.append({'key':key,'category':'THAUMICOMNIVERSE','displayColumn':4 if primal else 0,'displayRow':4,
        'pageCount':2,'complexity':3 if primal else 2,'parents':[NS + ':SINISTER_COMPASS'] if primal else ['INFUSION'],
        'aspects':{'alienis':6,'sensus':6,'iter':6},'icon_item':{'item':NS + ':' + name}})
    pages.append({'key':key,'pages':[{'type':'TEXT','text':'omniverse.page.' + name},
        {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[key]}}]})
    texts['item.' + NS + '.' + name] = title
    texts['tc.research_name.' + key] = title
    texts['tc.research_text.' + key] = ('寻找未征服的邪术方尖碑','Find unconquered eldritch obelisks')
# Vanilla-aligned frames and independent primal color cycling.
recolor_items('compasses')

write(DATA / 'research_catalog/compasses.json', {'format':1,'categories':[{'key':'THAUMICOMNIVERSE',
    'icon':NS + ':textures/gui/research_category.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':catalog}]})
write(DATA / 'research_pages/compasses.json', {'format':1,'entries':pages})
write(DATA / 'research_page_recipes/compasses.json', {'format':1,'entries':displays})
texts.update({
    'omniverse.compass.any':('任意种类','Any boss'),
    'omniverse.compass.mode':('搜索目标：%s','Search target: %s'),
    'omniverse.compass.distance':('邪术方尖碑：%s 格 · %s','Eldritch obelisk: %s blocks · %s'),
    'omniverse.compass.searching':('正在寻找未征服的邪术方尖碑……','Searching for an unconquered eldritch obelisk…'),
    'omniverse.compass.none':('此维度没有可寻找的邪术方尖碑','No searchable eldritch obelisks in this dimension'),
    'omniverse.page.sinister_compass':(
        '凶险磁石可以感知通往外域的裂隙。手持指南针即可逐步寻找当前维度最近的未征服方尖碑，并显示水平距离。指针在找到目标前会旋转。击败迷宫内掉落元始珍珠的首领后，该迷宫对应的方尖碑对所有玩家标记为已征服。',
        'A sinister stone senses rifts into the Outer Lands. Hold this compass to locate the nearest unconquered obelisk in your dimension and display its horizontal distance. The needle spins while searching. Defeating a pearl-bearing maze boss marks its obelisk conquered for everyone.'),
    'omniverse.page.primal_compass':(
        '元始珍珠令指南针能够辨识方尖碑对应的守卫。右键依次切换任意种类、邪术傀儡、邪术守卫、血腥教团和污秽。切换后会重新寻找最近的符合条件且尚未征服的方尖碑。',
        'A primordial pearl lets the compass distinguish an obelisk’s guardian. Use it to cycle between any boss, golem, warden, crimson cult and taint. Changing the filter starts a new search for the nearest matching unconquered obelisk.')
})
for index, language in enumerate(['zh_cn','en_us']):
    path = ASSETS / 'lang' / (language + '.json')
    values = json.loads(path.read_text(encoding='utf-8'))
    values.update({key:value[index] for key,value in texts.items()}); write(path,values)
print('Generated compass frames, recipes, research and translations')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
