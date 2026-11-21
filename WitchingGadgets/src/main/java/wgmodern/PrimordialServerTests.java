package wgmodern;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

@GameTestHolder("witchinggadgets") @PrefixGameTestTemplate(false)
public class PrimordialServerTests {
 static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h,String name){var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));p.getInventory().clearContent();p.getInventory().selected=0;p.removeAllEffects();p.stopUsingItem();p.containerMenu=p.inventoryMenu;var pos=h.absolutePos(new BlockPos(2,2,2));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;}
 @GameTest(template="empty") public static void primordialNodeCaptureUsesMaximumPrimalReductionAndChargesStoredGems(GameTestHelper h){var l=h.getLevel();var pos=h.absolutePos(new BlockPos(2,3,2));l.setBlockAndUpdate(pos,TCArcane.AURA_NODE.get().defaultBlockState());var node=(AuraNodeBlockEntity)l.getBlockEntity(pos);var values=new AspectAmounts().add(TCAspects.catalog().require("ignis"),100).add(TCAspects.catalog().require("lux"),25);node.configure(values,AuraNodeType.NORMAL,AuraNodeModifier.PALE,"WG_CAPTURE_TEST",false);var glove=new ItemStack(PrimordialContent.GLOVE.get());PrimordialGlove.contain(glove,node);var stored=PrimordialGlove.stored(glove);h.assertTrue(stored.amount(TCAspects.catalog().require("ignis"))==8&&stored.amount(TCAspects.catalog().require("aer"))==4,"Node compounds merge maxima before quality multiplier and square root, never sum overlapping fire");var p=player(h,"PrimordialNode");var gems=PrimordialGlove.gems(glove);var fire=InfusedGem.create("ignis",1);fire.setDamageValue(20);gems.set(0,fire);PrimordialGlove.gems(glove,gems);p.getInventory().setItem(0,glove);p.tickCount=10;glove.getItem().inventoryTick(glove,l,p,0,true);h.assertTrue(PrimordialGlove.gems(glove).get(0).getDamageValue()==12&&PrimordialGlove.stored(glove).view().equals(stored.view()),"Stored node recharges eight every ten ticks without draining node pool");var copy=ItemStack.of(glove.save(new CompoundTag()));h.assertTrue(PrimordialGlove.stored(copy).view().equals(stored.view()),"Stored transduced node survives item save");pass(h);}
 @GameTest(template="empty") public static void primordialMenuRetainsFiveGemsAndRejectsGloveReplacement(GameTestHelper h){var p=player(h,"PrimordialMenu");var glove=new ItemStack(PrimordialContent.GLOVE.get());p.getInventory().setItem(0,glove);var m=new PrimordialMenu(13,p.getInventory(),false);p.containerMenu=m;for(int i=0;i<5;i++)m.gems.setItem(i,InfusedGem.create(PrimordialGear.MODES.get(i),1));h.assertTrue(PrimordialGlove.gems(glove).stream().filter(s->!s.isEmpty()).count()==5,"All five slots save immediately");h.assertTrue(!m.slots.get(0).mayPlace(new ItemStack(Items.DIAMOND))&&m.slots.get(0).getMaxStackSize()==1,"Only infused gems, one per socket");m.clicked(32,0,net.minecraft.world.inventory.ClickType.THROW,p);h.assertTrue(p.getMainHandItem()==glove,"Bound hotbar slot cannot be thrown");var replacement=new ItemStack(PrimordialContent.GLOVE.get());p.getInventory().setItem(0,replacement);h.assertTrue(!m.stillValid(p),"Replacing glove invalidates open menu");m.gems.setItem(0,ItemStack.EMPTY);m.removed(p);h.assertTrue(PrimordialGlove.gems(glove).get(0).is(Content.INFUSED_GEM.get())&&PrimordialGlove.gems(replacement).stream().allMatch(ItemStack::isEmpty),"Stale menu neither overwrites removed glove nor replacement");p.containerMenu=p.inventoryMenu;pass(h);}
 @GameTest(template="empty") public static void primordialInfusionsReturnPearlsExactlyOnceAndHaveEveryRecipe(GameTestHelper h){var p=player(h,"PrimordialCraft");var output=new ItemStack(PrimordialContent.ARMOR.get("helmet").get());var matrix=new SimpleContainer(0);int before=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3),e->e.getItem().is(PrimordialContent.EMPTY_PEARL.get())).stream().mapToInt(e->e.getItem().getCount()).sum();PrimordialEvents.crafted(new PlayerEvent.ItemCraftedEvent(p,output,matrix));PrimordialEvents.crafted(new PlayerEvent.ItemCraftedEvent(p,output,matrix));int after=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3),e->e.getItem().is(PrimordialContent.EMPTY_PEARL.get())).stream().mapToInt(e->e.getItem().getCount()).sum();h.assertTrue(after-before==3&&output.getTag().getBoolean("wasCrafted"),"Armor returns exactly three pearls and duplicate events return none");for(String id:List.of("primordial_glove","primordial_sword","primordial_hammer","primordial_axe","primordial_helmet","primordial_chestplate","primordial_leggings","primordial_boots","empower_pearl","primordial_goggles","primordial_mask_grinning","primordial_mask_angry","primordial_mask_sipping","void_metal_block","void_metal_unpack"))h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation("witchinggadgets",id)).isPresent(),"Enabled original acquisition/upgrade recipe "+id);for(String key:List.of("PRIMORDIALGEARSET","EMPOWERPEARL","PRIMORDIALGLOVE","PRIMORDIALWEAPONRY","PRIMORDIALARMOR"))h.assertTrue(tc4revival.research.ResearchCatalogs.catalog().find("witchinggadgets:"+key).isPresent(),"Original research "+key);pass(h);}
 @GameTest(template="empty") public static void primordialCombatModesKeepOriginalDamageAndExperience(GameTestHelper h){var p=player(h,"PrimordialCombat");var sword=new ItemStack(PrimordialContent.WEAPONS.get("sword").get());p.setItemInHand(InteractionHand.MAIN_HAND,sword);var cow=new Cow(EntityType.COW,h.getLevel());PrimordialGear.mode(sword,2);PrimordialEvents.attack(new AttackEntityEvent(p,cow));h.assertTrue(cow.hasEffect(WGMagic.CINDER.get())&&cow.getEffect(WGMagic.CINDER.get()).getAmplifier()==1&&cow.getRemainingFireTicks()==80,"Fire mode gives original cinder amplifier and four seconds fire");PrimordialGear.mode(sword,3);PrimordialEvents.attack(new AttackEntityEvent(p,cow));h.assertTrue(cow.getEffect(WGMagic.DISSOLVE.get()).getAmplifier()==2,"Water mode dissolves at original strength");PrimordialGear.mode(sword,5);PrimordialEvents.attack(new AttackEntityEvent(p,cow));h.assertTrue(cow.getEffect(MobEffects.WEAKNESS).getDuration()==60&&cow.getEffect(MobEffects.HUNGER).getDuration()==120,"Entropy durations preserved");PrimordialGear.mode(sword,4);var xp=new LivingExperienceDropEvent(cow,p,10);PrimordialEvents.xp(xp);h.assertTrue(xp.getDroppedExperience()==50,"Order adds four original XP yields");var hammer=new ItemStack(PrimordialContent.WEAPONS.get("hammer").get());p.setItemInHand(InteractionHand.MAIN_HAND,hammer);var hurt=new LivingHurtEvent(new Slime(EntityType.SLIME,h.getLevel()),p.damageSources().playerAttack(p),8);PrimordialEvents.hurt(hurt);h.assertTrue(hurt.getAmount()==16,"Hammer doubles slime damage");pass(h);}
 @GameTest(template="empty") public static void primordialArmorCancelsArrowsDeflectsProjectilesAndWeakensNewWarpEffects(GameTestHelper h){var p=player(h,"PrimordialArmor");for(var s:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){String key=s==EquipmentSlot.HEAD?"helmet":s==EquipmentSlot.CHEST?"chestplate":s==EquipmentSlot.LEGS?"leggings":"boots";p.setItemSlot(s,new ItemStack(PrimordialContent.ARMOR.get(key).get()));}var arrow=new Arrow(h.getLevel(),p.getX()+2,p.getY()+1,p.getZ());arrow.setDeltaMovement(-1,0,0);h.getLevel().addFreshEntity(arrow);PrimordialEvents.armorTick(p,p.getItemBySlot(EquipmentSlot.HEAD));h.assertTrue(arrow.getDeltaMovement().x>0,"Incoming ownerless projectile reverses away without losing speed");var hurt=new LivingHurtEvent(p,p.damageSources().arrow(arrow,null),10);PrimordialEvents.hurt(hurt);h.assertTrue(hurt.isCanceled(),"Air armor cancels projectile damage");for(var s:p.getArmorSlots())PrimordialGear.mode(s,4);p.addEffect(new MobEffectInstance(TCEffects.VIS_EXHAUST.get(),1000,5));h.assertTrue(p.getEffect(TCEffects.VIS_EXHAUST.get()).getDuration()==200&&p.getEffect(TCEffects.VIS_EXHAUST.get()).getAmplifier()==1,"All four order pieces reduce a newly applied warp effect");p.addEffect(new MobEffectInstance(MobEffects.POISON,100));PrimordialGear.mode(p.getItemBySlot(EquipmentSlot.HEAD),3);PrimordialEvents.armorTick(p,p.getItemBySlot(EquipmentSlot.HEAD));h.assertTrue(!p.hasEffect(MobEffects.POISON),"Water armor cures listed ordinary afflictions");h.assertTrue(FortressProtection.applies(p),"Armor-stage hook recognizes primordial armor");arrow.discard();pass(h);}
 @GameTest(template="empty") public static void primordialBlockingRetainsHalfDamageInsteadOfModernShieldImmunity(GameTestHelper h){
  var p=player(h,"PrimordialBlock");var s=new ItemStack(PrimordialContent.WEAPONS.get("hammer").get());PrimordialGear.mode(s,1);p.setItemInHand(InteractionHand.MAIN_HAND,s);p.startUsingItem(InteractionHand.MAIN_HAND);
  var attacker=new Cow(EntityType.COW,h.getLevel());var source=p.damageSources().mobAttack(attacker);
  h.assertTrue(p.isUsingItem()&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"Fixture uses the real block stance and physical damage");
  var damage=new LivingHurtEvent(p,source,9);PrimordialEvents.hurt(damage);
  h.assertTrue(damage.getAmount()==5&&p.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==0,"Original (damage+1)/2 blocking and first earth resistance stack; damage="+damage.getAmount());
  PrimordialEvents.hurt(new LivingHurtEvent(p,source,9));PrimordialEvents.hurt(new LivingHurtEvent(p,source,9));
  h.assertTrue(p.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==2&&p.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration()==80,"Earth block stacks up to resistance three and eighty ticks");
  var bypass=new LivingHurtEvent(p,p.damageSources().generic(),9);PrimordialEvents.hurt(bypass);
  h.assertTrue(bypass.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)&&bypass.getAmount()==9,"Generic damage bypasses armor and must not receive sword blocking reduction");p.stopUsingItem();pass(h);
 }
 /** FakePlayer deliberately ignores all damage, so use an ordinary Player for the complete hurt pipeline. */
 public static class CombatPlayer extends net.minecraft.world.entity.player.Player {
  public CombatPlayer(GameTestHelper h,String name){super(h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),0,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));setNoGravity(true);}
  @Override public boolean isSpectator(){return false;}
  @Override public boolean isCreative(){return false;}
 }
 @GameTest(template="empty") public static void primordialBlockingWorksThroughPlayerHurtWithoutShieldImmunity(GameTestHelper h){
  for(String kind:List.of("sword","hammer","axe"))for(int mode=0;mode<6;mode++)for(float multiplier:new float[]{1,1.5f}){
   var p=new CombatPlayer(h,"PrimordialHurt_"+kind+mode);var attacker=new CombatPlayer(h,"PrimordialAttacker_"+kind+mode);
   var critDamage=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib","crit_damage"));
   var critChance=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib","crit_chance"));
   boolean criticalIntegration=critDamage!=null&&critChance!=null&&attacker.getAttribute(critDamage)!=null&&attacker.getAttribute(critChance)!=null;
   if(criticalIntegration){attacker.getAttribute(critDamage).setBaseValue(multiplier);attacker.getAttribute(critChance).setBaseValue(1);}
   p.setYRot(0);p.setXRot(0);var weapon=new ItemStack(PrimordialContent.WEAPONS.get(kind).get());PrimordialGear.mode(weapon,mode);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);
   weapon.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);for(int tick=0;tick<6;tick++)p.tick();
   attacker.setPos(p.getX(),p.getY(),p.getZ()+2);var source=p.damageSources().playerAttack(attacker);
   h.assertTrue(p.isUsingItem()&&p.getTicksUsingItem()>=5,"Block stance is beyond modern shield warmup");
   h.assertTrue(!p.isDamageSourceBlocked(source),"Primordial "+kind+" cannot use modern shield immunity");
   h.assertTrue(p.hurt(source,9),"Physical hit reaches the ordinary Player hurt path");
   float expected=20-(9*(criticalIntegration?multiplier:1)+1)/2*(mode==1?.8f:1);
   h.assertTrue(Math.abs(p.getHealth()-expected)<.001,"Legacy blocking follows actual pre-block damage and earth resistance: "+kind+" mode="+mode+" multiplier="+multiplier+" health="+p.getHealth()+" expected="+expected);
   if(mode==1)h.assertTrue(p.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==0,"Earth resistance is applied during actual damage handling");
   p.stopUsingItem();p.discard();attacker.discard();
  }
  pass(h);
 }
 @GameTest(template="empty") public static void primordialGemsRechargeInsideOpenMenuAndPointCutUsesFullCharge(GameTestHelper h){
  var p=player(h,"PrimordialGemCharge");p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var glove=new ItemStack(PrimordialContent.GLOVE.get());p.setItemInHand(InteractionHand.MAIN_HAND,glove);
  var fire=InfusedGem.create("ignis",1);fire.setDamageValue(20);var point=InfusedGem.create("aer",0);
  h.assertTrue(fire.isDamageableItem()&&fire.isDamaged()&&fire.getMaxDamage()==32,"Forge recognizes oval charge as damage");
  h.assertTrue(point.isDamageableItem()&&point.getMaxDamage()==32,"Original point gems also have 32 charge while socketed");
  var charge=new AspectAmounts().add(TCAspects.catalog().require("ignis"),8).add(TCAspects.catalog().require("aer"),8);var nodeTag=new CompoundTag();AspectNbt.write(nodeTag,charge);glove.getOrCreateTag().put("storedNode",nodeTag);
  var gems=PrimordialGlove.gems(glove);gems.set(0,fire);gems.set(1,point);PrimordialGlove.gems(glove,gems);
  var menu=new PrimordialMenu(14,p.getInventory(),false);p.containerMenu=menu;p.tickCount=10;glove.getItem().inventoryTick(glove,h.getLevel(),p,0,true);
  h.assertTrue(menu.gems.getItem(0).getDamageValue()==12&&PrimordialGlove.gems(glove).get(0).getDamageValue()==12,"Open menu and persistent glove both recharge without stale inventory overwrite");
  menu.removed(p);p.containerMenu=p.inventoryMenu;PrimordialGlove.select(glove,1);glove.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
  h.assertTrue(p.hasEffect(MobEffects.JUMP)&&PrimordialGlove.gems(glove).get(1).getDamageValue()==32,"Socketed point gem applies its effect and consumes all 32 charge without shattering");
  p.removeEffect(MobEffects.JUMP);glove.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
  h.assertTrue(!p.hasEffect(MobEffects.JUMP),"Empty point gem cannot cast a second time");
  for(int tick=20;tick<=50;tick+=10){p.tickCount=tick;glove.getItem().inventoryTick(glove,h.getLevel(),p,0,true);}
  h.assertTrue(PrimordialGlove.gems(glove).get(1).getDamageValue()==0&&PrimordialGlove.stored(glove).view().equals(charge.view()),"Four transduced-node pulses refill a point gem without draining stored node charge");
  glove.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.hasEffect(MobEffects.JUMP)&&PrimordialGlove.gems(glove).get(1).getDamageValue()==32,"Recharged point gem is usable again");pass(h);
 }
 @GameTest(template="empty") public static void primordialHammerHarvestsFacePlaneWithoutWearAndSneakingRestrictsItsArea(GameTestHelper h){
  var p=player(h,"PrimordialMining");p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  var hammer=new ItemStack(PrimordialContent.WEAPONS.get("hammer").get());p.setItemInHand(InteractionHand.MAIN_HAND,hammer);hammer.setDamageValue(99);
  var origin=h.absolutePos(new BlockPos(3,2,3));
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(origin.offset(x,0,z),Blocks.STONE.defaultBlockState());
  h.getLevel().setBlockAndUpdate(origin.below(),Blocks.STONE.defaultBlockState());
  ((PrimordialGear.Weapon)hammer.getItem()).breakPlane(hammer,origin,p,net.minecraft.core.Direction.Axis.Y);
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.assertTrue(h.getLevel().getBlockState(origin.offset(x,0,z)).isAir(),"Hammer harvests all nine blocks on clicked plane");
  h.assertTrue(h.getLevel().getBlockState(origin.below()).is(Blocks.STONE)&&hammer.getDamageValue()==99,"Depth is untouched and original area harvest consumes no durability");
  h.getLevel().setBlockAndUpdate(origin,Blocks.STONE.defaultBlockState());p.setShiftKeyDown(true);((PrimordialGear.Weapon)hammer.getItem()).breakPlane(hammer,origin,p,net.minecraft.core.Direction.Axis.Y);
  h.assertTrue(h.getLevel().getBlockState(origin).is(Blocks.STONE),"Sneaking hammer leaves direct mining to vanilla");
  var axe=new ItemStack(PrimordialContent.WEAPONS.get("axe").get());p.setItemInHand(InteractionHand.MAIN_HAND,axe);h.getLevel().setBlockAndUpdate(origin,Blocks.OAK_LOG.defaultBlockState());h.getLevel().setBlockAndUpdate(origin.east(),Blocks.OAK_LOG.defaultBlockState());
  ((PrimordialGear.Weapon)axe.getItem()).breakPlane(axe,origin,p,net.minecraft.core.Direction.Axis.Y);
  h.assertTrue(h.getLevel().getBlockState(origin).isAir()&&h.getLevel().getBlockState(origin.east()).isAir(),"Original axe retains area even when sneaking");p.setShiftKeyDown(false);pass(h);
 }
 @GameTest(template="empty") public static void primordialGloveConsumesOnlyTheSelectedPhysicalNodeAndPersistsItsCharge(GameTestHelper h){
  var p=player(h,"PrimordialCapture");var glove=new ItemStack(PrimordialContent.GLOVE.get());p.setItemInHand(InteractionHand.MAIN_HAND,glove);
  var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,TCArcane.AURA_NODE.get().defaultBlockState());
  var node=(AuraNodeBlockEntity)h.getLevel().getBlockEntity(pos);node.configure(new AspectAmounts().add(TCAspects.catalog().require("terra"),81),AuraNodeType.NORMAL,null,"WG_PHYSICAL_CAPTURE",false);
  var context=new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
  var result=glove.getItem().onItemUseFirst(glove,context);
  h.assertTrue(result.consumesAction()&&h.getLevel().getBlockState(pos).isAir(),"Using glove consumes actual aura node");h.assertTrue(PrimordialGlove.stored(glove).amount(TCAspects.catalog().require("terra"))==9,"Captured node is transduced into a persistent nine point earth source");pass(h);
 }
 private static void pass(GameTestHelper h){com.mojang.logging.LogUtils.getLogger().info("WITCHINGGADGETS_TEST_PASS {}",Thread.currentThread().getStackTrace()[2].getMethodName());h.succeed();}
}
