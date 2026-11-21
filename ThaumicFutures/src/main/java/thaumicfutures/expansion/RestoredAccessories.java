package thaumicfutures.expansion;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import tc4revival.content.TCAttributes;
import tc4revival.research.*;

@Mod.EventBusSubscriber(modid="thaumicfutures")
public class RestoredAccessories extends Item implements ICurioItem,tc4revival.content.RunicGear {
    private static final UUID DISCOUNT=UUID.fromString("74253fca-2432-4a44-a9c9-47c9f7650072"),WARP=UUID.fromString("80594c58-4332-4986-8c45-21dd120f3619");
    public RestoredAccessories(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
    public static Optional<SlotResult> worn(LivingEntity entity,Item item){
        return CuriosApi.getCuriosInventory(entity).resolve().flatMap(inv->inv.findFirstCurio(stack->stack.is(item)));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        if(player.onGround())player.getPersistentData().remove("OmniCloudJump");
        int discount=0;
        if(thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()&&worn(player,RestoredContent.VOIDSEER_PEARL.get()).isPresent())
            discount=KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).map(k->Math.min(100,k.warp(PlayerKnowledge.WarpKind.PERMANENT))/10).orElse(0);
        // One entity modifier, independent of slot identity, prevents stacked pearls and updates as warp changes.
        set(player,TCAttributes.VIS_COST.get(),DISCOUNT,-discount/100d);
        set(player,TCAttributes.WARP.get(),WARP,discount/5d);
    }
    private static void set(LivingEntity entity,Attribute attribute,UUID id,double value){
        var instance=entity.getAttribute(attribute);if(instance==null)return;
        var old=instance.getModifier(id);if(old!=null&&old.getAmount()==value)return;
        instance.removeModifier(id);
        if(value!=0)instance.addTransientModifier(new AttributeModifier(id,"Voidseer pearl",value,AttributeModifier.Operation.ADDITION));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(event.isCanceled()||!(event.getEntity() instanceof ServerPlayer player)||!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
        var equipped=worn(player,RestoredContent.UNDYING_CHARM.get());if(equipped.isEmpty())return;
        var stack=equipped.get().stack();
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
        net.minecraft.advancements.CriteriaTriggers.USED_TOTEM.trigger(player,stack.copy());
        stack.shrink(1);
        player.setHealth(1);player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));
        player.level().broadcastEntityEvent(player,(byte)35);event.setCanceled(true);
    }
    public static void cloudJump(ServerPlayer player){
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()||!player.isAlive()||player.isSpectator()||player.onGround()
                ||player.isInWaterOrBubble()||player.isInLava()||player.isPassenger()||player.getAbilities().flying
                ||player.getPersistentData().getBoolean("OmniCloudJump")||worn(player,RestoredContent.CLOUD_RING.get()).isEmpty())return;
        player.getPersistentData().putBoolean("OmniCloudJump",true);
        double height=.75;var boost=player.getEffect(MobEffects.JUMP);if(boost!=null)height+=(boost.getAmplifier()+1)*.1;
        var movement=player.getDeltaMovement();player.setDeltaMovement(movement.x,height,movement.z);
        if(player.isSprinting()){
            float radians=player.getYRot()*net.minecraft.util.Mth.DEG_TO_RAD;
            player.setDeltaMovement(player.getDeltaMovement().add(-Math.sin(radians)*.2,0,Math.cos(radians)*.2));
        }
        player.fallDistance=0;player.hurtMarked=true;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,player.getX(),player.getY()+.2,player.getZ(),12,.25,.1,.25,.03);
        player.level().playSound(null,player,net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP,net.minecraft.sounds.SoundSource.PLAYERS,.15f,1.2f);
        net.minecraftforge.common.ForgeHooks.onLivingJump(player);
    }
}
