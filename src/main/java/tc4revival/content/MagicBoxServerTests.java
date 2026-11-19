package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class MagicBoxServerTests {
    private static MagicBoxBlockEntity box(GameTestHelper test,BlockPos pos) {
        test.setBlock(pos,TCArcane.MAGIC_BOX.get());return (MagicBoxBlockEntity)test.getBlockEntity(pos);
    }
    private static ItemStack named(String name,int count) { var stack=new ItemStack(Items.DIAMOND,count);stack.setHoverName(net.minecraft.network.chat.Component.literal(name));return stack; }
    private static CompoundTag data(int sort,ItemStack... stacks) {
        var tag=new CompoundTag();tag.putByte("sort",(byte)sort);var items=new ListTag();for(var stack:stacks)items.add(stack.save(new CompoundTag()));tag.put("Items",items);return tag;
    }
    @GameTest(template="wand_test_empty")
    public static void magicBoxStorageLinksSortingAndPersistence(GameTestHelper test) {
        var a=box(test,new BlockPos(1,1,1));a.tick();test.assertTrue(a.getContainerSize()==27,"Isolated box has 27 slots after initialization");
        var b=box(test,new BlockPos(2,1,1));a.refreshLinks();b.tick();
        test.assertTrue(a.getContainerSize()==54&&b.getContainerSize()==54,"Each connected box contributes 27 slots once");
        a.setItem(53,named("A",5));b.setItem(0,named("B",7));
        test.assertTrue(a.getItem(53).getCount()==5&&a.getItem(0).isEmpty()&&b.getItem(0).getCount()==7,"High index remains stable; adjacency does not elect a shared master");
        a.setItem(1,named("C",3));var removed=a.removeItem(53,5);
        test.assertTrue(removed.getCount()==5&&a.getItem(1).getHoverName().getString().equals("C")&&a.getItem(53).isEmpty(),"Removing a stack does not shift other slots");
        var pointer=data(-1);pointer.putInt("w_x",a.getBlockPos().getX());pointer.putInt("w_y",a.getBlockPos().getY());pointer.putInt("w_z",a.getBlockPos().getZ());pointer.putInt("w_d",999);
        b.load(pointer);b.setItem(1,named("shared",9));
        test.assertTrue(a.getItem(1).getCount()==3 && b.getItem(1).getCount()==9,"Wrong-dimension pointer cannot share storage");
        pointer.putInt("w_d",0);b.load(pointer);b.setItem(1,named("shared",9));
        test.assertTrue(a.getItem(1).getCount()==9 && b.getItem(1)==a.getItem(1),"Valid same-dimension explicit master is shared");
        var saved=a.saveWithoutMetadata();var restored=new MagicBoxBlockEntity(a.getBlockPos(),a.getBlockState());restored.load(saved);
        test.assertTrue(restored.getItem(1).getCount()==9,"All slot entries survive NBT");restored.getItem(1).shrink(1);
        test.assertTrue(a.getItem(1).getCount()==9,"Loaded inventories have independent stacks");
        a.load(data(0,named("C",1),named("B",1),named("A",1),named("D",1)));
        test.assertTrue(a.getItem(0).getHoverName().getString().equals("A")&&a.getItem(1).getHoverName().getString().equals("B"),"Ascending name sorting is complete");
        a.load(data(0,named("same",40),named("same",30)));test.assertTrue(a.getItem(0).getCount()==64&&a.getItem(1).getCount()==6,"Equal NBT stacks merge up to item limit");
        a.load(data(1,named("A",1),named("C",1),named("B",1)));test.assertTrue(a.getItem(0).getHoverName().getString().equals("C"),"Descending name sort");
        a.load(data(2,named("a",1),named("b",1)));test.assertTrue(a.getItem(0).getCount()+a.getItem(1).getCount()==2,"Namespace sorting preserves all items");
        // Invalid serialized entries previously introduced nulls into sorting and crashed.
        var invalid=data(0);invalid.getList("Items",Tag.TAG_COMPOUND).add(new CompoundTag());a.load(invalid);test.assertTrue(a.isEmpty(),"Malformed empty entries are discarded safely");
        a.load(data(-1));a.tick();a.setItem(0,named("automation",60));
        var wrapper=new net.minecraftforge.items.wrapper.InvWrapper(a);var offered=named("automation",8);
        var simulated=wrapper.insertItem(0,offered,true);test.assertTrue(simulated.getCount()==4&&a.getItem(0).getCount()==60,"Automation simulation is nonmutating");
        var remainder=wrapper.insertItem(0,offered,false);test.assertTrue(remainder.getCount()==4&&a.getItem(0).getCount()==64&&offered.getCount()==8,"Automation partial insertion preserves remainder and input");
        tc4revival.TC4Revival.LOGGER.info("PASS magicBoxStorageLinksSortingAndPersistence");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void magicBoxMenuAndLegacyRemoval(GameTestHelper test) {
        var pos=new BlockPos(1,1,1);var a=box(test,pos);box(test,new BlockPos(2,1,1));a.tick();
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MagicBoxAudit"));player.setPos(a.getBlockPos().getCenter());
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());MagicBoxMenu menu;
        try {buffer.writeBlockPos(a.getBlockPos());menu=TCArcane.MAGIC_BOX_MENU.get().create(1,player.getInventory(),buffer);}finally{buffer.release();}
        test.assertTrue(menu.slots.size()==63&&menu.stillValid(player),"Actual menu factory has matching 27 plus 36 slots despite enlarged inventory");
        player.containerMenu=menu;
        a.setItem(53,named("page2",6));
        test.assertTrue(menu.pageCount()==2 && menu.clickMenuButton(player,1) && menu.getSlot(26).getItem().getCount()==6,"Second page exposes expanded storage");
        menu.quickMoveStack(player,26);test.assertTrue(a.getItem(53).isEmpty(),"Shift extraction from page two uses mapped slot");
        player.getInventory().clearContent();menu.clickMenuButton(player,0);
        a.setItem(0,named("move",12));test.assertTrue(!menu.quickMoveStack(player,0).isEmpty()&&a.isEmpty(),"Shift extraction uses actual menu boundary");
        int slot=-1;for(int i=27;i<63;i++)if(menu.getSlot(i).hasItem())slot=i;
        test.assertTrue(slot>=27&&!menu.quickMoveStack(player,slot).isEmpty()&&a.getItem(0).getCount()==12,"Shift insertion returns one exact stack");
        a.load(data(-1,named("drop0",1),named("drop1",2),named("drop2",3),named("drop3",4)));a.tick();
        test.getLevel().setBlock(a.getBlockPos(),Blocks.AIR.defaultBlockState(),3);
        var drops=test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(a.getBlockPos()).inflate(1));
        test.assertTrue(drops.size()==4&&drops.stream().mapToInt(e->e.getItem().getCount()).sum()==10,"Removing the box drops every occupied slot exactly once");
        test.assertTrue(!menu.stillValid(player)&&menu.quickMoveStack(player,0).isEmpty(),"Removed container cannot be used through stale menu");
        var missing=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {missing.writeBlockPos(a.getBlockPos());var detached=TCArcane.MAGIC_BOX_MENU.get().create(2,player.getInventory(),missing);test.assertTrue(detached.slots.size()==63&&!detached.stillValid(player),"In-flight menu after removal has safe layout and no access");}finally{missing.release();}
        test.assertTrue(net.minecraft.world.level.block.Block.getDrops(TCArcane.MAGIC_BOX.get().defaultBlockState(),test.getLevel(),a.getBlockPos(),null,player,ItemStack.EMPTY).stream().anyMatch(s->s.is(TCItems.MAGIC_BOX.get())),"Actual loaded loot table supplies the block item");
        test.assertTrue(AlchemyFurnaceBlockEntity.fuelTime(new ItemStack(TCItems.MAGIC_BOX.get()))==300,"Actual wooden box is fuel, unlike traveling trunk");
        tc4revival.TC4Revival.LOGGER.info("PASS magicBoxMenuAndLegacyRemoval");test.succeed();
    }
}
