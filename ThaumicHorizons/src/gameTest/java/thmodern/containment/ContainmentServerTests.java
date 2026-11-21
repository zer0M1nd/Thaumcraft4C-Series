package thmodern.containment;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.vat.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class ContainmentServerTests {
    private static FakePlayer player(GameTestHelper test){var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"ContainmentTest"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(test.absolutePos(new BlockPos(3,3,3)).getCenter());player.getInventory().clearContent();return player;}
    @GameTest(template="empty")
    public static void capturePreservesCreatureAndExchangesLastEmptyJarSlot(GameTestHelper test){
        var player=player(test);for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));player.getInventory().setItem(4,new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get()));
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(player.position().add(1,0,0));cow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,3));cow.setHealth(5);test.getLevel().addFreshEntity(cow);
        test.assertTrue(CreatureJar.capture(player,cow)&&cow.isRemoved(),"Single empty jar frees its own slot even when the rest of the inventory is full");
        var contained=CreatureJar.read(player.getInventory().getItem(4),test.getLevel());test.assertTrue(contained!=null&&contained.getUUID().equals(cow.getUUID())&&contained.getHealth()==5&&contained.getMainHandItem().getCount()==3,"Capture preserves UUID health and possessions rather than cloning or stripping the creature");test.succeed();
    }
    @GameTest(template="empty")
    public static void fullInventoryDoesNotConsumeStackedJarOrCreature(GameTestHelper test){
        var player=player(test);for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));player.getInventory().setItem(4,new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get(),2));
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(player.position().add(1,0,0));test.getLevel().addFreshEntity(cow);
        test.assertTrue(!CreatureJar.capture(player,cow)&&!cow.isRemoved()&&player.getInventory().getItem(4).getCount()==2,"Failed exchange leaves both inputs intact");test.succeed();
    }
    @GameTest(template="empty")
    public static void vatJarTransferKeepsExactlyOneSubject(GameTestHelper test){
        var player=player(test);var cow=EntityType.COW.create(test.getLevel());var jar=CreatureJar.bottle(cow);player.getInventory().setItem(0,jar);
        var pos=test.absolutePos(new BlockPos(3,5,3));test.getLevel().setBlock(pos,Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,31),2);var vat=(VatEntity)test.getLevel().getBlockEntity(pos);
        test.assertTrue(vat.exchangeJar(player,jar)&&vat.subject()!=null&&CreatureJar.empty(player.getInventory().getItem(0)),"Jar moves creature into idle vat and returns an empty jar");
        test.assertTrue(vat.exchangeJar(player,player.getInventory().getItem(0))&&vat.subject()==null&&player.getInventory().getItem(0).is(Horizons.CREATURE_JAR_ITEM.get()),"Bottling clears vat only after output is safely stored");
        test.assertTrue(CreatureJar.read(player.getInventory().getItem(0),test.getLevel()).getUUID().equals(cow.getUUID()),"Round trip preserves identity");test.succeed();
    }
    @GameTest(template="empty")
    public static void focusCannotCaptureThroughSolidBlocks(GameTestHelper test){
        var player=player(test);player.setYRot(0);player.setXRot(0);var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.position().add(0,0,4));test.getLevel().addFreshEntity(zombie);
        var wall=BlockPos.containing(player.getEyePosition().add(0,0,2));test.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(ContainmentFocusItem.target(player)==null,"Block collider stops targeting");test.getLevel().setBlock(wall,Blocks.AIR.defaultBlockState(),3);
        test.assertTrue(ContainmentFocusItem.target(player)==zombie,"Target becomes available after removing the obstruction");test.succeed();
    }
}
