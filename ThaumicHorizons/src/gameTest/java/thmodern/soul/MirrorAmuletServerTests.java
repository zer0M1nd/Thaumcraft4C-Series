package thmodern.soul;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.ItemStackHandler;
import tc4revival.content.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class MirrorAmuletServerTests {
    @GameTest(template="empty")
    public static void successfulCargoConsumesAmuletOnceAndLeavesPermanentPearl(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(5,3,5));level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,TCArcane.ITEM_MIRROR.get().defaultBlockState(),2);
        var player=new FakePlayer(level,new GameProfile(java.util.UUID.randomUUID(),"MirrorRescueTest"));player.setPos(test.absolutePos(new BlockPos(1,3,1)).getCenter());
        var worn=new ItemStackHandler(1);var amulet=new ItemStack(Horizons.MIRROR_AMULET.get());ItemMirrorItem.writeDestination(amulet.getOrCreateTag(),GlobalPos.of(level.dimension(),pos),"Test");worn.setStackInSlot(0,amulet);
        test.assertTrue(!MirrorAmulet.rescue(player,worn,0)&&!worn.getStackInSlot(0).isEmpty(),"Empty inventory does not consume a bound amulet");
        player.getInventory().setItem(0,new ItemStack(Items.EMERALD,7));player.getInventory().setItem(36,new ItemStack(Items.IRON_BOOTS));
        test.assertTrue(MirrorAmulet.rescue(player,worn,0)&&worn.getStackInSlot(0).isEmpty(),"Successful transfer consumes the worn amulet");
        test.assertTrue(player.getInventory().isEmpty(),"Main inventory and armour both transferred");
        var outputs=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1));test.assertTrue(outputs.stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum()==7&&outputs.stream().filter(e->e.getItem().is(Items.IRON_BOOTS)).count()==1,"Whole stacks arrive exactly once at the mirror");
        var pearls=level.getEntitiesOfClass(PermanentItemEntity.class,player.getBoundingBox().inflate(1));test.assertTrue(pearls.stream().filter(e->e.getItem().is(TCItems.PRIMORDIAL_PEARL.get())).count()==1,"One permanent pearl is left at the wearer");
        test.assertTrue(!MirrorAmulet.rescue(player,worn,0),"Consumed amulet cannot repeat the transfer");test.succeed();
    }
    @GameTest(template="empty")
    public static void missingMirrorPreservesCargoAndCustomMetadata(GameTestHelper test){
        var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(java.util.UUID.randomUUID(),"MirrorRefuseTest"));var amulet=new ItemStack(Horizons.MIRROR_AMULET.get());amulet.setHoverName(net.minecraft.network.chat.Component.literal("Remember me"));
        var pos=test.absolutePos(new BlockPos(5,4,5));level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);ItemMirrorItem.writeDestination(amulet.getOrCreateTag(),GlobalPos.of(level.dimension(),pos),"Test");
        var worn=new ItemStackHandler(1);worn.setStackInSlot(0,amulet);player.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
        test.assertTrue(!MirrorAmulet.rescue(player,worn,0)&&player.getInventory().getItem(0).is(Items.DIAMOND),"Absent mirror cannot debit cargo");test.assertTrue(ItemMirrorItem.destination(amulet)==null&&amulet.hasCustomHoverName(),"Invalid binding alone is cleared");test.succeed();
    }
}
