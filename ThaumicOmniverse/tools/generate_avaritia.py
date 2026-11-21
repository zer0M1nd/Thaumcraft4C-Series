"""Port the reviewed original Avaritia TC4 integration into Re-Avaritia resources."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

BASE = Path(__file__).resolve().parents[1]
ROOT = BASE / 'src/main/resources'
NS = 'thaumicomniverse'
DATA = ROOT / 'data' / NS
ASSETS = ROOT / 'assets' / NS
COND = [{'type':'forge:mod_loaded','modid':'avaritia'}]
def write(path, value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def merge_definition(kind, row):
    p=DATA/'thaumcraft'/(kind+'.json')
    rows=json.loads(p.read_text(encoding='utf-8')) if p.exists() else []
    write(p,[x for x in rows if x['tag']!=row['tag']]+[row])

# Registration accepts at most one million; the scoped rod-capacity hook restores the legacy
# 9,999,999 whole-vis capacity at use time, on both sides. Preserve full refill each tick.
merge_definition('wand_rods',{'tag':NS+':infinity','capacity':1000000,'craftCost':9999,'research':'',
    'glowing':True,'texture':NS+':textures/models/wand_rod_neutronium.png','required_mods':['avaritia']})
merge_definition('wand_caps',{'tag':NS+':matrix','cost':0,'craftCost':1000,'research':'',
    'texture':NS+':textures/models/wand_cap_crystal.png','required_mods':['avaritia']})
write(DATA/'recipes/collab/infinity_wand.json',{'type':'avaritia:shaped_table','tier':4,'category':'equipment',
    'pattern':['      C  ','     CIC ','     NIIC','    NINC ','   NXN   ',' CNIN    ','CIIN     ',' CIC     ','  C      '],
    'key':{k:{'item':'avaritia:'+v} for k,v in {'I':'infinity_ingot','C':'crystal_matrix_ingot','N':'neutron_ingot','X':'crystal_matrix'}.items()},
    'result':{'item':'thaumcraft:casting_wand','nbt':{'rod':NS+':infinity','cap':NS+':matrix','tc4_metadata':9000}},'conditions':COND})

catalog,pages,displays=[],[],[]
specs=[('extremely_primordial_pearl','BIG_PEARL','PRIMPEARL','thaumcraft:primordial_pearl',
    ['thaumcraft:primordial_pearl']*7,{'alienis':64,'praecantatio':64,'aqua':64,'ignis':64,'terra':64,'aer':64,'ordo':64,'perditio':64,NS+':terminus':32}),
    ('akashic_record','AKASHIC',NS+':BIG_PEARL','thaumcraft:knowledge_fragment',['avaritia:infinity_ingot']*4,
    {'cognitio':64,'praecantatio':64,NS+':terminus':32})]
for index,(item,short,parent,center,components,aspects) in enumerate(specs):
    key=NS+':'+short
    write(DATA/'recipes/collab'/(item+'.json'),{'type':'thaumcraft:infusion','research':key,'instability':12,
        'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':x}} for x in components],
        'aspects':aspects,'result':{'item':NS+':'+item},'conditions':COND})
    catalog.append({'key':key,'category':'THAUMICOMNIVERSE','displayColumn':index*4,'displayRow':8,
        'complexity':3,'isConcealed':True,'isSecondary':True,'parents':[parent],'pageCount':2,
        'aspects':{NS+':terminus':8,'praecantatio' if index==0 else 'cognitio':16},
        'icon_item':{'item':NS+':'+item},'omniverse_requires':['avaritia']})
    pages.append({'key':key,'omniverse_requires':['avaritia'],'pages':[{'type':'TEXT','text':'omniverse.page.'+item},
        {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[key]}}]})
    def stack(item): return {'kind':'stack','stack':{'item':item,'count':1}}
    displays.append({'key':key,'class':'thaumcraft.api.crafting.InfusionRecipe','omniverse_requires':['avaritia'],
        'data':{'kind':'infusion','research':key,'instability':12,'input':stack(center),'output':stack(NS+':'+item),
                'ingredients':{'kind':'sequence','elements':[stack(x) for x in components]},
                'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]}})
write(DATA/'research_catalog/avaritia.json',{'format':1,'categories':[{'key':'THAUMICOMNIVERSE',
    'icon':NS+':textures/gui/research_category.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':catalog}]})
write(DATA/'research_pages/avaritia.json',{'format':1,'entries':pages})
write(DATA/'research_page_recipes/avaritia.json',{'format':1,'entries':displays})
source=BASE.parents[1]/'Collaboration/jars/Avaritia-1.7.10-1.31.jar'
provenance=[]
with ZipFile(source) as z:
    for dest,original in {'item/akashic_record':'items/akashic_record','item/extremely_primordial_pearl':'items/big_pearl',
                          'models/wand_rod_neutronium':'models/wand_rod_neutronium','models/wand_cap_crystal':'models/wand_cap_crystal'}.items():
        for extension in ['.png','.png.mcmeta']:
            entry='assets/avaritia/textures/'+original+extension
            if entry not in z.namelist():continue
            p=ASSETS/'textures'/(dest+extension);p.parent.mkdir(parents=True,exist_ok=True)
            content=z.read(entry);p.write_bytes(content)
            provenance.append({'source':str(source),'entry':entry,'target':str(p.relative_to(ROOT)),'sha256':hashlib.sha256(content).hexdigest()})
for item in ['akashic_record','extremely_primordial_pearl']:
    write(ASSETS/'models/item'/(item+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+item}})
write(BASE/'evidence/avaritia-artwork.json',provenance)
texts={
    'item.'+NS+'.akashic_record':('阿卡夏记录','Akashic Record'),
    'item.'+NS+'.extremely_primordial_pearl':('极始珍珠','Extremely Primordial Pearl'),
    'tc.research_name.'+NS+':BIG_PEARL':('极始珍珠','Extremely Primordial Pearl'),
    'tc.research_name.'+NS+':AKASHIC':('阿卡夏记录','Akashic Record'),
    'tc.research_text.'+NS+':BIG_PEARL':('终焉的起源','An ultimate beginning'),
    'tc.research_text.'+NS+':AKASHIC':('无尽的知识','Unbounded knowledge'),
    'item.Wand.'+NS+':blood.cap':('血魔','Blood'),
    'item.Wand.'+NS+':matrix.cap':('水晶矩阵','Crystal Matrix'),
    'item.Wand.'+NS+':infinity.rod':('中子','Neutronium'),
    'omniverse.page.extremely_primordial_pearl':('将八枚元始珍珠的力量融合，得到极始珍珠。无尽催化剂的合成也需要它，但合成后珍珠会保留。',
        'Combine the power of eight primordial pearls. The resulting pearl is also required for infinity catalysts and remains after crafting.'),
    'omniverse.page.akashic_record':('使用阿卡夏记录可以为每一种已注册要素增加 999 研究点数。非创造模式下，记录会被消耗。',
        'Use an Akashic Record to gain 999 research points in every registered aspect. The record is consumed outside Creative mode.')}
for index,language in enumerate(['zh_cn','en_us']):
    p=ASSETS/'lang'/(language+'.json');data=json.loads(p.read_text(encoding='utf-8'))
    data.update({k:v[index] for k,v in texts.items()});write(p,data)
print('Generated Avaritia research, original recipes, wand definitions and artwork')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
