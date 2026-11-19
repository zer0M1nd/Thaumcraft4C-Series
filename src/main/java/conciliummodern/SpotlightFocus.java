package conciliummodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

public final class SpotlightFocus extends ConciliumFocus {
    @Override public String sortingPrefix(){return "CONCILIUM_SPOTLIGHT";}
    @Override public int color(ItemStack focus){return 0xffff00;}
    @Override public Animation animation(ItemStack focus){return Animation.CHARGE;}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){
        if(has(focus,"warming_up"))return vis("ignis",150,"ordo",100,"aer",150,"aqua",100,"terra",100);
        if(has(focus,"taint_purification"))return vis("ignis",50,"ordo",150,"aer",600,"aqua",300,"terra",300);
        return vis("ignis",550,"ordo",100,"aer",600);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return ranks(rank,List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY),"warming_up","taint_purification");}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!requireRing(player))return InteractionResultHolder.fail(wand);
        player.startUsingItem(hand);return InteractionResultHolder.consume(wand);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player.level() instanceof ServerLevel level) || !(stack.getItem() instanceof CastingWandItem wand))return;
        var focus=attached(stack);
        if(!ring(player) || !wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){player.stopUsingItem();return;}
        var actor=thaumcraft.api.wands.WandActors.resolve(player);
        var start=actor.getEyePosition();var hit=actor.pick(50,0,false);var end=hit.getLocation();
        var target=FocusTargeting.pointed(actor,0,50,.1,e->e instanceof LivingEntity);
        boolean pure=has(focus,"taint_purification"),warming=has(focus,"warming_up");
        if(target instanceof LivingEntity living){
            end=living.getEyePosition();
            if(warming)living.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20,5+(int)wand.focusPotency(stack,player)));
            else if(!pure || living instanceof TaintedMob || living instanceof thaumcraft.api.entities.IEldritchMob){
                if(!pure || !purify(living)){
                    float damage=1+player.getTicksUsingItem()/(pure?10f:20f)*wand.focusPotency(stack,player);
                    // Forge damage hooks and the normal death/loot path remain authoritative.
                    living.hurt(level.damageSources().indirectMagic(actor,actor),damage);
                }
            }
        }
        if(pure && hit instanceof BlockHitResult block && hit.getType()==HitResult.Type.BLOCK){
            BlockPos pos=block.getBlockPos();
            if(level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player,pos)
                    && player.mayUseItemAt(pos,block.getDirection(),stack) && level.getBiome(pos).is(TCBiomes.TAINTED_LAND)
                    && !ForgeEventFactory.onBlockPlace(player,BlockSnapshot.create(level.dimension(),level,pos),block.getDirection()))
                WorldBiomeChanges.changeColumn(level,pos,TCBiomes.MAGICAL_FOREST);
        }
        thaumcraft.api.fx.LegacyEffects.wandBeam(player,end,1,0xffff00,1,false);
        if(player.tickCount%10==0)level.playSound(null,actor.blockPosition(),ConciliumSounds.HUM.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
    }
    private static boolean purify(LivingEntity tainted){
        EntityType<?> type=null;
        if(tainted instanceof TaintedCreeperEntity)type=EntityType.CREEPER;
        else if(tainted instanceof TaintedAnimalEntity animal)type=switch(animal.kind()){
            case CHICKEN->EntityType.CHICKEN;case COW->EntityType.COW;case PIG->EntityType.PIG;
            case SHEEP->EntityType.SHEEP;case VILLAGER->EntityType.VILLAGER;case CREEPER->EntityType.CREEPER;
        };
        if(type==null)return false;
        var replacement=type.create(tainted.level());if(replacement==null)return true;
        replacement.moveTo(tainted.getX(),tainted.getY(),tainted.getZ(),tainted.getYRot(),tainted.getXRot());
        replacement.setCustomName(tainted.getCustomName());replacement.setCustomNameVisible(tainted.isCustomNameVisible());
        if(replacement instanceof Mob mob && tainted instanceof Mob old && old.isPersistenceRequired())mob.setPersistenceRequired();
        if(tainted.level().addFreshEntity(replacement))tainted.discard();
        return true;
    }
    @Override public void release(ItemStack stack,Level level,Player player,int remaining){
        long elapsed=Math.max(0L,(long)Integer.MAX_VALUE-remaining);
        WandCooldowns.begin(player,(int)Math.min(Integer.MAX_VALUE,1000+elapsed*100));
    }
}
