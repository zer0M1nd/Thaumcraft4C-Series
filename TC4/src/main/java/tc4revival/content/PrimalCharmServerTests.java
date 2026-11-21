package tc4revival.content;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class PrimalCharmServerTests {
    private static long seed(GameTestHelper test,int roll) {
        var random=test.getLevel().random;
        for(long seed=0;seed<1000000;seed++) { random.setSeed(seed);if(random.nextInt(20000)==roll)return seed; }
        throw new AssertionError("No deterministic charm seed found");
    }
    @GameTest(template="wand_test_empty")
    public static void primalCharmClueAndOrbBranches(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var area=new AABB(pos).inflate(2);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"CharmTest"));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var store=KnowledgeSavedData.get(level.getServer());store.ensure(player.getUUID(),bound->0);
        var item=TCItems.PRIMAL_CHARM.get();var stack=new ItemStack(item);stack.getOrCreateTag().putString("blurb","old hint");
        stack.getOrCreateTag().putString("custom","preserve");
        long clueSeed=seed(test,42),orbSeed=seed(test,0),nothingSeed=seed(test,20);
        try {
            level.random.setSeed(clueSeed);item.inventoryTick(stack,level,player,12,false);
            var knowledge=store.snapshot(player.getUUID()).orElseThrow();
            test.assertTrue(knowledge.researchMarkers().contains("@FOCUSPRIMAL") && !knowledge.researchMarkers().contains("FOCUSPRIMAL"),"Carried unselected charm grants the clue, not the completed focus research");
            test.assertTrue(!stack.getTag().contains("blurb") && stack.getTag().getString("custom").equals("preserve"),"Old transient blurb is removed without erasing unrelated NBT");
            test.assertTrue(level.getEntitiesOfClass(AspectOrbEntity.class,area).isEmpty(),"Clue branch does not also spawn an orb");
            long revision=store.revision(player.getUUID());
            level.random.setSeed(clueSeed);item.inventoryTick(stack,level,player,12,false);
            test.assertTrue(store.revision(player.getUUID())==revision,"Known clue suppresses repeated grants and notification branch");
            store.update(player.getUUID(),bound->0,k->ResearchCatalogs.progression().completeTrusted(k,"FOCUSPRIMAL",false));
            revision=store.revision(player.getUUID());
            level.random.setSeed(clueSeed);item.inventoryTick(stack,level,player,12,false);
            test.assertTrue(store.revision(player.getUUID())==revision,"Completed focus suppresses the clue branch");
            level.random.setSeed(nothingSeed);item.inventoryTick(stack,level,player,12,false);
            test.assertTrue(level.getEntitiesOfClass(AspectOrbEntity.class,area).isEmpty(),"Roll twenty is outside the orb interval");
            level.random.setSeed(orbSeed);item.inventoryTick(stack,level,player,12,false);
            var orbs=level.getEntitiesOfClass(AspectOrbEntity.class,area);
            test.assertTrue(orbs.size()==1 && orbs.get(0).value()==1 && orbs.get(0).aspect().isPrimal(),"Actual inventory callback spawns one primal vis orb even after focus completion");
            test.assertTrue(stack.getCount()==1,"Neither random branch consumes the charm");
        } finally { level.getEntitiesOfClass(AspectOrbEntity.class,area).forEach(Entity::discard); }
        tc4revival.TC4Revival.LOGGER.info("Primal charm PASS: clue-and-orb-branches");test.succeed();
    }
}
