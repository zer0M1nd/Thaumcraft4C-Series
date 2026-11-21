package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

/** Uses the actual item and Player.interactOn path, including creative stack-copy handling. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class GolemPlacementServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=160) public static void placedThaumiumLiquidCoreAndBellWork(GameTestHelper t){run(t,false);}
    @GameTest(template="wand_test_empty",timeoutTicks=160) public static void placedThaumiumAlchemyCoreAndBellWork(GameTestHelper t){run(t,true);}
    private static void run(GameTestHelper t,boolean alchemy) {
        var level=t.getLevel();var base=t.absolutePos(new BlockPos(2,2,3));
        for(var at:BlockPos.betweenClosed(base.offset(-1,-1,-2),base.offset(5,4,2)))level.setBlock(at,at.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        for(var at:BlockPos.betweenClosed(base.offset(-1,0,-2),base.offset(5,0,2)))
            level.setChunkForced(at.getX()>>4,at.getZ()>>4,true);
        var destination=base.east(2);
        level.setBlock(base,(alchemy?TCArcane.ALCHEMY_FURNACE.get():TCArcane.CRUCIBLE.get()).defaultBlockState(),3);
        var clicked=alchemy?base.above():base;
        if(alchemy) {
            level.setBlock(clicked,TCArcane.ALEMBIC.get().defaultBlockState(),3);
            ((AlembicBlockEntity)level.getBlockEntity(clicked)).addToContainer(tc4revival.aspect.TCAspects.catalog().require("aer"),32);
            level.setBlock(destination,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
        } else {
            for(var side:Direction.Plane.HORIZONTAL)level.setBlock(destination.relative(side),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(destination,Blocks.WATER.defaultBlockState(),3);
        }
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PlacementFlow"));
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        var placer=new ItemStack(TCItems.golem(GolemMaterial.THAUMIUM));player.setItemInHand(InteractionHand.MAIN_HAND,placer);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked).add(0,.5,0),Direction.UP,clicked,false));
        ((GolemPlacerItem)placer.getItem()).onItemUseFirst(placer,context);
        var golem=level.getEntitiesOfClass(GolemEntity.class,new AABB(clicked).inflate(2)).stream().findFirst().orElseThrow();
        t.assertTrue(!golem.configuration().hasTask(),"Placed blank golem has no phantom addon task");
        t.runAtTickTime(20,()->{
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.golemCore(alchemy?6:5)));
            player.interactOn(golem,InteractionHand.MAIN_HAND);
            t.assertTrue(golem.configuration().core()==(alchemy?GolemCoreType.ESSENTIA:GolemCoreType.LIQUID) && golem.configuration().customTask()==null,"Installing core leaves native AI selected");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.GOLEM_BELL.get()));player.interactOn(golem,InteractionHand.MAIN_HAND);
            t.assertTrue(GolemBellItem.selected(player.getMainHandItem(),level)==golem,"Actual creative held bell binds to placed golem");
            if(alchemy) {
                player.moveTo(destination.getX()+.5,destination.getY()+2,destination.getZ()+.5,0,90);
                var hit=new BlockHitResult(Vec3.atCenterOf(destination),Direction.UP,destination,false);
                TCItems.GOLEM_BELL.get().onItemUseFirst(player.getMainHandItem(),new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            } else {
                player.moveTo(destination.getX()+.5,destination.getY()+2,destination.getZ()+.5,0,90);
                TCItems.GOLEM_BELL.get().use(level,player,InteractionHand.MAIN_HAND);
            }
            t.assertTrue(golem.markers().inDimension(level.dimension().location()).stream().anyMatch(m->m.pos().equals(destination)),"Bell marker reaches the entity");
        });
        t.runAtTickTime(120,()->{
            int result=alchemy?((EssentiaJarBlockEntity)level.getBlockEntity(destination)).amount():((CrucibleBlockEntity)level.getBlockEntity(base)).contents().water();
            t.assertTrue(result==(alchemy?32:1000),"Placed golem must transfer in nearby layout, actual="+result);
            golem.discard();t.succeed();
        });
    }
}
