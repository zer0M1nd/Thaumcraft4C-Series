package fmmodern;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Twelve servings, each replenished by a random tick. The last serving destroys the cake. */
public final class ArcaneCake extends Block {
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 11);
    public ArcaneCake() { super(Properties.copy(Blocks.CAKE).randomTicks()); registerDefaultState(stateDefinition.any().setValue(BITES, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BITES); }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return box(1 + s.getValue(BITES) * 14.0 / 12, 0, 1, 15, 8, 15); }
    @Override public boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).isSolid(); }
    @Override public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) { return !canSurvive(s, l, p) ? Blocks.AIR.defaultBlockState() : super.updateShape(s,d,n,l,p,np); }
    @Override public void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { if (s.getValue(BITES) > 0) l.setBlock(p, s.setValue(BITES, s.getValue(BITES)-1), 2); }
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player player, InteractionHand h, BlockHitResult hit) { return eat(s,l,p,player); }
    @Override public void attack(BlockState s, Level l, BlockPos p, Player player) { eat(s,l,p,player); }
    private InteractionResult eat(BlockState s, Level l, BlockPos p, Player player) {
        if (!player.canEat(false)) return InteractionResult.PASS;
        if (!l.isClientSide) { player.getFoodData().eat(2,1); if (s.getValue(BITES)==11) l.removeBlock(p,false); else l.setBlock(p,s.setValue(BITES,s.getValue(BITES)+1),3); }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
