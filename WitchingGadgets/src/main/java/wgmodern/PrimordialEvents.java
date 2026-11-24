package wgmodern;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid="witchinggadgets")
public class PrimordialEvents {
    private static final boolean APOTHEOSIS = net.minecraftforge.fml.ModList.get().isLoaded("apotheosis");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> STEP_BOOTS =
            net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:traveller_step_boots"));

 public static final UUID STEP=UUID.fromString("d6a2b061-a983-4cc6-91f8-4d0e98c992d1");
 private static boolean sweeping;
 @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent e){if(e.getEntity().level().isClientSide)return;var s=e.getCrafting();int n=PrimordialContent.returnedPearls(s);if(n<=0||s.getOrCreateTag().getBoolean("wasCrafted"))return;s.getOrCreateTag().putBoolean("wasCrafted",true);var p=e.getEntity();var entity=new ItemEntity(p.level(),p.getX(),p.getY()+1,p.getZ(),new ItemStack(PrimordialContent.EMPTY_PEARL.get(),n));entity.setDeltaMovement(Vec3.ZERO);p.level().addFreshEntity(entity);}
 @SubscribeEvent public static void attack(AttackEntityEvent e){if(sweeping||e.getEntity().level().isClientSide||!(e.getTarget() instanceof LivingEntity target))return;var p=e.getEntity();var s=p.getMainHandItem();if(!(s.getItem() instanceof PrimordialGear.Weapon weapon))return;if(weapon.kind.equals("hammer"))target.addEffect(new MobEffectInstance(MobEffects.CONFUSION,20));if(weapon.kind.equals("axe"))for(var slot:List.of(EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){var armor=target.getItemBySlot(slot);if(armor.getItem() instanceof ArmorItem){if(armor.getItem() instanceof FortressArmorItem||armor.getItem() instanceof VoidRobeArmorItem||armor.getItem() instanceof PrimordialGear.Armor)armor.hurtAndBreak(4,target,x->x.broadcastBreakEvent(slot));else s.hurtAndBreak(5,target,x->x.broadcastBreakEvent(EquipmentSlot.MAINHAND));}}
  int mode=PrimordialGear.mode(s);if(mode==0){sweeping=true;try{for(var other:p.level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(target.position().subtract(2,2,2),target.position().add(2,2,2))))if(other!=p&&other.isAttackable()){float damage=(float)p.getAttributeValue(Attributes.ATTACK_DAMAGE);boolean critical=p.fallDistance>0&&!p.onGround()&&!p.onClimbable()&&!p.isInWater()&&!p.hasEffect(MobEffects.BLINDNESS)&&!p.isPassenger();if(critical)damage*=1.5;damage+=EnchantmentHelper.getDamageBonus(s,other.getMobType());if(other.hurt(p.damageSources().playerAttack(p),damage)){int knock=EnchantmentHelper.getKnockbackBonus(p)+(p.isSprinting()?1:0);if(knock>0){other.knockback(knock*.5,Math.sin(Math.toRadians(p.getYRot())),-Math.cos(Math.toRadians(p.getYRot())));p.setDeltaMovement(p.getDeltaMovement().multiply(.6,1,.6));p.setSprinting(false);}int fire=EnchantmentHelper.getFireAspect(p);if(fire>0)other.setSecondsOnFire(fire*4);EnchantmentHelper.doPostHurtEffects(other,p);EnchantmentHelper.doPostDamageEffects(p,other);p.causeFoodExhaustion(.3f);}}}finally{sweeping=false;}}
  if(mode==2){target.addEffect(new MobEffectInstance(WGMagic.CINDER.get(),80,1));target.setSecondsOnFire(4);}else if(mode==3)target.addEffect(new MobEffectInstance(WGMagic.DISSOLVE.get(),80,2));else if(mode==5){target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60));target.addEffect(new MobEffectInstance(MobEffects.HUNGER,120));}
 }
 @SubscribeEvent(priority=EventPriority.HIGH) public static void hurt(LivingHurtEvent e){if(e.getEntity().level().isClientSide)return;var direct=e.getSource().getDirectEntity();if(direct instanceof Player attacker){var s=attacker.getMainHandItem();if(s.getItem() instanceof PrimordialGear.Weapon w){if(w.kind.equals("hammer")&&(e.getEntity() instanceof Slime||e.getEntity().getClass().getSimpleName().endsWith("BlueSlime")||e.getEntity().getMobType()==MobType.ARTHROPOD))e.setAmount(e.getAmount()*2);if(w.kind.equals("axe")&&!e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)){float mod=1;for(var armor:e.getEntity().getArmorSlots())if(!armor.isEmpty())mod+=.5f;e.setAmount(e.getAmount()*mod);}}var helmet=attacker.getItemBySlot(EquipmentSlot.HEAD);if(helmet.getItem() instanceof PrimordialGear.Armor&&PrimordialGear.Armor.mask(helmet)==2&&attacker.getRandom().nextFloat()<e.getAmount()/12)attacker.heal(1);}
  if(e.getEntity() instanceof Player p){if(p.isUsingItem()&&p.getUseItem().getItem() instanceof PrimordialGear.Weapon)e.setAmount(LegacySwordRules.blockedDamage(e.getAmount(),e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)));if(p.isUsingItem()&&p.getUseItem().getItem() instanceof PrimordialGear.Weapon&&PrimordialGear.mode(p.getUseItem())==1){var old=p.getEffect(MobEffects.DAMAGE_RESISTANCE);p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,Math.min(80,(old==null?0:old.getDuration())+30),Math.min(2,(old==null?-1:old.getAmplifier())+1)));}for(var s:p.getArmorSlots())if(s.getItem() instanceof PrimordialGear.Armor){int mode=PrimordialGear.mode(s);if(mode==0&&e.getSource().is(DamageTypeTags.IS_PROJECTILE)){e.setCanceled(true);return;}if(mode==5&&direct instanceof LivingEntity attacker&&p.getRandom().nextInt(4)==0){attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,10));attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,10,3));}}var helmet=p.getItemBySlot(EquipmentSlot.HEAD);if(helmet.getItem() instanceof PrimordialGear.Armor&&PrimordialGear.Armor.mask(helmet)==1&&direct instanceof LivingEntity attacker&&p.getRandom().nextFloat()<e.getAmount()/10)attacker.addEffect(new MobEffectInstance(MobEffects.WITHER,80));}
 }
 @SubscribeEvent public static void xp(LivingExperienceDropEvent e){var p=e.getAttackingPlayer();if(p!=null&&p.getMainHandItem().getItem() instanceof PrimordialGear.Weapon&&PrimordialGear.mode(p.getMainHandItem())==4)e.setDroppedExperience(e.getDroppedExperience()+4*e.getOriginalExperience());}
 @SubscribeEvent public static void heads(LivingDropsEvent e){if(!(e.getSource().getDirectEntity() instanceof Player p)||!(p.getMainHandItem().getItem() instanceof PrimordialGear.Weapon w)||!w.kind.equals("sword")||p.getRandom().nextInt(6)>=EnchantmentHelper.getMobLooting(p))return;var victim=e.getEntity();Item item=victim instanceof WitherSkeleton?Items.WITHER_SKELETON_SKULL:victim instanceof Skeleton?Items.SKELETON_SKULL:victim instanceof Zombie?Items.ZOMBIE_HEAD:victim instanceof Creeper?Items.CREEPER_HEAD:victim instanceof Player?Items.PLAYER_HEAD:null;if(item==null)return;var head=new ItemStack(item);if(victim instanceof Player)head.getOrCreateTag().putString("SkullOwner",p.getGameProfile().getName());for(var drop:e.getDrops())if(drop.getItem().is(item))return;e.getDrops().add(new ItemEntity(victim.level(),victim.getX(),victim.getY(),victim.getZ(),head));}
 public static void armorTick(Player p,ItemStack s){int mode=PrimordialGear.mode(s);if(s.getItem() instanceof PrimordialGear.Armor a&&a.getType()==ArmorItem.Type.BOOTS){if(!p.getAbilities().flying&&p.zza>0&&p.onGround())p.moveRelative(p.isInWater()?.055f/4:.055f,new Vec3(0,0,1));if(p.fallDistance>0)p.fallDistance-=.25f;}if(p.level().isClientSide)return;if(mode==0)for(var shot:p.level().getEntitiesOfClass(Projectile.class,new net.minecraft.world.phys.AABB(p.position().subtract(4.5,4.5,4.5),p.position().add(4.5,4.5,4.5)))){if(shot.getOwner()==p||shot.getClass().getSimpleName().equalsIgnoreCase("IManaBurst"))continue;var away=shot.position().subtract(p.position());var v=shot.getDeltaMovement();double denom=away.length()*v.length();if(denom<1e-9||away.dot(v)/denom>Math.cos(3*Math.PI/4))continue;var target=shot.getOwner()==null?away:shot.getOwner().getEyePosition().subtract(shot.position());if(target.lengthSqr()>1e-9){shot.setDeltaMovement(target.normalize().scale(v.length()));shot.hurtMarked=true;}}if(mode==3)for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.POISON,MobEffects.WITHER,MobEffects.CONFUSION,TCEffects.FLUX_TAINT.get()))p.removeEffect(effect);}
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var p=e.player;var attribute=p.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());if(attribute==null)return;boolean active=p.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof PrimordialGear.Armor&&!(APOTHEOSIS && p.getItemBySlot(EquipmentSlot.FEET).is(STEP_BOOTS))&&!p.isShiftKeyDown()&&!p.getAbilities().flying&&p.zza>0;if(active&&attribute.getModifier(STEP)==null)attribute.addTransientModifier(new AttributeModifier(STEP,"Primordial boots step",.4,AttributeModifier.Operation.ADDITION));else if(!active)attribute.removeModifier(STEP);}
 
 public static boolean armored(LivingEntity e){if(!(e instanceof Player))return false;for(var s:e.getArmorSlots())if(s.getItem() instanceof PrimordialGear.Armor)return true;return false;}
 public static void setupProtection(){
  thaumcraft.api.equipment.ArmorProtectionProviders.registerFortressSetBonus("witchinggadgets:primordial",0,(slot,stack)->
   slot!=EquipmentSlot.FEET&&stack.getItem() instanceof PrimordialGear.Armor?.125+(stack.hasTag()&&stack.getTag().contains("mask")?.05:0):0);
  thaumcraft.api.equipment.ArmorProtectionProviders.register("witchinggadgets:primordial",100,stack->stack.getItem() instanceof PrimordialGear.Armor,context->{
   var stack=context.stack();var armor=(PrimordialGear.Armor)stack.getItem();double set=.875;
   for(var piece:context.wearer().getArmorSlots())if(piece.getItem() instanceof PrimordialGear.Armor)set+=.125+(piece.hasTag()&&piece.getTag().contains("mask")?.05:0);
   int mode=PrimordialGear.mode(stack),priority=0;double ratio=armor.getDefense()/25d;boolean extinguish=false;
   if(context.magic()){priority=1;ratio=armor.getDefense()/35d;}
   else if(context.elemental()){priority=1;ratio=mode==2?.75:armor.getDefense()/20d;extinguish=context.source().is(DamageTypeTags.IS_FIRE)&&mode==2;}
   else if(context.bypass()){priority=mode==1?1:0;ratio=mode==1?armor.getDefense()/50d:0;}
   return new thaumcraft.api.equipment.ArmorProtectionProviders.Protection(priority,ratio*set,Math.max(0,stack.getMaxDamage()+1-stack.getDamageValue()),context.source().is(net.minecraft.world.damagesource.DamageTypes.FALL),extinguish);
  });
 }
 public static float absorb(LivingEntity wearer,DamageSource source,float damage){return FortressProtection.absorb(wearer,source,damage);}
}
