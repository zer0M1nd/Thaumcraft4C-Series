package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class InfernalFurnaceBlock extends BaseEntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,10),LAYER=IntegerProperty.create("layer",0,2);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public InfernalFurnaceBlock() {
        super(Properties.of().strength(10,500).requiresCorrectToolForDrops().noOcclusion().lightLevel(state->state.getValue(PART)==0 || state.getValue(PART)==10 ? 13 : 3));
        registerDefaultState(stateDefinition.any().setValue(PART,1).setValue(LAYER,0).setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(PART,LAYER,FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(PART)==0 ? box(0,0,0,16,4,16) : net.minecraft.world.phys.shapes.Shapes.block();
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if (state.getValue(PART)!=10) return getShape(state,level,pos,context);
        return switch(state.getValue(FACING)) {
            case EAST->box(0,0,0,8,16,16); case WEST->box(8,0,0,16,16,16);
            case SOUTH->box(0,0,0,16,16,8); default->box(0,0,8,16,16,16);
        };
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) {
        return state.getValue(PART)==0 ? net.minecraft.world.phys.shapes.Shapes.empty() : net.minecraft.world.phys.shapes.Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        int part=state.getValue(PART);
        return part==0 || part==2 || part==4 || part==5 || part==6 || part==8 ? new InfernalFurnaceBlockEntity(pos,state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCArcane.INFERNAL_FURNACE_ENTITY.get(),(world,pos,block,furnace)->furnace.tick());
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (state.getValue(PART)!=0) return;
        double x=entity.getX()-pos.getX(),z=entity.getZ()-pos.getZ();
        entity.setDeltaMovement(entity.getDeltaMovement().add(x<.3 ? .0001 : x>.7 ? -.0001 : 0,0,z<.3 ? .0001 : z>.7 ? -.0001 : 0));
        if (entity instanceof ItemEntity item) {
            var motion=entity.getDeltaMovement(); entity.setDeltaMovement(motion.x,.025,motion.z);
            if (!level.isClientSide && entity.onGround() && level.getBlockEntity(pos) instanceof InfernalFurnaceBlockEntity furnace && furnace.insert(item.getItem())) item.discard();
        } else if (entity instanceof LivingEntity && !entity.fireImmune()) {
            entity.hurt(level.damageSources().lava(),3); entity.setSecondsOnFire(10);
        }
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block from,BlockPos fromPos,boolean moving) {
        if (!level.isClientSide && state.getValue(PART)==0 && !InfernalFurnaceStructure.mutating() && !InfernalFurnaceStructure.intact(level,pos))
            InfernalFurnaceStructure.dismantle(level,pos);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        super.onRemove(state,level,pos,next,moving);
        if (!state.is(next.getBlock()) && !level.isClientSide && !InfernalFurnaceStructure.mutating()) {
            if (state.getValue(PART)==0) InfernalFurnaceStructure.dismantle(level,pos);
            else for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
                var target=pos.offset(x,y,z);
                if (level.hasChunkAt(target)) level.updateNeighborsAt(target,this);
            }
        }
    }

    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int event,int value) {
        if (event==1) {
            if (level.isClientSide) tc4revival.client.ClientFocusEffects.blockSparkle(level,pos,0xFF6000,5);
            return true;
        }
        if (event==3) {
            if (level.isClientSide) tc4revival.client.InfernalFurnaceEffects.smelted(level,pos,state.getValue(FACING));
            return true;
        }
        return super.triggerEvent(state,level,pos,event,value);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if (state.getValue(PART)==0 && level.getBlockState(pos.above()).isAir()) for (int i=0;i<3;i++)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,pos.getX()+random.nextFloat(),pos.getY()+1+random.nextFloat()*.5,pos.getZ()+random.nextFloat(),0,0,0);
    }
}
