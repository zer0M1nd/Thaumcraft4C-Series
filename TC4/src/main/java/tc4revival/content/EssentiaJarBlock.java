package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class EssentiaJarBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public EssentiaJarBlock() {
        super(Properties.of().strength(.3f).noOcclusion().lightLevel(state->state.getValue(LIT)?9:0).sound(new net.minecraftforge.common.util.ForgeSoundType(1,1,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR)));
        registerDefaultState(stateDefinition.any().setValue(LIT,false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) { builder.add(LIT); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos) { return true; }
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos) { return 0; }
    @Override public float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos) { return 1; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(3,0,3,13,12,13); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return getShape(state,level,pos,context); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EssentiaJarBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,TCArcane.ESSENTIA_JAR_ENTITY.get(),(world,pos,block,jar)->jar.tick());
    }

    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) {
        return level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar ? jar.asItem() : super.getCloneItemStack(level,pos,state);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar) { jar.readItem(stack); if (placer!=null) jar.face(placer.getDirection().getOpposite()); }
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar ? net.minecraft.util.Mth.clamp(jar.amount()*14/64+(jar.amount()>0 ? 1 : 0),0,15) : 0;
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar)) return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,hit.getDirection(),held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && jar.filter()!=null && hit.getDirection()==jar.facing()) {
            jar.removeLabel(); var side=hit.getDirection();
            level.addFreshEntity(new ItemEntity(level,pos.getX()+.5+side.getStepX()/3d,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()/3d,new ItemStack(TCItems.JAR_LABEL.get())));
            level.playSound(null,pos,TCSounds.PAGE.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        } else if (player.isShiftKeyDown() && held.isEmpty()) {
            jar.empty(); level.playSound(null,pos,TCSounds.JAR.get(),net.minecraft.sounds.SoundSource.BLOCKS,.4f,1);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        } else if (held.is(TCItems.JAR_LABEL.get()) && jar.filter()==null) {
            var named=TCItems.JAR_LABEL.get().storedAspects(held);
            var selected=jar.amount()>0 ? jar.aspect() : named.isEmpty() ? null : named.aspects().get(0);
            if (selected!=null) {
                jar.label(selected,player.getDirection().getOpposite()); if(!player.getAbilities().instabuild)held.shrink(1);
                level.playSound(null,pos,TCSounds.JAR.get(),net.minecraft.sounds.SoundSource.BLOCKS,.4f,1);
            }
        }
        return InteractionResult.CONSUME;
    }
}
