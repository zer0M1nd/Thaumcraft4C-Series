package tc4revival.content;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class DeviceMenuLifecycleServerTests {
    @GameTest(template="wand_test_empty")
    public static void deviceMenuPacketsHandleMissingAndReplacedBlocks(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var player=FakePlayerFactory.get(level,new GameProfile(java.util.UUID.randomUUID(),"MenuLifecycle"));
        var types=List.of(TCArcane.ARCANE_SPA_MENU.get(),TCArcane.DECONSTRUCTION_TABLE_MENU.get(),TCArcane.ARCANE_BORE_MENU.get());
        var blocks=List.of(TCArcane.ARCANE_SPA.get(),TCArcane.DECONSTRUCTION_TABLE.get(),TCArcane.ARCANE_BORE.get());
        for(int kind=0;kind<types.size();kind++) for(int scenario=0;scenario<4;scenario++) {
            player.getInventory().clearContent();player.setPos(pos.getCenter());
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            if(scenario==1)level.setBlock(pos,Blocks.CHEST.defaultBlockState(),18);
            if(scenario>=2)level.setBlock(pos,blocks.get(kind).defaultBlockState(),18);
            var before=level.getBlockEntity(pos);var state=level.getBlockState(pos);
            if(scenario==3) {
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
                test.assertTrue(before.isRemoved(),"Removing the actual device invalidates its old block entity");
                before=level.getBlockEntity(pos);state=level.getBlockState(pos);
            }
            var buffer=new FriendlyByteBuf(Unpooled.buffer());AbstractContainerMenu menu;
            try {buffer.writeBlockPos(pos);menu=types.get(kind).create(1,player.getInventory(),buffer);test.assertTrue(!buffer.isReadable(),"Factory consumes exactly its block position");}
            finally {buffer.release();}
            player.containerMenu=menu;var entity=entity(menu);
            test.assertTrue(menu.slots.size()==(kind==2?38:37),"Opening races preserve synchronized slot layout");
            test.assertTrue(level.getBlockState(pos).equals(state)&&level.getBlockEntity(pos)==before,"Menu construction never replaces the world block entity");
            int slot=kind==2?1:0;var item=kind==0?TCCleansing.BATH_SALTS.get():kind==1?Items.DIAMOND:Items.DIAMOND_PICKAXE;
            menu.getSlot(slot).set(new ItemStack(item));
            if(scenario==2) {
                test.assertTrue(entity==before&&menu.stillValid(player),"A valid device uses the actual live inventory");
                test.assertTrue(!menu.quickMoveStack(player,slot).isEmpty()&&player.getInventory().countItem(item)==1,"Valid device shift-click still moves its item once");
                player.setPos(pos.getCenter().add(20,0,0));
                test.assertTrue(!menu.stillValid(player)&&menu.quickMoveStack(player,slot).isEmpty(),"Distance invalidation rejects shift-click");
            } else {
                test.assertTrue(entity!=before&&entity.getLevel()==null&&entity.isRemoved()&&!menu.stillValid(player),"Missing, wrong-type and removed devices use an invalid detached inventory");
                test.assertTrue(menu.quickMoveStack(player,slot).isEmpty()&&player.getInventory().countItem(item)==0&&menu.getSlot(slot).hasItem(),"In-flight slot synchronization cannot be extracted from an invalid menu");
                test.assertTrue(!menu.clickMenuButton(player,1),"Invalid device rejects its action button");
            }
            menu.getSlot(slot).set(ItemStack.EMPTY);player.containerMenu=player.inventoryMenu;
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
        }
        tc4revival.TC4Revival.LOGGER.info("deviceMenuPacketsHandleMissingAndReplacedBlocks PASS: three registered factories, missing/wrong/live/removed entities, slot sync and shift-click guards");test.succeed();
    }
    private static BlockEntity entity(AbstractContainerMenu menu) {
        if(menu instanceof ArcaneSpaMenu spa)return spa.spa();
        if(menu instanceof DeconstructionTableMenu table)return table.table();
        return ((ArcaneBoreMenu)menu).bore();
    }
}
