package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class GolemFishingServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=1600)
    public static void golemFishingCastCatchAndCancel(GameTestHelper test) {
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(4,3,4));var area=new AABB(center).inflate(12);
        for(var p:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,2,3))) {
            boolean wall=Math.abs(p.getX()-center.getX())==3 || Math.abs(p.getZ()-center.getZ())==3;
            level.setBlock(p,p.getY()<center.getY() || wall && p.getY()==center.getY()?Blocks.STONE.defaultBlockState():p.getY()==center.getY()?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        }
        var golem=TCEntities.GOLEM.get().create(level);var config=new GolemConfiguration(GolemMaterial.THAUMIUM,true);
        config.installCore(GolemCoreType.FISH);config.installUpgrade(0);config.installUpgrade(2);
        golem.state().configure(config);golem.state().home(center,Direction.UP);golem.setPos(center.getX()+.5,center.getY()+2,center.getZ()+.5);golem.setNoAi(true);level.addFreshEntity(golem);
        var goal=new GolemFishGoal(golem);cast(test,golem,goal);
        test.assertTrue(level.getEntitiesOfClass(GolemBobberEntity.class,area).isEmpty(),"Eligibility has no spawn side effects");goal.start();
        var hook=level.getEntitiesOfClass(GolemBobberEntity.class,area).get(0);
        test.startSequence().thenWaitUntil(()->{
            if(goal.canContinueToUse())goal.tick();
            test.assertTrue(!level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Native waiting interval yields actual fishing loot");
        }).thenExecute(()->{
            var catches=level.getEntitiesOfClass(ItemEntity.class,area);
            test.assertTrue(catches.stream().allMatch(item->!item.getItem().isEmpty() && item.getDeltaMovement().lengthSqr()>0),"Real catches have loot and thrown motion");
            test.assertTrue(hook.isRemoved(),"Catch removes its hook");goal.stop();
            cast(test,golem,goal);goal.start();var second=level.getEntitiesOfClass(GolemBobberEntity.class,area).get(0);golem.state().paused(true);
            test.assertTrue(!goal.canContinueToUse(),"Pausing stops fishing");goal.stop();test.assertTrue(second.isRemoved(),"Interrupted goal removes its hook");
            catches.forEach(net.minecraft.world.entity.Entity::discard);golem.discard();
        }).thenSucceed();
    }
    private static void cast(GameTestHelper test,GolemEntity golem,GolemFishGoal goal) {
        golem.tickCount=0;
        for(int seed=0;seed<1000;seed++){golem.getRandom().setSeed(seed);if(goal.canUse())return;}
        test.fail("No fishable water in prepared pool");
    }
}
