package tc4revival.content;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class EssentiaTubeBlock extends BaseEntityBlock {
    private final EssentiaTubeFlow.Kind kind;
    public EssentiaTubeBlock(EssentiaTubeFlow.Kind kind) { super(Properties.of().strength(.5f,3).sound(SoundType.METAL).noOcclusion()); this.kind=kind; }
    public EssentiaTubeFlow.Kind kind() { return kind; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EssentiaTubeBlockEntity(pos,state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCArcane.ESSENTIA_TUBE_ENTITY.get(),(world,pos,block,tube)->tube.tick());
    }
    /** Trace arms remain selectable even after closing their transport connection. */
    public static Map<Integer,AABB> parts(EssentiaTubeBlockEntity tube) {
        var parts=new LinkedHashMap<Integer,AABB>(); double low=.42f,high=.58f;
        for (var side:Direction.values()) if (tube.hasTransport(side)) {
            AABB box=switch(side) {
                case DOWN->new AABB(low,0,low,high,.5,high); case UP->new AABB(low,.5,low,high,1,high);
                case NORTH->new AABB(low,low,0,high,high,.5); case SOUTH->new AABB(low,low,.5,high,high,1);
                case WEST->new AABB(0,low,low,.5,high,high); case EAST->new AABB(.5,low,low,1,high,high);
            }; parts.put(side.get3DDataValue(),box);
        }
        double edge=tube.flow().kind()==EssentiaTubeFlow.Kind.BUFFER ? .25 : .34375;
        parts.put(6,new AABB(edge,edge,edge,1-edge,1-edge,1-edge)); return parts;
    }
    public static int tracePart(EssentiaTubeBlockEntity tube,Player player) {
        var start=player.getEyePosition(); var end=start.add(player.getLookAngle().scale(player.getBlockReach()));
        double nearest=Double.POSITIVE_INFINITY; int selected=-1;
        for (var entry:parts(tube).entrySet()) {
            var box=entry.getValue().move(tube.getBlockPos()); var hit=box.clip(start,end);
            if (hit.isPresent()) { double distance=start.distanceToSqr(hit.get()); if (distance<nearest) { nearest=distance; selected=entry.getKey(); } }
        }
        return selected;
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube)) return box(5.5,5.5,5.5,10.5,10.5,10.5);
        var shape=Shapes.empty(); for (var part:parts(tube).values()) shape=Shapes.or(shape,Shapes.create(part)); return shape;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        double x=.375,y=.375,z=.375,X=.625,Y=.625,Z=.625;
        if (level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube) for (var side:Direction.values()) if (tube.neighbor(side)!=null) {
            switch(side) { case DOWN->y=0; case UP->Y=1; case NORTH->z=0; case SOUTH->Z=1; case WEST->x=0; case EAST->X=1; }
        }
        return Shapes.create(new AABB(x,y,z,X,Y,Z));
    }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        if (kind!=EssentiaTubeFlow.Kind.BUFFER || !(level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube)) return 0;
        int amount=tube.essentiaAmount(null); return amount*14/8+(amount>0 ? 1 : 0);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube)) return InteractionResult.PASS;
        var held=player.getItemInHand(hand); var flow=tube.flow();
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,hit.getDirection(),held)) return InteractionResult.FAIL;
        if (kind==EssentiaTubeFlow.Kind.VALVE) {
            if (held.getItem() instanceof CastingWandItem || held.getItem() instanceof EssentiaResonatorItem || held.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof EssentiaTubeBlock) return InteractionResult.PASS;
            if (!level.isClientSide) flow.toggleValve(); return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind==EssentiaTubeFlow.Kind.FILTER) {
            if (player.isShiftKeyDown() && flow.filter()!=null) {
                if (!level.isClientSide) {
                    flow.filter(null); var side=hit.getDirection();
                    level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5+side.getStepX()/3.0,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()/3.0,new ItemStack(TCItems.JAR_LABEL.get())));
                    page(level,pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (flow.filter()==null && held.is(TCItems.JAR_LABEL.get())) {
                var named=TCItems.JAR_LABEL.get().storedAspects(held);
                if (!level.isClientSide && !named.aspects().isEmpty()) { flow.filter(named.aspects().get(0)); held.shrink(1); page(level,pos); }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
    private static void page(Level level,BlockPos pos) { level.playSound(null,pos,TCSounds.PAGE.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moved) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube && tube.flow().filter()!=null) popResource(level,pos,new ItemStack(TCItems.JAR_LABEL.get()));
        super.onRemove(state,level,pos,replacement,moved);
    }
}
