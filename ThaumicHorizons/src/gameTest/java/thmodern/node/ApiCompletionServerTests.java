package thmodern.node;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.*;
import thaumcraft.api.nodes.EnergizedNodeApi;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class ApiCompletionServerTests {
    private static EnergizedNodeBlockEntity node(GameTestHelper test,BlockPos pos){
        var level=test.getLevel();level.setBlock(pos.below(),TCArcane.NODE_STABILIZER.get().defaultBlockState(),2);
        level.setBlock(pos.above(),TCArcane.NODE_CONVERTER.get().defaultBlockState(),2);
        level.setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),2);
        var node=(EnergizedNodeBlockEntity)level.getBlockEntity(pos);var aura=new AspectAmounts();for(var a:TCAspects.catalog().primals())aura.add(a,20);
        node.configure(aura,AuraNodeType.NORMAL,null,"amplifier-test");((NodeConverterBlockEntity)level.getBlockEntity(pos.above())).checkStatus();return node;
    }
    private static TransductionAmplifierEntity amplifier(GameTestHelper test,BlockPos pos,Direction side){
        var level=test.getLevel();var at=pos.relative(side);
        level.setBlock(at,Horizons.TRANSDUCTION_AMPLIFIER.get().defaultBlockState().setValue(TransductionAmplifierBlock.FACING,side),2);
        level.setBlock(at.relative(side),Blocks.REDSTONE_BLOCK.defaultBlockState(),2);
        return (TransductionAmplifierEntity)level.getBlockEntity(at);
    }
    @GameTest(template="empty") public static void independentAmplifiersRevokeWithoutChangingSavedAura(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(6,5,6));var node=node(test,pos);var air=TCAspects.catalog().require("aer");
        var first=amplifier(test,pos,Direction.EAST);var second=amplifier(test,pos,Direction.WEST);first.tick();second.tick();
        test.assertTrue(EnergizedNodeApi.activeAmplifiers(node)==2&&node.effectiveAura().amount(air)==40,"Two independent leases add 20 aura");
        var saved=new EnergizedNodeBlockEntity(pos,node.getBlockState());saved.load(node.saveWithoutMetadata());
        test.assertTrue(saved.aura().amount(air)==20&&saved.effectiveAura().amount(air)==20,"Save contains only permanent aura");
        var data=first.saveWithoutMetadata();first.load(data);first.tick();test.assertTrue(EnergizedNodeApi.activeAmplifiers(node)==2,"Reloaded device renews its identity without a duplicate bonus");
        level.setBlock(pos.east(2),Blocks.AIR.defaultBlockState(),2);first.tick();test.assertTrue(node.effectiveAura().amount(air)==30,"Redstone loss removes only its own lease");
        level.destroyBlock(pos.west(),false);test.assertTrue(node.effectiveAura().amount(air)==20,"Breaking the other amplifier restores permanent aura");test.succeed();
    }
    @GameTest(template="empty") public static void missingDeviceLeaseExpires(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(6,5,6));var node=node(test,pos);var amplifier=amplifier(test,pos,Direction.EAST);amplifier.tick();
        // Model an unloaded/stalled producer: no further renewals, but keep its old lease.
        test.getLevel().setBlock(pos.east(2),Blocks.AIR.defaultBlockState(),2);amplifier.setRemoved();
        var amounts=new AspectAmounts();amounts.add(TCAspects.catalog().require("aer"),10);
        EnergizedNodeApi.amplify(node,UUID.randomUUID(),thaumcraft.api.aspects.AspectList.from(amounts),3);
        test.runAfterDelay(5,()->{test.assertTrue(EnergizedNodeApi.activeAmplifiers(node)==0&&node.effectiveAura().amount(TCAspects.catalog().require("aer"))==20,"Unrenewed lease expires while node remains loaded");test.succeed();});
    }
    @GameTest(template="empty") public static void fourPoweredAmplifiersCreateSurvivalRiftWithPermanentAura(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(6,5,6));node(test,pos);var amplifiers=new ArrayList<TransductionAmplifierEntity>();
        for(var side:Direction.Plane.HORIZONTAL)amplifiers.add(amplifier(test,pos,side));
        for(int tick=0;tick<50;tick++)for(var amplifier:amplifiers)if(!amplifier.isRemoved())amplifier.tick();
        test.assertTrue(level.getBlockEntity(pos) instanceof thmodern.plane.VortexEntity,"Four amplifiers produce the natural rift");
        var vortex=(thmodern.plane.VortexEntity)level.getBlockEntity(pos);
        test.assertTrue(!vortex.cheat()&&vortex.aspects().amount(TCAspects.catalog().require("aer"))==20,"Rift inherits permanent aura, not 40 bonus aura");
        test.runAfterDelay(3,()->{test.assertTrue(level.getBlockEntity(pos)==vortex,"Old converter cannot collapse the replacement rift on the following tick");test.succeed();});
    }
    @GameTest(template="empty") public static void brainJarReceivesXpAndFullJarPausesWithoutDrainingSoulBits(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));level.setBlock(pos,Horizons.SOUL_EXTRACTOR.get().defaultBlockState(),2);level.setBlock(pos.above(),TCArcane.BRAIN_JAR.get().defaultBlockState(),2);
        var extractor=(thmodern.soul.SoulExtractorEntity)level.getBlockEntity(pos);var jar=(BrainJarBlockEntity)level.getBlockEntity(pos.above());
        extractor.input.setStackInSlot(0,new ItemStack(Items.SOUL_SAND));level.random.setSeed(321654);
        for(int i=0;i<1200;i++)extractor.tick();
        test.assertTrue(extractor.remaining()==0&&jar.experience()>0&&jar.experience()<120,"Each soul bit has a one-percent XP chance, not guaranteed XP");
        int before=jar.experience();var saved=new BrainJarBlockEntity(pos.above(),jar.getBlockState());saved.load(jar.saveWithoutMetadata());test.assertTrue(saved.experience()==before,"Inserted XP persists");
        jar.insertExperience(BrainJarBlockEntity.CAPACITY,false);extractor.input.setStackInSlot(0,new ItemStack(Items.SOUL_SAND));extractor.tick();int remaining=extractor.remaining();
        for(int i=0;i<20;i++)extractor.tick();test.assertTrue(jar.experience()==BrainJarBlockEntity.CAPACITY&&remaining==0&&extractor.remaining()==0&&extractor.input.getStackInSlot(0).is(Items.SOUL_SAND)&&extractor.input.getStackInSlot(0).getCount()==1&&!extractor.working(),"Full jar pauses before consuming a second soul sand and cannot overflow");
        jar.extractExperience(1,false);extractor.tick();test.assertTrue(extractor.remaining()==1199&&extractor.input.getStackInSlot(0).isEmpty(),"Extraction reserves exactly one sand and processes one bit after room becomes available");
        jar.insertExperience(1,false);int reserved=extractor.remaining();for(int i=0;i<20;i++)extractor.tick();test.assertTrue(extractor.remaining()==reserved&&!extractor.working(),"A full jar also preserves an already-reserved partial sand");test.succeed();
    }
    @GameTest(template="empty") public static void disposableWandSpendsButCannotRechargeOrDrainAmulet(GameTestHelper test){
        var level=test.getLevel();var wand=Horizons.DISPOSABLE_WAND.get();var stack=wand.full();var air=TCAspects.catalog().require("aer");
        for(var primal:TCAspects.catalog().primals())test.assertTrue(wand.vis(stack,primal)==25000,"New crystal wand contains 250 vis of every primal");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DisposableTest"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        test.assertTrue(wand.consumeForPlayer(stack,player,Map.of(air,1000),false,false,true)&&wand.vis(stack,air)==24100,"Casting uses crystal cap's 0.9 multiplier");
        test.assertTrue(wand.receiveVis(stack,air,500,true)==0&&wand.receiveVis(stack,air,500,false)==0&&wand.addVis(stack,air,5,true,true)==5,"All recharge entry points refuse without mutating vis");
        var amulet=TCItems.LESSER_VIS_AMULET.get();var worn=new ItemStack(amulet);amulet.addVis(worn,air,100,false,true);player.tickCount=10;
        amulet.curioTick(new top.theillusivec4.curios.api.SlotContext("necklace",player,0,false,true),worn);
        test.assertTrue(amulet.vis(worn,air)==100&&wand.vis(stack,air)==24100,"Refused amulet transfer does not debit the source");
        var restored=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(!wand.rechargeable(restored)&&wand.vis(restored,air)==24100,"Reload preserves spent charge and non-rechargeability");test.succeed();
    }
    @GameTest(template="empty") public static void chargerDoesNotRequestNetworkVisForDisposableWand(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));level.setBlock(pos,TCArcane.WORKBENCH_CHARGER.get().defaultBlockState(),2);
        level.setBlock(pos.below(),TCArcane.BLOCK.get().defaultBlockState(),2);
        var stack=Horizons.DISPOSABLE_WAND.get().full();stack.getOrCreateTag().putInt("aer",0);((ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos.below())).setItem(10,stack);
        int[] requests={0};var charger=new WorkbenchChargerBlockEntity(pos,level.getBlockState(pos)){@Override public int consumeVis(Aspect aspect,int requested){requests[0]+=requested;return requested;}};
        level.setBlockEntity(charger);charger.tick();test.assertTrue(requests[0]==0&&Horizons.DISPOSABLE_WAND.get().vis(stack,TCAspects.catalog().require("aer"))==0,"Charger refuses before requesting upstream power");test.succeed();
    }
    @GameTest(template="empty") public static void crystalRiftOutputPersistsAsOneFullyChargedWand(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));level.setBlock(pos,Horizons.VORTEX.get().defaultBlockState(),2);var vortex=(thmodern.plane.VortexEntity)level.getBlockEntity(pos);vortex.initialize(new AspectAmounts(),true);
        var input=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Horizons.CRYSTAL_WAND.get()));level.addFreshEntity(input);
        test.assertTrue(!vortex.convert(input)&&input.isAlive(),"Unformed rift leaves blank wand intact");for(int i=0;i<50;i++)vortex.tick();
        test.assertTrue(vortex.convert(input)&&!input.isAlive()&&vortex.pendingOutputs()==1,"One blank becomes one queued wand");
        var tag=vortex.saveWithoutMetadata();var output=ItemStack.of(tag.getList("Outputs",10).getCompound(0));test.assertTrue(output.is(Horizons.DISPOSABLE_WAND.get()),"Output is the real casting item");for(var primal:TCAspects.catalog().primals())test.assertTrue(Horizons.DISPOSABLE_WAND.get().vis(output,primal)==25000,"Queued wand is fully initialized");
        vortex.load(tag);test.assertTrue(vortex.pendingOutputs()==1&&!vortex.convert(input),"Reload preserves output and cannot consume a dead input twice");test.succeed();
    }
}
