package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocusTargetBoundaryServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void focusTargetingPreservesLegacyZeroDistanceSentinel(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(3,12,3));var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TargetBounds"));owner.moveTo(Vec3.atBottomCenterOf(base));owner.setYRot(0);owner.setXRot(0);int cx=base.getX()>>4,cz=base.getZ()>>4;boolean forced=level.setChunkForced(cx,cz,true);
        test.runAfterDelay(20,()->{var entities=new ArrayList<Entity>();try {
            var overlap=EntityType.COW.create(level);overlap.moveTo(owner.position());level.addFreshEntity(overlap);entities.add(overlap);var farther=EntityType.COW.create(level);farther.moveTo(owner.position().add(0,0,3));level.addFreshEntity(farther);entities.add(farther);
            var query=level.getEntities(owner,owner.getBoundingBox().expandTowards(0,0,20).inflate(1.1));test.assertTrue(query.indexOf(overlap)>=0&&query.indexOf(overlap)<query.indexOf(farther),"Fixture supplies overlap then farther target in actual query order");
            test.assertTrue(overlap.getBoundingBox().inflate(.8).contains(owner.getEyePosition()),"Eye starts inside first padded target");
            test.assertTrue(FocusTargeting.pointed(owner,0,20,1.1)==overlap,"Zero-distance target remains the nearest");
            test.assertTrue(FocusTargeting.pointed(owner,0,20,1.1,e->e!=farther)==overlap,"Filter still returns overlap when later target excluded");
            overlap.discard();
            test.assertTrue(FocusTargeting.pointed(owner,4,20,1.1)==null,"Minimum range rejects nearer target");
            farther.moveTo(owner.position().add(0,0,22));test.assertTrue(FocusTargeting.pointed(owner,0,20,1.1)==null,"Padded target beyond twenty-block ray is excluded");
            farther.moveTo(owner.position().add(0,0,19));test.assertTrue(FocusTargeting.pointed(owner,0,20,1.1)==farther,"Target within twenty-block ray remains selectable");
            tc4revival.TC4Revival.LOGGER.info("PASS focusTargetingPreservesLegacyZeroDistanceSentinel: overlapping replacement, filter, minimum and ray-range boundaries");test.succeed();
        }finally{entities.forEach(Entity::discard);if(forced)level.setChunkForced(cx,cz,false);}});
    }
}
