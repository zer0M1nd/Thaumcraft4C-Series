package tc4revival.content;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The four original monument forms share full collision and connected destruction. */
public  class EldritchMonumentBlock extends BaseEntityBlock {
    public enum Kind { ALTAR, OBELISK, SEGMENT, CAP }
    private final Kind kind;
    private static final Set<Level> DISMANTLING=Collections.newSetFromMap(new IdentityHashMap<>());
    public EldritchMonumentBlock(Kind kind) {
        super(Properties.of().strength(50,20000).sound(SoundType.STONE).noOcclusion()
                .lightLevel(state->8).isValidSpawn((state,world,pos,type)->false));
        this.kind=kind;
    }
    public Kind kind() { return kind; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return kind==Kind.SEGMENT ? null : new EldritchMonumentBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCEldritch.MONUMENT_ENTITY.get(),(world,pos,current,entity)->entity.tick());
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (kind==Kind.ALTAR && !player.isShiftKeyDown() && player.getItemInHand(hand).is(TCEldritch.ELDRITCH_EYE.get())
                && level.getBlockEntity(pos) instanceof EldritchMonumentBlockEntity altar && altar.eyes()<4) {
            if (!level.isClientSide) altar.insertEye(player.getItemInHand(hand));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        super.onRemove(state,level,pos,replacement,moving);
        if (state.is(replacement.getBlock()) || level.isClientSide || !DISMANTLING.add(level)) return;
        // Iterative traversal preserves the connected collapse without exhausting the Java stack.
        var pending=new ArrayDeque<BlockPos>(); pending.add(pos.immutable());
        try {
            while (!pending.isEmpty()) {
                BlockPos origin=pending.removeFirst();
                for (BlockPos candidate:BlockPos.betweenClosed(origin.offset(-3,-2,-3),origin.offset(3,2,3))) {
                    if (!(level.getBlockState(candidate).getBlock() instanceof EldritchMonumentBlock)) continue;
                    BlockPos next=candidate.immutable();
                    if (level.removeBlock(next,false)) pending.addLast(next);
                }
                level.explode(null,origin.getX()+.5,origin.getY()+.5,origin.getZ()+.5,1,Level.ExplosionInteraction.NONE);
            }
        } finally { DISMANTLING.remove(level); }
    }
}
