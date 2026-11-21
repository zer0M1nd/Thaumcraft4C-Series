from pathlib import Path
import json
base=Path(__file__).resolve().parents[1];root=base/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item):return dict(kind='stack',stack=dict(item=item,count=1))
key='gadomancy:revealer';items=['thaumcraft:goggles','thaumcraft:salis_mundus','thaumcraft:salis_mundus'];aspects=dict(sensus=16,praecantatio=8)
write('data/gadomancy/recipes/revealer.json',dict(type='thaumcraft:infusion',research=key,instability=4,enchantment=key,aspects=aspects,components=[dict(ingredient=dict(item=item)) for item in items]))
write('data/gadomancy/research_catalog/revealer.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=key,category='GADOMANCY',displayColumn=1,displayRow=3,pageCount=2,complexity=1,aspects=dict(sensus=4,praecantatio=4),parents=['INFUSIONENCHANTMENT','GOGGLES'],isConcealed=True,icon_item=dict(item='thaumcraft:goggles',nbt='{Enchantments:[{id:"minecraft:protection",lvl:1s}]}'))])]))
write('data/gadomancy/research_pages/revealer.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='gadomancy.research_page.REVEALER.1'),dict(type='INFUSION_ENCHANTMENT',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))])]))
write('data/gadomancy/research_page_recipes/revealer.json',dict(format=1,entries=[dict(key=key,**{'class':'modern'},data=dict(kind='infusion_enchantment',research=key,enchantment=dict(id=0,name='gadomancy.enchantment.revealer',max_level=1),recipeXP=5,ingredients=dict(kind='sequence',elements=[stack(item) for item in items]),aspects=aspects,instability=4))]))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/gadomancy/lang/'+lang+'.json'].update({'tc.research_name.'+key:'tc.research_name.GADOMANCY.REVEALER','tc.research_text.'+key:'tc.research_text.GADOMANCY.REVEALER'})
write('data/gadomancy/thaumcraft/original_resources.json',data)
path=base/'src/main/java/gadomancymodern/Gadomancy.java';text=path.read_text()
if ' REVEALER=' not in text:
 text=text.replace('    public static final java.util.Map<String,RegistryObject<Item>> AURA_CORES=', '    public static final DeferredRegister<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,MOD_ID);\n    public static final RegistryObject<gadomancymodern.enchantment.RevealerEnchantment> REVEALER=ENCHANTMENTS.register("revealer",gadomancymodern.enchantment.RevealerEnchantment::new);\n    public static final java.util.Map<String,RegistryObject<Item>> AURA_CORES=').replace('RECIPES.register(bus);','RECIPES.register(bus);ENCHANTMENTS.register(bus);')
 path.write_text(text,encoding='utf8')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
