package thaumicexploration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public class FloatingCandle extends BaseEntityBlock {
    public final DyeColor color;
    public FloatingCandle(DyeColor color) {
        super(Properties.copy(Blocks.CANDLE).noCollission().lightLevel(s -> 14)); this.color = color;
    }
    @Override public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return box(6, 5, 6, 10, 15, 10);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return true; }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CandleEntity(pos,state);}
    public static float bob(BlockPos pos,float time){return .2F*(float)Math.sin(((pos.getX()+pos.getY()+pos.getZ())*10+time)/30F);}
    @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        float offset=bob(pos,level.getGameTime());
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,pos.getX()+.5,pos.getY()+.875+offset,pos.getZ()+.5,0,0,0);
        if(random.nextInt(5)==0)level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.94+offset,pos.getZ()+.5,0,.015,0);
    }
}
