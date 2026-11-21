package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumResourceParityServerTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void greedPowderPreservesCenterRelativeNearDistanceAndSpeedCap(GameTestHelper test){
        var cow=EntityType.COW.create(test.getLevel());var at=test.absolutePos(new BlockPos(16,3,16));cow.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);cow.setNoAi(true);test.getLevel().addFreshEntity(cow);
        var center=cow.position().add(0,cow.getBbHeight()/2,0);
        for(var offset:java.util.List.of(new Vec3(.2,.1,.2),new Vec3(1.4,0,0))){
            var powder=ConciliumEntities.SHARD_POWDER.get().create(test.getLevel());powder.powderType(PowderType.GREED);powder.setPos(center.add(offset));powder.initialAge(39);powder.setDeltaMovement(0,.03,0);test.getLevel().addFreshEntity(powder);powder.tick();
            var expected=offset.lengthSqr()>1?offset.normalize().scale(3):offset.scale(3);
            test.assertTrue(powder.isRemoved()&&cow.getDeltaMovement().distanceTo(expected)<.00001,"Actual expiring greed powder uses entity center and clamps only distances above one: "+offset);
        }
        cow.discard();test.succeed();
    }
    private ConciliumResourceParityServerTests(){}
}
