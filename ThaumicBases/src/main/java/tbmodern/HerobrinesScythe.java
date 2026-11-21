package tbmodern;

import java.util.*;
import com.google.common.collect.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;

@Mod.EventBusSubscriber(modid=ThaumicBases.ID)
public class HerobrinesScythe extends SwordItem implements IRepairable,IWarpingGear {
    private final Multimap<Attribute,AttributeModifier> attributes;
    public HerobrinesScythe() {
        super(Tiers.DIAMOND,3,-2.4F,new Properties().rarity(Rarity.UNCOMMON));
        attributes=ImmutableMultimap.<Attribute,AttributeModifier>builder()
            .put(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Weapon modifier",14.5,AttributeModifier.Operation.ADDITION))
            .put(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_UUID,"Weapon modifier",-2.4,AttributeModifier.Operation.ADDITION))
            .put(Attributes.MOVEMENT_SPEED,new AttributeModifier(UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CE"),"Scythe speed",.5,AttributeModifier.Operation.MULTIPLY_TOTAL)).build();
    }
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot==EquipmentSlot.MAINHAND?attributes:super.getDefaultAttributeModifiers(slot);
    }
    @Override public int getWarp(ItemStack stack,Player player) { return 3; }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.literal("Well, they’re nothing...").withStyle(ChatFormatting.ITALIC));
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean held) {
        if(!level.isClientSide && entity instanceof LivingEntity && entity.tickCount%20==0 && stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);
    }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity target) {
        if(!player.level().isClientSide && target instanceof LivingEntity living && living instanceof Enemy && living.isAlive())chain(player,living);
        return false;
    }
    public static void chain(Player player,LivingEntity first) {
        // Iterative traversal preserves the old single random chain without risking stack overflow.
        Set<UUID> visited=new HashSet<>();visited.add(first.getUUID());
        LivingEntity previous=first;
        while(player.isAlive() && player.getMainHandItem().getItem() instanceof HerobrinesScythe) {
            var candidates=player.level().getEntitiesOfClass(LivingEntity.class,new AABB(previous.getX()-7,previous.getY()-7,previous.getZ()-7,previous.getX()+7,previous.getY()+7,previous.getZ()+7),
                e->e instanceof Enemy && !(e instanceof Player) && e.isAlive() && !visited.contains(e.getUUID()));
            if(candidates.isEmpty())return;
            var next=candidates.get(player.getRandom().nextInt(candidates.size()));visited.add(next.getUUID());
            attack(player,next);
            WorkEffects.lightning(player.level(),previous.position().add(0,previous.getEyeHeight()*.5,0),next.position().add(0,next.getEyeHeight()*.5,0),0xffffff);
            WorkEffects.sound(player.level(),next.blockPosition(),"zap",1,.8F);
            previous=next;
        }
    }
    /** Each secondary hit uses the original full melee damage, with combat hooks and enchants. */
    private static void attack(Player player,LivingEntity target) {
        if(MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target)) || !target.isAttackable() || target.skipAttackInteraction(player))return;
        ItemStack weapon=player.getMainHandItem();
        float damage=(float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float bonus=EnchantmentHelper.getDamageBonus(weapon,target.getMobType());
        boolean critical=player.fallDistance>0 && !player.onGround() && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger();
        var criticalHook=net.minecraftforge.common.ForgeHooks.getCriticalHit(player,target,critical,critical?1.5F:1);
        if(criticalHook!=null)damage*=criticalHook.getDamageModifier();
        damage+=bonus;
        int fire=EnchantmentHelper.getFireAspect(player),knockback=EnchantmentHelper.getKnockbackBonus(player)+(player.isSprinting()?1:0);
        boolean preIgnite=fire>0 && !target.isOnFire();if(preIgnite)target.setSecondsOnFire(1);
        float before=target.getHealth();
        if(target.hurt(player.damageSources().playerAttack(player),damage)) {
            if(knockback>0) {
                target.knockback(knockback*.5,Mth.sin(player.getYRot()*Mth.DEG_TO_RAD),-Mth.cos(player.getYRot()*Mth.DEG_TO_RAD));
                player.setDeltaMovement(player.getDeltaMovement().multiply(.6,1,.6));player.setSprinting(false);
            }
            if(criticalHook!=null)player.crit(target);if(bonus>0)player.magicCrit(target);
            player.setLastHurtMob(target);EnchantmentHelper.doPostHurtEffects(target,player);EnchantmentHelper.doPostDamageEffects(player,target);
            weapon.hurtEnemy(target,player);player.awardStat(Stats.DAMAGE_DEALT,Math.round((before-target.getHealth())*10));
            if(fire>0)target.setSecondsOnFire(fire*4);player.causeFoodExhaustion(.3F);
        } else if(preIgnite)target.clearFire();
    }
    @SubscribeEvent public static void name(PlayerEvent.NameFormat event) {
        if(event.getEntity().getMainHandItem().getItem() instanceof HerobrinesScythe)event.setDisplayname(Component.literal("Herobrine"));
    }
    private static final Map<Player,Boolean> NAMED=Collections.synchronizedMap(new WeakHashMap<>());
    @SubscribeEvent public static void refreshName(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        boolean held=event.player.getMainHandItem().getItem() instanceof HerobrinesScythe;
        Boolean previous=NAMED.put(event.player,held);
        if(previous==null?held:previous!=held)event.player.refreshDisplayName();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { NAMED.remove(event.getEntity()); }
}
