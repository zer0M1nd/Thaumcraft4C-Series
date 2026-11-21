package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import thaumcraft.api.golems.GolemApi;
import thaumcraft.api.nodes.NodeApi;
import thaumcraft.api.aspects.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class AddonExpansionServerTests {
    private static ResourceLocation id(String path){return ResourceLocation.parse("api_test:"+path);}
    @GameTest(template="wand_test_empty")
    public static void extensionApiGolemMaterialTaskAndPersistence(GameTestHelper test){
        var material=id("block_material");var task=id("worker");var calls=new java.util.concurrent.atomic.AtomicInteger();
        if(GolemApi.material(material).isEmpty())GolemApi.registerMaterial(material,new GolemApi.Material(80,17,.5f,true,5,48,30,7,true,id("textures/golem.png")));
        if(GolemApi.task(task).isEmpty())GolemApi.registerTask(task,(g,w,t)->w.addGoal(1,new net.minecraft.world.entity.ai.goal.Goal(){public boolean canUse(){return true;}public boolean requiresUpdateEveryTick(){return true;}public void tick(){var data=g.configuration().addonData(id("task_data"));data.putInt("ticks",data.getInt("ticks")+1);GolemApi.data(g,id("task_data"),data);}}));
        var golem=TCEntities.GOLEM.get().create(test.getLevel());var position=test.absolutePos(new BlockPos(2,2,2));golem.moveTo(position.getCenter());golem.state().home(position,Direction.UP);test.getLevel().addFreshEntity(golem);
        var skin=Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING,Direction.WEST);GolemApi.configure(golem,material,task,skin);
        test.assertTrue(golem.getMaxHealth()==80&&golem.getArmorValue()==17&&golem.fireImmune()&&golem.configuration().carryLimit()==48&&golem.configuration().upgrades().length==5,"Custom material drives real entity statistics and inventory capacity");
        var payload=new CompoundTag();payload.putString("source","block payload");GolemApi.data(golem,id("payload"),payload);payload.putString("source","mutated");
        golem.configuration().installUpgrade(0);golem.configuration().installUpgrade(1);golem.configuration().installUpgrade(2);golem.configuration().installUpgrade(3);golem.configuration().installUpgrade(4);
        var saved=new CompoundTag();golem.state().save(saved);var loaded=new GolemState(()->{});loaded.load(saved);
        test.assertTrue(loaded.configuration().customMaterial().equals(material)&&loaded.configuration().customTask().equals(task)&&loaded.configuration().blockAppearance().equals(skin)&&loaded.configuration().upgrades()[4]==4,"Stable IDs, block properties and expanded upgrades survive save/load");
        test.assertTrue(loaded.configuration().addonData(id("payload")).getString("source").equals("block payload"),"Addon payload is copied, not shared");
        var portable=new net.minecraft.world.item.ItemStack(TCItems.golem(GolemMaterial.WOOD));
        var itemTag=portable.getOrCreateTag();golem.configuration().saveExtension(itemTag);itemTag.putByteArray("upgrades",golem.configuration().upgrades());
        var replacement=TCEntities.GOLEM.get().create(test.getLevel());GolemPlacerItem.restore(replacement,portable,GolemMaterial.WOOD,position,Direction.UP,"test");
        test.assertTrue(replacement.configuration().upgrades()[4]==4&&replacement.configuration().blockAppearance().equals(skin),"Portable golem restores custom properties and all upgrade slots");
        test.runAfterDelay(8,()->{test.assertTrue(golem.configuration().addonData(id("task_data")).getInt("ticks")>0,"Registered task actually executes through entity AI");golem.discard();test.succeed();});
    }
    @GameTest(template="wand_test_empty")
    public static void extensionApiNodePermanentTemporaryAndExpiry(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));test.getLevel().setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeBlockEntity)test.getLevel().getBlockEntity(pos);
        NodeApi.configure(node,new AspectList().add(Aspect.AIR,10),AuraNodeType.NORMAL,null,false);NodeApi.extract(node,Aspect.AIR,6,false);
        NodeApi.influence(node,id("amp_a"),new NodeApi.Influence(AuraNodeType.PURE,true,AuraNodeModifier.BRIGHT,2,3,10),100);
        test.assertTrue(node.base().amount(Aspect.AIR.unwrap())==20&&node.aspects().amount(Aspect.AIR.unwrap())==4&&node.nodeType()==AuraNodeType.PURE,"Temporary boost changes capacity/type without refilling");
        NodeApi.edit(node,new AspectList().add(Aspect.AIR,15),AuraNodeType.NORMAL,AuraNodeModifier.PALE);
        test.assertTrue(node.base().amount(Aspect.AIR.unwrap())==30&&NodeApi.getBase(node).getAmount(Aspect.AIR)==15,"Permanent edit under active modifier changes baseline independently");
        NodeApi.influence(node,id("amp_b"),new NodeApi.Influence(AuraNodeType.NORMAL,true,null,1,1,20),100);
        test.assertTrue(node.nodeType()==AuraNodeType.NORMAL&&node.nodeModifier()==null,"Higher priority can explicitly restore normal quality");NodeApi.removeInfluence(node,id("amp_b"));
        test.assertTrue(node.nodeModifier()==AuraNodeModifier.BRIGHT,"Removing one provider preserves the other");
        var disk=node.saveWithoutMetadata();test.assertTrue(disk.getByte("modifier")==AuraNodeModifier.PALE.ordinal(),"Disk stores permanent quality, not a temporary override");
        test.assertTrue(node.getUpdateTag().getByte("modifier")==AuraNodeModifier.BRIGHT.ordinal(),"Clients receive effective quality");
        NodeApi.removeInfluence(node,id("amp_a"));test.assertTrue(node.nodeModifier()==AuraNodeModifier.PALE&&node.base().amount(Aspect.AIR.unwrap())==15&&node.aspects().amount(Aspect.AIR.unwrap())==4,"Removal reveals latest permanent settings without restoring stale charge");
        NodeApi.influence(node,id("short"),new NodeApi.Influence(null,false,null,2,1,0),2);test.assertTrue(NodeApi.insert(node,Aspect.AIR,100,true)==26&&node.aspects().amount(Aspect.AIR.unwrap())==4,"Simulated charging is pure");NodeApi.insert(node,Aspect.AIR,100,false);
        test.runAfterDelay(4,()->{NodeApi.extract(node,Aspect.AIR,0,true);test.assertTrue(node.base().amount(Aspect.AIR.unwrap())==15&&node.aspects().amount(Aspect.AIR.unwrap())==15,"Expired amplifier clamps surplus, cannot leave permanent boosted capacity");test.getLevel().removeBlock(pos,false);test.succeed();});
    }
    @GameTest(template="wand_test_empty")
    public static void extensionApiEssentiaSidesSimulationAndReplacement(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var target=pos.east(2);level.setBlock(pos,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);level.setBlock(target,TCArcane.VOID_JAR.get().defaultBlockState(),3);
        var air=Aspect.AIR.unwrap();var fire=Aspect.FIRE.unwrap();var jar=(EssentiaJarBlockEntity)level.getBlockEntity(pos);jar.label(air,Direction.NORTH);
        var source=EssentiaStorageApi.at(level,pos,Direction.UP);var destination=EssentiaStorageApi.at(level,target,Direction.UP);
        test.assertTrue(source.insert(air,100,true)==64&&jar.amount()==0&&source.insert(fire,1,true)==0,"Simulation respects capacity/filter without mutation");
        test.assertTrue(EssentiaStorageApi.at(level,pos,Direction.DOWN).insert(air,10,false)==0,"Cannot bypass sided access");source.insert(air,50,false);
        var transfer=EssentiaStorageApi.transfer(source,destination,air,40);test.assertTrue(transfer.inserted()==40&&transfer.remainder()==0&&source.contents().amount(air)==10&&destination.contents().amount(air)==40,"Cross-container transfer conserves units");
        test.assertTrue(destination.insert(air,100,false)==24&&destination.insert(air,1,true)==0,"Network storage insertion never destroys overflow in void jars");
        level.removeBlockEntity(pos);level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);test.assertTrue(source.extract(air,20,false)==0&&source.contents().isEmpty(),"Old handles do not drain a removed jar");
        level.setBlock(pos,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);test.assertTrue(source.insert(fire,7,false)==7&&source.contents().amount(fire)==7,"Same handle re-resolves replacement storage");
        var copy=source.contents();copy.add(fire,90);test.assertTrue(source.contents().amount(fire)==7,"Snapshots cannot mutate storage");level.removeBlock(pos,false);level.removeBlock(target,false);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void extensionApiEssentiaRejectedCommitRetainsRemainder(GameTestHelper test){
        var air=Aspect.AIR.unwrap();var amount=new int[]{20};
        IEssentiaStorage source=new IEssentiaStorage(){public tc4revival.aspect.AspectAmounts contents(){return new tc4revival.aspect.AspectAmounts().add(air,amount[0]);}public int capacity(tc4revival.aspect.Aspect a){return 20;}public int insert(tc4revival.aspect.Aspect a,int n,boolean simulate){return 0;}public int extract(tc4revival.aspect.Aspect a,int n,boolean simulate){int result=Math.min(amount[0],n);if(!simulate)amount[0]-=result;return result;}};
        IEssentiaStorage changedTarget=new IEssentiaStorage(){public tc4revival.aspect.AspectAmounts contents(){return new tc4revival.aspect.AspectAmounts();}public int capacity(tc4revival.aspect.Aspect a){return 20;}public int insert(tc4revival.aspect.Aspect a,int n,boolean simulate){return simulate?n:0;}public int extract(tc4revival.aspect.Aspect a,int n,boolean simulate){return 0;}};
        var result=EssentiaStorageApi.transfer(source,changedTarget,air,8);test.assertTrue(result.inserted()==0&&result.remainder()==8&&amount[0]+result.remainder()==20,"Rejected commit returns caller-owned in-transit essentia instead of silently losing it");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void extensionApiTemporaryNodeConversionAndBuffer(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);
        NodeApi.configure(node,new AspectList().add(Aspect.AIR,10),AuraNodeType.NORMAL,AuraNodeModifier.PALE,false);NodeApi.influence(node,id("convert_amp"),new NodeApi.Influence(AuraNodeType.PURE,true,AuraNodeModifier.BRIGHT,3,2,1),100);NodeApi.insert(node,Aspect.AIR,100,false);
        test.assertTrue(node.portableCharge().amount(Aspect.AIR.unwrap())==10,"Portable payload excludes amplified surplus");var saved=node.saveWithoutMetadata();var restored=new AuraNodeBlockEntity(pos,node.getBlockState());restored.load(saved);test.assertTrue(restored.base().amount(Aspect.AIR.unwrap())==10&&restored.aspects().amount(Aspect.AIR.unwrap())==10&&restored.nodeModifier()==AuraNodeModifier.PALE,"Reload cannot freeze amplifier capacity, quality or charge");
        level.setBlock(pos.above(),TCArcane.NODE_CONVERTER.get().defaultBlockState(),3);level.setBlock(pos.below(),TCArcane.NODE_STABILIZER.get().defaultBlockState(),3);level.setBlock(pos.above().east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);var converter=(NodeConverterBlockEntity)level.getBlockEntity(pos.above());var data=new CompoundTag();data.putInt("count",1000);data.putInt("status",1);converter.load(data);converter.tick();var result=(EnergizedNodeBlockEntity)level.getBlockEntity(pos);
        test.assertTrue(result.aura().amount(Aspect.AIR.unwrap())==10&&result.nodeModifier()==AuraNodeModifier.PALE&&result.nodeType()==AuraNodeType.NORMAL,"Energizing does not make leased attributes permanent");
        int[] changes={0};var buffer=new EssentiaBuffer(10,a->true,()->changes[0]++);var air=Aspect.AIR.unwrap();var fire=Aspect.FIRE.unwrap();test.assertTrue(buffer.insert(air,8,true)==8&&changes[0]==0&&buffer.contents().isEmpty(),"Shared buffer simulation is pure");buffer.insert(air,8,false);test.assertTrue(buffer.insert(fire,5,false)==2&&buffer.capacity(air)==8,"Shared total capacity, not independent per-aspect capacity");var disk=buffer.save();var other=new EssentiaBuffer(10,a->true,()->{});other.load(disk);test.assertTrue(other.extract(air,20,false)==8&&other.contents().amount(fire)==2,"Buffer save/load and partial extraction conserve multiple aspects");boolean rejected=false;try{new EssentiaBuffer(1,a->true,()->{}).load(disk);}catch(IllegalArgumentException expected){rejected=true;}test.assertTrue(rejected,"Oversized persisted buffer is rejected, not truncated");
        level.removeBlock(pos.above().east(),false);level.removeBlock(pos.above(),false);level.removeBlock(pos,false);level.removeBlock(pos.below(),false);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void extensionApiBufferRejectsCorruptDataAtomically(GameTestHelper test){
        var air=Aspect.AIR.unwrap();var buffer=new EssentiaBuffer(10,a->true,()->{});buffer.insert(air,6,false);
        var unknown=buffer.save();unknown.getList("Aspects",10).getCompound(0).putString("key","absent:unknown");
        var negative=buffer.save();negative.getList("Aspects",10).getCompound(0).putInt("amount",-1);
        var malformed=new CompoundTag();malformed.putString("Aspects","invalid");
        for(var invalid:java.util.List.of(unknown,negative,malformed)){boolean failed=false;try{buffer.load(invalid);}catch(IllegalArgumentException expected){failed=true;}test.assertTrue(failed&&buffer.contents().amount(air)==6,"Invalid persisted data must not partially replace live inventory");}
        test.succeed();
    }

}

