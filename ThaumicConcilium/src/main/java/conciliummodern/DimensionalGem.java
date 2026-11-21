package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;

/** The original two gems each have inert and awakened registry identities. */
public final class DimensionalGem extends Item {
    private final boolean rift,awakened;
    public DimensionalGem(boolean rift,boolean awakened){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.rift=rift;this.awakened=awakened;}
    public boolean rift(){return rift;}
    public boolean awakened(){return awakened;}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public boolean shouldCauseReequipAnimation(ItemStack before,ItemStack after,boolean slotChanged){return slotChanged||before.getItem()!=after.getItem();}
    @Override public boolean canContinueUsing(ItemStack before,ItemStack after){return before.getItem()==after.getItem();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!awakened||!ConciliumGems.available())return InteractionResultHolder.pass(stack);
        if(rift&&player.isShiftKeyDown()){
            if(!level.isClientSide){int size=GemCoordinates.samples(stack).size();if(size>0)stack.getOrCreateTag().putInt("CURR",(GemCoordinates.selected(stack,size)+1)%size);}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public boolean onEntitySwing(ItemStack stack,LivingEntity entity){
        if(rift&&awakened&&entity instanceof Player player&&player.isShiftKeyDown()&&!player.level().isClientSide&&ConciliumGems.available())
            stack.getOrCreateTag().putBoolean("REVERSE",!GemCoordinates.reverse(stack));
        return false;
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(!(entity instanceof ServerPlayer player)||!awakened||!ConciliumGems.available()||(getUseDuration(stack)-remaining+1)%80!=0)return;
        if(!level.getEntitiesOfClass(RiftEntity.class,player.getBoundingBox().inflate(16),e->!e.isRemoved()).isEmpty())return;
        if(rift){openRift(player);player.stopUsingItem();}else captureEarth(player,stack);
    }
    private static BlockHitResult target(Player player){
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(player.getBlockReach()));
        return player.level().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
    }
    private static void openRift(ServerPlayer player){
        var hit=target(player);var target=hit.getType()==HitResult.Type.BLOCK?hit.getLocation():player.position().add(player.getLookAngle().scale(5));
        target=target.add(0,1.5,0);var pos=BlockPos.containing(target);var level=player.serverLevel();
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos)||!level.mayInteract(player,pos))return;
        var payment=GemVisPayment.prepare(player,"perditio");if(payment==null)return;
        var rift=ConciliumEntities.RIFT.get().create(level);if(rift==null)return;rift.setPos(target);rift.initialize(player.getRandom().nextInt(),50);RiftTransit.initialize(rift);
        if(!level.addFreshEntity(rift))return;
        if(!payment.commit()){rift.discard();return;}
        level.playSound(null,player.blockPosition(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1+(player.getRandom().nextFloat()-player.getRandom().nextFloat())*.2f);
        // The entity floats 1.5 blocks above the hit; the original bolt ends at the hit itself.
        ConciliumArcNetwork.bolt(player,target.add(0,-1.5,0),0x333333);
    }
    private static void captureEarth(ServerPlayer player,ItemStack stack){
        var hit=target(player);if(hit.getType()!=HitResult.Type.BLOCK)return;var pos=hit.getBlockPos();var level=player.serverLevel();
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos)||!level.mayInteract(player,pos)||!player.mayBuild()||!player.mayUseItemAt(pos,hit.getDirection(),stack))return;
        var state=level.getBlockState(pos);
        // Never silently consume inventories or half of a multi-block structure as a terrain sample.
        if(!state.blocksMotion()||state.getDestroySpeed(level,pos)<0||state.hasBlockEntity()||state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)||state.hasProperty(BlockStateProperties.BED_PART))return;
        var sampleItem=new ItemStack(state.getBlock());
        if(sampleItem.isEmpty()||thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(sampleItem).getAmount(thaumcraft.api.aspects.Aspect.EARTH)<=0)return;
        var payment=GemVisPayment.prepare(player,"terra");if(payment==null||MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)))return;
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);
        if(!level.removeBlock(pos,false))return;
        if(!payment.commit()){snapshot.restore(true,false);return;}
        stack.getOrCreateTag().put("Captured",new GemCoordinates.Sample(state,level.dimension()).save());
        level.playSound(null,pos,tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1+(player.getRandom().nextFloat()-player.getRandom().nextFloat())*.2f);
        ConciliumArcNetwork.bolt(player,hit.getLocation(),0x00ff00);
        GemCaptureNetwork.emit(level,pos,state,new Vec3((int)player.getX()+.5,(int)player.getY()+.5,(int)player.getZ()+.5));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){
        if(!awakened)return;
        lines.add(Component.translatable("item.Focus.cost1"));
        for(String key:java.util.List.of("aer",rift?"perditio":"terra")){
            var aspect=tc4revival.aspect.TCAspects.catalog().require(key);
            lines.add(Component.literal(" "+thaumcraft.api.aspects.AspectViews.display(aspect).displayName()).withStyle(style->style.withColor(thaumcraft.api.aspects.AspectViews.display(aspect).color()))
                    .append(Component.literal(" x 10").withStyle(net.minecraft.ChatFormatting.RESET)));
        }
        var sample=rift?GemCoordinates.selected(stack):GemCoordinates.captured(stack);
        if(rift){lines.add(Component.translatable(GemCoordinates.reverse(stack)?"tc.tooltip.riftgem.deflate":"tc.tooltip.riftgem.inflate"));int count=GemCoordinates.samples(stack).size();if(count>0)lines.add(Component.translatable("tc.tooltip.riftgem").append(" "+count+"/16"));}
        if(sample!=null)lines.add(Component.translatable("tc.tooltip.terragem").append(" ").append(sample.display().getHoverName()));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return DimensionalGemRenderer.instance();}
    });}
}
