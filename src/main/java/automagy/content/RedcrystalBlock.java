package automagy.content;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.api.wands.IWandable;

public final class RedcrystalBlock extends BaseEntityBlock implements IWandable {
    public static final DirectionProperty FACE = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public final String kind;
    public RedcrystalBlock(String kind) {
        super(Properties.of().strength(kind.equals("merc")?2.5f:.1f).noCollission().sound(kind.equals("merc")?SoundType.STONE:SoundType.GLASS).lightLevel(state -> (!automagy.AutomagyConfig.SPEC.isLoaded() || automagy.AutomagyConfig.REDCRYSTAL_LIGHT.get()) ? state.getValue(POWER)*2/3 : 0));
        this.kind = kind; registerDefaultState(stateDefinition.any().setValue(FACE, Direction.UP).setValue(POWER, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACE, POWER); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState().setValue(FACE, kind.equals("merc")?Direction.UP:context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction face = state.getValue(FACE); BlockPos support = pos.relative(face.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, face) || level.getBlockState(support).is(Blocks.GLOWSTONE);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double size = kind.equals("merc")?5.12:kind.equals("standard") ? 2.4 : 3.2;
        return switch (state.getValue(FACE)) {
            case UP -> box(1.6,0,1.6,14.4,size,14.4); case DOWN -> box(1.6,16-size,1.6,14.4,16,14.4);
            case NORTH -> box(1.6,1.6,16-size,14.4,14.4,16); case SOUTH -> box(1.6,1.6,0,14.4,14.4,size);
            case WEST -> box(16-size,1.6,1.6,16,14.4,14.4); case EAST -> box(0,1.6,1.6,size,14.4,14.4);
        };
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(!kind.equals("merc")||!(level.getBlockEntity(pos) instanceof RedcrystalBlockEntity tile)||!tile.mirrorValid)return;
        boolean powered=tile.power>0;int color=powered?0xff0000:((204+random.nextInt(52))<<16)|((204+random.nextInt(52))<<8)|(204+random.nextInt(52));
        if(powered&&random.nextInt(4)==0&&tile.mirror!=null&&tile.mirror.dimension().equals(level.dimension())){
            var here=pos.getCenter();var there=tile.mirror.pos().getCenter();
            thaumcraft.api.fx.LegacyEffects.emit(level,"wisp_path",tile.mirrorInput?there:here,tile.mirrorInput?here:there,0xff0000,.05f,36+random.nextInt(16),1,0,0);
        }
        var from=new net.minecraft.world.phys.Vec3(pos.getX()+.2+random.nextFloat()*.6,pos.getY()+(powered?(tile.mirrorInput?.8:.5):.2),pos.getZ()+.2+random.nextFloat()*.6);
        thaumcraft.api.fx.LegacyEffects.emit(level,"sparkle",from,net.minecraft.world.phys.Vec3.ZERO,color,1,18,1,0,powered&&tile.mirrorInput?.1f:-.1f);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RedcrystalBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide&&!kind.equals("merc") ? null : createTickerHelper(type, Content.REDCRYSTAL_TILE.get(), RedcrystalBlockEntity::tick);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!level.isClientSide&&kind.equals("merc")&&level.getBlockEntity(pos) instanceof RedcrystalBlockEntity tile){tile.mirror=stack.hasTag()?MirrorSignalLinks.read(stack.getTag().getCompound("Mirror")):null;tile.changed();}
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return RedcrystalNetwork.sampling() || !(level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be)
                || !be.output(side.getOpposite()) ? 0 : Math.min(15, be.power);
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return getSignal(state, level, pos, side); }
    @Override public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return side != null && level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be && (be.input(side.getOpposite()) || be.output(side.getOpposite()));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block from, BlockPos fromPos, boolean moving) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock())) for (Direction d : Direction.values()) level.updateNeighborsAt(pos.relative(d), this);
    }
    @Override public InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand, UseOnContext context) {
        return adjust(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getClickLocation());
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem
                ? adjust(level, pos, player, hit.getLocation()) : InteractionResult.PASS;
    }
    private InteractionResult adjust(Level level, BlockPos pos, Player player, Vec3 hit) {
        if (player == null || !player.mayBuild()) return InteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be) {
            Vec3 delta = hit.subtract(pos.getCenter()); Direction face = be.face();
            Direction selected = null; double distance = .15;
            for (Direction d : Direction.values()) if (d.getAxis() != face.getAxis()) {
                double value = delta.x*d.getStepX()+delta.y*d.getStepY()+delta.z*d.getStepZ();
                if (value > distance) { selected = d; distance = value; }
            }
            if (selected == null && (kind.equals("dim") || kind.equals("dense"))) {
                be.setting = kind.equals("dim") ? be.setting%14+1 : (be.setting-1)%14+2;
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.automagy.redcrystal."+kind, be.setting), true);
            } else if (selected != null) {
                if (kind.equals("amp")) be.connections = 1 << selected.ordinal();
                else be.connections ^= 1 << selected.ordinal();
            }
            be.changed();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
