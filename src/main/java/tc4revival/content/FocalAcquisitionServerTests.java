package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocalAcquisitionServerTests {
    @GameTest(template="wand_test_empty")
    public static void focalAcquisitionPaysAllSixAspectsAndRejectsStaleOutput(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),2);
        var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocalCraft"));
        player.setGameMode(GameType.SURVIVAL);player.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);
        var names=new String[]{"minecraft:iron_ingot","thaumcraft:arcane_stone_slab","minecraft:iron_ingot","thaumcraft:arcane_stone","thaumcraft:primal_charm","thaumcraft:arcane_stone","minecraft:gold_ingot","thaumcraft:table","minecraft:gold_ingot"};
        for(int i=0;i<9;i++)table.setItem(i,new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(names[i]))));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);table.setItem(10,wand);
        var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
        try {
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Loaded acquisition recipe requires research");
            KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{k.rememberResearch("FOCALMANIPULATION");return null;});
            menu.refresh();test.assertTrue(menu.getSlot(0).getItem().is(TCArcane.FOCAL_BLOCK.get().asItem()),"Research reveals actual loaded output");
            for(var aspect:TCAspects.catalog().primals()){
                int full=TCItems.CASTING_WAND.get().vis(wand,aspect);wand.getOrCreateTag().putInt(aspect.tag(),3199);
                menu.refresh();test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Each individual primal must cover 32 vis");
                wand.getOrCreateTag().putInt(aspect.tag(),full);
            }
            menu.refresh();var ingredient=table.getItem(4).copy();table.setItem(4,ItemStack.EMPTY);
            menu.clicked(0,0,ClickType.PICKUP,player);
            test.assertTrue(menu.getCarried().isEmpty(),"Stale preview after shared ingredient removal cannot be taken");
            table.setItem(4,ingredient);for(var aspect:TCAspects.catalog().primals())wand.getOrCreateTag().putInt(aspect.tag(),3200);
            menu.refresh();menu.clicked(0,0,ClickType.PICKUP,player);
            test.assertTrue(menu.getCarried().is(TCArcane.FOCAL_BLOCK.get().asItem())&&menu.getCarried().getCount()==1,"Actual menu craft yields one upgrade table");
            for(int slot=0;slot<9;slot++)test.assertTrue(table.getItem(slot).isEmpty(),"All nine cells consumed once");
            for(var aspect:TCAspects.catalog().primals())test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==0,"Exact 3200 centivis debit per primal");
            menu.clicked(0,0,ClickType.PICKUP,player);test.assertTrue(menu.getCarried().getCount()==1,"Repeated take cannot duplicate output");
            tc4revival.TC4Revival.LOGGER.info("PASS focalAcquisitionPaysAllSixAspectsAndRejectsStaleOutput: actual recipe, research, six insufficient primals, stale ingredient, exact debit and no replay");test.succeed();
        }finally{menu.setCarried(ItemStack.EMPTY);player.containerMenu=player.inventoryMenu;table.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }

    @GameTest(template="wand_test_empty")
    public static void focalResearchedUseOpensRealServerMenu(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var state=TCArcane.FOCAL_BLOCK.get().defaultBlockState();level.setBlock(pos,state,2);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocalOpen"));
        player.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);
        var packets=new ArrayList<net.minecraft.network.protocol.Packet<?>>();
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){packets.add(packet);}
        };
        new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server,connection,player);
        KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{k.rememberResearch("FOCALMANIPULATION");return null;});
        try {
            var result=state.use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            test.assertTrue(result.consumesAction()&&player.containerMenu instanceof FocalManipulatorMenu menu&&menu.stillValid(player)&&menu.getType()==TCArcane.FOCAL_MENU.get(),"Researched block use creates and installs the registered server menu");
            test.assertTrue(packets.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket payload&&payload.getIdentifier().toString().equals("thaumcraft:main")),"Opening sends knowledge over actual registered channel");
            tc4revival.TC4Revival.LOGGER.info("PASS focalResearchedUseOpensRealServerMenu: actual block use, registered menu installation and knowledge channel capture");test.succeed();
        }finally{player.containerMenu=player.inventoryMenu;KnowledgeSync.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
}
