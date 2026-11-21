package ttmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class AwakenedSwordItem extends SwordItem {
    private static final Set<UUID> SWEEPING=new HashSet<>();
    private static final Map<net.minecraft.world.damagesource.DamageSource,Float> RAW_DAMAGE=new WeakHashMap<>();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void capture(LivingHurtEvent event){
        if(!event.isCanceled()&&event.getAmount()>0&&event.getSource().getDirectEntity() instanceof Player player&&!player.level().isClientSide&&!SWEEPING.contains(player.getUUID())&&player.getMainHandItem().getItem() instanceof AwakenedSwordItem&&AwakenedModes.mode(player.getMainHandItem())==1)RAW_DAMAGE.put(event.getSource(),event.getAmount());
    }
    public AwakenedSwordItem(){super(IchorTools.TIER,4,-2.4f,new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return AwakenedModes.use(level,player,hand,"sword");}
    @Override public void appendHoverText(ItemStack stack,Level level,List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){lines.add(AwakenedModes.label(stack,"sword").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(AttackEntityEvent event){
        var player=event.getEntity();if(player.level().isClientSide||event.isCanceled()||!(event.getTarget() instanceof LivingEntity target)||target.hurtTime>0||!target.isAlive())return;
        var stack=player.getMainHandItem();if(stack.getItem() instanceof AwakenedSwordItem&&AwakenedModes.mode(stack)==2)target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1,1));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingHurtEvent event){
        if(event.isCanceled()||event.getAmount()<=0||!(event.getSource().getDirectEntity() instanceof Player player)||player.level().isClientSide)return;
        var stack=player.getMainHandItem();if(!(stack.getItem() instanceof AwakenedSwordItem)||AwakenedModes.mode(stack)!=1||!SWEEPING.add(player.getUUID()))return;
        try{
            float damage=RAW_DAMAGE.getOrDefault(event.getSource(),event.getAmount());RAW_DAMAGE.remove(event.getSource());
            var target=event.getEntity();var box=target.getBoundingBox().inflate(3);
            for(var other:player.level().getEntitiesOfClass(LivingEntity.class,box))if(other!=target&&other!=player&&target.getClass().isInstance(other)&&other.isAlive()&&(!(other instanceof Player victim)||player.canHarmPlayer(victim)))other.hurt(player.damageSources().playerAttack(player),damage);
        }finally{SWEEPING.remove(player.getUUID());}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void dealt(LivingDamageEvent event){
        if(event.isCanceled()||event.getAmount()<=0||!(event.getSource().getDirectEntity() instanceof Player player)||player.level().isClientSide)return;
        var stack=player.getMainHandItem();if(stack.getItem() instanceof AwakenedSwordItem&&AwakenedModes.mode(stack)==2)SoulHearts.add(player,1);
    }
}
