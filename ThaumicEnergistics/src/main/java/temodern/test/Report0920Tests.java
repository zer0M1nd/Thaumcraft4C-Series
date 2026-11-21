package temodern.test;

import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import temodern.crafting.*;
import temodern.essentia.*;

@GameTestHolder("thaumicenergistics") @PrefixGameTestTemplate(false)
public class Report0920Tests {
    @GameTest(template="test_empty",timeoutTicks=180) public static void cableTerminalHostAdaptersRemainValid(GameTestHelper t){
        var p=new BlockPos(2,2,2);t.setBlock(p,AEBlocks.CABLE_BUS.block());t.setBlock(p.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cable=(CableBusBlockEntity)t.getBlockEntity(p);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
        var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumicenergistics:arcane_terminal"));
        var part=(ArcaneTerminalPart)cable.addPart((appeng.api.parts.IPartItem<?>)item,Direction.SOUTH,null);
        t.runAfterDelay(100,()->{
            WorkHost host=part;
            t.assertTrue(host.workLevel()==t.getLevel()&&host.workPosition().equals(t.absolutePos(p)),"Cable host bridges level and position");
            host.markWorkChanged();
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TEHost"));player.setPos(t.absolutePos(p).getCenter());
            var menu=new ArcaneTerminalMenu(1,player.getInventory(),host);
            t.assertTrue(menu.stillValid(player),"Opened cable terminal survives the server player validity tick");
            host.workstation().inventory.setItem(0,new ItemStack(Items.STICK));
            menu.broadcastChanges();t.succeed();
        });
    }
    @GameTest(template="test_empty") public static void emptiedJarStacksAndBucketExposesFluid(GameTestHelper t){
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());
        var strategy=new ContainerStrategy();var key=EssentiaKey.parse("aer");
        player.getInventory().setItem(0,new ItemStack(TCItems.ESSENTIA_JAR.get()));
        var context=strategy.findPlayerSlotContext(player,0);
        t.assertTrue(strategy.insert(context,key,16,Actionable.MODULATE)==16,"Fill real jar");
        t.assertTrue(strategy.extract(context,key,16,Actionable.MODULATE)==16,"Drain real jar");
        t.assertTrue(ItemStack.isSameItemSameTags(player.getInventory().getItem(0),new ItemStack(TCItems.ESSENTIA_JAR.get())),"Emptied jar is exactly the stackable blank jar");
        var bucket=appeng.api.behaviors.ContainerItemStrategies.getContainedStack(new ItemStack(TCItems.LIQUID_DEATH_BUCKET.get()));
        t.assertTrue(bucket!=null&&bucket.what() instanceof AEFluidKey fluid&&fluid.getFluid()==TCFluids.DEATH.get()&&bucket.amount()==1000,"AE recognizes liquid death as a bucket of fluid");
        player.getInventory().setItem(0,ItemStack.EMPTY);t.succeed();
    }
    @GameTest(template="test_empty") public static void automationAccessesLabeledJarFromAllFaces(GameTestHelper t){
        var p=new BlockPos(2,2,2);t.setBlock(p,TCArcane.ESSENTIA_JAR.get());
        var handle=thaumcraft.api.aspects.EssentiaStorageApi.atInventory(t.getLevel(),t.absolutePos(p));var air=EssentiaKey.parse("aer").aspect();
        for(var side:Direction.values()){
            var bus=new EssentiaBus(t.getLevel(),t.absolutePos(p),side);
            t.assertTrue(bus.push(EssentiaKey.of(air),8,Actionable.MODULATE)==8,"Output bus accepts "+side);
            t.assertTrue(handle.extract(air,8,false)==8,"Automation can retrieve the inserted amount");
        }
        t.assertTrue(!temodern.RecipeConfig.itemEnabled("essentia_cell_1024k")&&!temodern.RecipeConfig.itemEnabled("storage_component_4096k"),"High capacities are disabled by default");t.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=180) public static void nativeTerminalEncodesArcanePattern(GameTestHelper t){
        var p=new BlockPos(2,2,2);t.setBlock(p,AEBlocks.CABLE_BUS.block());t.setBlock(p.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cable=(CableBusBlockEntity)t.getBlockEntity(p);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
        var part=cable.addPart(AEParts.PATTERN_ENCODING_TERMINAL.asItem(),Direction.SOUTH,null);
        t.runAfterDelay(100,()->{
            var host=(appeng.helpers.IPatternTerminalMenuHost)part;
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TE0920"));player.setPos(t.absolutePos(p).getCenter());
            var menu=new appeng.menu.me.items.PatternEncodingTermMenu(1,player.getInventory(),host);player.containerMenu=menu;
            ((ArcaneEncodingMenu)menu).teSetArcane(true);
            var inv=menu.getCraftingMatrix();
            for(int i=0;i<3;i++)inv.setItemDirect(i,new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER)));
            inv.setItemDirect(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));inv.setItemDirect(4,AEItems.ANNIHILATION_CORE.stack());
            host.getLogic().getBlankPatternInv().setItemDirect(0,AEItems.BLANK_PATTERN.stack(2));menu.encode();
            var output=host.getLogic().getEncodedPatternInv().getStackInSlot(0);
            t.assertTrue(new ArcanePattern.Decoder().decodePattern(output,t.getLevel(),false)!=null,"Native encoding creates a usable, validated arcane pattern");
            t.assertTrue(host.getLogic().getBlankPatternInv().getStackInSlot(0).getCount()==1,"Exactly one blank consumed");
            var saved=output.copy();menu.setMode(appeng.parts.encoding.EncodingMode.PROCESSING);
            host.getLogic().getEncodedPatternInv().setItemDirect(0,ItemStack.EMPTY);
            host.getLogic().getEncodedPatternInv().setItemDirect(0,saved);
            menu.broadcastChanges();
            t.assertTrue(((ArcaneEncodingMenu)menu).teIsArcane(),"Loading an arcane pattern restores its mode");
            t.assertTrue(menu.getCraftingMatrix().getStackInSlot(4).is(AEItems.ANNIHILATION_CORE.asItem()),"Loading restores the ingredient layout");
            player.containerMenu=player.inventoryMenu;t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=180) public static void emptyCursorBorrowsOnePhial(GameTestHelper t){
        var p=new BlockPos(2,2,2);t.setBlock(p,temodern.machine.Machines.ALL.get("arcane_terminal").get());
        t.setBlock(p.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());t.setBlock(p.east(),AEBlocks.DRIVE.block());
        var drive=(appeng.blockentity.storage.DriveBlockEntity)t.getBlockEntity(p.east());
        drive.getInternalInventory().setItemDirect(0,new ItemStack(temodern.TE.CELLS.get("1k").get()));
        drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_1K.stack());
        t.runAfterDelay(100,()->{
            var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TEPhial"));player.setPos(t.absolutePos(p).getCenter());
            var machine=(temodern.machine.NetworkMachine)t.getBlockEntity(p);
            class TestMenu extends ArcaneTerminalMenuProbe {TestMenu(){super(player,machine);}}
            var menu=new TestMenu();player.containerMenu=menu;
            var storage=machine.getInventory();var source=appeng.api.networking.security.IActionSource.empty();var air=EssentiaKey.parse("aer");
            storage.insert(air,16,Actionable.MODULATE,source);storage.insert(AEItemKey.of(TCItems.EMPTY_PHIAL.get()),2,Actionable.MODULATE,source);
            menu.clickAspect(air);
            var filled=new ContainerStrategy().getContainedStack(menu.getCarried());
            t.assertTrue(filled!=null&&filled.what().equals(air)&&filled.amount()==8,"Empty cursor receives one filled glass phial");
            t.assertTrue(storage.getAvailableStacks().get(air)==8&&storage.getAvailableStacks().get(AEItemKey.of(TCItems.EMPTY_PHIAL.get()))==1,"Exactly eight essentia and one empty phial debited");
            player.containerMenu=player.inventoryMenu;t.succeed();
        });
    }
    private static class ArcaneTerminalMenuProbe extends appeng.menu.me.common.MEStorageMenu {
        ArcaneTerminalMenuProbe(net.minecraft.server.level.ServerPlayer player,temodern.machine.NetworkMachine host){super(WorkMenu.TERMINAL.get(),1,player.getInventory(),host);}
        void clickAspect(EssentiaKey key){handleNetworkInteraction((net.minecraft.server.level.ServerPlayer)getPlayer(),key,appeng.helpers.InventoryAction.PICKUP_OR_SET_DOWN);}
    }
    @GameTest(template="test_empty") public static void busExtractsAdvancedAlchemyCore(GameTestHelper t){
        var pos=new BlockPos(2,2,2);t.setBlock(pos,TCArcane.ADVANCED_ALCHEMY_FURNACE.get());
        var core=(AdvancedAlchemyBlockEntity)t.getBlockEntity(pos);var air=EssentiaKey.parse("aer");
        var tag=core.saveWithFullMetadata();tc4revival.aspect.AspectNbt.write(tag,new tc4revival.aspect.AspectAmounts().add(air.aspect(),24));core.load(tag);
        for(var side:Direction.values()){
            var bus=thaumcraft.api.aspects.EssentiaStorageApi.atInventory(t.getLevel(),t.absolutePos(pos));
            t.assertTrue(bus.extract(air.aspect(),4,true)==4,"Core exposes inventory from "+side);
            t.assertTrue(bus.extract(air.aspect(),4,false)==4,"Core debits inventory from "+side);
        }
        t.assertTrue(core.process().amount()==0,"Six transfers conserve the furnace contents");t.succeed();
    }
    @GameTest(template="test_empty") public static void nodeBiomeDoesNotLeakAcrossGround(GameTestHelper t){
        var level=t.getLevel();var origin=t.absolutePos(new BlockPos(2,2,2));
        int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,origin.getX(),origin.getZ());
        var above=new BlockPos(origin.getX(),surface+8,origin.getZ());var below=above.atY(surface-16);var prior=level.getNoiseBiome(QuartPos.fromBlock(below.getX()),QuartPos.fromBlock(below.getY()),QuartPos.fromBlock(below.getZ()));
        tc4revival.content.WorldBiomeChanges.changeNodeBiome(level,above,above,TCBiomes.TAINTED_LAND);
        t.assertTrue(level.getNoiseBiome(QuartPos.fromBlock(above.getX()),QuartPos.fromBlock(above.getY()),QuartPos.fromBlock(above.getZ())).is(TCBiomes.TAINTED_LAND),"Above-ground effect changes surface biome");
        t.assertTrue(level.getNoiseBiome(QuartPos.fromBlock(below.getX()),QuartPos.fromBlock(below.getY()),QuartPos.fromBlock(below.getZ())).equals(prior),"Above-ground node leaves underground unchanged");
        tc4revival.content.WorldBiomeChanges.changeNodeBiome(level,below,below,TCBiomes.MAGICAL_FOREST);
        t.assertTrue(level.getNoiseBiome(QuartPos.fromBlock(below.getX()),QuartPos.fromBlock(below.getY()),QuartPos.fromBlock(below.getZ())).is(TCBiomes.MAGICAL_FOREST)&&level.getNoiseBiome(QuartPos.fromBlock(above.getX()),QuartPos.fromBlock(above.getY()),QuartPos.fromBlock(above.getZ())).is(TCBiomes.TAINTED_LAND),"Underground effect cannot change above-ground biome");t.succeed();
    }
}

