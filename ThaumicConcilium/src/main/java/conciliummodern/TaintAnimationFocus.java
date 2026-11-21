package conciliummodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;
import tc4revival.research.PlayerKnowledge.WarpKind;

public final class TaintAnimationFocus extends ConciliumFocus implements thaumcraft.api.IWarpingGear {
    @Override public int getWarp(ItemStack stack,Player player){return 4;}
    @Override public String sortingPrefix(){return "CONCILIUM_TAINT_ANIMATION";}
    @Override public String depthTexture(){return "thaumicconcilium:item/taint_animation_depth";}
    @Override public String ornamentTexture(){return "thaumicconcilium:item/taint_animation_orn";}
    @Override public int color(ItemStack focus){return 0xeeeeee;}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){
        if(has(focus,"thought_manifestation"))return vis("aqua",130,"terra",130,"perditio",130);
        if(has(focus,"flux_vaporization"))return vis("aqua",130,"terra",130,"perditio",130,"ignis",50);
        return vis("aqua",100,"terra",100,"perditio",100);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return ranks(rank,List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,FocusUpgrade.ENLARGE),"thought_manifestation","flux_vaporization");}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){player.startUsingItem(hand);return InteractionResultHolder.consume(stack);}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player instanceof ServerPlayer server) || remaining%10!=0 || !(stack.getItem() instanceof CastingWandItem wand))return;
        var level=server.serverLevel();var focus=attached(stack);
        if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){player.stopUsingItem();return;}
        var actor=thaumcraft.api.wands.WandActors.resolve(player);
        var pointed=FocusTargeting.pointed(actor,0,32,1.1,e->e instanceof LivingEntity);
        LivingEntity target=pointed instanceof LivingEntity living && !(living instanceof TaintedMob)?living:null;
        if(player.isShiftKeyDown()){
            for(var original:level.getEntitiesOfClass(Mob.class,actor.getBoundingBox().inflate(16),e->e instanceof TaintedMob)){
                Mob mob=original;
                if(mob instanceof TaintSpiderEntity && !(mob instanceof SmartTaintSpider)){
                    var replacement=ConciliumEntities.SMART_TAINT_SPIDER.get().create(level);
                    if(replacement==null)continue;replacement.moveTo(mob.position());replacement.setYRot(mob.getYRot());
                    if(!level.addFreshEntity(replacement))continue;mob.discard();mob=replacement;
                }
                if(target!=null && mob!=target){
                    mob.setTarget(target);int potency=wand.focusUpgrade(stack,FocusUpgrade.POTENCY);
                    if(potency>0)mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,100,net.minecraft.util.Mth.clamp(warp(server,WarpKind.PERMANENT)/10*potency,1,200)));
                }
            }
            return;
        }
        if(has(focus,"thought_manifestation")){
            var spiders=level.getEntitiesOfClass(MindSpiderEntity.class,actor.getBoundingBox().inflate(16));
            if(!spiders.isEmpty()){
                var original=spiders.get(player.getRandom().nextInt(spiders.size()));var spider=ConciliumEntities.SMART_TAINT_SPIDER.get().create(level);
                if(spider!=null){spider.moveTo(original.position());if(level.addFreshEntity(spider)){original.discard();ConciliumArcNetwork.taint(level,spider.position());}}
            }
        }
        int distance=4+2*wand.focusUpgrade(stack,FocusUpgrade.ENLARGE);
        BlockPos chosen=null;int found=0;var center=actor.blockPosition();
        for(var cursor:BlockPos.betweenClosed(center.offset(-distance,-distance,-distance),center.offset(distance,distance,distance))){
            if(!level.hasChunkAt(cursor) || level.isOutsideBuildHeight(cursor) || !level.getWorldBorder().isWithinBounds(cursor))continue;
            var state=level.getBlockState(cursor);
            if((state.is(TCArcane.TAINT_FIBRES.get()) || state.is(TCArcane.FLUX_GAS.get()) || has(focus,"flux_vaporization") && state.is(TCArcane.FLUX_GOO.get()))
                    && player.getRandom().nextInt(++found)==0)chosen=cursor.immutable();
        }
        if(chosen==null || !level.mayInteract(player,chosen) || !player.mayUseItemAt(chosen,Direction.UP,stack)
                || ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,chosen)==-1)return;
        var state=level.getBlockState(chosen);
        if(state.is(TCArcane.FLUX_GOO.get())){
            if(!ForgeEventFactory.onBlockPlace(player,BlockSnapshot.create(level.dimension(),level,chosen),Direction.UP))level.setBlock(chosen,TCArcane.FLUX_GAS.get().defaultBlockState(),3);
            return;
        }
        Mob animated=state.is(TCArcane.TAINT_FIBRES.get())?TCEntities.TAINTACLE.get().create(level):TCEntities.TAINT_SWARM.get().create(level);
        if(animated==null)return;animated.moveTo(chosen.getX(),chosen.getY()+.5,chosen.getZ(),player.getRandom().nextFloat()*360,0);animated.setTarget(target);
        if(!level.addFreshEntity(animated))return;
        boolean consumed=state.is(TCArcane.TAINT_FIBRES.get()) && state.getValue(TaintFibreBlock.VARIANT)>0
                ?level.setBlock(chosen,state.setValue(TaintFibreBlock.VARIANT,state.getValue(TaintFibreBlock.VARIANT)-1),3)
                // removeBlock restores the block's FluidState; modern flux gas would remain in place.
                :level.setBlock(chosen,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        if(!consumed){animated.discard();return;}
        thaumcraft.api.fx.LegacyEffects.sparkle(level,Vec3.atCenterOf(chosen),14483711,1,20,.5f);
    }
}
