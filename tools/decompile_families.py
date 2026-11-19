import pathlib,zipfile,subprocess,concurrent.futures
ROOT=pathlib.Path(__file__).resolve().parents[1];BASE=ROOT.parents[1]
GROUPS={
 'Mekanism-1.7.10-9.1.1.1031.jar':['ItemIngot.class','ItemOtherDust.class','BlockOre.class'],
 'NuclearCraft-1.9g--1.7.10.jar':['ItemMaterial.class','ItemPart.class','BlockOre.class','ItemBlockOre.class'],
 'TConstruct-1.7.10-1.8.8.jar':['TinkerTools.class','TinkerSmeltery.class','TinkerWorld.class','TinkerWeaponry.class','ToolMaterial.class','ToolPart.class','MetalPattern.class','WeaponryPattern.class'],
 'Pam\'s HarvestCraft 1.7.10Lb.jar':['ItemRegistry.class','BlockRegistry.class'],
 'WitchingGadgets-1.7.10-1.1.10.jar':['BlockWGStoneDevice.class','BlockWGWoodenDevice.class','ItemBlockStoneDevice.class','ItemBlockWoodenDevice.class','ItemBaubles.class'],
 'Railcraft_1.7.10-9.12.2.0.jar':['ItemBrick.class','EnumBrick.class'],
 'BiomesOPlenty-1.7.10-2.1.0.1889-universal.jar':['BOPBlocks.class','BOPItems.class','ItemBlockSapling.class','ItemBlockColorizedSapling.class','ItemBlockLeaves.class','ItemBlockPlank.class','ItemBlockLog.class','BlockBOPLog.class','BlockBOPLeaves.class','BlockBOPColorizedLeaves.class','ItemBlockFlower.class','ItemBlockFlower2.class','ItemBlockFoliage.class','ItemBlockPlant.class','ItemBOPMisc.class'],
 'Botania r1.8-249.jar':['LibItemNames.class'],
 'forestry_1.7.10-4.2.16.64.jar':['EnumPollenCluster.class','EnumPropolis.class','BlockSoil$SoilType.class'],
 'industrialcraft-2-2.2.828-experimental.jar':['ItemIngot.class','ItemDust.class','BlockMachine.class','BlockMachine2.class','BlockElectric.class','BlockGenerator.class','ItemCell.class','ItemCable.class','ItemCrushedOre.class','ItemPurifiedCrushedOre.class','ItemPlate.class','ItemDensePlate.class','ItemsmallDust.class','ItemDust2.class'],
 'magicbees-1.7.10-2.4.4.jar':['CombType.class','WaxType.class','ResourceType.class','PropolisType.class'],
 'TofuFactoryR-1.3.0-MC1.7.10.jar':['ItemTFDust.class','ItemTofuIngot.class','ItemTFDust$Info.class','ItemTofuIngot$Info.class'],
}
jobs=[]
for jar,classes in GROUPS.items():
 p=BASE/'Collaboration/jars'/jar
 if not p.exists():continue
 with zipfile.ZipFile(p) as z:
  for name in z.namelist():
   if name.rsplit('/',1)[-1] not in classes:continue
   dest=ROOT/'evidence/legacy-classes'/jar[:-4]/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(name));jobs.append(dest)
def run(p):
 subprocess.run(['D:/Program Files/Java/jdk-17.0.5/bin/java.exe','-jar',str(BASE/'Collaboration/tools/cfr-0.152.jar'),str(p),'--outputdir',str(ROOT/'evidence/legacy-source'),'--silent','true'],check=True,capture_output=True)
 print(p.name,flush=True)
with concurrent.futures.ThreadPoolExecutor(4) as pool:list(pool.map(run,jobs))
