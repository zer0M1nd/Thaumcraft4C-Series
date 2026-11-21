package temodern.test;

import java.util.List;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.StorageCells;
import appeng.api.util.AEColor;
import appeng.core.definitions.*;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import thaumcraft.api.aspects.*;
import temodern.TE;
import temodern.essentia.*;
import temodern.storage.*;
import temodern.machine.*;
import temodern.crafting.*;

@GameTestHolder(TE.ID) @PrefixGameTestTemplate(false)
public final class ServerTests {
    private static final IActionSource SOURCE=IActionSource.empty();
    private static EssentiaKey air(){return EssentiaKey.of(TCAspects.catalog().require("aer"));}
    @GameTest(template="test_empty") public static void keysSerializeAndReload(GameTestHelper t){
        var original=air();var copy=AEKey.fromTagGeneric(original.toTagGeneric());
        t.assertTrue(original.equals(copy)&&original.hashCode()==copy.hashCode(),"Native AE key round trip");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());original.writeToPacket(buffer);
        t.assertTrue(original.equals(EssentiaKeyType.INSTANCE.readFromPacket(buffer)),"Stable string network encoding");buffer.release();
        t.assertTrue(EssentiaKey.parse("uninstalled:aspect")==null,"Unknown aspects rejected");t.succeed();
    }
    @GameTest(template="test_empty") public static void cellsConserveLargeCounts(GameTestHelper t){
        for(var tier:CellTier.ALL){
            if(tier.creative())continue;
            var stack=new ItemStack(TE.CELLS.get(tier.id()).get());var cell=StorageCells.getCellInventory(stack,null);
            t.assertTrue(cell!=null,"Cell registered: "+tier.id());
            t.assertTrue(cell.insert(air(),Long.MAX_VALUE,Actionable.SIMULATE,SOURCE)==tier.capacity(),"Simulated capacity: "+tier.id());
            t.assertTrue(cell.getAvailableStacks().isEmpty(),"Simulation immutable");
            t.assertTrue(cell.insert(air(),Long.MAX_VALUE,Actionable.MODULATE,SOURCE)==tier.capacity(),"Exact long capacity");
            t.assertTrue(cell.insert(air(),1,Actionable.MODULATE,SOURCE)==0,"No overflow insertion");
            cell.persist();var restored=ItemStack.of(stack.save(new CompoundTag()));var loaded=StorageCells.getCellInventory(restored,null);
            t.assertTrue(loaded.extract(air(),Long.MAX_VALUE,Actionable.MODULATE,SOURCE)==tier.capacity(),"Reload conserves count");
            t.assertTrue(loaded.getAvailableStacks().isEmpty(),"Complete extraction clears cell");
        }t.succeed();
    }
    @GameTest(template="test_empty") public static void partitionsAndTypeLimits(GameTestHelper t){
        var stack=new ItemStack(TE.CELLS.get("1k").get());var item=(EssentiaCell)stack.getItem();var cell=StorageCells.getCellInventory(stack,null);
        item.getConfigInventory(stack).setStack(0,new GenericStack(air(),1));
        var fire=EssentiaKey.of(TCAspects.catalog().require("ignis"));
        t.assertTrue(cell.insert(fire,8,Actionable.MODULATE,SOURCE)==0&&cell.insert(air(),8,Actionable.MODULATE,SOURCE)==8,"Partition enforced");
        var unrestricted=new ItemStack(TE.CELLS.get("1k").get());var storage=StorageCells.getCellInventory(unrestricted,null);
        for(int i=0;i<13;i++)t.assertTrue(storage.insert(EssentiaKey.of(TCAspects.catalog().all().get(i)),1,Actionable.MODULATE,SOURCE)==(i<12?1:0),"Type limit "+i);
        var creative=StorageCells.getCellInventory(new ItemStack(TE.CELLS.get("creative").get()),null);
        t.assertTrue(creative.extract(air(),100000,Actionable.MODULATE,SOURCE)==100000,"Creative source");t.succeed();
    }
    @GameTest(template="test_empty") public static void jarsHonorSidesAndNeverVoidViaStorageApi(GameTestHelper t){
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,1,2));level.setBlockAndUpdate(pos,TCArcane.VOID_JAR.get().defaultBlockState());
        var up=EssentiaStorageApi.at(level,pos,Direction.UP);var west=EssentiaStorageApi.at(level,pos,Direction.WEST);var aspect=air().aspect();
        t.assertTrue(west.insert(aspect,8,true)==0,"Wrong-side insert rejected");
        t.assertTrue(up.insert(aspect,64,true)==64&&up.contents().total()==0,"Capacity simulated without mutation");
        t.assertTrue(up.insert(aspect,100,false)==64&&up.insert(aspect,8,false)==0,"No silent void overflow");
        var storage=new EssentiaStorage(up,false,()->{});
        t.assertTrue(storage.extract(air(),40,Actionable.MODULATE,SOURCE)==40&&up.contents().amount(aspect)==24,"ME adapter exact extraction");
        level.removeBlock(pos,false);t.assertTrue(storage.extract(air(),1,Actionable.MODULATE,SOURCE)==0,"Removed BE handle invalidated");t.succeed();
    }
    @GameTest(template="test_empty") public static void escrowSurvivesNbtAndUnknownTags(GameTestHelper t){
        var data=BusEscrow.get(t.getLevel());var ledger=data.at(t.absolutePos(new BlockPos(1,1,1)),Direction.UP,false);ledger.retain("aer",17);ledger.retain("uninstalled:aspect",3);
        ledger.deliver("aer",n->5);data.setDirty();var tag=data.save(new CompoundTag());
        t.assertTrue(tag.getList("entries",Tag.TAG_COMPOUND).stream().anyMatch(row->((CompoundTag)row).getString("aspect").equals("aer")&&((CompoundTag)row).getLong("amount")==12),"Unaccepted resources persisted");
        t.assertTrue(tag.getList("entries",Tag.TAG_COMPOUND).stream().anyMatch(row->((CompoundTag)row).getString("aspect").equals("uninstalled:aspect")),"Unknown keys retained, not deleted");
        ledger.deliver("aer",n->n);ledger.deliver("uninstalled:aspect",n->n);data.setDirty();t.succeed();
    }
    @GameTest(template="test_empty") public static void containerItemsRespectPhialGranularityAndJarLabels(GameTestHelper t){
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());
        var strategy=new ContainerStrategy();player.getInventory().setItem(0,new ItemStack(TCItems.EMPTY_PHIAL.get()));
        var context=strategy.findPlayerSlotContext(player,0);
        t.assertTrue(strategy.insert(context,air(),7,Actionable.MODULATE)==0,"No partial phial fill");
        t.assertTrue(strategy.insert(context,air(),8,Actionable.SIMULATE)==8&&player.getInventory().getItem(0).is(TCItems.EMPTY_PHIAL.get()),"Container simulation immutable");
        t.assertTrue(strategy.insert(context,air(),8,Actionable.MODULATE)==8,"Full phial created");
        t.assertTrue(strategy.extract(context,air(),7,Actionable.MODULATE)==0&&strategy.extract(context,air(),8,Actionable.MODULATE)==8,"Full phial drain");
        var jar=new ItemStack(TCItems.ESSENTIA_JAR.get());jar.getOrCreateTag().putString("AspectFilter","ignis");player.getInventory().setItem(0,jar);context=strategy.findPlayerSlotContext(player,0);
        t.assertTrue(strategy.insert(context,air(),8,Actionable.MODULATE)==0,"Jar filter preserved");player.getInventory().setItem(0,ItemStack.EMPTY);t.succeed();
    }
    @GameTest(template="test_empty") public static void dataRecipesAndCatalysts(GameTestHelper t){
        for(var name:java.util.List.of("diffusion_core","coalescence_core","arcane_terminal","vis_interface","arcane_assembler","infusion_provider","essentia_provider","golem_wireless_backpack","cell_microscope","knowledge_core","thaumium_gearbox"))
            t.assertTrue(t.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TE.ID,name)).isPresent(),"Survival recipe loaded: "+name);
        var grid=new CraftGrid();var catalyst=new ItemStack(Items.DIAMOND);catalyst.getOrCreateTag().putString("test","preserved");grid.setItem(4,catalyst);
        t.assertTrue(CraftRemainders.catalyst(new ItemStack(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()),grid,4),"Construct catalyst retained");
        t.assertTrue(!CraftRemainders.catalyst(new ItemStack(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()),grid,3),"Other ingredients consumed");t.succeed();
    }
    private static DriveBlockEntity network(GameTestHelper t,BlockPos drive){
        t.setBlock(drive.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());t.setBlock(drive,AEBlocks.DRIVE.block());
        var be=(DriveBlockEntity)t.getBlockEntity(drive);be.getInternalInventory().setItemDirect(0,new ItemStack(TE.CELLS.get("1k").get()));return be;
    }
    @GameTest(template="test_empty",timeoutTicks=1200) public static void realImportAndExportBuses(GameTestHelper t){
        var drive=network(t,new BlockPos(2,2,2));var inPos=new BlockPos(3,2,2);var outPos=new BlockPos(3,2,3);
        t.setBlock(inPos,AEBlocks.CABLE_BUS.block());t.setBlock(outPos,AEBlocks.CABLE_BUS.block());
        var in=(CableBusBlockEntity)t.getBlockEntity(inPos);var out=(CableBusBlockEntity)t.getBlockEntity(outPos);
        in.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);out.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
        in.addPart(AEParts.IMPORT_BUS.asItem(),Direction.DOWN,null);var exporter=out.addPart(AEParts.EXPORT_BUS.asItem(),Direction.DOWN,null);
        exporter.getConfig().setStack(0,new GenericStack(air(),1));
        t.setBlock(inPos.below(),TCArcane.ESSENTIA_JAR.get());t.setBlock(outPos.below(),TCArcane.ESSENTIA_JAR.get());
        var source=EssentiaStorageApi.at(t.getLevel(),t.absolutePos(inPos.below()),Direction.UP);var destination=EssentiaStorageApi.at(t.getLevel(),t.absolutePos(outPos.below()),Direction.UP);source.insert(air().aspect(),32,false);
        t.succeedWhen(()->{
            t.assertTrue(drive.getMainNode().getGrid()!=null && in.getPart(Direction.DOWN).getGridNode()!=null && exporter.getGridNode()!=null,"Grid booting");
            t.assertTrue(destination.contents().amount(air().aspect())==32,"Ordinary buses deliver 32 essentia: source="+source.contents().total()+", destination="+destination.contents().total()+", network="+drive.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(air())+", import="+in.getPart(Direction.DOWN).getGridNode().isActive()+", export="+exporter.getGridNode().isActive());
            t.assertTrue(source.contents().total()==0,"Source debited exactly");
            t.assertTrue(drive.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(air())==0,"No phantom network copy");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void storageBusMountsRealJar(GameTestHelper t){
        var drive=network(t,new BlockPos(2,2,2));drive.getInternalInventory().setItemDirect(0,ItemStack.EMPTY);
        var cablePos=new BlockPos(3,2,2);t.setBlock(cablePos,AEBlocks.CABLE_BUS.block());
        var cable=(CableBusBlockEntity)t.getBlockEntity(cablePos);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);cable.addPart(AEParts.STORAGE_BUS.asItem(),Direction.DOWN,null);
        t.setBlock(cablePos.below(),TCArcane.ESSENTIA_JAR.get());var jar=EssentiaStorageApi.at(t.getLevel(),t.absolutePos(cablePos.below()),Direction.UP);
        t.runAfterDelay(100,()->{var inv=drive.getMainNode().getGrid().getStorageService().getInventory();
            t.assertTrue(inv.insert(air(),70,Actionable.MODULATE,SOURCE)==64&&jar.contents().total()==64,"Storage bus inserts into jar without a cell");
            t.assertTrue(inv.extract(air(),23,Actionable.MODULATE,SOURCE)==23&&jar.contents().total()==41,"Storage bus extracts physical contents");t.succeed();});
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void visOutputAndAdvancedDemandPersist(GameTestHelper t){
        network(t,new BlockPos(2,2,2));t.setBlock(new BlockPos(3,2,2),Machines.ALL.get("creative_vis_interface").get());
        t.setBlock(new BlockPos(4,2,2),Machines.ALL.get("vis_interface").get());t.setBlock(new BlockPos(5,2,2),Machines.ALL.get("infusion_provider").get());
        var relay=(NetworkMachine)t.getBlockEntity(new BlockPos(4,2,2));
        var provider=(NetworkMachine)t.getBlockEntity(new BlockPos(5,2,2));
        t.runAfterDelay(100,()->{
            var operator=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());
            var card=AEItems.MEMORY_CARD.stack();operator.setShiftKeyDown(true);
            ((NetworkMachine)t.getBlockEntity(new BlockPos(3,2,2))).memoryCard(operator,card);operator.setShiftKeyDown(false);relay.memoryCard(operator,card);
            t.assertTrue(relay.source()&&relay.consumeVis(air().aspect(),250)==250,"Output relay supplies real centivis from ME");
            t.assertTrue(relay.saveWithFullMetadata().getBoolean("visOutput"),"Output mode persisted");
            t.assertTrue(!provider.takeFromContainer(air().aspect(),1)&&provider.saveWithFullMetadata().getString("demand").equals("aer"),"Missing essentia schedules persistent crafting demand");
            relay.node().destroy();t.assertTrue(relay.consumeVis(air().aspect(),1)==0,"Disconnected relay cannot supply vis");t.succeed();});
    }
    @GameTest(template="test_empty",timeoutTicks=240) public static void arcaneAssemblerConsumesInputsAndNetworkVis(GameTestHelper t){
        network(t,new BlockPos(2,2,2));t.setBlock(new BlockPos(3,2,2),Machines.ALL.get("creative_vis_interface").get());t.setBlock(new BlockPos(4,2,2),Machines.ALL.get("arcane_assembler").get());
        var machine=(NetworkMachine)t.getBlockEntity(new BlockPos(4,2,2));var vis=(NetworkMachine)t.getBlockEntity(new BlockPos(3,2,2));
        var core=new ItemStack(TE.MATERIALS.get("knowledge_core").get());machine.workstation.inventory.setItem(10,core);
        // Keep ingredients out until the real network has booted, then exercise one atomic operation.
        t.runAfterDelay(100,()->{
            t.assertTrue(machine.node().isActive(),"Assembler is powered and has a channel");
            var grid=new CraftGrid();grid.setItem(0,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:quicksilver_drop"))));grid.setItem(1,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:quicksilver_drop"))));grid.setItem(2,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:quicksilver_drop"))));grid.setItem(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));grid.setItem(4,AEItems.ANNIHILATION_CORE.stack());
            var recipe=t.getLevel().getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().toString().equals("thaumicenergistics:diffusion_core")).findFirst().orElseThrow();
            var encoded=ArcanePattern.encode(grid,recipe,t.getLevel());var pattern=(ArcanePattern)new ArcanePattern.Decoder().decodePattern(encoded,t.getLevel(),false);
            temodern.item.KnowledgeCore.toggle(core,encoded,t.getLevel());
            var operator=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());var card=AEItems.MEMORY_CARD.stack();
            operator.setShiftKeyDown(true);vis.memoryCard(operator,card);operator.setShiftKeyDown(false);machine.memoryCard(operator,card);
            var wand=((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,true);
            var inputs=new KeyCounter[pattern.getInputs().length];for(int i=0;i<inputs.length;i++){inputs[i]=new KeyCounter();var input=pattern.getInputs()[i];var choice=input.getPossibleInputs()[0];inputs[i].add(choice.what(),choice.amount()*input.getMultiplier());}
            t.assertTrue(machine.workstation.pushPattern(pattern,inputs,Direction.WEST),"Assembler accepts native AE plan inputs");
            t.assertTrue(!machine.workstation.craft(null)&&!machine.workstation.inventory.getItem(1).isEmpty(),"Without a wand, pending craft retains ingredients");
            machine.workstation.inventory.setItem(9,wand);
            var perditio=TCAspects.catalog().require("perditio");
            int full=machine.storedVis(perditio);wand.getOrCreateTag().putInt("perditio",0);
            var withoutVis=machine.saveWithoutMetadata();
            t.assertTrue(!machine.workstation.craft(null)&&withoutVis.equals(machine.saveWithoutMetadata()),"Insufficient wand vis changes neither ingredients nor outputs");
            wand.getOrCreateTag().putInt("perditio",full);
            for(int i=12;i<18;i++)machine.workstation.inventory.setItem(i,new ItemStack(Items.STONE,64));
            var blocked=machine.saveWithoutMetadata();
            t.assertTrue(!machine.workstation.craft(null)&&blocked.equals(machine.saveWithoutMetadata()),"Full output does not consume wand vis or inputs");
            for(int i=12;i<18;i++)machine.workstation.inventory.setItem(i,ItemStack.EMPTY);
            var persisted=machine.saveWithoutMetadata();
            var restored=new NetworkMachine(machine.getBlockPos(),machine.getBlockState());restored.setLevel(t.getLevel());restored.load(persisted);
            t.assertTrue(ItemStack.matches(wand,restored.workstation.inventory.getItem(9))&&ItemStack.matches(core,restored.workstation.inventory.getItem(10)),"Wand, separate knowledge core and pending craft survive NBT reload");
            int before=machine.storedVis(TCAspects.catalog().require("perditio"));t.assertTrue(machine.workstation.craft(null),"Knowledge-authorized arcane craft succeeds");
            t.assertTrue(machine.workstation.inventory.getItem(12).is(TE.MATERIALS.get("diffusion_core").get()),"Expected output");
            t.assertTrue(machine.workstation.inventory.getItem(1).isEmpty()&&assemblerStoredVis(machine,TCAspects.catalog().require("perditio"))==before-(int)(200*machine.workstation.modifier(TCAspects.catalog().require("perditio"))),"Materials and centivis consumed once");
            t.assertTrue(!machine.workstation.craft(null),"Cannot craft again without inputs");t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void providersHonorPowerAndAvoidRecursiveStorage(GameTestHelper t){
        var drive=network(t,new BlockPos(2,2,2));t.setBlock(new BlockPos(3,2,2),Machines.ALL.get("infusion_provider").get());
        var provider=(NetworkMachine)t.getBlockEntity(new BlockPos(3,2,2));
        t.runAfterDelay(80,()->{
            var storage=drive.getMainNode().getGrid().getStorageService().getInventory();storage.insert(air(),20,Actionable.MODULATE,SOURCE);
            t.assertTrue(provider.contains(air().aspect(),20),"Infusion consumer sees powered reservoir");
            t.assertTrue(provider.takeFromContainer(air().aspect(),8)&&storage.getAvailableStacks().get(air())==12,"Remote draw commits once");
            t.assertTrue(EssentiaStorageApi.at(t.getLevel(),provider.getBlockPos(),Direction.UP).contents().isEmpty(),"ME source cannot be recursively mounted");
            provider.node().destroy();t.assertTrue(!provider.takeFromContainer(air().aspect(),1),"Disconnected provider cannot extract");t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void transportProviderIsBidirectional(GameTestHelper t){
        var drive=network(t,new BlockPos(2,2,2));t.setBlock(new BlockPos(3,2,2),Machines.ALL.get("essentia_provider").get());
        var provider=(NetworkMachine)t.getBlockEntity(new BlockPos(3,2,2));
        t.runAfterDelay(100,()->{
            t.assertTrue(provider.canInput(Direction.UP)&&provider.canOutput(Direction.UP),"Provider accepts both directions");
            t.assertTrue(provider.addEssentia(air().aspect(),19,Direction.UP)==19,"Pipe input reaches ME");
            t.assertTrue(provider.takeEssentia(air().aspect(),7,Direction.DOWN)==7&&drive.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(air())==12,"Pipe output conserves remaining essentia");t.succeed();});
    }
    @GameTest(template="test_empty",timeoutTicks=200) public static void vibrationChamberBootstrapsUnpoweredGrid(GameTestHelper t){
        t.setBlock(new BlockPos(2,2,2),AEBlocks.ENERGY_CELL.block());t.setBlock(new BlockPos(3,2,2),Machines.ALL.get("essentia_vibration_chamber").get());
        var machine=(NetworkMachine)t.getBlockEntity(new BlockPos(3,2,2));var fire=TCAspects.catalog().require("ignis");
        machine.insert(fire,1,false);
        t.runAfterDelay(80,()->{
            t.assertTrue(machine.node().getGrid()!=null&&((appeng.blockentity.networking.EnergyCellBlockEntity)t.getBlockEntity(new BlockPos(2,2,2))).getAECurrentPower()>0,"Generator supplies an initially unpowered grid");
            t.assertTrue(machine.contents().amount(fire)==0,"Exactly one fuel consumed");
            var nbt=machine.saveWithFullMetadata();t.assertTrue(nbt.getDouble("energy")>0&&nbt.getDouble("energy")<4000,"Unspent burn energy persists");t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=220) public static void wirelessCoverageAndBackpackPersistence(GameTestHelper t){
        var drive=network(t,new BlockPos(2,2,2));drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_1K.stack());
        var pos=new BlockPos(3,2,2);t.setBlock(pos,AEBlocks.WIRELESS_ACCESS_POINT.block());
        var ap=(appeng.api.implementations.blockentities.IWirelessAccessPoint)t.getBlockEntity(pos);
        t.runAfterDelay(10,()->appeng.api.networking.GridHelper.createConnection(drive.getMainNode().getNode(),ap.getActionableNode()));
        t.runAfterDelay(100,()->{
            var golem=TCEntities.GOLEM.get().create(t.getLevel());var location=t.absolutePos(new BlockPos(4,2,2));golem.moveTo(location.getX()+.5,location.getY(),location.getZ()+.5,0,0);
            var config=new GolemConfiguration(GolemMaterial.WOOD,false);config.installCore(GolemCoreType.GATHER);golem.state().configure(config);
            var backpack=new ItemStack(TE.MATERIALS.get("golem_wireless_backpack").get());((temodern.golem.WirelessBackpack)backpack.getItem()).link(backpack,GlobalPos.of(t.getLevel().dimension(),t.absolutePos(pos)));
            thaumcraft.api.golems.GolemApi.data(golem,temodern.golem.WirelessBackpack.ID,backpack.save(new CompoundTag()));
            thaumcraft.api.golems.GolemApi.configure(golem,null,temodern.golem.WirelessBackpack.ID,null);
            t.assertTrue(temodern.golem.WirelessWork.access(golem)!=null,"Linked active access point covers golem");
            golem.carried(new ItemStack(Items.IRON_INGOT,7));
            temodern.golem.WirelessWork.transfer(golem);
            t.assertTrue(golem.carried().isEmpty()&&drive.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(AEItemKey.of(Items.IRON_INGOT))==7,"Wireless gather moves real cargo exactly once");
            var tag=new CompoundTag();golem.saveWithoutId(tag);var restored=TCEntities.GOLEM.get().create(t.getLevel());restored.load(tag);
            t.assertTrue(!temodern.golem.WirelessBackpack.data(restored).isEmpty()&&restored.configuration().customTask().equals(temodern.golem.WirelessBackpack.ID),"Backpack and task survive entity NBT");
            golem.moveTo(location.getX()+1000,location.getY(),location.getZ(),0,0);
            t.assertTrue(temodern.golem.WirelessWork.access(golem)==null,"Outside coverage rejects access");
            golem.carried(new ItemStack(Items.GOLD_INGOT,3));temodern.golem.WirelessWork.transfer(golem);
            t.assertTrue(golem.carried().getCount()==3,"Out-of-range cargo retained");golem.discard();restored.discard();t.succeed();
        });
    }
    private static int assemblerStoredVis(NetworkMachine machine,tc4revival.aspect.Aspect aspect){
        return machine.storedVis(aspect);
    }
    @GameTest(template="test_empty",timeoutTicks=600) public static void realCpuUsesKnowledgeCoreWithoutPatternProvider(GameTestHelper t){
        var drive=network(t,new BlockPos(2,2,2));drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_4K.stack());
        t.setBlock(new BlockPos(3,2,2),Machines.ALL.get("creative_vis_interface").get());
        t.setBlock(new BlockPos(4,2,2),Machines.ALL.get("arcane_assembler").get());
        t.setBlock(new BlockPos(2,3,2),AEBlocks.CRAFTING_STORAGE_1K.block());
        var machine=(NetworkMachine)t.getBlockEntity(new BlockPos(4,2,2));var vis=(NetworkMachine)t.getBlockEntity(new BlockPos(3,2,2));
        machine.workstation.inventory.setItem(9,((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,false));
        var sample=new CraftGrid();var mercury=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:quicksilver_drop"));
        for(int i=0;i<3;i++)sample.setItem(i,new ItemStack(mercury));sample.setItem(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));sample.setItem(4,AEItems.ANNIHILATION_CORE.stack());
        var recipe=t.getLevel().getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().toString().equals("thaumicenergistics:diffusion_core")).findFirst().orElseThrow();
        var core=new ItemStack(TE.MATERIALS.get("knowledge_core").get());temodern.item.KnowledgeCore.toggle(core,ArcanePattern.encode(sample,recipe,t.getLevel()),t.getLevel());machine.workstation.inventory.setItem(10,core);
        final java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan>> calculation=new java.util.concurrent.atomic.AtomicReference<>();
        t.runAfterDelay(100,()->{
            var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(t.getLevel());var card=AEItems.MEMORY_CARD.stack();
            player.setShiftKeyDown(true);vis.memoryCard(player,card);player.setShiftKeyDown(false);machine.memoryCard(player,card);
            var grid=machine.node().getGrid();var storage=grid.getStorageService().getInventory();
            for(var item:sample.getItems())if(!item.isEmpty())t.assertTrue(storage.insert(AEItemKey.of(item),1,Actionable.MODULATE,SOURCE)==1,"CPU input inserted");
            calculation.set(grid.getCraftingService().beginCraftingCalculation(t.getLevel(),machine,AEItemKey.of(TE.MATERIALS.get("diffusion_core").get()),1,appeng.api.networking.crafting.CalculationStrategy.REPORT_MISSING_ITEMS));
        });
        var submitted=new java.util.concurrent.atomic.AtomicBoolean();
        t.onEachTick(()->{
            var future=calculation.get();if(future==null)return;
            if(!submitted.get()&&future.isDone())try{
                var plan=future.get();t.assertTrue(!plan.simulation(),"Real CPU plan contains no missing ingredients");
                var result=machine.node().getGrid().getCraftingService().submitJob(plan,null,null,false,machine.action());
                t.assertTrue(result.successful(),"CPU submission accepted: "+result.errorCode());submitted.set(true);
            }catch(Exception e){throw new RuntimeException(e);}
            if(submitted.get()&&machine.node().getGrid().getStorageService().getInventory().getAvailableStacks().get(AEItemKey.of(TE.MATERIALS.get("diffusion_core").get()))==1)t.succeed();
        });
    }

}
