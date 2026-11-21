package thmodern.vat;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.containment.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class SoulJarServerTests {
    @GameTest(template="empty")
    public static void soulCaptureUsesSeparatePayloadAndConsumesRealEntity(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"SoulCaptureTest"));var pos=test.absolutePos(new BlockPos(3,4,3));player.setPos(pos.getCenter());
        var soul=Horizons.SOUL.get().create(test.getLevel());soul.moveTo(pos.above().getCenter());test.getLevel().addFreshEntity(soul);player.getInventory().setItem(0,new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get()));
        test.assertTrue(!soul.hurt(soul.damageSources().generic(),100)&&soul.isAlive(),"Wandering souls cannot be killed by ordinary damage");
        test.assertTrue(CreatureJar.capture(player,soul)&&soul.isRemoved()&&SoulJar.contains(player.getInventory().getItem(0)),"Capture exchanges the real empty jar then removes the actual soul");
        var tag=player.getInventory().getItem(0).getTag();test.assertTrue(!tag.contains("Captured")&&CreatureJar.readContents(tag,test.getLevel()) instanceof SoulEntity,"Soul payload cannot be confused with captured creature NBT");
        test.getLevel().setBlock(pos,Horizons.CREATURE_JAR.get().defaultBlockState(),2);var placed=(CreatureJarEntity)test.getLevel().getBlockEntity(pos);placed.contents(tag);
        test.assertTrue(!placed.release(player)&&SoulJar.contains(placed.item()),"Unlike creature jars, soul jars cannot be opened with a wand");test.succeed();
    }
    @GameTest(template="empty")
    public static void effigyAndSoulProduceOneVillagerWithProfession(GameTestHelper test){
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(3,5,3));for(int part=0;part<36;part++)level.setBlock(center.offset(VatStructure.offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
        var vat=(VatEntity)level.getBlockEntity(center);var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SoulBodyTest"));player.setPos(center.above().getCenter());
        var jar=SoulJar.create(ResourceLocation.parse("minecraft:librarian"));player.getInventory().setItem(0,jar);test.assertTrue(!vat.exchangeJar(player,jar),"A soul cannot become a villager without a prepared body");
        vat.setItem(0,new ItemStack(Horizons.CORPSE_EFFIGY.get()));vat.tick();for(int i=0;i<80;i++)vat.tick();vat.accept("victus",8);vat.accept("sano",8);vat.tick();
        test.assertTrue(vat.exchangeJar(player,jar)&&vat.subject() instanceof Villager&&((Villager)vat.subject()).getVillagerData().getProfession()==VillagerProfession.LIBRARIAN&&!vat.hasEffigy()&&vat.mode()==VatEntity.IDLE,"A paid body and soul produce a single detached villager retaining profession");
        test.assertTrue(CreatureJar.empty(player.getInventory().getItem(0))&&!vat.exchangeJar(player,jar),"The soul jar is replaced by an empty jar and cannot be spent twice");
        test.assertTrue(level.getEntity(vat.subject().getUUID())==null,"Revived villager is not duplicated in the world before release");test.succeed();
    }
}
