package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.network.*;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0916ServerTests {
    @GameTest(template="wand_test_empty") public static void nodeDisplayLimitDoesNotClampStoredVis(GameTestHelper t) {
        var pos=t.absolutePos(new BlockPos(3,3,3));var level=t.getLevel();level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),18);
        var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);var air=TCAspects.catalog().require("aer");
        node.configure(new tc4revival.aspect.AspectAmounts().add(air,10000),AuraNodeType.NORMAL,null,"large",false);
        var saved=node.saveWithoutMetadata();node.load(saved);
        t.assertTrue(node.aspects().amount(air)==10000 && node.base().amount(air)==10000,"Large command node retains real capacity and stored vis");
        t.assertTrue(node.take(air,500) && node.aspects().amount(air)==9500,"Display limit does not reduce extractable vis");t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void stabilizersAndAsymmetryDetermineRiskSign(GameTestHelper t) {
        var o=floor(t);var level=t.getLevel();var pos=o.offset(4,3,3);
        for(int x=1;x<=3;x++)for(int z=-2;z<=2;z++)for(int sign:new int[]{-1,1})
            level.setBlock(pos.offset(x*sign,-2,z*sign),Blocks.SKELETON_SKULL.defaultBlockState(),18);
        int stable=InfusionLayout.scan(level,pos).symmetry();
        t.assertTrue(stable<=-2 && InfusionRisk.preview(2,stable).safe(),"Mirrored stabilizers cancel recipe risk");
        var lone=pos.offset(4,-2,0);level.setBlock(lone,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
        ((ArcanePedestalBlockEntity)level.getBlockEntity(lone)).setItem(0,new ItemStack(Items.DIAMOND));
        int asymmetric=InfusionLayout.scan(level,pos).symmetry();
        t.assertTrue(asymmetric>stable && !InfusionRisk.preview(2,asymmetric).safe(),"Occupied unpaired pedestal adds risk and changes color");t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void continuousFociStartUsingWithoutDefaultSwing(GameTestHelper t) {
        var player=new FakePlayer(t.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocusAnimation"));
        for(var item:List.of(TCItems.FIRE_FOCUS.get(),TCItems.EXCAVATION_FOCUS.get())) {
            var wand=new ItemStack(TCItems.CASTING_WAND.get());WandFocusItem.attach(wand,new ItemStack(item));player.setItemInHand(InteractionHand.MAIN_HAND,wand);
            var result=item.cast(wand,t.getLevel(),player,InteractionHand.MAIN_HAND,null);
            t.assertTrue(player.isUsingItem() && !result.getResult().shouldSwing() && !player.swinging,"Continuous focus consumes use without a vanilla arm swing");
            t.assertTrue(wand.getUseAnimation()==UseAnim.NONE,"No vanilla eat/drink/bow pose");player.stopUsingItem();
        }
        t.succeed();
    }
    private static BlockPos floor(GameTestHelper t) {
        var origin=t.absolutePos(new BlockPos(1,2,1));var level=t.getLevel();
        for(var pos:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(10,5,7)))
            level.setBlock(pos,pos.getY()==origin.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        for(var pos:BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(10,0,7)))level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        return origin;
    }
    private static GolemEntity spawn(GameTestHelper t,BlockPos home,Direction face,GolemCoreType core) {
        var golem=TCEntities.GOLEM.get().create(t.getLevel());var config=new GolemConfiguration(GolemMaterial.WOOD,false);config.installCore(core);
        golem.state().configure(config);golem.state().home(home,face);golem.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,0,0);
        golem.restrictTo(home,32);golem.refreshConfiguration();t.getLevel().addFreshEntity(golem);return golem;
    }
    private static void essentia(GameTestHelper t,boolean clickAlembic) {
        var o=floor(t);var level=t.getLevel();var furnace=o.offset(1,0,3);var jarPos=o.offset(7,0,3);
        level.setBlock(furnace,TCArcane.ALCHEMY_FURNACE.get().defaultBlockState(),3);
        level.setBlock(furnace.above(),TCArcane.ALEMBIC.get().defaultBlockState(),3);
        level.setBlock(jarPos,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
        var alembic=(AlembicBlockEntity)level.getBlockEntity(furnace.above());alembic.face(Direction.EAST);
        alembic.addToContainer(TCAspects.catalog().require("aer"),12);
        var jar=(EssentiaJarBlockEntity)level.getBlockEntity(jarPos);
        var home=(clickAlembic?furnace.above():furnace).east();var golem=spawn(t,home,Direction.EAST,GolemCoreType.ESSENTIA);
        golem.markers().replace(List.of(new GolemMarker(jarPos,level.dimension().location(),Direction.NORTH,(byte)-1)));
        t.runAtTickTime(650,()->{
            t.assertTrue(jar.amount()==12 && alembic.amount()==0 && golem.essentiaAmount()==0,
                    "AI must perform repeated round trips: jar="+jar.amount()+", alembic="+alembic.amount()+", carried="+golem.essentiaAmount()+", pos="+golem.position());
            golem.discard();t.succeed();
        });
    }
    @GameTest(template="wand_test_empty",timeoutTicks=700) public static void alchemyGolemFromFurnace(GameTestHelper t){essentia(t,false);}
    @GameTest(template="wand_test_empty",timeoutTicks=700) public static void alchemyGolemFromAlembicFront(GameTestHelper t){essentia(t,true);}
    private static void liquid(GameTestHelper t,boolean tank) {
        var o=floor(t);var level=t.getLevel();var home=o.offset(1,0,3);var source=o.offset(7,0,3);
        level.setBlock(home,TCArcane.CRUCIBLE.get().defaultBlockState(),3);var crucible=(CrucibleBlockEntity)level.getBlockEntity(home);
        if(tank){level.setBlock(source,TCArcane.CRUCIBLE.get().defaultBlockState(),3);((CrucibleBlockEntity)level.getBlockEntity(source)).fluids().fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);}
        else {for(var side:Direction.Plane.HORIZONTAL)level.setBlock(source.relative(side),Blocks.STONE.defaultBlockState(),3);level.setBlock(source,Blocks.WATER.defaultBlockState(),3);}
        var golem=spawn(t,home.east(),Direction.EAST,GolemCoreType.LIQUID);
        golem.markers().replace(List.of(new GolemMarker(source,level.dimension().location(),Direction.UP,(byte)-1)));
        t.runAtTickTime(650,()->{
            t.assertTrue(crucible.contents().water()==1000 && golem.fluid().isEmpty(),"Liquid AI must pump and return: water="+crucible.contents().water()+", cargo="+golem.fluid()+", pos="+golem.position());
            if(tank)t.assertTrue(((CrucibleBlockEntity)level.getBlockEntity(source)).contents().water()==0,"Tank source debited once");
            else t.assertTrue(level.getFluidState(source).isEmpty(),"Natural source consumed once");
            golem.discard();t.succeed();
        });
    }
    @GameTest(template="wand_test_empty",timeoutTicks=700) public static void liquidGolemPumpsMarkedWater(GameTestHelper t){liquid(t,false);}
    @GameTest(template="wand_test_empty",timeoutTicks=700) public static void liquidGolemPumpsCapabilityTank(GameTestHelper t){liquid(t,true);}
    @GameTest(template="wand_test_empty") public static void bellMarksFluidSurfaceThroughAirUse(GameTestHelper t) {
        var o=floor(t);var pos=o.offset(4,0,3);var level=t.getLevel();level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BellFluid"));
        player.moveTo(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,0,90);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.GOLEM_BELL.get()));
        TCItems.GOLEM_BELL.get().use(level,player,InteractionHand.MAIN_HAND);
        var markers=new GolemMarkers();markers.load(player.getMainHandItem().getOrCreateTag().getList("markers",10));
        t.assertTrue(markers.inDimension(level.dimension().location()).stream().anyMatch(m->m.pos().equals(pos)),"Air-use raycast marks water rather than ignoring it");t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void jarsTransmitSkylightWithoutCubeShading(GameTestHelper t) {
        var pos=t.absolutePos(new BlockPos(3,3,3));var level=t.getLevel();
        for(var block:List.of(TCArcane.ESSENTIA_JAR.get(),TCArcane.VOID_JAR.get())) {
            level.setBlock(pos,block.defaultBlockState(),3);var state=level.getBlockState(pos);
            t.assertTrue(state.propagatesSkylightDown(level,pos) && state.getLightBlock(level,pos)==0 && state.getShadeBrightness(level,pos)==1,
                    "Transparent jar passes skylight and does not shade adjacent corners as a full cube");
        }
        t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void infusionRiskPreviewAndRunningSnapshot(GameTestHelper t) {
        var o=floor(t);var level=t.getLevel();var pos=o.offset(4,3,3);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlock(pos.offset(x,-2,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState(),18);
        level.setBlock(pos,TCArcane.INFUSION_MATRIX.get().defaultBlockState(),18);
        level.setBlock(pos.below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
        var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);var center=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.below(2));
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RiskPreview"));
        var data=KnowledgeSavedData.get(player.server);data.ensure(player.getUUID(),bound->0);
        t.assertTrue(matrix.riskFor(player).isEmpty(),"Empty altar has no preview");
        center.setItem(0,new ItemStack(TCItems.golemCore(5)));
        var water=net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),net.minecraft.world.item.alchemy.Potions.WATER);
        var ingredients=List.of(new ItemStack(TCItems.ESSENTIA_JAR.get()),water,water,water);
        int i=0;for(var offset:List.of(new BlockPos(3,-2,0),new BlockPos(-3,-2,0),new BlockPos(0,-2,3),new BlockPos(0,-2,-3))) {
            level.setBlock(pos.offset(offset),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
            ((ArcanePedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).setItem(0,ingredients.get(i++).copy());
        }
        t.runAtTickTime(25,()->{
            t.assertTrue(matrix.riskFor(player).isEmpty(),"Unknown research cannot preview a usable recipe");
            data.update(player.getUUID(),bound->0,k->{k.rememberResearch("COREALCHEMY");return null;});
            var preview=matrix.riskFor(player).orElseThrow();
            t.assertTrue(preview.recipe()==2 && preview.environment()==0 && preview.actual()==2 && !preview.safe(),"Balanced recipe displays its actual positive risk");
            matrix.activate();matrix.start(player);t.assertTrue(matrix.crafting(),"Preview recipe really starts");
            center.setItem(0,ItemStack.EMPTY);
            var running=matrix.riskFor(player).orElseThrow();t.assertTrue(running.recipe()==2 && running.actual()==matrix.instability(),"In-progress summary uses transaction after ingredients change");
            var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{new InfusionRiskReply(pos,running).encode(buf);t.assertTrue(InfusionRiskReply.decode(buf).risk().equals(running),"Snapshot packet round-trip");}finally{buf.release();}
            t.assertTrue(!new InfusionRiskRequest(pos).eligible(player),"No goggles rejects remote display requests");
            t.succeed();
        });
    }
}
