package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingProtectionServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void wardingSurvivesExplosionAndPoweredPistonWithMovableControl(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(4,20,4));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardProtect"));
        var previous=new LinkedHashMap<BlockPos,BlockState>();
        BlockPos.betweenClosed(origin.offset(-6,-6,-6),origin.offset(6,6,6)).forEach(p->previous.put(p.immutable(),level.getBlockState(p)));
        var setting=tc4revival.config.TCConfig.SERVER.wardedStone;boolean old=setting.get();
        Runnable cleanup=()->{setting.set(old);previous.forEach((p,state)->level.setBlock(p,state,2));level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);};
        try {
            previous.keySet().forEach(p->level.setBlock(p,Blocks.AIR.defaultBlockState(),2));
            level.setBlock(origin,Blocks.STONE.defaultBlockState(),2);test.assertTrue(WardedBlockEntity.protect(player,origin),"Protected fixture created");
            var ward=(WardedBlockEntity)level.getBlockEntity(origin);var state=ward.getBlockState();
            for(boolean enabled:new boolean[]{true,false}) {
                setting.set(enabled);
                test.assertTrue(state.getDestroyProgress(player,level,origin)==0,"Legacy proxy remains unbreakable even when research is disabled");
                test.assertTrue(!state.canEntityDestroy(level,origin,EntityType.WITHER.create(level)),"Wither destruction hook is denied");
                test.assertTrue(!state.isValidSpawn(level,origin,EntityType.ZOMBIE),"Creature spawning denied");
            }
            var glass=origin.east(2);level.setBlock(glass,Blocks.GLASS.defaultBlockState(),2);
            level.explode(null,origin.getX()+1.5,origin.getY()+.5,origin.getZ()+.5,4,false,Level.ExplosionInteraction.BLOCK);
            test.assertTrue(level.getBlockEntity(origin)==ward&&ward.ownedBy(player),"Real terrain explosion preserves ward and owner");
            test.assertTrue(level.getBlockState(glass).isAir(),"Explosion destroys nearby ordinary control");
            var piston=origin.west();level.setBlock(piston,Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST),3);
            level.setBlock(piston.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
            test.runAtTickTime(10,()->{
                try {
                    test.assertTrue(level.getBlockEntity(origin)==ward&&!level.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED),"Powered piston cannot move ward during world ticks");
                    level.setBlockAndUpdate(origin,Blocks.STONE.defaultBlockState());
                    level.neighborChanged(piston,Blocks.STONE,origin);
                } catch(Throwable failure){cleanup.run();throw failure;}
            });
            test.runAtTickTime(20,()->{
                try {
                    test.assertTrue(level.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED)&&level.getBlockState(origin.east()).is(Blocks.STONE),"Same powered piston moves ordinary replacement control");
                    tc4revival.TC4Revival.LOGGER.info("PASS wardingSurvivesExplosionAndPoweredPistonWithMovableControl: real explosion/control, powered piston world ticks/control, hardness/entity/spawn guards in both config modes");test.succeed();
                } finally {cleanup.run();}
            });
        } catch(Throwable failure){cleanup.run();throw failure;}
    }
}
