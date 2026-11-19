package wgmodern;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;

public class WGMagic {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,"witchinggadgets");
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"witchinggadgets");
    public static final Map<String,RegistryObject<Enchantment>> ENCHANTS=new LinkedHashMap<>();
    public static final Map<String,String> LEGACY=Map.of("gemstone_potency","gemstonePotency","gemstone_brittle","gemstoneBrittle","invisible_gear","invisibleGear","unveiling","unveiling","stealth","stealth","backstab","backstab","ride_protect","rideProtect");
    static{LEGACY.forEach((name,legacy)->ENCHANTS.put(name,ENCHANTMENTS.register(name,()->new WGEnchantment(name))));}
    public static final RegistryObject<MobEffect> KNOCKBACK=EFFECTS.register("knockback_resistance",()->new Effect("knockbackRes",MobEffectCategory.BENEFICIAL,0x6e6e6e,1).addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,"00000019-793b-8343-0000-000000000001",.34,AttributeModifier.Operation.ADDITION));
    public static final RegistryObject<MobEffect> DISSOLVE=EFFECTS.register("dissolve",()->new Effect("dissolve",MobEffectCategory.HARMFUL,0x450b45,2));
    public static final RegistryObject<MobEffect> CINDER=EFFECTS.register("cinder_coat",()->new Effect("cinderCoat",MobEffectCategory.HARMFUL,0x8f3f1f,3));
    public static class Effect extends MobEffect {
        public final String kind;public final int icon;public Effect(String kind,MobEffectCategory category,int color,int icon){super(category,color);this.kind=kind;this.icon=icon;}
        @Override public String getDescriptionId(){return "wg.potion"+Character.toUpperCase(kind.charAt(0))+kind.substring(1);}
        @Override public boolean isDurationEffectTick(int time,int amplifier){if(!kind.equals("dissolve"))return false;int rate=40>>Math.min(30,Math.max(0,amplifier));return rate<=0||time%rate==0;}
        @Override public void applyEffectTick(LivingEntity e,int amplifier){if(!e.level().isClientSide&&kind.equals("dissolve")){var key=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("thaumcraft","dissolve"));e.hurt(new DamageSource(e.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key)),1);}}
        // MobEffect initializes client extensions before this constructor assigns icon.
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer){consumer.accept(new WGMagicClient.EffectIcon(()->icon));}
    }
    public static class WGEnchantment extends Enchantment {
        public final String kind;WGEnchantment(String kind){super(Rarity.VERY_RARE,EnchantmentCategory.BREAKABLE,EquipmentSlot.values());this.kind=kind;}
        @Override public String getDescriptionId(){return "enchantment.wg."+LEGACY.get(kind);}
        @Override public int getMinCost(int level){return kind.startsWith("gemstone")?10+11*(level-1):kind.equals("stealth")||kind.equals("backstab")?5+11*(level-1):kind.equals("ride_protect")?9:6;}
        @Override public int getMaxCost(int level){return kind.startsWith("gemstone")?51+10*level:getMinCost(level)+20;}
        @Override public int getMaxLevel(){return kind.equals("stealth")?5:kind.startsWith("gemstone")||kind.equals("backstab")?3:kind.equals("invisible_gear")?2:1;}
        @Override public boolean isAllowedOnBooks(){return !kind.startsWith("gemstone");}@Override public boolean isDiscoverable(){return false;}@Override public boolean isTradeable(){return false;}
        @Override public boolean canApplyAtEnchantingTable(ItemStack stack){return false;}
        @Override public boolean canEnchant(ItemStack stack){if(stack.isEmpty())return false;if(kind.startsWith("gemstone"))return stack.getItem() instanceof InfusedGem && GemRules.PRIMALS.contains(InfusedGem.aspect(stack));return switch(kind){case "invisible_gear"->true;case "unveiling"->stack.getItem() instanceof ArmorItem armor&&armor.getEquipmentSlot()==EquipmentSlot.HEAD&&stack.getItem() instanceof tc4revival.content.RevealingGear;case "stealth"->stack.getItem() instanceof ArmorItem armor&&(armor.getEquipmentSlot()==EquipmentSlot.FEET||armor.getEquipmentSlot()==EquipmentSlot.LEGS);case "ride_protect"->stack.getItem() instanceof ArmorItem||stack.getItem() instanceof top.theillusivec4.curios.api.type.capability.ICurioItem;case "backstab"->stack.getItem() instanceof SwordItem||stack.getItem() instanceof AxeItem;default->false;};}
        @Override protected boolean checkCompatibility(Enchantment other){return super.checkCompatibility(other)&&(!kind.equals("backstab")||other!=Enchantments.SMITE&&other!=Enchantments.BANE_OF_ARTHROPODS);}
    }
    public static int level(ItemStack stack,String name){return EnchantmentHelper.getItemEnchantmentLevel(ENCHANTS.get(name).get(),stack);}
    public static float backstabMultiplier(int level,boolean sameDirection,boolean unaware){return level>0&&sameDirection?GemRules.backstab(1,level,unaware):1;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingHurtEvent e){var target=e.getEntity();if(e.getSource().is(DamageTypeTags.IS_FIRE)&&target.hasEffect(CINDER.get()))e.setAmount(e.getAmount()*(2+target.getEffect(CINDER.get()).getAmplifier()));if(e.getSource().getDirectEntity() instanceof Player p){int enchant=level(p.getMainHandItem(),"backstab");var t=target.getLookAngle();var a=p.getLookAngle();e.setAmount(e.getAmount()*backstabMultiplier(enchant,Math.signum(t.x)==Math.signum(a.x)&&Math.signum(t.z)==Math.signum(a.z),target instanceof Mob mob&&mob.getTarget()!=p));}}
    @SubscribeEvent public static void targeting(LivingChangeTargetEvent e){if(!(e.getNewTarget() instanceof Player p)||!p.isShiftKeyDown()||!(e.getEntity() instanceof Mob mob))return;int strength=level(p.getItemBySlot(EquipmentSlot.FEET),"stealth")+level(p.getItemBySlot(EquipmentSlot.LEGS),"stealth");if(strength==0)return;var a=p.getLookAngle();var t=mob.getLookAngle();float chance=.1f*strength;if(Math.signum(t.x)!=Math.signum(a.x)||Math.signum(t.z)!=Math.signum(a.z))chance-=.1f;if(p.getRandom().nextFloat()<chance)e.setNewTarget(null);else for(var nearby:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(5)))if(nearby.getTarget()==p&&!(nearby instanceof net.minecraft.world.entity.boss.wither.WitherBoss)&&!(nearby instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)){e.setNewTarget(null);break;}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.START||e.player.level().isClientSide||level(e.player.getItemBySlot(EquipmentSlot.HEAD),"ride_protect")==0)return;for(var passenger:List.copyOf(e.player.getPassengers()))if(passenger instanceof LivingEntity living){living.hurt(e.player.damageSources().playerAttack(e.player),1);living.push(e.player.getRandom().nextFloat()*.4,.1,e.player.getRandom().nextFloat()*.4);living.stopRiding();}}
}
