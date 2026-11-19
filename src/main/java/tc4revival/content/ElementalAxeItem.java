package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public  class ElementalAxeItem extends AxeItem implements RepairableItem {
    public ElementalAxeItem() { super(ElementalToolTier.INSTANCE,3,-3,new Properties().rarity(Rarity.RARE)); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    // Native axe transformations consume a block click; air use retains item attraction.
    @Override public InteractionResult useOn(UseOnContext context) { return super.useOn(context); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        player.startUsingItem(hand); return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining) {
        if (!(user instanceof Player player)) return;
        var center=player.position();
        for (var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(center,center).inflate(10),
                entity->entity.isAlive() && (!(entity instanceof FollowingItemEntity following) || !following.hasFollowingTarget()))) {
            var offset=new Vec3(item.getX()-player.getX(),item.getY()-player.getY()+player.getBbHeight()/2,item.getZ()-player.getZ());
            double length=offset.length(); if (length<1e-9) continue;
            var velocity=item.getDeltaMovement().subtract(offset.scale(.3/length));
            item.setDeltaMovement(Mth.clamp(velocity.x,-.35,.35),Mth.clamp(velocity.y,-.35,.35),Mth.clamp(velocity.z,-.35,.35));
            if (level.isClientSide) tc4revival.client.CrucibleEffects.elementalBubble(level,
                    (float)item.getX()+(level.random.nextFloat()-level.random.nextFloat())*.125f,
                    (float)item.getY()+(level.random.nextFloat()-level.random.nextFloat())*.125f,
                    (float)item.getZ()+(level.random.nextFloat()-level.random.nextFloat())*.125f);
        }
    }
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player) {
        var level=player.level();
        if (player.isShiftKeyDown() || !level.getBlockState(pos).is(BlockTags.LOGS)) return false;
        if (player instanceof ServerPlayer server) {
            var target=ElementalTreeSearch.furthest(level,pos);
            if (ElementalHarvest.harvest(server,target,10)) {
                for (int x=-3;x<=3;x++) for (int y=-3;y<=3;y++) for (int z=-3;z<=3;z++) {
                    var update=target.offset(x,y,z);
                    int delay=150+level.random.nextInt(150);
                    if (!level.hasChunkAt(update) || level.isOutsideBuildHeight(update)) continue;
                    var state=level.getBlockState(update);
                    if (!state.isAir()) {
                        server.serverLevel().scheduleTick(update,state.getBlock(),delay);
                        if (state.isRandomlyTicking()) LampPlantGrowth.schedule(server.serverLevel(),update,state,delay);
                    }
                }
            }
            level.playSound(null,pos,TCSounds.BUBBLE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.15f,1);
            tc4revival.network.FocusEffectPacket.elementalBlockBubble(level,pos);
        }
        stack.hurtAndBreak(1,player,owner->owner.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        return true;
    }
}
