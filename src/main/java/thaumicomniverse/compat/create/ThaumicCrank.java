package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.kinetics.crank.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ThaumicCrank extends HandCrankBlock {
    public ThaumicCrank(){super(Properties.copy(Blocks.OAK_PLANKS).strength(3).noOcclusion());}
    @Override public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType(){return ArcaneCreateContent.CRANK_TILE.get();}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!ArcaneContraptions.enabled()||player.isSpectator())return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var golem=thaumcraft.api.golems.GolemInteraction.current();var chosen=pos;int duration=80;
        if(golem.isPresent()){
            duration+=40*Math.min(2,golem.get().configuration().upgrades(1));
            double radius=golem.get().configuration().range();
            var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(pos);int shortest=Integer.MAX_VALUE;
            while(!queue.isEmpty()&&seen.size()<4096){
                var at=queue.remove();if(!seen.add(at)||at.distSqr(pos)>radius*radius||!level.hasChunkAt(at)||!(level.getBlockEntity(at) instanceof Tile crank))continue;
                if(crank.inUse<shortest){chosen=at;shortest=crank.inUse;}
                for(var side:Direction.values())queue.add(at.relative(side));
            }
        }
        if(level.getBlockEntity(chosen) instanceof Tile tile)tile.turnFor(player.isShiftKeyDown(),duration);
        return InteractionResult.CONSUME;
    }
    public static class Tile extends HandCrankBlockEntity {
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.CRANK_TILE.get(),pos,state);}
        @Override public void turn(boolean backwards){turnFor(backwards,80);}
        public void turnFor(boolean backwards,int duration){this.backwards=backwards;inUse=duration;updateGeneratedRotation();setChanged();sendData();}
        @Override protected Block getStressConfigKey(){return com.simibubi.create.AllBlocks.HAND_CRANK.get();}
        @Override public float getGeneratedSpeed(){return ArcaneContraptions.enabled()?super.getGeneratedSpeed():0;}
    }
}
