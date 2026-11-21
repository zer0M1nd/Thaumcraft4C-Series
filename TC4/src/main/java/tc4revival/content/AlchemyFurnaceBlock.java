package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public  class AlchemyFurnaceBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public static final BooleanProperty CONTENTS=BooleanProperty.create("contents");
    public AlchemyFurnaceBlock() { super(Properties.of().strength(3,15).requiresCorrectToolForDrops().noOcclusion().lightLevel(s->s.getValue(LIT) ? 12 : 0)); registerDefaultState(stateDefinition.any().setValue(LIT,false).setValue(CONTENTS,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) { builder.add(LIT,CONTENTS); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new AlchemyFurnaceBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type,TCArcane.ALCHEMY_FURNACE_ENTITY.get(),(world,pos,block,furnace)->furnace.tick()); }
    @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        if (player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) net.minecraftforge.network.NetworkHooks.openScreen(server,furnace,pos);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack) {
        if (stack.hasCustomHoverName() && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) furnace.setCustomName(stack.getHoverName().getString());
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!next.is(this) && !AdvancedAlchemyStructure.mutating() && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) { if (!level.isClientSide) net.minecraft.world.Containers.dropContents(level,pos,furnace); level.updateNeighbourForOutputSignal(pos,this); }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if (!state.getValue(LIT)) return;
        double y=pos.getY()+.2+random.nextFloat()*5/16f,offset=random.nextFloat()*.5-.25;
        for (var side:net.minecraft.core.Direction.Plane.HORIZONTAL) for (var particle:java.util.List.of(net.minecraft.core.particles.ParticleTypes.SMOKE,net.minecraft.core.particles.ParticleTypes.FLAME))
            level.addParticle(particle,pos.getX()+.5+(side.getStepX()==0 ? offset : side.getStepX()*.52),y,pos.getZ()+.5+(side.getStepZ()==0 ? offset : side.getStepZ()*.52),0,0,0);
    }
}
