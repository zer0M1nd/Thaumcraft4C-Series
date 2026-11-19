package temodern.test;

import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;
import temodern.TE;
import temodern.crafting.*;
import temodern.machine.*;

@GameTestHolder(TE.ID) @PrefixGameTestTemplate(false)
public class ResourceReviewTests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static CableBusBlockEntity cable(GameTestHelper t){
        t.setBlock(POS,AEBlocks.CABLE_BUS.block());t.setBlock(POS.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cable=(CableBusBlockEntity)t.getBlockEntity(POS);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);return cable;
    }
    private static ArcaneTerminalPart terminal(CableBusBlockEntity cable){
        var item=(IPartItem<?>)net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumicenergistics:arcane_terminal"));
        return (ArcaneTerminalPart)cable.addPart(item,Direction.SOUTH,null);
    }
    private static ServerPlayer player(GameTestHelper t){
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TEReview"));
        player.setPos(t.absolutePos(POS).getCenter());return player;
    }
    private static void diffusion(Workstation work){
        for(int i=0;i<3;i++)work.inventory.setItem(i,new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER)));
        work.inventory.setItem(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));work.inventory.setItem(4,AEItems.ANNIHILATION_CORE.stack());
    }
    @GameTest(template="test_empty",timeoutTicks=180) public static void resultPreviewRechecksKnowledgeVisAndCursorBeforeTaking(GameTestHelper t){
        var terminal=terminal(cable(t));var work=terminal.workstation();diffusion(work);
        var wandItem=(CastingWandItem)TCItems.CASTING_WAND.get();var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);
        work.inventory.setItem(9,wand);
        t.runAfterDelay(100,()->{
            var p=player(t);var knowledge=KnowledgeSavedData.get(p.getServer());knowledge.ensure(p.getUUID(),n->0);
            var menu=new ArcaneTerminalMenu(1,p.getInventory(),terminal);p.containerMenu=menu;
            t.assertTrue(menu.resultSlot().getItem().isEmpty(),"Missing research hides the craft result");
            knowledge.update(p.getUUID(),n->0,k->k.rememberResearch("thaumicenergistics:CORES"));menu.broadcastChanges();
            var expected=menu.resultSlot().getItem().copy();t.assertTrue(expected.is(TE.MATERIALS.get("diffusion_core").get()),"Known recipe is immediately takeable");
            var entropy=tc4revival.aspect.TCAspects.catalog().require("perditio");int before=wandItem.vis(wand,entropy);
            for(int i=0;i<5;i++)menu.broadcastChanges();
            t.assertTrue(wandItem.vis(wand,entropy)==before&&!work.inventory.getItem(0).isEmpty(),"Preview consumes neither materials nor vis");
            menu.setCarried(new ItemStack(Items.STONE,64));menu.takeResult("one");
            t.assertTrue(wandItem.vis(wand,entropy)==before&&!work.inventory.getItem(0).isEmpty(),"Incompatible cursor cannot consume a craft");
            menu.setCarried(ItemStack.EMPTY);wand.getOrCreateTag().putInt("perditio",0);menu.takeResult("one");
            t.assertTrue(menu.getCarried().isEmpty()&&menu.resultSlot().getItem().isEmpty(),"Vis removed after preview is rechecked before taking");
            wand.getOrCreateTag().putInt("perditio",before);menu.takeResult("one");
            t.assertTrue(menu.getCarried().is(expected.getItem())&&work.inventory.getItem(0).isEmpty(),"Taking output consumes the ingredients exactly once");
            t.assertTrue(wandItem.vis(wand,entropy)<before,"Taking pays wand vis");
            menu.takeResult("one");t.assertTrue(menu.getCarried().getCount()==expected.getCount(),"Repeated stale take cannot duplicate the result");
            p.containerMenu=p.inventoryMenu;t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=180) public static void shiftCraftChecksInventoryAndReturnsContainers(GameTestHelper t){
        var terminal=terminal(cable(t));var work=terminal.workstation();
        t.runAfterDelay(100,()->{
            var p=player(t);var menu=new ArcaneTerminalMenu(1,p.getInventory(),terminal);p.containerMenu=menu;
            work.inventory.setItem(0,new ItemStack(Items.OAK_LOG,2));
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            menu.takeResult("inventory");t.assertTrue(work.inventory.getItem(0).getCount()==2,"Full inventory rejects shift craft without consuming ingredients");
            p.getInventory().clearContent();menu.takeResult("inventory");
            t.assertTrue(p.getInventory().countItem(Items.OAK_PLANKS)==8&&work.inventory.getItem(0).isEmpty(),"Shift craft repeats the recipe into player inventory");
            for(int slot:new int[]{0,1,3,4})work.inventory.setItem(slot,new ItemStack(Items.HONEY_BOTTLE));
            menu.takeResult("one");
            t.assertTrue(menu.getCarried().is(Items.HONEY_BLOCK),"Ordinary recipes need no wand");
            for(int slot:new int[]{0,1,3,4})t.assertTrue(work.inventory.getItem(slot).is(Items.GLASS_BOTTLE),"Craft containers stay in their original grid slots");
            var helmet=new ItemStack(Items.IRON_HELMET);work.inventory.setItem(18,helmet);menu.swapArmor();
            t.assertTrue(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET)&&work.inventory.getItem(18).isEmpty(),"Legacy armor button exchanges stored and worn armor");
            menu.swapArmor();t.assertTrue(work.inventory.getItem(18).is(Items.IRON_HELMET),"Armor exchange is reversible");
            p.containerMenu=p.inventoryMenu;t.succeed();
        });
    }
    @GameTest(template="test_empty",timeoutTicks=230) public static void boundCableInterfaceChargesTerminalWandAndUnbindStopsIt(GameTestHelper t){
        var cable=cable(t);var terminal=terminal(cable);
        var source=(VisInterfacePart)cable.addPart((IPartItem<?>)TE.ITEMS.getEntries().stream().filter(e->e.getId().getPath().equals("creative_vis_interface")).findFirst().orElseThrow().get(),Direction.UP,null);
        var wandItem=(CastingWandItem)TCItems.CASTING_WAND.get();var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false);
        terminal.workstation().inventory.setItem(9,wand);var p=player(t);
        t.runAfterDelay(100,()->{
            var card=AEItems.MEMORY_CARD.stack();p.setShiftKeyDown(true);source.memoryCard(p,card);p.setShiftKeyDown(false);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,card);
            terminal.onPartActivate(p,net.minecraft.world.InteractionHand.MAIN_HAND,t.absolutePos(POS).getCenter());
            var saved=new net.minecraft.nbt.CompoundTag();terminal.writeToNBT(saved);
            t.assertTrue(saved.getUUID("linkedVis").equals(source.visIdentity()),"Memory-card activation binds the terminal instead of opening its menu");
            t.setBlock(POS.south(3),AEBlocks.CABLE_BUS.block());
            var restoredHost=(CableBusBlockEntity)t.getBlockEntity(POS.south(3));
            restoredHost.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
            var restored=terminal(restoredHost);restored.readFromNBT(saved);
            var copy=new net.minecraft.nbt.CompoundTag();restored.writeToNBT(copy);
            t.assertTrue(copy.getUUID("linkedVis").equals(source.visIdentity()),"Binding survives a fresh terminal NBT roundtrip");
        });
        t.runAfterDelay(145,()->{
            var current=terminal.workstation().inventory.getItem(9);
            for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())t.assertTrue(wandItem.vis(current,aspect)>0,"Ticking bound source charges "+aspect.tag());
            terminal.memoryCard(p,AEItems.MEMORY_CARD.stack());
            terminal.workstation().inventory.setItem(9,wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false));
        });
        t.runAfterDelay(190,()->{
            var current=terminal.workstation().inventory.getItem(9);
            for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())t.assertTrue(wandItem.vis(current,aspect)==0,"Cleared binding stops all charging");
            t.succeed();
        });
    }
    @GameTest(template="test_empty") public static void arcanePatternUsesNativeEncodedItemAndExposesInputsAndOutputs(GameTestHelper t){
        t.setBlock(POS,Machines.ALL.get("knowledge_inscriber").get());var work=((NetworkMachine)t.getBlockEntity(POS)).workstation;diffusion(work);
        var recipe=t.getLevel().getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().toString().equals("thaumicenergistics:diffusion_core")).findFirst().orElseThrow();
        var encoded=ArcanePattern.encode(work.grid(),recipe,t.getLevel());
        t.assertTrue(encoded.getItem() instanceof appeng.crafting.pattern.EncodedPatternItem,"Pattern inherits AE2 input/output tooltip handling");
        var item=(temodern.item.ArcanePatternItem)encoded.getItem();var decoded=item.decode(encoded,t.getLevel(),false);
        t.assertTrue(decoded!=null&&decoded.getInputs().length==5&&decoded.getOutputs().length>=1,"Decoded pattern contains every input and the output list");
        t.assertTrue(item.decode(appeng.api.stacks.AEItemKey.of(encoded),t.getLevel()).getPrimaryOutput().what().equals(appeng.api.stacks.AEItemKey.of(TE.MATERIALS.get("diffusion_core").get())),"Both native decoder entry points expose the same output");
        t.succeed();
    }
}
