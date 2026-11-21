package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class AdvancedAlchemyBlock extends BaseEntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,4);
    public AdvancedAlchemyBlock() {
        super(Properties.of().strength(3,17).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(PART,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(PART); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return state.getValue(PART)<=1 ? new AdvancedAlchemyBlockEntity(pos,state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCArcane.ADVANCED_ALCHEMY_ENTITY.get(),(world,pos,block,entity)->entity.tick());
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(PART)==0 && !(context instanceof EntityCollisionContext entity && entity.getEntity() instanceof LivingEntity)
                ? box(0,0,0,16,11.2,16) : net.minecraft.world.phys.shapes.Shapes.block();
    }
    @Override public int getLightEmission(BlockState state,BlockGetter level,BlockPos pos) {
        if (state.getValue(PART)==0 && level.getBlockEntity(pos) instanceof AdvancedAlchemyBlockEntity entity && entity.process().heat()>100)
            return Math.min(15,(int)((float)entity.process().heat()/AdvancedAlchemyProcess.MAX_POWER*12));
        return 0;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (!level.isClientSide && state.getValue(PART)==0 && entity instanceof ItemEntity item && !item.getItem().isEmpty()
                && level.getBlockEntity(pos) instanceof AdvancedAlchemyBlockEntity core
                && core.process().process(AlchemyFurnaceBlockEntity.inputAspects(item.getItem()),core)) {
            var remaining=item.getItem().copy(); remaining.shrink(1);
            level.playSound(null,entity.getX(),entity.getY(),entity.getZ(),TCSounds.BUBBLE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1+level.random.nextFloat()*.4f);
            if (remaining.isEmpty()) item.discard(); else item.setItem(remaining);
        }
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if (state.getValue(PART)==0 && level.getBlockEntity(pos) instanceof AdvancedAlchemyBlockEntity core && core.process().amount()>0)
            tc4revival.client.AlchemyBubbleParticles.furnace(level,pos,random);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        if (state.getValue(PART)==1 && level.getBlockEntity(pos) instanceof AdvancedAlchemyBlockEntity entity) {
            var core=entity.controller(); return core==null || core.process().amount()==0 ? 0 : Math.min(15,1+core.process().amount()*14/AdvancedAlchemyProcess.CAPACITY);
        }
        return 0;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !AdvancedAlchemyStructure.mutating()) {
            if (state.getValue(PART)==0) AdvancedAlchemyStructure.dismantle(level,pos,false);
            else {
                search: for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++) {
                    var target=pos.offset(x,y,z);
                    if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof AdvancedAlchemyBlockEntity core && core.core()) {
                        core.requestDismantle(); break search;
                    }
                }
            }
        }
        super.onRemove(state,level,pos,next,moving);
    }

}
