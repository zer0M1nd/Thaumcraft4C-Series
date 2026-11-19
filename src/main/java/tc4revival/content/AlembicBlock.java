package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public  class AlembicBlock extends BaseEntityBlock {
    public AlembicBlock() { super(Properties.of().strength(3,10.2f).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new AlembicBlockEntity(pos,state); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack) {
        if (placer!=null && level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic) alembic.face(placer.getDirection().getOpposite());
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic)) return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,hit.getDirection(),held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && alembic.filter()!=null) {
            alembic.removeLabel(); var side=hit.getDirection();
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5+side.getStepX()/3d,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()/3d,new ItemStack(TCItems.JAR_LABEL.get())));
            sound(level,pos,TCSounds.PAGE.get(),1,1.1f); return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) { alembic.empty(); sound(level,pos,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,.5f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f); }
            else {
                int message=alembic.aspect()==null || alembic.amount()==0 ? 1 : alembic.amount()<32*.4 ? 2 : alembic.amount()<32*.8 ? 3 : alembic.amount()<32 ? 4 : 5;
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tile.alembic.msg."+message).withStyle(net.minecraft.ChatFormatting.DARK_AQUA),false);
            }
            sound(level,pos,TCSounds.ALEMBIC_KNOCK.get(),.2f,1);
        } else if (held.is(TCItems.JAR_LABEL.get()) && alembic.filter()==null) {
            var named=TCItems.JAR_LABEL.get().storedAspects(held); var selected=alembic.amount()>0 ? alembic.aspect() : named.isEmpty() ? null : named.aspects().get(0);
            if (selected!=null) { alembic.label(selected); held.shrink(1); sound(level,pos,TCSounds.PAGE.get(),1,.9f); }
        } else if (held.getItem() instanceof EssentiaJarItem && alembic.amount()>0) {
            if (fillJar(held,alembic,player)) sound(level,pos,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,.5f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        }
        return InteractionResult.CONSUME;
    }
    private static boolean fillJar(ItemStack held,AlembicBlockEntity alembic,Player player) {
        var type=(EssentiaJarItem)held.getItem(); boolean empty=held.is(TCItems.ESSENTIA_JAR.get()) || held.is(TCItems.VOID_JAR.get());
        boolean voidJar=held.is(TCItems.VOID_JAR.get()) || held.is(TCItems.FILLED_VOID_JAR.get());
        var stored=type.storedAspects(held); var selected=alembic.aspect();
        var filter=held.hasTag() ? tc4revival.aspect.TCAspects.catalog().find(held.getTag().getString("AspectFilter")).orElse(null) : null;
        if (!empty && ((stored.total()!=0 && stored.amount(selected)<=0) || filter!=null && !filter.equals(selected))) return false;
        int taken=empty || voidJar ? alembic.amount() : (int)Math.min(Math.max(0,64-stored.total()),alembic.amount());
        if (taken<=0 || !alembic.takeFromContainer(selected,taken)) return false;
        var result=empty ? new ItemStack(voidJar ? TCItems.FILLED_VOID_JAR.get() : TCItems.FILLED_ESSENTIA_JAR.get()) : held;
        var contents=empty ? new tc4revival.aspect.AspectAmounts() : stored.copy();
        contents.add(selected,Math.min(taken,Math.max(0,64-contents.amount(selected)))); type.setStoredAspects(result,contents);
        if (empty) { held.shrink(1); if (!player.getInventory().add(result)) player.drop(result,false); }
        player.containerMenu.broadcastChanges(); return true;
    }
    private static void sound(Level level,BlockPos pos,net.minecraft.sounds.SoundEvent sound,float volume,float pitch) { level.playSound(null,pos,sound,net.minecraft.sounds.SoundSource.BLOCKS,volume,pitch); }
}
