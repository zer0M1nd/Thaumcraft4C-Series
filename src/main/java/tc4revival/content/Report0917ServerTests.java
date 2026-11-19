package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0917ServerTests {
    @GameTest(template="wand_test_empty") public static void inventoryBookOpeningRequiresTheOriginalBookAndPreservesInventory(GameTestHelper t) {
        var p=new FakePlayer(t.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BookRecipe"));
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().setItem(12,new ItemStack(TCItems.THAUMONOMICON.get()));
        p.getInventory().setItem(0,new ItemStack(Items.DIAMOND));
        var session=new ServerResearchBookSession(p,12,s->s.getItem() instanceof ThaumonomiconItem);
        t.assertTrue(session.valid()&&p.getInventory().selected==0&&p.getMainHandItem().is(Items.DIAMOND),"Book opens without moving items or selecting its slot");
        t.assertTrue(session.authorize(p,UUID.randomUUID(),0)==ServerResearchBookSession.Rejection.TOKEN,"Forged opening denied");
        p.getInventory().setItem(12,ItemStack.EMPTY);
        t.assertTrue(!session.valid(),"Removing the inventory book revokes session");
        t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void golemFluidWorkUsesLegacyCenterDistances(GameTestHelper t) {
        var level=t.getLevel();var golem=TCEntities.GOLEM.get().create(level);
        var pos=t.absolutePos(new BlockPos(2,2,2));
        golem.moveTo(pos.getX()+2.5,pos.getY()+.5,pos.getZ()+.5,0,0);
        t.assertTrue(!GolemWorkRange.adjacent(golem,pos),"Liquid gathering is strictly less than 4 squared");
        t.assertTrue(GolemWorkRange.reaches(golem,pos),"Essentia empty allows exactly 4 squared");
        golem.setPos(pos.getX()+2.6,pos.getY()+.5,pos.getZ()+.5);
        t.assertTrue(!GolemWorkRange.reaches(golem,pos),"Bounding-box edges do not extend legacy reach");
        t.assertTrue(GolemWorkRange.reaches(golem,pos,5),"Liquid home threshold is 5 squared");
        golem.setPos(pos.getX()+3,pos.getY()+.5,pos.getZ()+.5);
        t.assertTrue(!GolemWorkRange.reaches(golem,pos,6),"Essentia gather threshold is 6 squared");
        var config=new GolemConfiguration(GolemMaterial.WOOD,false);config.installCore(GolemCoreType.ESSENTIA);
        golem.state().configure(config);golem.state().home(pos.east(),Direction.EAST);
        golem.setPos(pos.getX()+3.1,pos.getY()+.5,pos.getZ()+.5);
        t.assertTrue(new GolemReturnHomeGoal(golem).canUse(),"Returning alchemy golem must not idle outside source reach");
        t.succeed();
    }
}
