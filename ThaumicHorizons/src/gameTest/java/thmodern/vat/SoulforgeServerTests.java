package thmodern.vat;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.soul.SoulforgeEntity;
import thmodern.containment.SoulJar;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class SoulforgeServerTests {
    @GameTest(template="empty")
    public static void assemblyConsumesCognitionAndBottlesExactlyOnce(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlock(pos,Horizons.SOULFORGE.get().defaultBlockState(),2);var forge=(SoulforgeEntity)level.getBlockEntity(pos);var mind=tc4revival.aspect.TCAspects.catalog().require("cognitio");
        test.assertTrue(forge.addEssentia(mind,1,Direction.DOWN)==0&&forge.addEssentia(tc4revival.aspect.TCAspects.catalog().require("aer"),1,Direction.UP)==0,"Only cognition through the top is accepted");
        int remaining=9600;while(remaining>0){forge.addEssentia(mind,1,Direction.UP);int accepted=forge.acceptSoulBits(Math.min(1000,remaining));test.assertTrue(accepted>0,"Funded receiver advances");remaining-=accepted;}
        test.assertTrue(forge.souls()==1&&forge.progress()==0&&forge.energy()==400,"One soul consumes exactly 9600 bits and energy units");
        var copy=new SoulforgeEntity(pos,forge.getBlockState());copy.setLevel(level);copy.load(forge.saveWithoutMetadata());test.assertTrue(copy.souls()==1&&copy.energy()==400,"Assembly inventory survives reload");
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ForgeBottleTest"));player.setPos(pos.above().getCenter());var jar=new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get());player.getInventory().setItem(0,jar);
        test.assertTrue(forge.bottle(player,jar)&&forge.souls()==0&&SoulJar.contains(player.getInventory().getItem(0)),"Real jar exchanged for one profession-bearing soul");
        test.assertTrue(!forge.bottle(player,jar),"Stale item reference cannot produce a second soul");test.succeed();
    }
}
