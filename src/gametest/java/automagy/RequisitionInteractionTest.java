package automagy;

import automagy.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class RequisitionInteractionTest {
    @GameTest(template="empty",templateNamespace="automagy")
    public static void mirrorLabelsDistinguishRangeMissingAndUnlinked(GameTestHelper h){
        var relative=new BlockPos(2,2,2);h.setBlock(relative,Content.REQUISITION_TOME.get());
        var level=h.getLevel();var tome=(RequisitionTomeBlockEntity)level.getBlockEntity(h.absolutePos(relative));
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MirrorLabels"));
        var eye=new ItemStack(Content.CRYSTAL_EYE.get());
        h.assertTrue(tome.mirrorStatus(0)==0,"Empty eye should not show status");
        tome.configureEye(0,eye,player);h.assertTrue(tome.mirrorStatus(0)==2,"Unbound eye has no in-range coordinates");
        var near=relative.east();var absolute=h.absolutePos(near);
        eye.getOrCreateTag().putString("Dimension",level.dimension().location().toString());
        eye.getOrCreateTag().putLong("Target",absolute.asLong());
        tome.configureEye(0,eye,player);h.assertTrue(tome.mirrorStatus(0)==3,"Empty position must report no mirror");
        h.setBlock(near.below(),net.minecraft.world.level.block.Blocks.STONE);
        var mirrorState=tc4revival.content.TCArcane.ITEM_MIRROR.get().defaultBlockState().setValue(tc4revival.content.ItemMirrorBlock.FACING,Direction.UP);
        h.setBlock(near,mirrorState);
        h.assertTrue(tome.mirrorStatus(0)==4,"Existing unlinked mirror was mistaken for missing mirror");
        var other=near.east();h.setBlock(other.below(),net.minecraft.world.level.block.Blocks.STONE);h.setBlock(other,mirrorState);
        var mirror=(tc4revival.content.ItemMirrorBlockEntity)level.getBlockEntity(absolute);
        mirror.remember(GlobalPos.of(level.dimension(),h.absolutePos(other)));
        h.assertTrue(mirror.restoreLink(),"Native mirror pair failed to link");
        h.assertTrue(tome.mirrorStatus(0)==1,"Linked native mirror should be valid");
        h.setBlock(near,Content.MELDING_MIRROR.get());
        h.assertTrue(tome.mirrorStatus(0)==4,"Unlinked melding mirror was mistaken for missing mirror");
        h.setBlock(near,Content.MULTI_DEST_MIRROR.get());
        h.assertTrue(tome.mirrorStatus(0)==3,"Multi-destination intake is not an output endpoint");
        eye.getOrCreateTag().putLong("Target",h.absolutePos(relative.offset(6,0,0)).asLong());
        tome.configureEye(0,eye,player);h.assertTrue(tome.mirrorStatus(0)==2,"Distant eye must report out of range");
        eye.getOrCreateTag().putLong("Target",absolute.asLong());
        eye.getOrCreateTag().putString("Dimension","minecraft:the_nether");
        tome.configureEye(0,eye,player);h.assertTrue(tome.mirrorStatus(0)==2,"Cross-dimension eye must report out of range");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="automagy")
    public static void poweredTerminalOpensBeforeHeldItemActions(GameTestHelper h){
        var center=new BlockPos(3,4,3);
        h.setBlock(center.below(2),Content.GLOWING_AMBER.get());
        h.setBlock(center.below(2).east(),Content.AMBER_MEMORY.get());
        h.setBlock(center.below(2).west(),Content.AMBER_MEMORY.get());
        h.setBlock(center.below(),net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:order_crystal_cluster")));
        int[][] shelves={{-2,-1},{-2,1},{2,-1},{2,1},{-1,-2},{1,-2},{-1,2},{1,2}};
        for(var offset:shelves)h.setBlock(center.offset(offset[0],-1,offset[1]),Content.GREATWOOD_BOOKSHELF.get());
        h.setBlock(center,Content.INVENTARIUM.get());
        var level=h.getLevel();var core=(InventariumBlockEntity)level.getBlockEntity(h.absolutePos(center));
        InventariumBlockEntity.tick(level,core.getBlockPos(),core.getBlockState(),core);
        var relative=new BlockPos(6,2,5);h.setBlock(relative,Content.REQUISITION_TOME.get());
        var pos=h.absolutePos(relative);var tome=(RequisitionTomeBlockEntity)level.getBlockEntity(pos);
        h.assertTrue(core.linkMemory(pos),"Real structure did not accept terminal");
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TomeAccess"));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var block=(RequisitionTomeBlock)Content.REQUISITION_TOME.get();
        try{
            var original=player.containerMenu;
            block.use(tome.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(player.containerMenu==original,"Unpowered terminal opened");
            // Buffer fixture isolates activation from the independently tested vis network.
            var tag=tome.saveWithoutMetadata();tag.putInt("PoweredTicks",100);tome.load(tag);
            for(var item:new Item[]{tc4revival.content.TCItems.CASTING_WAND.get(),Items.PAPER}){
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
                block.use(tome.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,hit);
                h.assertTrue(player.containerMenu instanceof InventariumMenu,"Held item intercepted terminal GUI");
                h.assertTrue(player.containerMenu.stillValid(player),"Powered linked menu invalid");
                h.assertTrue(player.getMainHandItem().is(item)&&player.getMainHandItem().getCount()==1,"Opening menu consumed held item");
                player.closeContainer();
            }
            var menu=new InventariumMenu(1,player.getInventory(),pos);
            var scribePos=center.offset(0,-2,3);h.setBlock(scribePos,Content.SCRIBE.get().defaultBlockState().setValue(UnseenScribeBlock.FACING,Direction.NORTH));
            var scribe=(UnseenScribeBlockEntity)level.getBlockEntity(h.absolutePos(scribePos));
            h.assertTrue(core.link(scribe.getBlockPos()),"Core refused inventory source");
            var scribeTag=scribe.saveWithoutMetadata();scribeTag.putInt("PoweredTicks",100);scribe.load(scribeTag);
            var chestPos=scribePos.north();h.setBlock(chestPos,net.minecraft.world.level.block.Blocks.CHEST);
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(h.absolutePos(chestPos))).setItem(0,new ItemStack(Items.IRON_INGOT,32));
            // Relink invalidates the core's per-tick snapshot after constructing the source.
            core.link(scribe.getBlockPos());menu.broadcastChanges();
            var iron=new ItemStack(Items.IRON_INGOT);
            h.assertTrue(menu.getSlot(0).getItem().is(Items.IRON_INGOT),"Source inventory was not displayed");
            menu.request(player,0,iron,64,false,false);
            h.assertTrue(core.requests().requests().get(0).amount()==32,"Ordinary request exceeded stock");
            menu.request(player,0,iron,10,true,false);
            h.assertTrue(core.requests().requests().get(0).amount()==22,"Right click on inventory did not cancel");
            menu.request(player,InventariumMenu.PAGE_SIZE,iron,1,false,false);
            h.assertTrue(core.requests().requests().get(0).amount()==23,"Left click on demand did not increase");
            menu.request(player,0,iron,64,false,true);
            h.assertTrue(core.requests().requests().get(0).amount()==87,"Alt request did not allow unavailable items");
            menu.request(player,0,new ItemStack(Items.DIAMOND),1,false,true);
            menu.request(player,0,iron,65,false,true);
            h.assertTrue(core.requests().requests().get(0).amount()==87,"Invalid item/count packet changed demand");
            menu.request(player,InventariumMenu.PAGE_SIZE,iron,64,true,false);
            menu.request(player,InventariumMenu.PAGE_SIZE,iron,64,true,false);
            h.assertTrue(core.requests().requests().isEmpty(),"Demand cancellation left negative/nonzero request");
            var mirrors=new MirrorSettingsMenu(2,player.getInventory(),pos);
            h.assertTrue(mirrors.stillValid(player),"Powered mirror settings rejected access");
            mirrors.broadcastChanges();h.assertTrue(!mirrors.showOutput()&&!mirrors.getSlot(1).isActive(),"Output settings visible without multi-destination mirror");
            mirrors.setCarried(new ItemStack(Content.CRYSTAL_EYE.get(),4));
            mirrors.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            h.assertTrue(tome.mirrorEye(1).isEmpty(),"Hidden output slot accepted input");
            var multi=new BlockPos(5,2,6);h.setBlock(multi,Content.MULTI_DEST_MIRROR.get());
            h.assertTrue(core.linkMemory(h.absolutePos(multi)),"Multi-destination mirror link failed");
            mirrors.broadcastChanges();h.assertTrue(mirrors.showOutput()&&mirrors.getSlot(1).isActive(),"Linked multi-destination mirror did not expose output");
            mirrors.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            h.assertTrue(tome.mirrorEye(0).getCount()==1&&mirrors.getCarried().getCount()==4,"Powered ghost selection consumed eye or failed");
            mirrors.clicked(0,1,net.minecraft.world.inventory.ClickType.PICKUP,player);
            h.assertTrue(tome.mirrorEye(0).isEmpty()&&player.getInventory().countItem(Content.CRYSTAL_EYE.get())==0,"Ghost removal created item");
            tag.putInt("PoweredTicks",0);tome.load(tag);
            h.assertTrue(!mirrors.stillValid(player),"Mirror settings survived power loss");
            h.assertTrue(!menu.stillValid(player),"Menu remained valid after power loss");
        }finally{player.closeContainer();player.getInventory().clearContent();}
        h.succeed();
    }
}
