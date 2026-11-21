package tc4revival.content;

public  class WandPedestalFocusBlock extends net.minecraft.world.level.block.Block {
    public WandPedestalFocusBlock() { super(Properties.of().strength(3, 15).requiresCorrectToolForDrops().noOcclusion()); }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return box(1, 0, 1, 15, 7, 15); }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return net.minecraft.world.phys.shapes.Shapes.block(); }
    @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return WandPedestalBlock.interact(level, pos.below(), player, hand);
    }
}
