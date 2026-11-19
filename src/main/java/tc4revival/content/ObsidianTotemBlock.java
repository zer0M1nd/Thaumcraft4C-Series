package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public  class ObsidianTotemBlock extends BaseEntityBlock {
    public static final IntegerProperty FORM=IntegerProperty.create("form",0,2),PHASE=IntegerProperty.create("phase",0,18);
    private final boolean node;
    public ObsidianTotemBlock(boolean node) { super(Properties.of().strength(30,999).sound(SoundType.STONE));this.node=node;registerDefaultState(stateDefinition.any().setValue(FORM,0).setValue(PHASE,9)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FORM,PHASE); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public static boolean isTotem(BlockState state) { return state.getBlock() instanceof ObsidianTotemBlock; }
    public BlockState at(BlockGetter level,BlockPos pos) {
        int form=isTotem(level.getBlockState(pos.above()))?1:isTotem(level.getBlockState(pos.below()))?2:0;
        return defaultBlockState().setValue(FORM,form).setValue(PHASE,pos.getX()%4+pos.getY()%4+pos.getZ()%4+9);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return at(context.getLevel(),context.getClickedPos()); }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) { return at(level,pos); }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving);var expected=at(level,pos);if(state!=expected)level.setBlock(pos,expected,3);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return node?new AuraNodeBlockEntity(pos,state):null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return node&&type==TCArcane.AURA_NODE_ENTITY.get()?(world,pos,current,tile)->((AuraNodeBlockEntity)tile).tick():null; }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player) { if(node) { AuraNodeBlock.dropEssence(level,pos);if(!level.isClientSide) { tc4revival.network.FocusEffectPacket.burst(level,pos.getCenter(),1);level.playSound(null,pos,TCSounds.CRAFT_FAIL.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1); } }super.playerWillDestroy(level,pos,state,player); }
    public static int faceIndex(int phase,int side) { return Math.abs((side+phase-9)%4); }
}
