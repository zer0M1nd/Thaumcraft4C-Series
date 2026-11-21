import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,value):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
def text(key):return {'type':'TEXT','text':key}
def infusion(key):return {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:{key}']}}
slot=f'{MOD}:keystone_slot'
write(f'data/{MOD}/recipes/infusion/keystone_slot.json',{'type':'thaumcraft:infusion','research':'planarKeystone','instability':8,'aspects':{'vacuos':32,'iter':24,'machina':24,'alienis':24},'center':{'ingredient':{'item':f'{MOD}:planar_conduit'}},'components':[{'ingredient':{'item':i}} for i in ['minecraft:gold_ingot','thaumcraft:void_ingot']*4],'result':{'item':slot}})
display=[]
for key in ['keystone','keystone_alternate','keystone_slot']:
 recipe=read(f'data/{MOD}/recipes/infusion/{key}.json')
 display.append({'key':f'{MOD}:{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':recipe['research'],'instability':recipe['instability'],'aspects':recipe['aspects'],'input':stack(recipe['center']['ingredient']['item']),'output':stack(recipe['result']['item']),'ingredients':{'kind':'sequence','elements':[stack(i['ingredient']['item']) for i in recipe['components']]}}})
catalog=[];pages=[]
def entry(key,x,y,complexity,aspects,parents,icon,content,warp=0,hidden=None,special=False):
 row={'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()],'parents':parents,'isConcealed':True,'warp':warp,'pageCount':len(content)}
 row['icon_resource' if icon.endswith('.png') else 'icon_item']=icon if icon.endswith('.png') else {'item':icon}
 if hidden:row['parentsHidden']=hidden
 if special:row['isSpecial']=True
 catalog.append(row);pages.append({'key':key,'pages':content})
# Original prerequisite restored after the energized-node lease API became available.
entry('planarRift',14,8,2,{'vacuos':4,'praecantatio':4,'alienis':2},['vortexStabilizer','transductionAmplifier'],f'{MOD}:textures/misc/rift.png',[text(f'planarRift{i}') for i in range(1,4)],3,special=True)
entry('wispSpawn',15,6,1,{'praecantatio':2,'vitreus':2,'victus':2},['planarRift'],f'{MOD}:textures/misc/wisp.png',[text('wispSpawn1')])
entry('voidGolem',16,7,1,{'vacuos':2,'motus':2,'spiritus':2,'humanus':2},['wispSpawn'],f'{MOD}:textures/misc/voidgolem.png',[text('voidGolem1')],1,['golemPowder'])
entry('pocketPlane',18,8,3,{'iter':24,'vacuos':21,'fabrico':18,'auram':15,'machina':21,'alienis':6,'praecantatio':6,'cognitio':3},['planarRift','PRIMPEARL'],f'{MOD}:textures/misc/pocketplane.png',[text(f'pocketPlane{i}') for i in range(1,4)]+[infusion('keystone'),text(f'{MOD}.plane.creation')],4)
entry('planarKeystone',20,8,3,{'iter':15,'vacuos':12,'auram':9,'alienis':6,'praecantatio':3},['pocketPlane'],slot,[text('planarKeystone1'),infusion('keystone_alternate'),text('planarKeystone2'),infusion('keystone_slot'),text(f'{MOD}.gateway.structure')],5)
write(f'data/{MOD}/research_catalog/planes.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':catalog}]})
write(f'data/{MOD}/research_pages/planes.json',{'format':1,'entries':pages})
write(f'data/{MOD}/research_page_recipes/planes.json',{'format':1,'entries':display})
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 for icon in ['rift','wisp','voidgolem','pocketplane']:
  path=f'assets/{MOD}/textures/misc/{icon}.png';assert path in jar.namelist();manifest['mapping'][path]=path
for lang in manifest['languages'].values():
 for row in catalog:
  for prefix in ['tc.research_name.','tc.research_text.']:lang[prefix+row['key']]=prefix+row['key']
 for row in pages:
  for page in row['pages']:
   if page['type']=='TEXT' and not page['text'].startswith(MOD+'.'):lang[page['text']]=page['text']
write(p,manifest)
translations={
 'en_us':{
  f'{MOD}.plane.creation':'Throw one Primordial Pearl into a formed rift held by six stabilizer beams. A named pearl names the plane. Generation runs in stages and retains the pearl until the central return portal is ready. A failed job can return its pearl when the source is used with a wand; removing the unfinished source also queues a refund. Extra pearls are not consumed. Touch the completed rift to enter. Use a blank keystone on the return rift inside the plane to bind it.',
  f'{MOD}.gateway.structure':'Build a vertical 5 by 5 frame along X or Z: four advanced alchemical constructs at the corners, a keystone slot at the top centre, and eleven eldritch stone blocks along the remaining edges. Leave the 3 by 3 centre empty. Insert a bound keystone and use a wand with 250 of every primal vis (crafting discounts apply). Each plane supports four simultaneous gateways. Empty-hand use removes the keystone and closes both ends. Breaking the slot returns the original key. A damaged frame closes the link.'},
 'zh_cn':{
  f'{MOD}.plane.creation':'向已经成形且由六束稳定器光束维持的裂隙投入一颗原始珍珠。命名珍珠会为位面命名。生成分阶段进行，中央返回门户完成前珍珠保存在来源裂隙中；任务失败时用法杖取回，拆除未完成的来源也会安排退还。额外珍珠不会消耗。接触完成的裂隙即可进入；在位面内对返回裂隙使用空白钥石进行绑定。',
  f'{MOD}.gateway.structure':'沿X或Z轴建造竖直5×5框架：四角使用高级炼金构件，顶部中央放钥石插槽，其余11个边框使用邪术石，中央3×3留空。插入绑定钥石后用法杖开启，需要每种基础要素250灵力，可应用制作折扣。每个位面最多同时连接四座网关。空手使用插槽取出钥石并关闭两端；拆除插槽返还原钥石，破坏框架也会关闭连接。'}}
for lang,values in translations.items():
 p=f'assets/{MOD}/lang/{lang}.json';data=read(p);data.update(values);write(p,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
