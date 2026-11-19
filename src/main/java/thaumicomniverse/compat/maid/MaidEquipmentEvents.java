package thaumicomniverse.compat.maid;

import java.util.*;
import java.nio.charset.StandardCharsets;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import tc4revival.content.*;
import thaumcraft.api.wands.WandActors;

/** Registered by the host extension, never resolved on installations without the maid mod. */
public final class MaidEquipmentEvents {
    private static final String SUMMONER="thaumicomniverse:maid_summoner";
    private static final class State {
        final RunicShieldState shield=new RunicShieldState();
        final Map<Attribute,List<UUID>> modifiers=new HashMap<>();
        long rechargeAfter;
    }
    private static final Map<EntityMaid,State> STATES=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static State state(EntityMaid maid) { return STATES.computeIfAbsent(maid,key->new State()); }
    private static void equipment(EntityMaid maid,State state) {
        state.modifiers.forEach((attribute,ids)->{var instance=maid.getAttribute(attribute);if(instance!=null)ids.forEach(instance::removeModifier);});state.modifiers.clear();
        if(!thaumicomniverse.RevivalFeatures.Group.MAID.enabled()) {
            state.shield.equipment(new RunicShieldState.Equipment(0,0,0,0,0));return;
        }
        var baubles=maid.getMaidBauble();
        var upgrades=new int[4];
        var worn=new ArrayList<net.minecraft.world.item.ItemStack>();maid.getArmorSlots().forEach(worn::add);
        for(int slot=0;slot<baubles.getSlots();slot++) {
            var stack=baubles.getStackInSlot(slot);if(stack.isEmpty())continue;worn.add(stack);
            String name="necklace";
            for(String candidate:List.of("ring","belt","necklace")) if(stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("curios:"+candidate)))) {name=candidate;break;}
            var uuid=UUID.nameUUIDFromBytes(("OmniverseMaidBauble:"+slot).getBytes(StandardCharsets.UTF_8));
            var modifiers=top.theillusivec4.curios.api.CuriosApi.getAttributeModifiers(new top.theillusivec4.curios.api.SlotContext(name,maid,0,false,true),uuid,stack);
            for(var entry:modifiers.entries()) {
                var attribute=entry.getKey();var instance=maid.getAttribute(attribute);if(instance==null)continue;
                var original=entry.getValue();
                var unique=UUID.nameUUIDFromBytes((uuid+":"+original.getId()).getBytes(StandardCharsets.UTF_8));
                instance.removeModifier(unique);instance.addTransientModifier(new AttributeModifier(unique,"Maid arcane bauble",original.getAmount(),original.getOperation()));
                state.modifiers.computeIfAbsent(attribute,key->new ArrayList<>()).add(unique);
            }
        }
        for(var stack:worn) if(stack.getItem() instanceof RunicUpgradeGear gear) switch(gear.runicUpgrade(stack)) {
            case CHARGED->upgrades[0]++;case KINETIC->upgrades[1]++;case HEALING->upgrades[2]++;case EMERGENCY->upgrades[3]++;default->{}
        }
        state.shield.equipment(new RunicShieldState.Equipment((int)Math.max(0,maid.getAttributeValue(TCAttributes.RUNIC_SHIELD.get())),upgrades[0],upgrades[1],upgrades[2],upgrades[3]));
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide)return;
        var state=state(maid);if(maid.tickCount%10==0)equipment(maid,state);
        long tick=maid.level().getGameTime();
        if(tick<state.rechargeAfter)return;
        var wand=maid.getMainHandItem();
        state.shield.recharge(tick*50,tc4revival.config.TCConfig.SERVER.shieldRechargeMilliseconds(),()->wand.getItem() instanceof CastingWandItem item && item.consumeForPlayer(wand,MaidWandCombat.actor(maid),Map.of(
                tc4revival.aspect.TCAspects.catalog().require("aer"),tc4revival.config.TCConfig.SERVER.shieldCostCentiVis(),
                tc4revival.aspect.TCAspects.catalog().require("terra"),tc4revival.config.TCConfig.SERVER.shieldCostCentiVis()),false,true,true));
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void protect(LivingAttackEvent event) {
        var source=WandActors.resolve(event.getSource().getEntity());
        var caster=MaidWandCombat.casting();if(caster==null)caster=summoner(source);
        if(caster!=null && (event.getEntity()==caster || !caster.canAttack(event.getEntity())))event.setCanceled(true);
    }
    @SubscribeEvent public static void projectile(EntityJoinLevelEvent event) {
        if(!event.getLevel().isClientSide && event.getEntity() instanceof Projectile projectile) {
            var actor=WandActors.resolve(projectile.getOwner());if(actor instanceof EntityMaid)projectile.setOwner(actor);
        }
        if(!event.getLevel().isClientSide && MaidWandCombat.casting()!=null && event.getEntity() instanceof net.minecraft.world.entity.Mob summoned) {
            var maid=MaidWandCombat.casting();summoned.getPersistentData().putUUID(SUMMONER,maid.getUUID());
            maid.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).ifPresent(summoned::setTarget);
        }
    }
    private static EntityMaid summoner(net.minecraft.world.entity.Entity entity) {
        if(entity instanceof EntityMaid maid)return maid;
        if(entity!=null && entity.level() instanceof net.minecraft.server.level.ServerLevel level && entity.getPersistentData().hasUUID(SUMMONER)
                && level.getEntity(entity.getPersistentData().getUUID(SUMMONER)) instanceof EntityMaid maid)return maid;
        return null;
    }
    @SubscribeEvent public static void targeting(LivingChangeTargetEvent event) {
        var maid=summoner(event.getEntity());var target=event.getNewTarget();
        if(maid!=null && target!=null && (target==maid || !maid.canAttack(target)))event.setNewTarget(null);
    }
    @SubscribeEvent public static void effect(MobEffectEvent.Applicable event) {
        var maid=MaidWandCombat.casting();
        if(maid!=null && event.getEffectInstance().getEffect().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL
                && (event.getEntity()==maid || !maid.canAttack(event.getEntity())))event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if(!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide)return;
        var source=event.getSource();
        if(source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)||source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)||source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)||source.is(net.minecraft.world.damagesource.DamageTypes.STARVE))return;
        var state=state(maid);var hit=state.shield.hurt(event.getAmount(),maid.level().getGameTime()*50);
        if(!hit.shielded())return;event.setAmount(hit.remainder());
        if(hit.broken())state.rechargeAfter=maid.level().getGameTime()+tc4revival.config.TCConfig.SERVER.shieldWaitTicks();
        int target=source.getEntity()==null?-1:source.getEntity().getId();
        tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(()->maid),new tc4revival.network.RunicHitPacket(maid.getId(),target));
        if(hit.healing()>0)maid.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,240,hit.healing()));
        if(hit.kinetic()>0)maid.level().explode(maid,maid.getX(),maid.getY()+maid.getBbHeight()/2,maid.getZ(),1.5f+hit.kinetic()*.5f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
    }
}
