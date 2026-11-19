package dyesmodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumicdyes") @PrefixGameTestTemplate(false)
public final class DyesServerTests {
    @GameTest(template="empty")
    public static void modernDurabilityDoesNotDyeFreshArmor(GameTestHelper test){
        var recipe=new DyesRecipe(net.minecraft.resources.ResourceLocation.parse("thaumicdyes:color_regression"),CraftingBookCategory.MISC);
        int checked=0;
        for(var entry:DyesItems.ALL.values())if(entry.get() instanceof DyedArmorItem item){
            var fresh=new ItemStack(item);test.assertTrue(item.getColor(fresh)==0xFFFFFF,"New "+item+" starts white despite modern Damage tag");
            fresh.setDamageValue(12);test.assertTrue(item.getColor(fresh)==0xFFFFFF,"Durability alone does not change the original white color");
            var restored=ItemStack.of(fresh.save(new CompoundTag()));test.assertTrue(item.getColor(restored)==0xFFFFFF,"White survives a real ItemStack NBT round trip");
            var input=grid();input.setItem(0,restored);input.setItem(1,new ItemStack(Items.RED_DYE));
            var dyed=recipe.assemble(input,test.getLevel().registryAccess());test.assertTrue(item.getColor(dyed)==LegacyDyeMixing.mix(0xFFFFFF,List.of(14))&&dyed.getDamageValue()==12,"First dye starts from white and preserves durability");
            test.assertTrue(item.getColor(restored)==0xFFFFFF,"Dye crafting does not tint its input");
            item.setColor(restored,0x123456);test.assertTrue(item.getColor(ItemStack.of(restored.save(new CompoundTag())))==0x123456,"Explicit color remains authoritative");checked++;
        }
        test.assertTrue(checked==24,"All dyeable original pieces exercised");test.succeed();
    }
    @GameTest(template="empty")
    public static void runicRotationRespectsRealBlockInteractions(GameTestHelper test){
        var player=player(test);
        try{
            for(String key:List.of("itemRunicChestEnhanced","itemRunicChestPrimal")){
                var armor=stack(key);armor.getOrCreateTag().putByte("upgrade",(byte)1);armor.getOrCreateTag().putByte("upgrade2",(byte)4);
                if(item(armor).primal())armor.getOrCreateTag().putByte("upgrade3",(byte)6);
                var original=armor.copy();player.setItemInHand(InteractionHand.MAIN_HAND,armor);
                var pos=test.absolutePos(new BlockPos(2,2,2));var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                test.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);test.getLevel().setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
                test.assertTrue(player.gameMode.useItemOn(player,test.getLevel(),armor,InteractionHand.MAIN_HAND,hit).consumesAction()&&player.containerMenu instanceof ChestMenu,"Real chest opens while holding upgraded runic armor");
                test.assertTrue(ItemStack.matches(original,armor),"Chest click preserves upgrade order");player.closeContainer();
                test.getLevel().setBlock(pos,net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:arcane_pedestal")).defaultBlockState(),3);
                test.assertTrue(player.gameMode.useItemOn(player,test.getLevel(),armor,InteractionHand.MAIN_HAND,hit).consumesAction(),"Real pedestal consumes the click");
                var pedestal=(ArcanePedestalBlockEntity)test.getLevel().getBlockEntity(pos);
                test.assertTrue(player.getMainHandItem().isEmpty()&&ItemStack.matches(original,pedestal.getItem(0)),"Pedestal accepts the armor with unchanged upgrades");
                player.setItemInHand(InteractionHand.MAIN_HAND,pedestal.removeItemNoUpdate(0));armor=player.getMainHandItem();
                test.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
                test.assertTrue(player.gameMode.useItemOn(player,test.getLevel(),armor,InteractionHand.MAIN_HAND,hit).consumesAction()&&DyesArmorItem.upgrade(armor,0)==4,"Ordinary solid block still rotates upgrades through normal item use");
            }
        }finally{player.discard();}
        test.succeed();
    }
    @GameTest(template="empty")
    public static void fullItemCatalogAndEquipmentBridge(GameTestHelper test){
        test.assertTrue(DyesItems.ALL.size()==57,"All 57 original registrations exist");int armors=0,dyed=0,runic=0;
        for(var item:DyesItems.ALL.values()){if(item.get() instanceof DyesArmorItem armor){armors++;if(armor.dyeable())dyed++;if(armor.runic())runic++;test.assertTrue(new ItemStack(armor).getMaxStackSize()==1,"Armor is unstackable");}}
        test.assertTrue(armors==56&&dyed==24&&runic==14,"56 armor, eight three-piece dyeable sets, fourteen runic pieces");
        for(String role:List.of("archer","ranger"))for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS)){var stack=DyesEquipment.forConcilium(role,slot);test.assertTrue(!stack.isEmpty()&&((ArmorItem)stack.getItem()).getEquipmentSlot()==slot,"Brigade receives real correct-slot equipment");}
        test.assertTrue(!DyesEquipment.forConcilium("paladin",EquipmentSlot.HEAD).isEmpty()&&DyesEquipment.forConcilium("paladin",EquipmentSlot.CHEST).isEmpty()&&DyesEquipment.forConcilium("unknown",EquipmentSlot.HEAD).isEmpty(),"Paladin helmet-only and unknown roles cannot substitute fake equipment");test.succeed();
    }
    @GameTest(template="empty")
    public static void dyeCraftPreservesArmorAndRejectsInvalidInputs(GameTestHelper test){
        var recipe=new DyesRecipe(net.minecraft.resources.ResourceLocation.parse("thaumicdyes:test"),CraftingBookCategory.MISC);var grid=grid();var armor=stack("itemFortressHelmetDyed");armor.setDamageValue(12);armor.getOrCreateTag().putInt("mask",2);grid.setItem(0,armor);grid.setItem(1,new ItemStack(Items.RED_DYE));
        test.assertTrue(recipe.matches(grid,test.getLevel()),"A single armor and dye matches");var result=recipe.assemble(grid,test.getLevel().registryAccess());
        test.assertTrue(((DyedArmorItem)result.getItem()).getColor(result)==LegacyDyeMixing.mix(6961280,List.of(14)),"Existing legacy NBT without color uses original fallback");
        test.assertTrue(result.getDamageValue()==12&&result.getTag().getInt("mask")==2&&!armor.getTag().contains("display"),"Craft preserves damage/mask and does not mutate input");
        grid.setItem(2,stack("itemFortressChestDyed"));test.assertTrue(!recipe.matches(grid,test.getLevel()),"Two armor pieces rejected");grid.setItem(2,ItemStack.EMPTY);grid.setItem(1,new ItemStack(Items.DIRT));test.assertTrue(!recipe.matches(grid,test.getLevel()),"Non-dye ingredient rejected");grid.setItem(0,stack("itemCultistArcherHelmet"));grid.setItem(1,new ItemStack(Items.RED_DYE));test.assertTrue(!recipe.matches(grid,test.getLevel()),"Undyeable original armor rejected");test.succeed();
    }
    @GameTest(template="empty")
    public static void cauldronWashingConsumesOneLevelAndKeepsUpgrades(GameTestHelper test){
        var player=player(test);var armor=stack("itemCultistRangerHelmetDyed");var item=(DyedArmorItem)armor.getItem();item.setColor(armor,0x123456);armor.getOrCreateTag().putInt("mask",0);player.setItemInHand(InteractionHand.MAIN_HAND,armor);
        var pos=test.absolutePos(new BlockPos(2,2,2));test.getLevel().setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3),3);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        test.assertTrue(item.onItemUseFirst(armor,context).consumesAction(),"Washing uses actual item interaction");test.assertTrue(test.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==2&&!armor.getTagElement("display").contains("color")&&armor.getTag().getInt("mask")==0,"One water level consumed, only dye removed");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void runicCapacityRotationAndNbtRoundTrip(GameTestHelper test){
        for(String key:List.of("itemRunicHelmet","itemRunicChest","itemRunicLegs","itemRunicBoots")){var item=stack(key);var armor=(DyesArmorItem)item.getItem();int base=armor.intrinsicRunicCharge(item);item.getOrCreateTag().putByte("upgrade",(byte)2);test.assertTrue(armor.intrinsicRunicCharge(item)==base*3/2&&item.getMaxDamage()==0,"Reinforced adds half base and keeps original zero durability");}
        var primal=stack("itemRunicChestPrimal");for(String key:List.of("upgrade","upgrade2","upgrade3"))primal.getOrCreateTag().putByte(key,(byte)2);
        test.assertTrue(((DyesArmorItem)primal.getItem()).intrinsicRunicCharge(primal)==60,"Three reinforced slots grant original chest capacity 60");primal.getOrCreateTag().putByte("upgrade",(byte)1);primal.getOrCreateTag().putByte("upgrade2",(byte)4);primal.getOrCreateTag().putByte("upgrade3",(byte)6);primal.getOrCreateTag().putByte("RS.HARDEN",(byte)2);
        var armor=(DyesArmorItem)primal.getItem();armor.rotateUpgrades(primal);test.assertTrue(DyesArmorItem.upgrade(primal,0)==4&&DyesArmorItem.upgrade(primal,1)==6&&DyesArmorItem.upgrade(primal,2)==1,"Three-slot cyclic rotation matches original");
        var restored=ItemStack.of(primal.save(new CompoundTag()));test.assertTrue(ItemStack.matches(primal,restored)&&RunicGear.charge(restored)==26,"Save/load retains upgrades and standard hardening");test.succeed();
    }
    @GameTest(template="empty")
    public static void passiveUpgradesHaveCorrectSlotsAndDoNotMultiplyDuplicates(GameTestHelper test){
        var armor=stack("itemRunicChestPrimal");for(String tag:List.of("upgrade","upgrade2","upgrade3"))armor.getOrCreateTag().putByte(tag,(byte)8);
        double health=armor.getAttributeModifiers(EquipmentSlot.CHEST).get(Attributes.MAX_HEALTH).stream().mapToDouble(a->a.getAmount()).sum();test.assertTrue(health==10,"Duplicate vitality is an OR condition, not triple bonus");test.assertTrue(armor.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.MAX_HEALTH).isEmpty(),"Held armor grants no passive health");
        armor.getOrCreateTag().putByte("upgrade",(byte)9);test.assertTrue(armor.getAttributeModifiers(EquipmentSlot.CHEST).get(Attributes.MOVEMENT_SPEED).isEmpty(),"Speed upgrade only functions on boots");
        var boots=stack("itemRunicBootsPrimal");boots.getOrCreateTag().putByte("upgrade3",(byte)9);test.assertTrue(boots.getAttributeModifiers(EquipmentSlot.FEET).get(Attributes.MOVEMENT_SPEED).stream().anyMatch(a->Math.abs(a.getAmount()-.03)<1e-9),"Third-slot boot speed is .03");test.succeed();
    }
    @GameTest(template="empty")
    public static void primalEnchantmentGateRemovesIllegalEnchantments(GameTestHelper test){
        var player=player(test);var primal=stack("itemRunicHelmetPrimal");var armor=(DyesArmorItem)primal.getItem();primal.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION,3);
        test.assertTrue(!armor.isBookEnchantable(primal,new ItemStack(Items.ENCHANTED_BOOK)),"Unhardened primal disallows books");armor.inventoryTick(primal,test.getLevel(),player,0,false);test.assertTrue(!primal.isEnchanted(),"Illegal existing enchantments are removed");
        primal.getOrCreateTag().putByte("upgrade3",(byte)5);primal.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION,3);armor.inventoryTick(primal,test.getLevel(),player,0,false);test.assertTrue(primal.isEnchanted()&&armor.isBookEnchantable(primal,new ItemStack(Items.ENCHANTED_BOOK)),"Hardened in any primal slot allows and preserves enchantments");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void discountsWarpGogglesAndRepairsMatchFamilies(GameTestHelper test){
        var player=player(test);var helmet=stack("itemThaumiumRobeHelmetDyed");var chest=stack("itemThaumiumRobeChestDyed");test.assertTrue(item(helmet).visDiscount(helmet,player,null)==5&&item(chest).visDiscount(chest,player,null)==3,"Thaumium robes use 5/3 discount");
        var voidBoots=stack("itemVoidBootsTraveller");test.assertTrue(item(voidBoots).visDiscount(voidBoots,player,null)==5&&item(voidBoots).warp(voidBoots,player)==2,"Void boots discount and warp");
        var ranger=stack("itemCultistRangerHelmetDyed");test.assertTrue(!item(ranger).showNodes(ranger,player),"No goggles before upgrade");ranger.getOrCreateTag().putInt("mask",0);test.assertTrue(item(ranger).showNodes(ranger,player),"Ranger mask zero reveals nodes");
        test.assertTrue(item(voidBoots).isValidRepairItem(voidBoots,new ItemStack(TCItems.material(MaterialType.VOID)))&&!item(voidBoots).isValidRepairItem(voidBoots,new ItemStack(Items.IRON_INGOT)),"Void repairs require the original ingredient");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void berserkerBreakAndCooldown(GameTestHelper test){
        var player=runicPlayer(test,"itemRunicHelmet",1);postHurt(player,3);var strength=player.getEffect(MobEffects.DAMAGE_BOOST);test.assertTrue(strength!=null&&strength.getDuration()==100&&strength.getAmplifier()==1,"Selected berserker fires when shield is empty");player.removeEffect(MobEffects.DAMAGE_BOOST);postHurt(player,3);test.assertTrue(!player.hasEffect(MobEffects.DAMAGE_BOOST),"Berserker cooldown prevents immediate reuse");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void healingBreakAndHealthyShieldBoundary(GameTestHelper test){
        var player=runicPlayer(test,"itemRunicHelmet",4);RunicShieldEvents.state(player).restore(20);postHurt(player,1);test.assertTrue(!player.hasEffect(MobEffects.REGENERATION),"Healthy shield cannot trigger healing");RunicShieldEvents.state(player).hurt(1000,0);postHurt(player,2);var effect=player.getEffect(MobEffects.REGENERATION);test.assertTrue(effect!=null&&effect.getDuration()==200&&effect.getAmplifier()==1,"Healing uses original 200 ticks and count amplifier");player.removeEffect(MobEffects.REGENERATION);postHurt(player,2);test.assertTrue(!player.hasEffect(MobEffects.REGENERATION),"Healing cooldown applies");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void emergencyRestoresEightAndHasSeparateCooldown(GameTestHelper test){
        var player=runicPlayer(test,"itemRunicHelmet",6);postHurt(player,1);test.assertTrue(RunicShieldEvents.state(player).exactCharge()==8,"Emergency restores eight authoritative shield points");RunicShieldEvents.state(player).hurt(100,0);postHurt(player,1);test.assertTrue(RunicShieldEvents.state(player).exactCharge()==0,"Emergency cannot refill during 2400-tick cooldown");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void kineticExplosionDamagesEntitiesWithoutBreakingBlocks(GameTestHelper test){
        var player=runicPlayer(test,"itemRunicHelmet",3);var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.position().add(1,0,0));zombie.setNoAi(true);test.getLevel().addFreshEntity(zombie);var stone=player.blockPosition().below();test.getLevel().setBlock(stone,Blocks.STONE.defaultBlockState(),3);postHurt(player,1);
        test.assertTrue(zombie.getHealth()<zombie.getMaxHealth()&&test.getLevel().getBlockState(stone).is(Blocks.STONE),"Actual kinetic explosion harms nearby mobs without terrain damage");zombie.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void inactiveUpgradeSlotsNeedRotationBeforeBreak(GameTestHelper test){
        var player=runicPlayer(test,"itemRunicHelmetPrimal",2);var helmet=player.getItemBySlot(EquipmentSlot.HEAD);helmet.getOrCreateTag().putByte("upgrade2",(byte)1);helmet.getOrCreateTag().putByte("upgrade3",(byte)4);postHurt(player,1);test.assertTrue(!player.hasEffect(MobEffects.DAMAGE_BOOST)&&!player.hasEffect(MobEffects.REGENERATION),"Second and third active effects stay inactive");item(helmet).rotateUpgrades(helmet);postHurt(player,1);test.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST)&&!player.hasEffect(MobEffects.REGENERATION),"Rotation activates exactly the new first slot");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void maskRayBlocksWallsRangeAndWrongEquipment(GameTestHelper test){
        var player=player(test);player.setItemSlot(EquipmentSlot.HEAD,stack("itemHelmetMaskTX"));var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.position().add(0,0,4));zombie.setNoAi(true);test.getLevel().addFreshEntity(zombie);aim(player,zombie.getEyePosition());
        test.assertTrue(DyesMask.pointed(player)==zombie&&DyesMask.attackGaze(player,zombie),"Mask ray selects visible target and attacks");test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1&&zombie.getHealth()<zombie.getMaxHealth(),"Attack consumes mask durability and deals damage");var wall=BlockPos.containing(player.getEyePosition().add(0,0,2));test.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);test.assertTrue(DyesMask.pointed(player)==null&&!DyesMask.attackGaze(player,zombie),"Opaque wall prevents remote attack");test.getLevel().setBlock(wall,Blocks.AIR.defaultBlockState(),3);player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));test.assertTrue(!DyesMask.attackGaze(player,zombie),"Wrong helmet cannot nominate target");player.setItemSlot(EquipmentSlot.HEAD,stack("itemHelmetMaskTX"));zombie.moveTo(player.position().add(0,0,25));test.assertTrue(DyesMask.pointed(player)==null,"Mask range is 24 blocks");zombie.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void maskConeAppliesOneOriginalDebuffAndProvokesMob(GameTestHelper test){
        var player=player(test);player.setItemSlot(EquipmentSlot.HEAD,stack("itemHelmetMaskTX"));var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.position().add(0,0,5));zombie.setNoAi(true);test.getLevel().addFreshEntity(zombie);aim(player,zombie.getBoundingBox().getCenter());DyesMask.deathGaze(player);
        test.assertTrue(zombie.getTarget()==player&&zombie.getActiveEffects().stream().anyMatch(e->e.getEffect()==MobEffects.WEAKNESS||e.getEffect()==MobEffects.WITHER||e.getEffect()==MobEffects.MOVEMENT_SLOWDOWN),"Cone applies one of three original effects and retaliation");test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1,"Cone pays durability per affected mob");zombie.removeAllEffects();aim(player,player.getEyePosition().add(0,0,-5));DyesMask.deathGaze(player);test.assertTrue(zombie.getActiveEffects().isEmpty(),"Targets behind player are outside cone");zombie.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void voidRepairsAndTravellerMotionUseOriginalRates(GameTestHelper test){
        var player=player(test);var boots=stack("itemVoidBootsTraveller");boots.setDamageValue(20);player.tickCount=20;item(boots).inventoryTick(boots,test.getLevel(),player,0,false);test.assertTrue(boots.getDamageValue()==19,"Void inventory auto repair once each second");player.zza=1;player.setOnGround(true);player.fallDistance=3;player.setDeltaMovement(Vec3.ZERO);item(boots).onArmorTick(boots,test.getLevel(),player);test.assertTrue(boots.getDamageValue()==18&&Math.abs(player.fallDistance-2.5)<.00001&&Math.abs(player.getDeltaMovement().length()-.1)<.00001,"Worn void boots repair and grant .1 ground acceleration/.5 fall reduction");player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void cancelledMaskAttacksDoNotAlterVictimOrEquipment(GameTestHelper test){
        var player=player(test);player.setItemSlot(EquipmentSlot.HEAD,stack("itemHelmetMaskTX"));var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.position().add(0,0,4));zombie.setNoAi(true);test.getLevel().addFreshEntity(zombie);aim(player,zombie.getEyePosition());
        java.util.function.Consumer<net.minecraftforge.event.entity.player.AttackEntityEvent> cancel=event->{if(event.getEntity()==player)event.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.entity.player.AttackEntityEvent.class,cancel);
        try{test.assertTrue(!DyesMask.attackGaze(player,zombie),"Cancelled player attack is rejected");DyesMask.deathGaze(player);test.assertTrue(zombie.getHealth()==zombie.getMaxHealth()&&zombie.getActiveEffects().isEmpty()&&zombie.getTarget()==null&&player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==0,"Cancelled gaze changes neither health, effects, target nor mask durability");}
        finally{MinecraftForge.EVENT_BUS.unregister(cancel);zombie.discard();player.discard();}test.succeed();
    }
    @GameTest(template="empty")
    public static void maskBeamCadenceWaitsFiveConsecutiveTicks(GameTestHelper test){
        var player=player(test);player.setItemSlot(EquipmentSlot.HEAD,stack("itemHelmetMaskTX"));var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.position().add(0,0,4));zombie.setNoAi(true);test.getLevel().addFreshEntity(zombie);aim(player,zombie.getEyePosition());
        for(int i=1;i<=4;i++){player.tickCount=i;DyesMask.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));}test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==0,"No beam cost before fifth continuous gaze tick");player.tickCount=5;DyesMask.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1,"Fifth tick emits exactly one damaging beam");
        aim(player,player.getEyePosition().add(0,0,-5));player.tickCount=6;DyesMask.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));aim(player,zombie.getEyePosition());player.tickCount=7;DyesMask.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));test.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1,"Lost target resets continuous gaze counter");zombie.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void legacyProtectionRunsOnceAndRunicPiecesDoNotWear(GameTestHelper test){
        var player=player(test);
        try{
            var helmet=stack("itemRunicHelmetEnhanced");helmet.getOrCreateTag().putByte("upgrade",(byte)5);player.setItemSlot(EquipmentSlot.HEAD,helmet);test.assertTrue(FortressProtection.applies(player),"Dyes mixin joins base legacy armor stage");float after=FortressProtection.absorb(player,player.damageSources().magic(),10);
            test.assertTrue(Math.abs(after-9.96)<.0001&&helmet.getDamageValue()==0,"Enhanced hardened preserves original defense+.04 ratio and zero-durability cap, no wear");
            var robe=stack("itemVoidRobeHelmetDyed");player.setItemSlot(EquipmentSlot.HEAD,robe);
            var physical=player.damageSources().playerAttack(player);test.assertTrue(!physical.is(DamageTypeTags.BYPASSES_ARMOR)&&!physical.is(FortressProtection.MAGIC),"Player attack is a real non-bypassing physical damage source");
            float normal=FortressProtection.absorb(player,physical,10);test.assertTrue(Math.abs(normal-8.8)<.0001&&robe.getDamageValue()==1,"Void robes absorb three armor points /25 and take one legacy durability point");
            var bypass=player.damageSources().generic();test.assertTrue(bypass.is(DamageTypeTags.BYPASSES_ARMOR)&&!bypass.is(FortressProtection.MAGIC),"Generic damage bypasses armor without magical armor protection");
            int damageBefore=robe.getDamageValue();float unblocked=FortressProtection.absorb(player,bypass,10);test.assertTrue(Math.abs(unblocked-10)<.0001&&robe.getDamageValue()==damageBefore,"Generic damage remains ten and does not wear void robes");
        }finally{player.discard();}
        test.succeed();
    }
    @GameTest(template="empty")
    public static void fakePlayersCannotTriggerActiveRunicEffects(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"DyesFake"));var helmet=stack("itemRunicHelmet");helmet.getOrCreateTag().putByte("upgrade",(byte)1);player.setItemSlot(EquipmentSlot.HEAD,helmet);DyesRunicEvents.hurt(new LivingHurtEvent(player,player.damageSources().generic(),1));test.assertTrue(!player.hasEffect(MobEffects.DAMAGE_BOOST),"Original fake-player exclusion prevents automated berserker");test.succeed();
    }
    private static ItemStack stack(String key){return new ItemStack(DyesItems.get(key));}
    private static DyesArmorItem item(ItemStack stack){return (DyesArmorItem)stack.getItem();}
    private static CraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}public boolean stillValid(Player p){return true;}},3,3);}
    private static ServerPlayer player(GameTestHelper test){var player=new ServerPlayer(test.getLevel().getServer(),test.getLevel(),new GameProfile(UUID.randomUUID(),"DyesAudit"));new ServerGamePacketListenerImpl(player.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},player);player.setGameMode(GameType.SURVIVAL);var pos=test.absolutePos(new BlockPos(4,3,4));player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);test.getLevel().addNewPlayer(player);return player;}
    private static ServerPlayer runicPlayer(GameTestHelper test,String key,int upgrade){var player=player(test);var armor=stack(key);armor.getOrCreateTag().putByte("upgrade",(byte)upgrade);player.setItemSlot(((ArmorItem)armor.getItem()).getEquipmentSlot(),armor);player.getAttribute(TCAttributes.RUNIC_SHIELD.get()).setBaseValue(50);RunicShieldEvents.state(player).equipment(new RunicShieldState.Equipment(50,0,0,0,0));return player;}
    private static void postHurt(ServerPlayer player,float amount){MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(player,player.damageSources().generic(),amount));}
    private static void aim(Player player,Vec3 target){var d=target.subtract(player.getEyePosition());player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));player.yRotO=player.getYRot();player.xRotO=player.getXRot();}
}
