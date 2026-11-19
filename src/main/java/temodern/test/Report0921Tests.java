package temodern.test;

import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import temodern.TE;
import temodern.crafting.*;
import temodern.machine.*;

@GameTestHolder(TE.ID) @PrefixGameTestTemplate(false)
public class Report0921Tests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    @GameTest(template="test_empty",timeoutTicks=220)
    public static void wirelessRangePowerPersistenceWandPriorityAndCharging(GameTestHelper t){
        t.setBlock(POS,AEBlocks.CABLE_BUS.block());t.setBlock(POS.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cable=(CableBusBlockEntity)t.getBlockEntity(POS);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
        t.setBlock(POS.above(),AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.UP));
        var source=(VisInterfacePart)cable.addPart((appeng.api.parts.IPartItem<?>)TE.ITEMS.getEntries().stream().filter(e->e.getId().getPath().equals("creative_vis_interface")).findFirst().orElseThrow().get(),Direction.SOUTH,null);
        t.runAfterDelay(100,()->{
            var p=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TEWireless"));p.setPos(t.absolutePos(POS).getCenter());
            var stack=new ItemStack(TE.MATERIALS.get("wireless_arcane_terminal").get());p.getInventory().setItem(0,stack);
            var item=(temodern.item.WirelessArcaneTerminalItem)stack.getItem();item.injectAEPower(stack,10000,appeng.api.config.Actionable.MODULATE);
            appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER.link(stack,GlobalPos.of(t.getLevel().dimension(),t.absolutePos(POS.above())));
            var host=new WirelessArcaneHost(p,0,stack,(who,menu)->{});t.assertTrue(host.active(),"Linked powered terminal reaches the native access point");
            var wandItem=(CastingWandItem)TCItems.CASTING_WAND.get();var internal=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false);
            var inventory=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);p.getInventory().setItem(1,inventory);host.workstation().inventory.setItem(9,internal);
            var work=host.workstation();
            for(int i=0;i<3;i++)work.inventory.setItem(i,new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER)));
            work.inventory.setItem(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));work.inventory.setItem(4,AEItems.ANNIHILATION_CORE.stack());
            var knowledge=tc4revival.research.KnowledgeSavedData.get(p.getServer());knowledge.update(p.getUUID(),n->0,k->k.rememberResearch("thaumicenergistics:CORES"));
            t.assertTrue(work.manualPreview(p).output().isEmpty(),"Inventory wands are disabled by default");host.inventoryWands(true);
            var expected=work.manualPreview(p).output();t.assertTrue(expected.is(TE.MATERIALS.get("diffusion_core").get()),"Falls back to one fully charged inventory wand");
            var entropy=tc4revival.aspect.TCAspects.catalog().require("perditio");int before=wandItem.vis(inventory,entropy);
            int cost=work.manualPreview(p).costs().get(entropy);
            internal.getOrCreateTag().putInt("perditio",cost-1);inventory.getOrCreateTag().putInt("perditio",cost-1);
            t.assertTrue(work.manualPreview(p).output().isEmpty(),"Two insufficient wands must never pool vis");
            internal.getOrCreateTag().putInt("perditio",0);inventory.getOrCreateTag().putInt("perditio",before);
            t.assertTrue(work.takeManualResult(p,expected,s->true,s->p.getInventory().add(s)),"Server transaction can consume the inventory wand");
            t.assertTrue(wandItem.vis(inventory,entropy)<before&&wandItem.vis(internal,entropy)==0,"Only the selected affordable wand is charged");
            for(String id:java.util.List.of("wireless_arcane_terminal","vis_charging_card")){
                var recipe=t.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumicenergistics:"+id)).orElseThrow();
                t.assertTrue(recipe instanceof tc4revival.content.ArcaneRecipe,"New item has a loaded server arcane recipe: "+id);
            }
            host.charge();t.assertTrue(wandItem.vis(internal,entropy)==0,"No charging without a vis card");
            t.assertTrue(host.getUpgrades().addItems(new ItemStack(TE.MATERIALS.get("vis_charging_card").get())).isEmpty(),"Native upgrade inventory accepts the vis card");
            host.charge();t.assertTrue(wandItem.vis(internal,entropy)>0,"Same-grid input vis charges the inserted wand");
            t.assertTrue(wandItem.vis(inventory,entropy)==before-cost,"Charging must not recharge the player's external wand");
            double capacity=item.getAEMaxPower(stack);
            t.assertTrue(host.getUpgrades().addItems(AEItems.ENERGY_CARD.stack(2)).isEmpty(),"Two energy cards fit alongside the vis card");
            t.assertTrue(item.getAEMaxPower(stack)>capacity,"Energy cards increase actual battery capacity");
            t.assertTrue(!host.getUpgrades().addItems(AEItems.ENERGY_CARD.stack()).isEmpty(),"A third energy card is rejected");
            var reopened=new WirelessArcaneHost(p,0,stack,(who,menu)->{});
            t.assertTrue(reopened.inventoryWands()&&wandItem.vis(reopened.workstation().inventory.getItem(9),entropy)>0,"Reopening preserves settings and charged wand NBT");
            var menu=new ArcaneTerminalMenu(1,p.getInventory(),reopened);p.containerMenu=menu;
            t.assertTrue(menu.wireless()&&menu.getSlots(ArcaneTerminalMenu.ARMOR).isEmpty(),"Wireless terminal has no armor storage slots");
            t.assertTrue(menu.getSlots(ArcaneTerminalMenu.WIRELESS_UPGRADES).size()==4&&menu.getSlots(ArcaneTerminalMenu.SINGULARITY).size()==1,"All upgrade slots and the singularity slot are reachable in the actual menu");
            var physicalSlots=menu.slots.stream().filter(appeng.menu.slot.AppEngSlot.class::isInstance).map(appeng.menu.slot.AppEngSlot.class::cast).toList();
            t.assertTrue(physicalSlots.size()==physicalSlots.stream().map(slot->java.util.List.of(slot.getInventory(),slot.getContainerSlot())).distinct().count(),"Each physical AE inventory slot appears once in the menu");
            var singularitySlot=menu.getSlots(ArcaneTerminalMenu.SINGULARITY).get(0);
            t.assertTrue(!singularitySlot.mayPlace(new ItemStack(Items.STONE))&&singularitySlot.mayPlace(AEItems.QUANTUM_ENTANGLED_SINGULARITY.stack()),"Only an entangled singularity can be inserted");
            p.setPos(t.absolutePos(POS).getCenter().add(1000,0,0));t.assertTrue(!reopened.active()&&!menu.stillValid(p),"Leaving native wireless range invalidates the menu");
            p.setPos(t.absolutePos(POS).getCenter());item.extractAEPower(stack,Double.MAX_VALUE,appeng.api.config.Actionable.MODULATE);
            t.assertTrue(!reopened.active(),"An empty battery cannot operate");p.containerMenu=p.inventoryMenu;t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=180)
    public static void jeiTransferUsesRealInventoryAndAssemblerSupportsFiveUpgrades(GameTestHelper t){
        t.setBlock(POS,Machines.ALL.get("arcane_terminal").get());t.setBlock(POS.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var machine=(NetworkMachine)t.getBlockEntity(POS);
        t.runAfterDelay(100,()->{
            var p=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TETransfer"));p.setPos(t.absolutePos(POS).getCenter());
            p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,3));
            p.getInventory().setItem(1,((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true));
            var menu=new ArcaneTerminalMenu(1,p.getInventory(),machine);p.containerMenu=menu;
            var ingredients=new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();ingredients.add(net.minecraft.world.item.crafting.Ingredient.of(Items.OAK_LOG));
            while(ingredients.size()<9)ingredients.add(net.minecraft.world.item.crafting.Ingredient.EMPTY);
            menu.fillCraftingGrid(ingredients);
            t.assertTrue(machine.workstation.inventory.getItem(0).is(Items.OAK_LOG)&&p.getInventory().countItem(Items.OAK_LOG)==2,"JEI moves one actual ingredient without duplication");
            t.assertTrue(!menu.wand().isEmpty()&&p.getInventory().getItem(1).isEmpty(),"JEI moves the owned wand to the empty wand slot");
            menu.clearGrid(true);t.assertTrue(machine.workstation.inventory.getItem(0).isEmpty()&&p.getInventory().countItem(Items.OAK_LOG)==3,"Take-items button returns the exact input");
            p.containerMenu=p.inventoryMenu;
            t.setBlock(POS.east(2),Machines.ALL.get("arcane_assembler").get());var assembler=(NetworkMachine)t.getBlockEntity(POS.east(2));
            t.assertTrue(assembler.getUpgrades().addItems(AEItems.SPEED_CARD.stack(5)).isEmpty(),"Assembler accepts all five native acceleration slots");
            t.assertTrue(assembler.workstation.upgrades()==5&&assembler.workstation.workInterval()>0,"Five acceleration cards have a nonzero work interval");
            var copy=new NetworkMachine(assembler.getBlockPos(),assembler.getBlockState());copy.load(assembler.saveWithoutMetadata());
            t.assertTrue(copy.getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD)==5,"All acceleration cards survive save/load");
            var asmMenu=new ArcaneAssemblerMenu(2,p.getInventory(),assembler);t.assertTrue(asmMenu.getUpgrades().size()==5,"Native machine menu exposes five upgrade slots");
            t.succeed();
        });
    }
}
