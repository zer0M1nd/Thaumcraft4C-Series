package tbmodern;

import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.content.*;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.nodes.NodeApi;

public final class ConcentratedTaint extends Item {
    public ConcentratedTaint() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();
        if(player==null || !level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(level instanceof ServerLevel server) {
            if(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node)
                NodeApi.edit(node,NodeApi.getBase(node),AuraNodeType.TAINTED,node.permanentModifier());
            else {
                var columns=new ArrayList<BlockPos>();
                for(int i=0;i<100;i++) {
                    BlockPos target=pos.offset((int)Math.floor((level.random.nextDouble()*2-1)*16),level.random.nextInt(2)-level.random.nextInt(2),(int)Math.floor((level.random.nextDouble()*2-1)*16));
                    if(!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target) || !level.mayInteract(player,target))continue;
                    columns.add(target);
                    var fibre=TCArcane.TAINT_FIBRES.get().defaultBlockState();
                    if(level.getBlockState(target.below()).isSolidRender(level,target.below()) && level.getBlockState(target).canBeReplaced() && fibre.canSurvive(level,target))level.setBlock(target,fibre,3);
                    server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,new ItemStack(tc4revival.content.TCItems.TAINT_BOTTLE.get())),target.getX()+.5,target.getY()+.5,target.getZ()+.5,8,0,0,0,.15);
                }
                WorldBiomeChanges.changeColumns(server,columns,TCBiomes.TAINTED_LAND);
            }
            context.getItemInHand().shrink(1);
            level.playSound(null,pos,SoundEvents.GLASS_BREAK,SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player user,LivingEntity target,InteractionHand hand) {
        if(target instanceof TaintedMob)return InteractionResult.PASS;
        if(!target.level().isClientSide) {
            if(target instanceof Player victim && user instanceof ServerPlayer attacker && !attacker.canHarmPlayer(victim))return InteractionResult.FAIL;
            target.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200,1,true,true));
            if(target instanceof Player) {
                for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
                    var armor=target.getItemBySlot(slot);
                    if(armor.isEmpty())continue;
                    var drop=new ItemEntity(target.level(),target.getX(),target.getY(),target.getZ(),armor.copy());drop.setPickUpDelay(1200);
                    if(target.level().addFreshEntity(drop))target.setItemSlot(slot,ItemStack.EMPTY);
                }
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200,0,true,true));
                target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,200,0,true,true));
            }
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(target.level().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server && !ThaumcraftApiHelper.isResearchComplete(server,"TB.TaintMinor"))return InteractionResultHolder.fail(player.getItemInHand(hand));
        player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        if(!(entity instanceof ServerPlayer player) || !ThaumcraftApiHelper.isResearchComplete(player,"TB.TaintMinor"))return stack;
        if(player.getHealth()<player.getMaxHealth()) {
            var source=new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("thaumcraft","taint"))));
            player.hurt(source,Float.MAX_VALUE);
        } else {
            player.setHealth(1);
            for(var effect:new MobEffect[]{MobEffects.BLINDNESS,MobEffects.NIGHT_VISION,MobEffects.MOVEMENT_SLOWDOWN,MobEffects.WEAKNESS})
                player.addEffect(new MobEffectInstance(effect,600,effect==MobEffects.MOVEMENT_SLOWDOWN || effect==MobEffects.WEAKNESS?4:0,true,true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,100,0,true,true));
            String key;
            if(player.getY()>6) { key="howlBelow";ThaumcraftApiHelper.addWarpToPlayer(player,6,true); }
            else if(level.dimension().equals(Level.NETHER)) {
                key="taintDevelop";ThaumcraftApiHelper.addWarpToPlayer(player,18,true);ThaumcraftApiHelper.addStickyWarpToPlayer(player,6);
                ThaumcraftApiHelper.addWarpToPlayer(player,1,false);ThaumcraftApiHelper.completeResearch(player,"TB.TaintProgress");
            } else { key="howlDeeper";ThaumcraftApiHelper.addWarpToPlayer(player,12,true); }
            player.displayClientMessage(Component.translatable("tb.txt."+key).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC),false);
        }
        if(player.getAbilities().instabuild)return stack;
        stack.shrink(1);return new ItemStack(TCItems.EMPTY_PHIAL.get());
    }
}
