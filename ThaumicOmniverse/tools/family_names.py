"""Recover metadata names from the audited legacy class definitions (not modern TC integrations)."""
import pathlib,re
ROOT=pathlib.Path(__file__).resolve().parents[1];BASE=ROOT.parents[1]
def array(text,var):
 m=re.search(r'\b'+re.escape(var)+r'\s*=\s*new String\[\]\s*\{([^}]+)\}',text)
 return re.findall(r'"([^"]*)"',m.group(1)) if m else []
def families():
 result={}
 def arrays(id,source,var,transform=lambda s:s):
  p=ROOT/'evidence/legacy-source'/source
  if p.exists():
   for i,n in enumerate(array(p.read_text(encoding='utf-8'),var)):result[(id,str(i))]=transform(n)
 def switches(id,source):
  p=ROOT/'evidence/legacy-source'/source
  if p.exists():
   for i,n in re.findall(r'case (\d+):\s*\{?\s*return "([^"]+)";',p.read_text(encoding='utf-8')):result[(id,i)]=n
 arrays('Mekanism:Ingot','mekanism/common/item/ItemIngot.java','en_USNames',lambda n:n.lower()+'Ingot')
 arrays('Mekanism:OtherDust','mekanism/common/item/ItemOtherDust.java','subtypes',lambda n:n.lower()+'Dust')
 switches('NuclearCraft:material','nc/item/ItemMaterial.java')
 switches('NuclearCraft:blockOre','nc/itemblock/basic/ItemBlockOre.java')
 switches('NuclearCraft:parts','nc/item/ItemPart.java')
 arrays('TConstruct:materials','tconstruct/tools/TinkerTools.java','materialStrings')
 arrays('TConstruct:metalPattern','tconstruct/tools/TinkerTools.java','patternTypes')
 arrays('TConstruct:woodPattern','tconstruct/tools/TinkerTools.java','patternTypes')
 arrays('Botania:manaResource','vazkii/botania/common/lib/LibItemNames.java','MANA_RESOURCE_NAMES')
 for id,file,constructor in [('TofuCraft:foodSet','ItemFoodSet.java','Food'),('TofuCraft:materials','ItemTcMaterials.java','TcMaterial')]:
  p=ROOT/'evidence/legacy-source/tsuteto/tofu/item'/file
  if p.exists():
   for meta,name in re.findall(r'new '+constructor+r'\((\d+),[^;\n]*?"([^"]+)"',p.read_text(encoding='utf-8')):result[(id,meta)]=name
 for id,file,var,suffix in [
  ('BiomesOPlenty:saplings','ItemBlockSapling.java','saplings',' Sapling'),
  ('BiomesOPlenty:colorizedSaplings','ItemBlockColorizedSapling.java','saplings',' Sapling'),
  ('BiomesOPlenty:flowers','ItemBlockFlower.java','plants',''),
  ('BiomesOPlenty:flowers2','ItemBlockFlower2.java','plants',''),
  ('BiomesOPlenty:foliage','ItemBlockFoliage.java','foliageTypes',''),
  ('BiomesOPlenty:plants','ItemBlockPlant.java','plants','')]:
  arrays(id,'biomesoplenty/common/itemblocks/'+file,var,lambda n:n+suffix)
 arrays('BiomesOPlenty:misc','biomesoplenty/common/items/ItemBOPMisc.java','items')
 # AE2 enum IDs are explicit in the legacy binaries. Resolve their actual old translations.
 ae=BASE/'Collaboration/decompiled/appliedenergistics2-rv3-beta-6/appeng'
 for suffix,file in [('ItemMultiMaterial','items/materials/MaterialType.java'),('ItemMultiPart','items/parts/PartType.java')]:
  for name,meta in re.findall(r'^\s*(\w+)\((\d+)[,)]',(ae/file).read_text(encoding='utf-8'),re.M):
   result[('appliedenergistics2:item.'+suffix,meta)]='appliedenergistics2.'+('ItemMaterial.' if suffix=='ItemMultiMaterial' else 'ItemPart.')+name
 gems=BASE/'Collaboration/decompiled/SilentGems-1.7.10-1.3.14-172/net/silentchaos512/gems/lib/EnumGem.java'
 for name,meta in re.findall(r'\w+\((\d+), "([^"]+)"\)',gems.read_text(encoding='utf-8')):
  # swapped below intentionally named by tuple position
  result[('SilentGems:Gem',name)]=meta
  result[('SilentGems:GemOre',name)]=meta+' Ore'
 # CoFH metadata registration explicitly names each material.
 tf=BASE/'Collaboration/decompiled/ThermalFoundation-[1.7.10]1.2.6-118/cofh/thermalfoundation/item/TFItems.java'
 for meta,name in re.findall(r'itemMaterial\.add(?:OreDict)?Item\((\d+), "([^"]+)"',tf.read_text(encoding='utf-8')):result[('ThermalFoundation:material',meta)]=name
 for id,file in [('itemIngot','ItemIngot'),('itemDust','ItemDust'),('itemDust2','ItemDust2'),('itemDustSmall','ItemsmallDust'),('itemCrushedOre','ItemCrushedOre'),('itemPurifiedCrushedOre','ItemPurifiedCrushedOre'),('itemPlates','ItemPlate'),('itemDensePlates','ItemDensePlate')]:
  p=ROOT/'evidence/legacy-source/ic2/core/item/resources'/(file+'.java')
  if p.exists():
   for meta,key in re.findall(r'case (\d+):\s*\{\s*ret = InternalName\.(\w+);',p.read_text(encoding='utf-8')):result[('IC2:'+id,meta)]='ic2.'+key
 return result
FAMILIES=families()
def names(id,meta,lang):
 n=FAMILIES.get((id,meta))
 if not n:return set()
 candidates={n};normalized=lambda k:re.sub('[^a-z0-9]','',k.lower())
 # Match the unlocalized key, allowing the mod's exact namespace prefix only.
 keys={normalized(n),normalized(n.removeprefix('ic2.')),normalized(id.split(':')[0]+':'+n)}
 for k,v in lang.items():
  root=k[:-5] if k.endswith('.name') else k
  for prefix in ['item.','tile.']:
   if root.startswith(prefix):root=root[len(prefix):]
  if normalized(root) in keys:candidates.add(v)
 return candidates
