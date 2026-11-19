package temodern.test;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.util.AEColor;
import appeng.core.definitions.*;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import tc4revival.research.*;
import temodern.TE;
import temodern.essentia.*;
import temodern.machine.*;
import temodern.crafting.*;
import temodern.item.*;

@GameTestHolder(TE.ID) @PrefixGameTestTemplate(false)
public final class PortIntegrationTests {
    @GameTest(template="test_empty") public static void assemblerMenuSynchronizesWithoutUnsupportedSettings(GameTestHelper t){
        var pos=new BlockPos(2,2,2);t.setBlock(pos,Machines.ALL.get("arcane_assembler").get());
        var host=(NetworkMachine)t.getBlockEntity(pos);var p=player(t,"TE0922",pos);
        var menu=new ArcaneAssemblerMenu(1,p.getInventory(),host);
        menu.broadcastChanges();menu.broadcastChanges();
        t.assertTrue(menu.getSlots(appeng.menu.SlotSemantics.MACHINE_CRAFTING_GRID).size()==9,"All nine crafting slots are exposed to the native screen");
        t.succeed();
    }
    private static final IActionSource SOURCE=IActionSource.empty();
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper t,String name,BlockPos pos){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),name));
        player.setPos(t.absolutePos(pos).getCenter());return player;
    }
    private static CraftGrid diffusion(){
        var sample=new CraftGrid();var mercury=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:quicksilver_drop"));
        for(int i=0;i<3;i++)sample.setItem(i,new ItemStack(mercury));sample.setItem(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));sample.setItem(4,AEItems.ANNIHILATION_CORE.stack());return sample;
    }
    private static ArcaneRecipe diffusionRecipe(GameTestHelper t){return t.getLevel().getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().toString().equals("thaumicenergistics:diffusion_core")).findFirst().orElseThrow();}
    @GameTest(template="test_empty") public static void ghostInscriberRequiresKnowledgeAndNeverDropsSamples(GameTestHelper t){
        var pos=new BlockPos(2,2,2);t.setBlock(pos,Machines.ALL.get("knowledge_inscriber").get());var machine=(NetworkMachine)t.getBlockEntity(pos);
        var player=player(t,"TEGhost",pos);var knowledge=KnowledgeSavedData.get(player.getServer());knowledge.ensure(player.getUUID(),n->0);
        var core=new ItemStack(TE.MATERIALS.get("knowledge_core").get());machine.workstation.inventory.setItem(10,core);
        var menu=new InscriberMenu(1,player.getInventory(),machine.workstation.inventory,t.absolutePos(pos));
        var sample=diffusion();for(int i=0;i<9;i++){
            menu.setCarried(sample.getItem(i).copy());menu.clicked(i+1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            t.assertTrue(ItemStack.matches(menu.getCarried(),sample.getItem(i)),"Ghost click leaves cursor unchanged");
        }
        t.assertTrue(!menu.clickMenuButton(player,0)&&KnowledgeCore.patterns(core).isEmpty(),"Unknown research cannot be written");
        knowledge.update(player.getUUID(),n->0,k->k.rememberResearch("thaumicenergistics:CORES"));
        t.assertTrue(menu.clickMenuButton(player,0)&&KnowledgeCore.patterns(core).size()==1,"Known recipe saved to core");
        t.assertTrue(menu.saveState()==4&&menu.saveRevision()==1&&!menu.preview().isEmpty(),"Server synchronizes delete mode, save animation revision and output preview");
        var output=menu.preview().copy();menu.clicked(46,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
        t.assertTrue(ItemStack.matches(output,menu.preview()),"Preview output cannot be taken as a real item");
        t.assertTrue(menu.clickMenuButton(player,1),"Stored output reloads matrix");
        t.assertTrue(menu.clickMenuButton(player,0)&&KnowledgeCore.patterns(core).isEmpty(),"Saving same output toggles removal");
        t.assertTrue(menu.saveState()==3&&menu.saveRevision()==1,"Deletion restores save mode without playing a save animation");
        machine.workstation.dropContents();
        var drops=t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(t.absolutePos(pos)).inflate(1));
        t.assertTrue(drops.stream().allMatch(e->e.getItem().getItem() instanceof KnowledgeCore)&&drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Breaking ghost workstation drops only the physical core");t.succeed();
    }
    @GameTest(template="test_empty") public static void microscopeWritesRealKnowledgeOnce(GameTestHelper t){
        var player=player(t,"TEScan",new BlockPos(2,2,2));var data=KnowledgeSavedData.get(player.getServer());data.ensure(player.getUUID(),n->0);
        data.update(player.getUUID(),n->0,k->{TCAspects.catalog().all().forEach(k::discover);return true;});
        var before=data.snapshot(player.getUUID()).orElseThrow().scannedKeys(PlayerKnowledge.ScanKind.OBJECT).size();
        CellMicroscope.scanStoredItem(player,new ItemStack(Items.DIRT));
        var after=data.snapshot(player.getUUID()).orElseThrow().scannedKeys(PlayerKnowledge.ScanKind.OBJECT).size();
        t.assertTrue(after==before+1,"Cell contents produce a real object scan identity");
        CellMicroscope.scanStoredItem(player,new ItemStack(Items.DIRT,64));
        t.assertTrue(data.snapshot(player.getUUID()).orElseThrow().scannedKeys(PlayerKnowledge.ScanKind.OBJECT).size()==after,"Repeat stacks cannot duplicate scan identities");t.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=180) public static void cableTerminalHostsNativeMenuAndPreservesInventory(GameTestHelper t){
        var pos=new BlockPos(2,2,2);t.setBlock(pos,AEBlocks.CABLE_BUS.block());t.setBlock(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cable=(CableBusBlockEntity)t.getBlockEntity(pos);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
        var item=(appeng.api.parts.IPartItem<ArcaneTerminalPart>)net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumicenergistics:arcane_terminal"));
        var terminal=cable.addPart(item,Direction.SOUTH,null);terminal.workstation().inventory.setItem(0,new ItemStack(Items.DIAMOND,3));
        t.runAfterDelay(100,()->{
            t.assertTrue(terminal.node().isActive(),"Cable terminal has an active native grid node");
            var player=player(t,"TETerminal",pos);var menu=new ArcaneTerminalMenu(1,player.getInventory(),terminal);
            t.assertTrue(menu.stillValid(player),"Native terminal menu accepts cable part locator");
            var tag=cable.saveWithFullMetadata();var other=pos.south();t.setBlock(other,AEBlocks.CABLE_BUS.block());
            var restoredCable=(CableBusBlockEntity)t.getBlockEntity(other);restoredCable.load(tag);var restored=(ArcaneTerminalPart)restoredCable.getPart(Direction.SOUTH);
            t.assertTrue(restored.workstation().inventory.getItem(0).getCount()==3,"Part inventory survives NBT");
            var drops=new ArrayList<ItemStack>();terminal.addAdditionalDrops(drops,true);terminal.clearContent();
            t.assertTrue(drops.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==3&&terminal.workstation().inventory.isEmpty(),"Wrench drops and clears physical grid exactly once");t.succeed();
        });
    }
    @GameTest(template="test_empty") public static void encoderAndFacadesProduceUsableNativeRecipes(GameTestHelper t){
        var aspects=AlchemyFurnaceBlockEntity.inputAspects(new ItemStack(Items.DIRT));
        var encoded=appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(new GenericStack[]{new GenericStack(AEItemKey.of(Items.DIRT),1)},aspects.aspects().stream().map(aspect->new GenericStack(EssentiaKey.of(aspect),aspects.amount(aspect))).toArray(GenericStack[]::new));
        var pattern=appeng.api.crafting.PatternDetailsHelper.decodePattern(encoded,t.getLevel());
        t.assertTrue(pattern!=null&&pattern.getOutputs().length>0&&pattern.getOutputs()[0].what() instanceof EssentiaKey,"Native processing pattern supports distillation outputs without removed encoder");
        var recipe=t.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumicenergistics:facade_warded_glass")).orElseThrow();
        var facade=recipe.getResultItem(t.getLevel().registryAccess());
        t.assertTrue(facade.getItem() instanceof appeng.items.parts.FacadeItem item&&item.getTextureItem(facade).is(TCItems.WARDED_GLASS.get())&&facade.getCount()==4,"Facade recipe preserves native texture NBT");
        t.succeed();
    }
    @GameTest(template="test_empty") public static void assemblerUsesModernAttributesAndGeneratorRejectsMixedFuel(GameTestHelper t){
        var pos=new BlockPos(2,2,2);t.setBlock(pos,Machines.ALL.get("arcane_assembler").get());var assembler=(NetworkMachine)t.getBlockEntity(pos);
        assembler.workstation.inventory.setItem(9,((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true));
        var air=TCAspects.catalog().require("aer");float base=assembler.workstation.modifier(air);
        var armor=new ItemStack(Items.DIAMOND_HELMET);armor.addAttributeModifier(TCAttributes.VIS_COST.get(),new net.minecraft.world.entity.ai.attributes.AttributeModifier(UUID.randomUUID(),"TE test discount",-.2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),net.minecraft.world.entity.EquipmentSlot.HEAD);assembler.workstation.inventory.setItem(18,armor);
        t.assertTrue(assembler.workstation.modifier(air)<base,"Vanilla attribute modifier reduces arcane cost without legacy armor interface");
        var genPos=pos.east();t.setBlock(genPos,Machines.ALL.get("essentia_vibration_chamber").get());var generator=(NetworkMachine)t.getBlockEntity(genPos);var fire=TCAspects.catalog().require("ignis");var power=TCAspects.catalog().require("potentia");
        t.assertTrue(generator.insert(fire,8,false)==8&&generator.insert(power,1,true)==0&&generator.insert(power,1,false)==0,"Generator rejects mixing fuels in both modes");
        t.assertTrue(!generator.canInput(Direction.NORTH)&&generator.canInput(Direction.UP)&&generator.suction(Direction.UP)>64,"Generator front rejects pipes and suction depends on fill");t.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=1400) public static void advancedProviderCraftsThroughRealFurnaceAndImportBus(GameTestHelper t){
        var drivePos=new BlockPos(2,2,2);t.setBlock(drivePos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());t.setBlock(drivePos,AEBlocks.DRIVE.block());
        var drive=(DriveBlockEntity)t.getBlockEntity(drivePos);drive.getInternalInventory().setItemDirect(0,new ItemStack(TE.CELLS.get("1k").get()));drive.getInternalInventory().setItemDirect(1,AEItems.ITEM_CELL_4K.stack());
        t.setBlock(drivePos.above(),AEBlocks.CRAFTING_STORAGE_1K.block());var providerPos=new BlockPos(3,2,2);t.setBlock(providerPos,AEBlocks.PATTERN_PROVIDER.block());
        var provider=(appeng.blockentity.crafting.PatternProviderBlockEntity)t.getBlockEntity(providerPos);
        var furnacePos=providerPos.east();t.setBlock(furnacePos,TCArcane.ALCHEMY_FURNACE.get());var furnace=(AlchemyFurnaceBlockEntity)t.getBlockEntity(furnacePos);furnace.setItem(1,new ItemStack(Items.COAL));
        t.setBlock(furnacePos.above(),TCArcane.ALEMBIC.get());
        var cablePos=providerPos.above();t.setBlock(cablePos,AEBlocks.CABLE_BUS.block());var cable=(CableBusBlockEntity)t.getBlockEntity(cablePos);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);cable.addPart(AEParts.IMPORT_BUS.asItem(),Direction.EAST,null);
        var requestPos=drivePos.south();t.setBlock(requestPos,Machines.ALL.get("infusion_provider").get());var requester=(NetworkMachine)t.getBlockEntity(requestPos);
        var aspect=TCAspects.catalog().require("terra");var amount=AlchemyFurnaceBlockEntity.inputAspects(new ItemStack(Items.DIRT)).amount(aspect);
        t.assertTrue(amount>0,"Dirt has baseline earth essentia");
        var pattern=appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(new GenericStack[]{new GenericStack(AEItemKey.of(Items.DIRT),1)},new GenericStack[]{new GenericStack(EssentiaKey.of(aspect),amount)});
        provider.getLogic().getPatternInv().setItemDirect(0,pattern);
        t.runAfterDelay(100,()->{
            drive.getMainNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.DIRT),8,Actionable.MODULATE,SOURCE);
            t.assertTrue(!requester.takeFromContainer(aspect,8),"Missing essentia starts autonomous crafting request");
        });
        t.succeedWhen(()->{
            t.assertTrue(requester.contains(aspect,8),"CPU, provider, real furnace, alembic and ordinary import bus deliver requested essentia");
            t.assertTrue(requester.takeFromContainer(aspect,8),"Infusion consumer receives exactly requested amount");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=650) public static void wirelessLiquidAiFillsRealCrucible(GameTestHelper t){
        var drivePos=new BlockPos(2,2,2);t.setBlock(drivePos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());t.setBlock(drivePos,AEBlocks.DRIVE.block());var drive=(DriveBlockEntity)t.getBlockEntity(drivePos);drive.getInternalInventory().setItemDirect(0,AEItems.FLUID_CELL_1K.stack());
        var accessPos=drivePos.east();t.setBlock(accessPos,AEBlocks.WIRELESS_ACCESS_POINT.block());var access=(appeng.api.implementations.blockentities.IWirelessAccessPoint)t.getBlockEntity(accessPos);
        var cruciblePos=new BlockPos(4,1,2);t.setBlock(cruciblePos,TCArcane.CRUCIBLE.get());var crucible=(CrucibleBlockEntity)t.getBlockEntity(cruciblePos);
        for(int x=3;x<=5;x++)for(int z=1;z<=3;z++)t.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);
        final GolemEntity[] holder=new GolemEntity[1];
        t.runAfterDelay(10,()->appeng.api.networking.GridHelper.createConnection(drive.getMainNode().getNode(),access.getActionableNode()));
        t.runAfterDelay(100,()->{
            var water=AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER);t.assertTrue(drive.getMainNode().getGrid().getStorageService().getInventory().insert(water,1000,Actionable.MODULATE,SOURCE)==1000,"Water stored");
            var golem=TCEntities.GOLEM.get().create(t.getLevel());var home=t.absolutePos(cruciblePos.above());golem.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,0,0);
            var config=new GolemConfiguration(GolemMaterial.WOOD,false);config.installCore(GolemCoreType.LIQUID);golem.state().configure(config);golem.state().home(home,Direction.UP);
            var backpack=new ItemStack(TE.MATERIALS.get("golem_wireless_backpack").get());((temodern.golem.WirelessBackpack)backpack.getItem()).link(backpack,GlobalPos.of(t.getLevel().dimension(),t.absolutePos(accessPos)));
            thaumcraft.api.golems.GolemApi.data(golem,temodern.golem.WirelessBackpack.ID,backpack.save(new CompoundTag()));thaumcraft.api.golems.GolemApi.configure(golem,null,temodern.golem.WirelessBackpack.ID,null);holder[0]=golem;t.getLevel().addFreshEntity(golem);
        });
        t.succeedWhen(()->{
            t.assertTrue(crucible.fluids().getFluidInTank(0).getAmount()==1000,"Actual golem AI pulls and delivers 1000mB from ME to crucible: tank="+crucible.fluids().getFluidInTank(0).getAmount()+", held="+(holder[0]==null?-1:holder[0].fluid().getAmount())+", age="+(holder[0]==null?-1:holder[0].tickCount));
            t.assertTrue(holder[0].fluid().isEmpty()&&drive.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER))==0,"No duplicate water or stranded cargo");holder[0].discard();
        });
    }
    @GameTest(template="test_empty") public static void wrenchRotationChargesOnlyStartNotRelease(GameTestHelper t){
        var pos=new BlockPos(3,2,3);t.setBlock(pos,Machines.ALL.get("vis_interface").get());var player=player(t,"TEWrench",pos.north(2));
        var wandItem=(CastingWandItem)TCItems.CASTING_WAND.get();var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);WandFocusItem.attach(wand,new ItemStack(TE.MATERIALS.get("wrench_focus").get()));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);player.setShiftKeyDown(true);player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,t.absolutePos(pos).getCenter());
        var air=TCAspects.catalog().require("aer");int before=wandItem.vis(wand,air);
        var start=new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,t.absolutePos(pos),Direction.NORTH,net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        WrenchFocus.rotate(start);int after=wandItem.vis(wand,air);var facing=t.getBlockState(pos).getValue(MachineBlock.FACING);
        t.assertTrue(facing==Direction.EAST&&after<before,"Left focus rotates and charges actual centivis: facing="+facing+", before="+before+", after="+after+", look="+player.getLookAngle());
        WrenchFocus.rotate(new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,t.absolutePos(pos),Direction.NORTH,net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.ABORT));
        t.assertTrue(wandItem.vis(wand,air)==after&&t.getBlockState(pos).getValue(MachineBlock.FACING)==facing,"Releasing click does not rotate or charge again");
        t.assertTrue(player.getMainHandItem()==wand&&player.isShiftKeyDown(),"Temporary wrench and sneak overrides restored");t.succeed();
    }
}
