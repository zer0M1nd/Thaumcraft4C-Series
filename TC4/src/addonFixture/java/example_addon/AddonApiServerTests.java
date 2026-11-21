package example_addon;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.level.GameType;
import tc4revival.aspect.*;
import tc4revival.content.*;
import tc4revival.research.*;
import thaumcraft.api.ThaumcraftApiHelper;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class AddonApiServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void addonJsonDefinitionsAndRealWandTransaction(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(1,2,1));
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AddonApi"));player.setGameMode(GameType.SURVIVAL);player.moveTo(pos.getCenter());
        var catalog=TCAspects.catalog();var custom=catalog.require("example_addon:insight");
        test.assertTrue(TCAspects.REGISTRY.get().getKey(custom).toString().equals("example_addon:insight"),"Addon namespace registered by Forge");
        test.assertTrue(catalog.combine(catalog.require("aer"),catalog.require("cognitio")).orElseThrow().equals(custom),"New aspect participates in combinations");
        test.assertTrue(tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Items.AMETHYST_SHARD)).orElseThrow().amount(custom)==4,"Addon item aspects use new aspect");
        test.assertTrue(ResearchCatalogs.catalog().find("example_addon:START").isPresent(),"Addon research loaded");
        test.assertTrue(ResearchDefinitions.server().pages().require("example_addon:START").size()==2,"Addon pages paired");
        test.assertTrue(ThaumcraftApiHelper.completeResearch(player,"example_addon:START"),"Trusted research completion persists");
        test.assertTrue(ThaumcraftApiHelper.isResearchComplete(player,"example_addon:START"),"Knowledge read sees committed change");
        level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);
        try {
            var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
            table.setItem(2,new ItemStack(Items.DIAMOND));table.setItem(4,new ItemStack(Items.AMETHYST_SHARD));table.setItem(6,new ItemStack(Items.DIAMOND));
            var payer=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);table.setItem(10,payer);
            var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;menu.refresh();
            test.assertTrue(!menu.getSlot(0).getItem().isEmpty(),"Real arcane recipe recognizes addon materials");
            menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            var result=menu.getCarried();test.assertTrue(WandParts.rod(result.getTag()).tag.equals("example_addon:crystal"),"Crafted custom rod");
            test.assertTrue(WandParts.cap(result.getTag()).tag.equals("example_addon:diamond"),"Crafted custom cap");
            test.assertTrue(TCItems.CASTING_WAND.get().capacity(result)==12000,"Custom capacity");
            test.assertTrue(table.getItem(2).isEmpty()&&table.getItem(4).isEmpty()&&table.getItem(6).isEmpty(),"Ingredients consumed");
            for(var aspect:catalog.primals())test.assertTrue(TCItems.CASTING_WAND.get().vis(payer,aspect)==7600,"Exact 24 vis crafting debit");
            var copy=ItemStack.of(result.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(WandParts.rod(copy.getTag()).tag.equals("example_addon:crystal"),"NBT reload preserves custom rod");
        } finally {player.containerMenu=player.inventoryMenu;level.removeBlock(pos,false);}
        tc4revival.TC4Revival.LOGGER.info("PASS addonJsonDefinitionsAndRealWandTransaction");test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void addonUpgradeAndApiPayments(GameTestHelper test) {
        var level=test.getLevel();var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AddonVis"));
        var focus=new ItemStack(TCItems.FIRE_FOCUS.get());var upgrade=FocusUpgrade.require("example_addon:economy");
        test.assertTrue(TCItems.FIRE_FOCUS.get().availableUpgrades(focus,1).contains(upgrade),"JSON offers upgrade on original focus");
        test.assertTrue(TCItems.FIRE_FOCUS.get().applyUpgrade(focus,upgrade,1),"Apply named upgrade");
        var saved=ItemStack.of(focus.save(new net.minecraft.nbt.CompoundTag()));
        test.assertTrue(TCItems.FIRE_FOCUS.get().upgradeLevel(saved,upgrade.id)==1,"Named upgrade survives NBT");
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,saved);
        var air=TCAspects.catalog().require("aer");var cost=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR,10);
        test.assertTrue(ThaumcraftApiHelper.consumeVisFromWand(wand,player,cost,false,false),"Simulated payment accepted");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,air)==10000,"Simulation does not debit");
        test.assertTrue(ThaumcraftApiHelper.consumeVisFromWand(wand,player,cost,true,false),"Commit payment");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,air)==9100,"Callback discount uses whole-vis API units");
        ThaumcraftApiHelper.addWarpToPlayer(player,0,false);
        ThaumcraftApiHelper.addWarpToPlayer(player,2,false);
        test.assertTrue(KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElseThrow().warp(PlayerKnowledge.WarpKind.PERMANENT)==2,"API warp persisted");
        var packet=new tc4revival.network.AddonDefinitionsPacket(tc4revival.network.AddonDefinitionsPacket.fingerprintNow(),ResearchDefinitions.server().json());
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{packet.encode(buffer);var decoded=tc4revival.network.AddonDefinitionsPacket.decode(buffer);test.assertTrue(decoded.research().equals(packet.research()),"Research network roundtrip on dedicated server");}finally{buffer.release();}
        tc4revival.TC4Revival.LOGGER.info("PASS addonUpgradeAndApiPayments");test.succeed();
    }
}
