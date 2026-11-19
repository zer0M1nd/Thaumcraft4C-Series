package tc4revival.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;

public class CrabVentChecks {
    public static int verify(CommandSourceStack source) {
        var level=source.getLevel();var pos=new BlockPos(340,200,340);
        level.getChunkAt(pos);
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"VentAudit"));
        try {
            player.moveTo(pos.getCenter().add(3,0,0));player.setGameMode(GameType.SURVIVAL);level.addNewPlayer(player);
            for(var side:Direction.values()) {
                level.removeBlock(pos,false);level.setBlock(pos,TCEldritch.CRAB_SPAWNER.get().defaultBlockState(),3);
                var tile=(EldritchCrabSpawnerBlockEntity)level.getBlockEntity(pos);tile.setFacing(side);
                var copy=new EldritchCrabSpawnerBlockEntity(pos,tile.getBlockState());copy.load(tile.getUpdateTag());
                require(copy.facing()==side,"Facing lost on sync/save");
                require(tile.activated(),"Nearby player not detected");
                for(int i=0;i<150;i++)tile.tick();
                var crabs=level.getEntitiesOfClass(EldritchCrabEntity.class,new AABB(pos).inflate(4));
                require(crabs.size()==1,"Expected one spawned crab; force-load the test region first");
                var crab=crabs.get(0);require(!crab.hasHelm()&&crab.position().distanceTo(pos.relative(side).getCenter())<1e-5,"Crab spawn orientation/helmet differs");
                require(crab.getDeltaMovement().distanceTo(new net.minecraft.world.phys.Vec3(side.getStepX()*.2,side.getStepY()*.2,side.getStepZ()*.2))<1e-6,"Crab launch vector differs");
                crabs.forEach(Entity::discard);
                require(tile.triggerEvent(1,0),"Missing vent event");
                var random=net.minecraft.util.RandomSource.create(291);
                for(int i=0;i<20;i++)require(tile.advanceClientVenting(random)==3,"Burst must last 20 ticks at 3 particles/tick");
                for(int i=0;i<100;i++)require(tile.advanceClientVenting(random)<=1,"Burst failed to stop");
                var state=level.getBlockState(pos);
                require(state.getDestroySpeed(level,pos)==15&&state.getLightEmission(level,pos)==4,"Hardness/light differ from legacy");
                require(state.getShape(level,pos).bounds().equals(new AABB(0,0,0,1,1,1)),"Selection shape must remain a full cube");
                for(int i=0;i<20;i++){int xp=state.getBlock().getExpDrop(state,level,level.random,pos,0,0);require(xp>=6&&xp<=10,"XP differs");}
                require(player.gameMode.destroyBlock(pos)&&level.getBlockEntity(pos)==null,"Survival player cannot destroy vent");
            }
            source.sendSuccess(()->Component.literal("Crab vent passed: six facing directions, persistence/sync, real spawn/launch, smoke event cadence, hardness/light/selection/XP and survival destruction."),false);
            return 1;
        }catch(RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Crab vent regression failed",failure);source.sendFailure(Component.literal(failure.toString()));return 0;
        }finally {
            player.discard();
            level.getEntitiesOfClass(EldritchCrabEntity.class,new AABB(pos).inflate(4)).forEach(Entity::discard);
            level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new AABB(pos).inflate(4)).forEach(Entity::discard);
            snapshot.restore(true,false);
        }
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
