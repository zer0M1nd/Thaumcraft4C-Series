package ttmodern;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class InfusedFarmlandBlock extends FarmBlock implements EntityBlock {
    public InfusedFarmlandBlock() { super(Properties.copy(Blocks.FARMLAND).strength(.6f)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new InfusedFarmlandEntity(pos,state); }
    @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel world,BlockPos pos,net.minecraft.util.RandomSource random) {}
    @Override public void randomTick(BlockState state,net.minecraft.server.level.ServerLevel world,BlockPos pos,net.minecraft.util.RandomSource random) {}
    @Override public void fallOn(Level world,BlockState state,BlockPos pos,net.minecraft.world.entity.Entity entity,float distance) { entity.causeFallDamage(distance,1,world.damageSources().fall()); }
    @Override public boolean canSustainPlant(BlockState state,BlockGetter world,BlockPos pos,Direction face,net.minecraftforge.common.IPlantable plant) { return face==Direction.UP&&plant instanceof InfusedCropBlock; }
}
