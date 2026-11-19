package thmodern.lens;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class ScholarLensServerTests {
    @GameTest(template="empty",timeoutTicks=180)
    public static void realServerTargetNeedsContinuousEquippedObservation(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,2));var player=new FakePlayer(level,new GameProfile(java.util.UUID.randomUUID(),"ScholarLensTest"));player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        level.setBlock(pos.offset(0,1,3),Blocks.STONE.defaultBlockState(),2);
        var goggles=new ItemStack(tc4revival.content.TCItems.GOGGLES.get());goggles.getOrCreateTag().put(LensEquipment.DATA,new ItemStack(Horizons.LENSES.get("order_entropy").get()).save(new net.minecraft.nbt.CompoundTag()));player.setItemSlot(EquipmentSlot.HEAD,goggles);
        var store=KnowledgeSavedData.get(level.getServer());store.ensure(player.getUUID(),level.random::nextInt);var target=ScanTargetSelector.resolve(player).orElseThrow();
        test.assertTrue(target.canTrack(store.snapshot(player.getUUID()).orElseThrow(),PlayerKnowledge.ScanMode.THAUMOMETER),"Target starts unscanned");
        test.startSequence().thenExecuteFor(40,()->ScholarLens.tick(player)).thenExecute(()->{player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);ScholarLens.tick(player);player.setItemSlot(EquipmentSlot.HEAD,goggles);})
            .thenExecuteFor(79,()->ScholarLens.tick(player)).thenExecute(()->test.assertTrue(target.canTrack(store.snapshot(player.getUUID()).orElseThrow(),PlayerKnowledge.ScanMode.THAUMOMETER),"Removing goggles cancels partial progress"))
            .thenExecuteFor(3,()->ScholarLens.tick(player)).thenExecute(()->test.assertTrue(!target.canTrack(store.snapshot(player.getUUID()).orElseThrow(),PlayerKnowledge.ScanMode.THAUMOMETER),"Continuous observation uses the base scan knowledge transaction"))
            .thenExecute(()->{player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);ScholarLens.tick(player);}).thenSucceed();
    }
}
